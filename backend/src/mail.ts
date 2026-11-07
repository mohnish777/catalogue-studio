import nodemailer from "nodemailer";
import { z } from "zod";
import type { Product } from "./catalogue.js";
import type { Enquiry } from "./enquiry.js";

export interface EnquiryMailer {
  send(enquiry: Enquiry, product: Product): Promise<void>;
}

const smtpSchema = z.object({
  SMTP_HOST: z.string().trim().min(1).default("127.0.0.1"),
  SMTP_PORT: z.coerce.number().int().min(1).max(65535).default(1025),
  SMTP_SECURE: z.enum(["true", "false"]).default("false"),
  SMTP_USER: z.string().min(1).optional(),
  SMTP_PASS: z.string().min(1).optional(),
  MAIL_FROM: z.email().default("catalogue@catalogue-studio.test"),
  SALES_EMAIL: z.email().default("sales@catalogue-studio.test"),
}).refine((value) => Boolean(value.SMTP_USER) === Boolean(value.SMTP_PASS), {
  message: "Set both SMTP_USER and SMTP_PASS, or neither.",
});

export function createSmtpMailer(env: NodeJS.ProcessEnv = process.env): EnquiryMailer {
  const config = smtpSchema.parse(env);
  const transport = nodemailer.createTransport({
    host: config.SMTP_HOST,
    port: config.SMTP_PORT,
    secure: config.SMTP_SECURE === "true",
    ...(config.SMTP_USER ? { auth: { user: config.SMTP_USER, pass: config.SMTP_PASS! } } : {}),
    connectionTimeout: 3_000,
    greetingTimeout: 3_000,
    socketTimeout: 5_000,
    dnsTimeout: 3_000,
    disableFileAccess: true,
    disableUrlAccess: true,
  });

  return {
    async send(enquiry, product) {
      const info = await transport.sendMail({
        from: config.MAIL_FROM,
        to: config.SALES_EMAIL,
        replyTo: { name: enquiry.name, address: enquiry.email },
        subject: "New catalogue product enquiry",
        text: [
          "Catalogue Studio product enquiry",
          `Product: ${product.title}`,
          `Product ID: ${product.id}`,
          `Price: USD ${product.price.toFixed(2)}`,
          "",
          `Name: ${enquiry.name}`,
          `Email: ${enquiry.email}`,
          "",
          "Message:",
          enquiry.message || "(No message provided)",
        ].join("\n"),
      });
      // SMTP acceptance is the success boundary; it is not proof of external delivery.
      if (info.accepted.length !== 1 || info.rejected.length !== 0) {
        throw new Error("The configured sales recipient was not accepted.");
      }
    },
  };
}
