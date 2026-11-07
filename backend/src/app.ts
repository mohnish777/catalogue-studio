import express, { type ErrorRequestHandler } from "express";
import { z } from "zod";
import { DemoProductSource, type Product, type ProductSource } from "./catalogue.js";
import { buildCatalogue } from "./catalogue-rules.js";
import { loadProductImage } from "./product-images.js";

const catalogueQuerySchema = z.object({
  category: z.string().trim().max(100).optional(),
  q: z.string().trim().max(200).optional(),
});

const productIdSchema = z.string()
  .regex(/^[1-9]\d*$/)
  .transform(Number)
  .pipe(z.number().int().positive().max(Number.MAX_SAFE_INTEGER));

// Change the response copy, preserving the upstream URL held in the live source/cache.
function publicProduct(product: Product): Product {
  return { ...product, imageUrl: `/images/${product.id}.jpg` };
}

export function createApp(
  source: ProductSource = new DemoProductSource(),
  fetchImage: typeof fetch = fetch,
) {
  const app = express();
  app.disable("x-powered-by");

  app.get("/health", (_request, response) => {
    response.json({ status: "ok" });
  });

  app.get("/api/catalogue", async (request, response) => {
    const query = catalogueQuerySchema.safeParse(request.query);
    if (!query.success) {
      response.status(400).json({
        error: {
          code: "INVALID_QUERY",
          message: "Use a single category (up to 100 characters) and q (up to 200 characters).",
        },
      });
      return;
    }

    const products = await source.list();
    const catalogue = buildCatalogue(products.map(publicProduct), {
      category: query.data.category,
      query: query.data.q,
    });
    response.json({ mode: source.mode, ...catalogue });
  });

  app.get("/api/products/:id", async (request, response) => {
    const id = productIdSchema.safeParse(request.params.id);
    if (!id.success) {
      response.status(400).json({
        error: { code: "INVALID_PRODUCT_ID", message: "Product ID must be a positive safe integer." },
      });
      return;
    }

    const products = await source.list();
    const product = products.find((item) => item.id === id.data);
    if (!product) {
      response.status(404).json({
        error: { code: "PRODUCT_NOT_FOUND", message: "Product not found." },
      });
      return;
    }
    response.json(publicProduct(product));
  });

  app.get("/images/:id.jpg", async (request, response) => {
    const id = productIdSchema.safeParse(request.params.id);
    if (!id.success) {
      response.status(400).json({
        error: { code: "INVALID_PRODUCT_ID", message: "Product ID must be a positive safe integer." },
      });
      return;
    }
    const products = await source.list();
    const product = products.find((item) => item.id === id.data);
    if (!product) {
      response.status(404).json({
        error: { code: "PRODUCT_NOT_FOUND", message: "Product not found." },
      });
      return;
    }

    try {
      const image = await loadProductImage(product, source.mode, fetchImage);
      response.set("Cache-Control", "public, max-age=60");
      response.set("X-Content-Type-Options", "nosniff");
      response.type(image.contentType).send(image.bytes);
    } catch (error) {
      console.error("Product image failed:", error);
      response.status(source.mode === "live" ? 502 : 500).json({
        error: { code: "IMAGE_UNAVAILABLE", message: "Unable to load product image. Please try again." },
      });
    }
  });

  app.use((_request, response) => {
    response.status(404).json({
      error: { code: "ROUTE_NOT_FOUND", message: "Route not found." },
    });
  });

  // Express 5 forwards rejected async handlers here. Never expose file paths or stacks.
  const handleError: ErrorRequestHandler = (error, _request, response, _next) => {
    console.error("Catalogue request failed:", error);
    response.status(500).json({
      error: { code: "CATALOGUE_UNAVAILABLE", message: "Unable to load products. Please try again." },
    });
  };
  app.use(handleError);

  return app;
}
