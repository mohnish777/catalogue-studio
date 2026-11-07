import { z } from "zod";

// Check raw header fields before trimming, so leading/trailing newlines are rejected too.
const singleLine = z.string()
  .refine((value) => !/[\u0000-\u001f\u007f-\u009f]/u.test(value))
  .transform((value) => value.trim());

export const enquirySchema = z.strictObject({
  productId: z.number().int().positive().max(Number.MAX_SAFE_INTEGER),
  name: singleLine.pipe(z.string().min(1).max(100)),
  email: singleLine.pipe(z.email().max(254)),
  message: z.string().trim().max(2_000).default(""),
});

export type Enquiry = z.infer<typeof enquirySchema>;
