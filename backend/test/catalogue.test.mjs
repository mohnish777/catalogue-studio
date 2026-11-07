import assert from "node:assert/strict";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { pathToFileURL } from "node:url";
import { ZodError } from "zod";
import { DemoProductSource } from "../dist/catalogue.js";

test("demo source loads the bundled products without duplicate identities", async () => {
  const source = new DemoProductSource();
  const products = await source.list();

  assert.equal(source.mode, "demo");
  assert.equal(products.length, 13);
  assert.equal(new Set(products.map((product) => product.id)).size, 13);
  assert.equal(new Set(products.map((product) => product.category)).size, 4);
});

test("malformed product data is rejected rather than silently coerced", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "catalogue-fixture-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const file = join(directory, "products.json");
  const [product] = await new DemoProductSource().list();
  await writeFile(file, JSON.stringify([{ ...product, price: "22.30" }]));

  const source = new DemoProductSource(pathToFileURL(file));

  await assert.rejects(() => source.list(), ZodError);
});

test("a missing fixture fails instead of returning an empty catalogue", async () => {
  const source = new DemoProductSource(
    new URL("../fixtures/not-present.json", import.meta.url),
  );

  await assert.rejects(() => source.list(), { code: "ENOENT" });
});
