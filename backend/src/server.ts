import "dotenv/config";
import { z } from "zod";
import { createApp } from "./app.js";
import { DemoProductSource, FakeStoreProductSource } from "./catalogue.js";
import { smtpMailer } from "./mail.js";

const mode = z
  .enum(["demo", "live"])
  .parse(process.env.PRODUCT_SOURCE ?? "demo");
const port = z.coerce
  .number()
  .int()
  .min(1)
  .max(65535)
  .parse(process.env.PORT ?? 3000);
const source =
  mode === "live" ? new FakeStoreProductSource() : new DemoProductSource();
const server = createApp(source, smtpMailer()).listen(port, "0.0.0.0", () => {
  console.log(
    `Catalogue Studio listening on :${port}; product source: ${mode}`,
  );
});
for (const signal of ["SIGINT", "SIGTERM"] as const)
  process.on(signal, () => server.close(() => process.exit(0)));
