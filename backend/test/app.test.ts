import { test } from "node:test";
import assert from "node:assert/strict";
import request from "supertest";
import { randomUUID } from "node:crypto";
import { createApp } from "../src/app.js";
import {
  DemoProductSource,
  FakeStoreProductSource,
  buildCatalogue,
  featuredIds,
  type ProductSource,
} from "../src/catalogue.js";
import { smtpMailer, type EnquiryMailer, type Enquiry } from "../src/mail.js";

const source = new DemoProductSource();
const products = await source.list();
const valid = {
  productId: 1,
  name: "Ada Lovelace",
  email: "ada@example.test",
  message: "Could you tell me more?",
};
const delivered: Enquiry[] = [];
const mailer: EnquiryMailer = {
  async send(enquiry) {
    delivered.push(enquiry);
  },
};

test("home features exactly five editorial picks without duplicate tiles", async () => {
  const response = await request(createApp(source, mailer))
    .get("/api/catalogue")
    .expect(200);
  assert.deepEqual(
    response.body.featured.map((p: { id: number }) => p.id),
    featuredIds,
  );
  const ids = [...response.body.featured, ...response.body.products].map(
    (p: { id: number }) => p.id,
  );
  assert.equal(new Set(ids).size, products.length);
  assert.equal(ids.length, products.length);
  assert.equal(response.body.featured[0].imageUrl, "/api/products/1/image");
});
test("categories show top-rated picks and additional products in that category", async () => {
  const response = await request(createApp(source, mailer))
    .get("/api/catalogue")
    .query({ category: "men's clothing" })
    .expect(200);
  assert.deepEqual(
    response.body.featured.map((p: { id: number }) => p.id),
    [3, 1],
  );
  assert.ok(response.body.products.length > 0);
  for (const p of [...response.body.featured, ...response.body.products])
    assert.equal(p.category, "men's clothing");
});
test("case-insensitive search matches titles and descriptions and omits featured section", async () => {
  const response = await request(createApp(source, mailer))
    .get("/api/catalogue")
    .query({ search: "  COTTON  " })
    .expect(200);
  assert.deepEqual(response.body.featured, []);
  assert.equal(response.body.products.length, 3);
  const descriptions = buildCatalogue(products, undefined, "commute");
  assert.deepEqual(
    descriptions.products.map((p) => p.id),
    [1],
  );
});
test("search combines with category and returns an honest empty result", async () => {
  const response = await request(createApp(source, mailer))
    .get("/api/catalogue")
    .query({ category: "jewelery", search: "cotton" })
    .expect(200);
  assert.deepEqual(response.body.products, []);
  assert.deepEqual(response.body.featured, []);
});
test("product detail and image are served by the backend", async () => {
  const app = createApp(source, mailer);
  const response = await request(app).get("/api/products/1").expect(200);
  assert.equal(response.body.id, 1);
  const image = await request(app)
    .get(response.body.imageUrl)
    .expect(200)
    .expect("Content-Type", /image\/jpeg/);
  assert.ok(image.body.length > 1000);
  await request(app).get("/api/products/99999").expect(404);
  await request(app).get("/api/products/not-an-id").expect(400);
});
test("valid enquiry is normalised and sent before reporting success", async () => {
  const sent: Enquiry[] = [];
  const app = createApp(source, {
    async send(enquiry) {
      sent.push(enquiry);
    },
  });
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, name: "  Ada Lovelace  ", email: " ada@example.test " })
    .expect(201);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].name, "Ada Lovelace");
  assert.equal(sent[0].email, "ada@example.test");
});
test("invalid contact details, unknown products and extra recipient fields never send mail", async () => {
  let sends = 0;
  const app = createApp(source, {
    async send() {
      sends++;
    },
  });
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, email: "not-an-email" })
    .expect(400);
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, name: " " })
    .expect(400);
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, message: "x".repeat(2001) })
    .expect(400);
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, to: "attacker@example.test" })
    .expect(400);
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, productId: 99999 })
    .expect(404);
  assert.equal(sends, 0);
});
test("SMTP failure returns a retryable error instead of a false thank-you", async () => {
  const app = createApp(source, {
    async send() {
      throw new Error("SMTP unavailable");
    },
  });
  const response = await request(app)
    .post("/api/enquiries")
    .send(valid)
    .expect(503);
  assert.match(response.body.error, /could not send/i);
  assert.equal(response.body.message, undefined);
});
test("malformed JSON, oversized requests and repeated submissions are bounded", async () => {
  const app = createApp(source, mailer);
  await request(app)
    .post("/api/enquiries")
    .set("Content-Type", "application/json")
    .send("{oops")
    .expect(400);
  await request(app)
    .post("/api/enquiries")
    .send({ ...valid, message: "x".repeat(9000) })
    .expect(413);
  for (let i = 0; i < 10; i++)
    await request(app).post("/api/enquiries").send(valid).expect(201);
  await request(app).post("/api/enquiries").send(valid).expect(429);
});
test("provider failures are explicit and do not silently substitute demo data", async () => {
  const broken: ProductSource = {
    mode: "live",
    async list() {
      throw new Error("Provider blocked");
    },
  };
  await request(createApp(broken, mailer)).get("/api/catalogue").expect(502);
});
test("live provider maps real API shape and caches successful responses", async () => {
  let calls = 0;
  const live = new FakeStoreProductSource(async () => {
    calls++;
    return new Response(
      JSON.stringify([
        {
          id: 1,
          title: "Backpack",
          description: "Everyday",
          price: 12.5,
          category: "bags",
          image: "https://fakestoreapi.com/img/example.jpg",
          rating: { rate: 4.5, count: 12 },
        },
      ]),
    );
  });
  const result = await live.list();
  await live.list();
  assert.equal(calls, 1);
  assert.equal(result[0].rating, 4.5);
  assert.equal(result[0].reviewCount, 12);
});
test("blocked or malformed live responses fail instead of poisoning the cache", async () => {
  let calls = 0;
  const live = new FakeStoreProductSource(async () => {
    calls++;
    return new Response("blocked", { status: 403 });
  });
  await assert.rejects(() => live.list());
  await assert.rejects(() => live.list());
  assert.equal(calls, 2);
});
test(
  "real local SMTP delivery appears in Mailpit",
  { skip: process.env.RUN_SMTP_TEST !== "1" },
  async () => {
    const marker = `SMTP integration ${randomUUID()}`;
    const localSource: ProductSource = {
      mode: "demo",
      async list() {
        return [{ ...products[0], title: marker }];
      },
    };
    const app = createApp(
      localSource,
      smtpMailer({
        SMTP_HOST: "127.0.0.1",
        SMTP_PORT: "1025",
        SALES_EMAIL: "sales@catalogue.test",
      }),
    );
    await request(app).post("/api/enquiries").send(valid).expect(201);
    const inbox = await fetch("http://127.0.0.1:8025/api/v1/messages").then(
      (r) => r.json(),
    );
    const message = inbox.messages.find((m: { Subject: string }) =>
      m.Subject.includes(marker),
    );
    assert.ok(message, "enquiry was captured by local SMTP inbox");
    const full = await fetch(
      `http://127.0.0.1:8025/api/v1/message/${message.ID}`,
    ).then((r) => r.json());
    assert.match(full.Text, /Ada Lovelace/);
    assert.match(full.Text, /Could you tell me more/);
  },
);
