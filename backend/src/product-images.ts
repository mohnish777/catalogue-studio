import { readFile } from "node:fs/promises";
import type { Product, ProductSource } from "./catalogue.js";

const MAX_IMAGE_BYTES = 2 * 1024 * 1024;
const SUPPORTED_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

export async function loadProductImage(
  product: Product,
  mode: ProductSource["mode"],
  fetchImage: typeof fetch = fetch,
): Promise<{ bytes: Buffer; contentType: string }> {
  if (mode === "demo") {
    const file = new URL(`../public/images/${product.id}.jpg`, import.meta.url);
    return { bytes: await readFile(file), contentType: "image/jpeg" };
  }

  const url = new URL(product.imageUrl);
  if (url.origin !== "https://fakestoreapi.com" ||
      !url.pathname.startsWith("/img/") || url.username || url.password) {
    throw new Error("Product image URL is outside the allowed provider image path.");
  }

  const response = await fetchImage(url, {
    signal: AbortSignal.timeout(5_000),
    redirect: "error",
    headers: { Accept: "image/jpeg, image/png, image/webp" },
  });
  const contentType = (response.headers.get("content-type") ?? "")
    .split(";")[0].trim().toLowerCase();
  if (!response.ok || !SUPPORTED_TYPES.has(contentType) || !response.body) {
    await response.body?.cancel();
    throw new Error(`Invalid image response: HTTP ${response.status}, type ${contentType}.`);
  }

  // Count actual streamed bytes; Content-Length may be absent or incorrect.
  const reader = response.body.getReader();
  const chunks: Uint8Array[] = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > MAX_IMAGE_BYTES) throw new Error("Product image exceeds 2 MiB.");
      chunks.push(value);
    }
    if (size === 0) throw new Error("Product image is empty.");
    return { bytes: Buffer.concat(chunks, size), contentType };
  } finally {
    await reader.cancel().catch(() => {});
    reader.releaseLock();
  }
}
