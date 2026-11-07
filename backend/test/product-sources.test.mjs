import assert from "node:assert/strict";
import test from "node:test";
import { ZodError } from "zod";
import { DemoProductSource } from "../dist/catalogue.js";
import { LiveProductSource, createProductSource } from "../dist/product-sources.js";

const upstreamProduct = {
  id: 1,
  title: "Canvas backpack",
  description: "An everyday bag",
  price: 42,
  category: "clothing",
  image: "https://fakestoreapi.com/img/backpack.jpg",
  rating: { rate: 4.5, count: 120 },
};
const jsonResponse = (data = [upstreamProduct]) => Response.json(data);

test("source selection defaults to demo and rejects misspelled or empty modes", () => {
  assert.ok(createProductSource() instanceof DemoProductSource);
  assert.ok(createProductSource("demo") instanceof DemoProductSource);
  assert.ok(createProductSource("live") instanceof LiveProductSource);
  for (const mode of ["LIVE", "", "unknown"]) {
    assert.throws(() => createProductSource(mode), /PRODUCT_SOURCE must be demo or live/);
  }
});

test("live source validates and maps the upstream contract using a bounded request", async () => {
  const source = new LiveProductSource(async (url, options) => {
    assert.equal(url, "https://fakestoreapi.com/products");
    assert.ok(options.signal instanceof AbortSignal);
    assert.equal(options.headers.Accept, "application/json");
    return jsonResponse();
  });

  assert.equal(source.mode, "live");
  assert.deepEqual(await source.list(), [{
    id: 1, title: "Canvas backpack", description: "An everyday bag", price: 42,
    category: "clothing", imageUrl: upstreamProduct.image, rating: 4.5, reviewCount: 120,
  }]);
});

test("successful results stay cached until exactly 60 seconds after loading", async () => {
  let time = 1_000;
  let requests = 0;
  const source = new LiveProductSource(async () => {
    requests++;
    time += 100;
    return jsonResponse();
  }, () => time);

  await source.list();
  time = 61_099;
  await source.list();
  assert.equal(requests, 1);
  time = 61_100;
  await source.list();
  assert.equal(requests, 2);
});

test("simultaneous callers share a single in-progress upstream request", async () => {
  let requests = 0;
  let finish;
  const response = new Promise((resolve) => { finish = resolve; });
  const source = new LiveProductSource(async () => { requests++; return response; });

  const first = source.list();
  const second = source.list();
  assert.equal(requests, 1);
  finish(jsonResponse());
  assert.deepEqual(await first, await second);
  assert.equal(requests, 1);
});

test("HTTP, malformed JSON, invalid fields, network and timeout failures reject and permit retry", async () => {
  const failures = [
    async () => new Response("Forbidden", { status: 403 }),
    async () => new Response("not JSON"),
    async () => jsonResponse([{ ...upstreamProduct, rating: { rate: 7, count: 2 } }]),
    async () => { throw new TypeError("Network unavailable"); },
    async () => { throw new DOMException("Request timed out", "TimeoutError"); },
  ];

  for (const fail of failures) {
    let requests = 0;
    const source = new LiveProductSource(async () => {
      requests++;
      return requests === 1 ? fail() : jsonResponse();
    });
    await assert.rejects(() => source.list());
    assert.equal((await source.list())[0].id, 1);
    assert.equal(requests, 2);
    assert.equal(source.mode, "live");
  }
});

test("malformed image and rating fields cannot enter the cache", async () => {
  for (const override of [
    { image: "not a URL" },
    { price: "42" },
    { rating: { rate: 4.5, count: -1 } },
    { rating: 4.5 },
  ]) {
    const source = new LiveProductSource(async () => jsonResponse([{ ...upstreamProduct, ...override }]));
    await assert.rejects(() => source.list(), ZodError);
  }
});

test("an expired cache is not served when refresh fails", async () => {
  let time = 0;
  let requests = 0;
  const source = new LiveProductSource(async () => {
    requests++;
    if (requests === 2) return new Response("Unavailable", { status: 503 });
    return jsonResponse();
  }, () => time);

  await source.list();
  time = 60_000;
  await assert.rejects(() => source.list(), /HTTP 503/);
  assert.equal((await source.list()).length, 1);
  assert.equal(requests, 3);
});

test("concurrent callers both observe failure and the next call can recover", async () => {
  let requests = 0;
  const source = new LiveProductSource(async () => {
    requests++;
    return requests === 1 ? new Response("Unavailable", { status: 503 }) : jsonResponse();
  });

  const results = await Promise.allSettled([source.list(), source.list()]);
  assert.deepEqual(results.map((r) => r.status), ["rejected", "rejected"]);
  assert.equal(requests, 1);
  await source.list();
  assert.equal(requests, 2);
});
