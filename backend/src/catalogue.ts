import { z } from "zod";
import { readFile } from "node:fs/promises";

export const productSchema = z.object({
  id: z.number().int().positive(),
  title: z.string(),
  description: z.string(),
  price: z.number().nonnegative(),
  category: z.string(),
  imageUrl: z.string(),
  rating: z.number().min(0).max(5),
  reviewCount: z.number().int().nonnegative(),
});
export type Product = z.infer<typeof productSchema>;
export interface ProductSource {
  readonly mode: "demo" | "live";
  list(): Promise<Product[]>;
}

// Explicit editorial picks rather than silently treating rating order as curation.
export const featuredIds = [1, 2, 5, 7, 9];
export function buildCatalogue(
  all: Product[],
  category?: string,
  search?: string,
) {
  const categories = [...new Set(all.map((p) => p.category))].sort();
  let selection = category ? all.filter((p) => p.category === category) : all;
  const term = search?.trim().toLocaleLowerCase();
  if (term)
    selection = selection.filter((p) =>
      `${p.title} ${p.description}`.toLocaleLowerCase().includes(term),
    );
  const featured = term
    ? []
    : category
      ? [...selection]
          .sort((a, b) => b.rating - a.rating || a.id - b.id)
          .slice(0, Math.min(2, Math.max(0, selection.length - 1)))
      : featuredIds.flatMap((id) => selection.filter((p) => p.id === id));
  const ids = new Set(featured.map((p) => p.id));
  return {
    categories,
    featured,
    products: selection.filter((p) => !ids.has(p.id)),
  };
}

export class DemoProductSource implements ProductSource {
  readonly mode = "demo" as const;
  async list(): Promise<Product[]> {
    const json = await readFile("fixtures/products.json", "utf8");
    return z.array(productSchema).parse(JSON.parse(json));
  }
}

const fakeStoreProduct = z.object({
  id: z.number().int().positive(),
  title: z.string(),
  description: z.string(),
  price: z.number().nonnegative(),
  category: z.string(),
  image: z.url(),
  rating: z.object({
    rate: z.number().min(0).max(5),
    count: z.number().int().nonnegative(),
  }),
});
export class FakeStoreProductSource implements ProductSource {
  readonly mode = "live" as const;
  private cached?: Product[];
  private expires = 0;
  private pending?: Promise<Product[]>;
  constructor(private readonly request: typeof fetch = fetch) {}
  async list(): Promise<Product[]> {
    if (this.cached && Date.now() < this.expires) return this.cached;
    if (this.pending) return this.pending;
    this.pending = this.load();
    try {
      return await this.pending;
    } finally {
      this.pending = undefined;
    }
  }
  private async load(): Promise<Product[]> {
    const response = await this.request("https://fakestoreapi.com/products", {
      signal: AbortSignal.timeout(8000),
    });
    if (!response.ok)
      throw new Error(`Product provider returned ${response.status}`);
    const products = z
      .array(fakeStoreProduct)
      .parse(await response.json())
      .map((p) => ({
        id: p.id,
        title: p.title,
        description: p.description,
        price: p.price,
        category: p.category,
        imageUrl: p.image,
        rating: p.rating.rate,
        reviewCount: p.rating.count,
      }));
    this.cached = products;
    this.expires = Date.now() + 60_000;
    return products;
  }
}

// Client-facing URLs always point back to this backend, including images.
export const publicProduct = (product: Product): Product => ({
  ...product,
  imageUrl: `/api/products/${product.id}/image`,
});
