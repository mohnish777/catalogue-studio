import { readFile } from "node:fs/promises";
import { z } from "zod";

export const productSchema = z.object({
  id: z.number().int().positive(),
  title: z.string().min(1),
  description: z.string(),
  price: z.number().nonnegative(),
  category: z.string().min(1),
  imageUrl: z.string().min(1),
  rating: z.number().min(0).max(5),
  reviewCount: z.number().int().nonnegative(),
});

export type Product = z.infer<typeof productSchema>;

export interface ProductSource {
  readonly mode: "demo" | "live";
  list(): Promise<Product[]>;
}

export class DemoProductSource implements ProductSource {
  readonly mode = "demo";

  constructor(
    private readonly fixtureUrl = new URL(
      "../fixtures/products.json",
      import.meta.url,
    ),
  ) {}

  async list(): Promise<Product[]> {
    const raw: unknown = JSON.parse(await readFile(this.fixtureUrl, "utf8"));
    return z.array(productSchema).parse(raw);
  }
}
