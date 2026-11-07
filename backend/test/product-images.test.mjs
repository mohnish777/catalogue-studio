import assert from "node:assert/strict";
import test from "node:test";
import { loadProductImage } from "../dist/product-images.js";

const product = { id: 1, imageUrl: "https://fakestoreapi.com/img/bag.jpg" };
const imageResponse = (bytes = new Uint8Array([255, 216, 255, 217])) =>
  new Response(bytes, { headers: { "Content-Type": "image/jpeg" } });

test("live images preserve bytes and MIME type with a bounded non-redirecting request", async () => {
  const bytes = new Uint8Array([255, 216, 255, 217]);
  const image = await loadProductImage(product, "live", async (url, options) => {
    assert.equal(url.href, product.imageUrl);
    assert.equal(options.redirect, "error");
    assert.ok(options.signal instanceof AbortSignal);
    return imageResponse(bytes);
  });

  assert.equal(image.contentType, "image/jpeg");
  assert.deepEqual(image.bytes, Buffer.from(bytes));
});

test("untrusted image locations are rejected without fetching", async () => {
  let requests = 0;
  for (const imageUrl of [
    "http://fakestoreapi.com/img/bag.jpg",
    "https://example.com/img/bag.jpg",
    "https://fakestoreapi.com.example.com/img/bag.jpg",
    "https://fakestoreapi.com/products",
    "https://user:password@fakestoreapi.com/img/bag.jpg",
    "https://fakestoreapi.com/img/../products",
    "not-a-url",
  ]) {
    await assert.rejects(() => loadProductImage({ ...product, imageUrl }, "live", async () => {
      requests++;
      return imageResponse();
    }));
  }
  assert.equal(requests, 0);
});

test("non-success responses, HTML, SVG, missing content type and empty bodies are rejected", async () => {
  for (const response of [
    new Response("Not found", { status: 404 }),
    new Response("<html>Challenge</html>", { headers: { "Content-Type": "text/html" } }),
    new Response("<svg/>", { headers: { "Content-Type": "image/svg+xml" } }),
    new Response(new Uint8Array([1])),
    new Response(new Uint8Array(), { headers: { "Content-Type": "image/png" } }),
  ]) {
    await assert.rejects(() => loadProductImage(product, "live", async () => response));
  }
});

test("the 2 MiB limit counts streamed bytes even when Content-Length claims a smaller body", async () => {
  let cancelled = false;
  let chunks = 0;
  const body = new ReadableStream({
    pull(controller) {
      chunks++;
      controller.enqueue(new Uint8Array(1024 * 1024));
    },
    cancel() { cancelled = true; },
  });
  const response = new Response(body, {
    headers: { "Content-Type": "image/jpeg", "Content-Length": "1" },
  });

  await assert.rejects(
    () => loadProductImage(product, "live", async () => response),
    /exceeds 2 MiB/,
  );
  assert.equal(cancelled, true);
  assert.ok(chunks <= 4, "does not consume an unbounded response");
});

test("a supported image exactly at the size limit is accepted", async () => {
  const bytes = new Uint8Array(2 * 1024 * 1024);
  const image = await loadProductImage(product, "live", async () => imageResponse(bytes));
  assert.equal(image.bytes.length, bytes.length);
});

test("network, timeout and body-stream failures propagate without a placeholder fallback", async () => {
  for (const failure of [
    new TypeError("Network unavailable"),
    new DOMException("Timed out", "TimeoutError"),
  ]) {
    await assert.rejects(() => loadProductImage(product, "live", async () => { throw failure; }),
      (error) => error === failure);
  }
  const body = new ReadableStream({ start(controller) { controller.error(new Error("Body failed")); } });
  await assert.rejects(() => loadProductImage(product, "live", async () =>
    new Response(body, { headers: { "Content-Type": "image/jpeg" } })), /Body failed/);
});
