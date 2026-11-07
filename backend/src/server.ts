import { createApp } from "./app.js";
import { createProductSource } from "./product-sources.js";

const port = Number(process.env.PORT ?? "3000");
const host = process.env.HOST ?? "127.0.0.1";
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error("PORT must be an integer between 1 and 65535.");
}

const source = createProductSource(process.env.PRODUCT_SOURCE);
const app = createApp(source);
const server = app.listen(port, host, () => {
  console.info(`Catalogue Studio backend listening on http://${host}:${port}`);
  console.info(`Product source: ${source.mode}`);
});

process.once("SIGINT", () => server.close());
process.once("SIGTERM", () => server.close());
