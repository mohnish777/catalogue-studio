import assert from "node:assert/strict";
import { once } from "node:events";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { createApp } from "../dist/app.js";
import { LiveProductSource } from "../dist/product-sources.js";

async function startApp(t, source, fetchImage) {
  const server = createApp(source, fetchImage).listen(0, "127.0.0.1");
  t.after(() => new Promise((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve());
  }));
  await once(server, "listening");
  return `http://127.0.0.1:${server.address().port}`;
}

test("catalogue endpoint loads demo products and returns the editorial selection", async (t) => {
  const base = await startApp(t);
  const response = await fetch(`${base}/api/catalogue`);
  const body = await response.json();

  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type"), /application\/json/);
  assert.equal(response.headers.get("x-powered-by"), null);
  assert.equal(body.mode, "demo");
  assert.equal(body.categories.length, 4);
  assert.deepEqual(body.featured.map((p) => p.id), [1, 2, 5, 7, 9]);
  assert.equal(body.remaining.length, 8);
});

test("HTTP query parameters reach category ranking and combined search rules", async (t) => {
  const base = await startApp(t);
  const categoryResponse = await fetch(`${base}/api/catalogue?category=electronics`);
  const category = await categoryResponse.json();
  assert.equal(categoryResponse.status, 200);
  assert.deepEqual(category.featured.map((p) => p.id), [10]);
  assert.deepEqual(category.remaining.map((p) => p.id), [9]);

  const searchResponse = await fetch(`${base}/api/catalogue?category=electronics&q=%20PORTABLE%20`);
  const search = await searchResponse.json();
  assert.equal(searchResponse.status, 200);
  assert.deepEqual(search.featured, []);
  assert.deepEqual(search.remaining.map((p) => p.id), [9]);
});

test("unknown categories and unmatched searches return 200 with empty lists", async (t) => {
  const base = await startApp(t);
  for (const query of ["category=unknown", "q=not-a-product"]) {
    const response = await fetch(`${base}/api/catalogue?${query}`);
    const body = await response.json();
    assert.equal(response.status, 200);
    assert.deepEqual(body.featured, []);
    assert.deepEqual(body.remaining, []);
    assert.equal(body.categories.length, 4);
  }
});

test("repeated or oversized query values are rejected before reading the source", async (t) => {
  let loads = 0;
  const base = await startApp(t, { mode: "demo", list: async () => { loads++; return []; } });
  for (const query of [
    "q=one&q=two",
    "category=one&category=two",
    `q=${"x".repeat(201)}`,
    `category=${"x".repeat(101)}`,
  ]) {
    const response = await fetch(`${base}/api/catalogue?${query}`);
    assert.equal(response.status, 400);
    assert.equal((await response.json()).error.code, "INVALID_QUERY");
  }
  assert.equal(loads, 0);
});

test("detail endpoint returns the complete product", async (t) => {
  const base = await startApp(t);
  const response = await fetch(`${base}/api/products/9`);
  const product = await response.json();

  assert.equal(response.status, 200);
  assert.equal(product.id, 9);
  assert.equal(product.title, "Portable everyday drive");
  assert.equal(product.imageUrl, "/images/9.jpg");
  assert.equal(typeof product.description, "string");
  assert.equal(typeof product.price, "number");
});

test("well-formed but missing product IDs return 404", async (t) => {
  const base = await startApp(t);
  const response = await fetch(`${base}/api/products/999`);
  assert.equal(response.status, 404);
  assert.equal((await response.json()).error.code, "PRODUCT_NOT_FOUND");
});

test("malformed product IDs are rejected before reading the source", async (t) => {
  let loads = 0;
  const base = await startApp(t, { mode: "demo", list: async () => { loads++; return []; } });
  for (const id of ["abc", "0", "-1", "1.5", "1e2", "9007199254740992"]) {
    const response = await fetch(`${base}/api/products/${id}`);
    assert.equal(response.status, 400);
    assert.equal((await response.json()).error.code, "INVALID_PRODUCT_ID");
  }
  assert.equal(loads, 0);
});

test("source failures return safe JSON errors while health remains independent", async (t) => {
  const errors = [];
  t.mock.method(console, "error", (...args) => errors.push(args));
  const failure = new Error("Cannot read /private/internal/products.json");
  const base = await startApp(t, { mode: "demo", list: async () => { throw failure; } });

  for (const path of ["/api/catalogue", "/api/products/1"]) {
    const response = await fetch(`${base}${path}`);
    assert.equal(response.status, 500);
    assert.deepEqual(await response.json(), {
      error: { code: "CATALOGUE_UNAVAILABLE", message: "Unable to load products. Please try again." },
    });
  }
  assert.equal(errors.length, 2);
  assert.equal(errors[0][1], failure);

  const health = await fetch(`${base}/health`);
  assert.equal(health.status, 200);
  assert.deepEqual(await health.json(), { status: "ok" });
});

