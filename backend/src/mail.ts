import nodemailer from "nodemailer";
import { z } from "zod";
import type { Product } from "./catalogue.js";

export const enquirySchema = z
  .object({
    productId: z.number().int().positive(),
    name: z.string().trim().min(2).max(100),
    email: z.string().trim().email().max(254),
    message: z.string().trim().max(2000).default(""),
  })
  .strict();
export type Enquiry = z.infer<typeof enquirySchema>;
export interface EnquiryMailer {
  send(enquiry: Enquiry, product: Product): Promise<void>;
}

export function smtpMailer(
  env: NodeJS.ProcessEnv = process.env,
): EnquiryMailer {
  const transport = nodemailer.createTransport({
    host: env.SMTP_HOST ?? "127.0.0.1",
    port: Number(env.SMTP_PORT ?? 1025),
    secure: env.SMTP_SECURE === "true",
    ...(env.SMTP_USER
      ? { auth: { user: env.SMTP_USER, pass: env.SMTP_PASSWORD ?? "" } }
      : {}),
    connectionTimeout: 5000,
    greetingTimeout: 5000,
    socketTimeout: 8000,
  });
  return {
    async send(enquiry, product) {
      const result = await transport.sendMail({
        from: env.SMTP_FROM ?? "Catalogue Studio <hello@catalogue.test>",
        to: env.SALES_EMAIL ?? "sales@catalogue.test",
        replyTo: enquiry.email,
        subject: `Product enquiry: ${product.title.replace(/[\r\n]/g, " ")}`,
        text: [
          "A new product enquiry",
          "",
          `Product: ${product.title} (#${product.id})`,
          `Name: ${enquiry.name}`,
          `Email: ${enquiry.email}`,
          "",
          enquiry.message || "No additional message.",
        ].join("\n"),
      });
      if (result.accepted.length === 0)
        throw new Error("SMTP server did not accept the recipient");
    },
  };
}
