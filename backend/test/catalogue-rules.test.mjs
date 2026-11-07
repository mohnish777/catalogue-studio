import assert from "node:assert/strict";
import test from "node:test";
import { buildCatalogue } from "../dist/catalogue-rules.js";

function product(id, overrides = {}) {
  return {
    id,
    title: `Product ${id}`,
    description: "An everyday item",
    price: 10,
    category: "clothing",
    imageUrl: `/images/${id}.jpg`,
    rating: 3,
    reviewCount: 10,
    ...overrides,
  };
}

const ids = (products) => products.map((item) => item.id);

test("home uses editorial IDs in editorial order, rather than highest ratings", () => {
  const products = [8, 9, 7, 5, 2, 1].map((id) =>
    product(id, { rating: id === 8 ? 5 : 2 }),
  );

  const catalogue = buildCatalogue(products);

  assert.deepEqual(ids(catalogue.featured), [1, 2, 5, 7, 9]);
  assert.deepEqual(ids(catalogue.remaining), [8]);
});

test("missing editorial picks are skipped without substituting other products", () => {
  const catalogue = buildCatalogue([product(3), product(2), product(4)]);

  assert.deepEqual(ids(catalogue.featured), [2]);
  assert.deepEqual(ids(catalogue.remaining), [3, 4]);
});

test("category features the best two ratings, breaks ties by ID, and excludes other categories", () => {
  const products = [
    product(8, { rating: 4.8 }),
    product(6, { rating: 4.8 }),
    product(4, { rating: 4.9 }),
    product(2, { rating: 5, category: "electronics" }),
  ];

  const catalogue = buildCatalogue(products, { category: "clothing" });

  assert.deepEqual(ids(catalogue.featured), [4, 6]);
  assert.deepEqual(ids(catalogue.remaining), [8]);
  assert.deepEqual(catalogue.categories, ["clothing", "electronics"]);
});

test("small categories keep an item in the remaining list", () => {
  for (const [size, featuredCount, remainingCount] of [
    [0, 0, 0],
    [1, 0, 1],
    [2, 1, 1],
    [3, 2, 1],
    [4, 2, 2],
  ]) {
    const products = Array.from({ length: size }, (_, index) => product(index + 1));
    const catalogue = buildCatalogue(products, { category: "clothing" });

    assert.equal(catalogue.featured.length, featuredCount, `size ${size}`);
    assert.equal(catalogue.remaining.length, remainingCount, `size ${size}`);
  }
});

test("search matches title OR description, ignores case and trims surrounding whitespace", () => {
  const products = [
    product(1, { title: "CANVAS backpack", description: "For travel" }),
    product(2, { title: "Weekend bag", description: "Made from canvas" }),
    product(3, { title: "Cotton top", description: "Soft cotton" }),
  ];

  const catalogue = buildCatalogue(products, { query: "  CaNvAs  " });

  assert.deepEqual(catalogue.featured, []);
  assert.deepEqual(ids(catalogue.remaining), [1, 2]);
});

test("category and search filters apply together while category choices stay available", () => {
  const products = [
    product(1, { title: "Portable bag" }),
    product(2, { title: "Portable drive", category: "electronics" }),
    product(3, { title: "Desktop drive", category: "electronics" }),
  ];

  const catalogue = buildCatalogue(products, {
    category: "electronics",
    query: "portable",
  });

  assert.deepEqual(catalogue.featured, []);
  assert.deepEqual(ids(catalogue.remaining), [2]);
  assert.deepEqual(catalogue.categories, ["clothing", "electronics"]);
});

test("unknown categories and unmatched searches return honest empty results", () => {
  const products = [product(1)];
  for (const filters of [{ category: "unknown" }, { query: "not present" }]) {
    const catalogue = buildCatalogue(products, filters);
    assert.deepEqual(catalogue.featured, []);
    assert.deepEqual(catalogue.remaining, []);
    assert.deepEqual(catalogue.categories, ["clothing"]);
  }
});

test("a blank search behaves like browsing", () => {
  const catalogue = buildCatalogue([product(1), product(3)], { query: "   " });

  assert.deepEqual(ids(catalogue.featured), [1]);
  assert.deepEqual(ids(catalogue.remaining), [3]);
});

test("building a catalogue does not mutate its input products or order", () => {
  const products = Object.freeze([
    Object.freeze(product(3, { rating: 2 })),
    Object.freeze(product(1, { rating: 5 })),
    Object.freeze(product(2, { rating: 4 })),
  ]);

  const catalogue = buildCatalogue(products, { category: "clothing" });

  assert.deepEqual(ids(products), [3, 1, 2]);
  assert.deepEqual(ids(catalogue.featured), [1, 2]);
  assert.deepEqual(ids(catalogue.remaining), [3]);
});
