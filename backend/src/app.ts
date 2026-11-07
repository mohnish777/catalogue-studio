import express, { type ErrorRequestHandler } from "express";
import { z } from "zod";
import { DemoProductSource, type ProductSource } from "./catalogue.js";
import { buildCatalogue } from "./catalogue-rules.js";

const catalogueQuerySchema = z.object({
  category: z.string().trim().max(100).optional(),
  q: z.string().trim().max(200).optional(),
});

const productIdSchema = z.string()
  .regex(/^[1-9]\d*$/)
  .transform(Number)
  .pipe(z.number().int().positive().max(Number.MAX_SAFE_INTEGER));

export function createApp(source: ProductSource = new DemoProductSource()) {
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
    const catalogue = buildCatalogue(products, {
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
    response.json(product);
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
