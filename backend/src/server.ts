import { createApp } from "./app.js";

const port = Number(process.env.PORT ?? "3000");
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error("PORT must be an integer between 1 and 65535.");
}

const app = createApp();
const server = app.listen(port, "127.0.0.1", () => {
  console.info(`Catalogue Studio backend listening on http://127.0.0.1:${port}`);
});

process.once("SIGINT", () => server.close());
process.once("SIGTERM", () => server.close());
