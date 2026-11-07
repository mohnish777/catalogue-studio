import { z } from "zod";
import { DemoProductSource, productSchema, type Product, type ProductSource } from "./catalogue.js";

const upstreamProductSchema = productSchema
  .omit({ imageUrl: true, rating: true, reviewCount: true })
  .extend({
    image: z.string().url(),
    rating: z.object({
      rate: productSchema.shape.rating,
      count: productSchema.shape.reviewCount,
    }),
  });

export class LiveProductSource implements ProductSource {
  readonly mode = "live";
  private cache?: { products: Product[]; expiresAt: number };
  private pending?: Promise<Product[]>;

  constructor(
    private readonly fetchProducts: typeof fetch = fetch,
    private readonly now: () => number = Date.now,
  ) {}

  async list(): Promise<Product[]> {
    if (this.cache && this.now() < this.cache.expiresAt) {
      return this.cache.products;
    }
    if (!this.pending) {
      this.pending = this.load().finally(() => {
        this.pending = undefined;
      });
    }
    return this.pending;
  }

  private async load(): Promise<Product[]> {
    const response = await this.fetchProducts("https://fakestoreapi.com/products", {
      signal: AbortSignal.timeout(5_000),
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      throw new Error(`Product provider returned HTTP ${response.status}.`);
    }

    const raw: unknown = await response.json();
    const products = z.array(upstreamProductSchema).parse(raw).map((item) => ({
      id: item.id,
      title: item.title,
      description: item.description,
      price: item.price,
      category: item.category,
      imageUrl: item.image,
      rating: item.rating.rate,
      reviewCount: item.rating.count,
    }));

    // Start the TTL after a successful fetch and validation, not when the request began.
    this.cache = { products, expiresAt: this.now() + 60_000 };
    return products;
  }
}

export function createProductSource(mode = "live"): ProductSource {
  switch (mode) {
    case "demo": return new DemoProductSource();
    case "live": return new LiveProductSource();
    default: throw new Error("PRODUCT_SOURCE must be demo or live.");
  }
}
