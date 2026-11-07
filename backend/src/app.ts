import express, { type ErrorRequestHandler } from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { readFile } from "node:fs/promises";
import { z } from "zod";
import {
  buildCatalogue,
  publicProduct,
  type ProductSource,
} from "./catalogue.js";
import { enquirySchema, type EnquiryMailer } from "./mail.js";

const querySchema = z.object({
  category: z.string().max(100).optional(),
  search: z.string().max(200).optional(),
});
const idSchema = z.coerce.number().int().positive();

export function createApp(source: ProductSource, mailer: EnquiryMailer) {
  const app = express();
  app.disable("x-powered-by");
  app.use(helmet());
  app.use(express.json({ limit: "8kb" }));
  app.get("/health", (_req, res) =>
    res.json({ status: "ok", productSource: source.mode }),
  );
  app.get("/api/catalogue", async (req, res) => {
    const query = querySchema.safeParse(req.query);
    if (!query.success)
      return res.status(400).json({ error: "Invalid catalogue filters." });
    const all = await source.list();
    const result = buildCatalogue(all, query.data.category, query.data.search);
    return res.json({
      ...result,
      featured: result.featured.map(publicProduct),
      products: result.products.map(publicProduct),
    });
  });
  app.get("/api/products/:id", async (req, res) => {
    const id = idSchema.safeParse(req.params.id);
    if (!id.success)
      return res.status(400).json({ error: "Invalid product ID." });
    const product = (await source.list()).find((p) => p.id === id.data);
    if (!product) return res.status(404).json({ error: "Product not found." });
    return res.json(publicProduct(product));
  });
  app.get("/api/products/:id/image", async (req, res) => {
    const id = idSchema.safeParse(req.params.id);
    if (!id.success)
      return res.status(400).json({ error: "Invalid product ID." });
    const product = (await source.list()).find((p) => p.id === id.data);
    if (!product) return res.status(404).json({ error: "Product not found." });
    if (source.mode === "demo") {
      const file = await readFile(`public/images/${product.id}.jpg`);
      return res
        .type("jpg")
        .set("Cache-Control", "public, max-age=3600")
        .send(file);
    }
    const url = new URL(product.imageUrl);
    if (url.protocol !== "https:" || url.hostname !== "fakestoreapi.com")
      throw new Error("Unexpected product image host");
    const response = await fetch(url, {
      signal: AbortSignal.timeout(8000),
      redirect: "error",
    });
    if (!response.ok) throw new Error("Image provider unavailable");
    const mime = response.headers.get("content-type")?.split(";")[0];
    if (mime !== "image/jpeg" && mime !== "image/png" && mime !== "image/webp")
      throw new Error("Invalid product image");
    const bytes = Buffer.from(await response.arrayBuffer());
    if (bytes.length > 5_000_000) throw new Error("Image too large");
    return res
      .type(mime)
      .set("Cache-Control", "public, max-age=3600")
      .send(bytes);
  });
  app.post(
    "/api/enquiries",
    rateLimit({
      windowMs: 60_000,
      limit: 10,
      standardHeaders: "draft-8",
      legacyHeaders: false,
    }),
    async (req, res) => {
      const enquiry = enquirySchema.safeParse(req.body);
      if (!enquiry.success)
        return res.status(400).json({
          error: "Please provide a name, valid email, and product ID.",
        });
      const product = (await source.list()).find(
        (p) => p.id === enquiry.data.productId,
      );
      if (!product)
        return res.status(404).json({ error: "Product not found." });
      try {
        await mailer.send(enquiry.data, product);
      } catch {
        return res
          .status(503)
          .json({ error: "We could not send your enquiry. Please try again." });
      }
      return res
        .status(201)
        .json({ message: "Thank you. Your enquiry has been sent." });
    },
  );
  app.use((_req, res) => res.status(404).json({ error: "Route not found." }));
  const handleError: ErrorRequestHandler = (error, _req, res, _next) => {
    if (error instanceof SyntaxError && "body" in error)
      return res.status(400).json({ error: "Invalid JSON." });
    if (error.status === 413)
      return res.status(413).json({ error: "Request body too large." });
    console.error(
      "Request failed:",
      error instanceof Error ? error.message : "unknown error",
    );
    return res.status(502).json({
      error: "The catalogue is temporarily unavailable. Please try again.",
    });
  };
  app.use(handleError);
  return app;
}