test("unknown routes use the same JSON error shape", async (t) => {
  const base = await startApp(t);
  const response = await fetch(`${base}/not-a-route`);
  assert.equal(response.status, 404);
  assert.equal((await response.json()).error.code, "ROUTE_NOT_FOUND");
});

test("catalogue and detail requests share one configured live source and cache", async (t) => {
  let requests = 0;
  const source = new LiveProductSource(async () => {
    requests++;
    return Response.json([{
      id: 1, title: "Live backpack", description: "A bag", price: 42,
      category: "clothing", image: "https://fakestoreapi.com/img/bag.jpg",
      rating: { rate: 4.5, count: 120 },
    }]);
  });
  const base = await startApp(t, source);

  const catalogueResponse = await fetch(`${base}/api/catalogue`);
  const catalogue = await catalogueResponse.json();
  assert.equal(catalogueResponse.status, 200);
  assert.equal(catalogue.mode, "live");
  assert.equal(catalogue.featured[0].title, "Live backpack");
  assert.equal(catalogue.featured[0].imageUrl, "/images/1.jpg");
  const detail = await fetch(`${base}/api/products/1`);
  assert.equal(detail.status, 200);
  const product = await detail.json();
  assert.equal(product.rating, 4.5);
  assert.equal(product.imageUrl, "/images/1.jpg");
  assert.equal((await source.list())[0].imageUrl, "https://fakestoreapi.com/img/bag.jpg");
  assert.equal(requests, 1);
});

test("every demo product image URL serves its bundled JPEG through the backend", async (t) => {
  const base = await startApp(t);
  const catalogue = await (await fetch(`${base}/api/catalogue`)).json();
  for (const product of [...catalogue.featured, ...catalogue.remaining]) {
    const response = await fetch(new URL(product.imageUrl, base));
    const expected = await readFile(new URL(`../public/images/${product.id}.jpg`, import.meta.url));
    assert.equal(response.status, 200);
    assert.match(response.headers.get("content-type"), /image\/jpeg/);
    assert.equal(response.headers.get("x-content-type-options"), "nosniff");
    assert.equal(response.headers.get("cache-control"), "public, max-age=60");
    assert.deepEqual(Buffer.from(await response.arrayBuffer()), expected);
    assert.equal(expected.subarray(0, 3).toString("hex"), "ffd8ff");
  }
});

test("live image route uses the source URL and returns the upstream bytes", async (t) => {
  const product = { id: 1, imageUrl: "https://fakestoreapi.com/img/bag.jpg" };
  const bytes = new Uint8Array([255, 216, 255, 217]);
  const base = await startApp(t, { mode: "live", list: async () => [product] }, async (url) => {
    assert.equal(url.href, product.imageUrl);
    return new Response(bytes, { headers: { "Content-Type": "image/jpeg" } });
  });

  const response = await fetch(`${base}/images/1.jpg`);
  assert.equal(response.status, 200);
  assert.deepEqual(Buffer.from(await response.arrayBuffer()), Buffer.from(bytes));
});

test("invalid image IDs and unknown products do not trigger upstream image requests", async (t) => {
  let requests = 0;
  const base = await startApp(t, { mode: "live", list: async () => [] }, async () => { requests++; });
  for (const [path, status, code] of [
    ["/images/abc.jpg", 400, "INVALID_PRODUCT_ID"],
    ["/images/999.jpg", 404, "PRODUCT_NOT_FOUND"],
  ]) {
    const response = await fetch(`${base}${path}`);
    assert.equal(response.status, status);
    assert.equal((await response.json()).error.code, code);
  }
  assert.equal(requests, 0);
});

test("missing demo files and rejected live images return safe JSON errors", async (t) => {
  t.mock.method(console, "error", () => {});
  for (const [mode, id, status] of [["demo", 99, 500], ["live", 1, 502]]) {
    const base = await startApp(t, {
      mode, list: async () => [{ id, imageUrl: "https://example.com/private-image.jpg" }],
    });
    const response = await fetch(`${base}/images/${id}.jpg`);
    assert.equal(response.status, status);
    assert.deepEqual(await response.json(), {
      error: { code: "IMAGE_UNAVAILABLE", message: "Unable to load product image. Please try again." },
    });
  }
});
