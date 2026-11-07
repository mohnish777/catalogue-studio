import assert from "node:assert/strict";
import { once } from "node:events";
import { randomUUID } from "node:crypto";
import test from "node:test";
import { createApp } from "../dist/app.js";
import { createSmtpMailer } from "../dist/mail.js";

const product = { id: 9, title: "Portable everyday drive", price: 64.99 };
const valid = { productId: 9, name: " Alex ", email: " alex@example.test ", message: " More information. " };
async function start(t, send, list = async () => [product]) {
  const server = createApp({ mode: "demo", list }, undefined, { send }).listen(0, "127.0.0.1");
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, "listening");
  return `http://127.0.0.1:${server.address().port}/api/enquiries`;
}
const post = (url, body) => fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });

test("valid enquiry normalizes input and hands the looked-up product to the mailer", async t => {
  const received = [];
  const url = await start(t, async (...args) => { received.push(args); });
  const response = await post(url, valid);
  assert.equal(response.status, 201);
  assert.deepEqual(await response.json(), { status: "accepted" });
  assert.deepEqual(received, [[{ productId: 9, name: "Alex", email: "alex@example.test", message: "More information." }, product]]);
});

test("invalid/header/unknown-field enquiries are rejected before lookup or SMTP", async t => {
  let calls = 0;
  const url = await start(t, async () => { calls++; }, async () => { calls++; return [product]; });
  for (const body of [
    { ...valid, productId: 0 }, { ...valid, name: " " }, { ...valid, name: "Alex\r\nBcc: other@example.test" },
    { ...valid, email: "invalid" }, { ...valid, email: "alex@example.test\n" },
    { ...valid, message: "x".repeat(2001) }, { ...valid, to: "other@example.test" },
  ]) assert.equal((await post(url, body)).status, 400);
  assert.equal(calls, 0);
});

test("unknown products and SMTP failures cannot return acceptance", async t => {
  t.mock.method(console, "error", () => {});
  let sends = 0;
  const url = await start(t, async () => { sends++; throw new Error("SMTP unavailable"); });
  assert.equal((await post(url, { ...valid, productId: 999 })).status, 404);
  assert.equal(sends, 0);
  const failed = await post(url, valid);
  assert.equal(failed.status, 503);
  assert.equal((await failed.json()).error.code, "EMAIL_UNAVAILABLE");
  assert.equal(sends, 1);
});

test("malformed oversized and non-JSON bodies return safe errors without SMTP", async t => {
  let sends = 0;
  const url = await start(t, async () => { sends++; });
  for (const [type, body, status] of [
    ["application/json", "{", 400], ["application/json", JSON.stringify({ ...valid, message: "x".repeat(17000) }), 413],
    ["text/plain", "hello", 415],
  ]) {
    const response = await fetch(url, { method: "POST", headers: { "Content-Type": type }, body });
    assert.equal(response.status, status);
    assert.equal(typeof (await response.json()).error.message, "string");
  }
  assert.equal(sends, 0);
});

test("invalid attempts count toward the enquiry rate bound", async t => {
  const url = await start(t, async () => { assert.fail("SMTP should not run"); });
  for (let i = 0; i < 10; i++) assert.equal((await post(url, {})).status, 400);
  const limited = await post(url, {});
  assert.equal(limited.status, 429);
  assert.equal((await limited.json()).error.code, "TOO_MANY_ENQUIRIES");
});

test("local Mailpit capture contains fixed recipient customer Reply-To and product details", {
  skip: process.env.RUN_SMTP_TEST !== "1",
}, async t => {
  const inbox = "http://127.0.0.1:8025";
  const before = new Set((await (await fetch(`${inbox}/api/v1/messages`)).json()).messages.map(m => m.ID));
  const mailer = createSmtpMailer({ SMTP_HOST: "127.0.0.1", SMTP_PORT: "1025" });
  const url = await start(t, (enquiry, selected) => mailer.send(enquiry, selected));
  const marker = `Automated local SMTP check ${randomUUID()}`;
  assert.equal((await post(url, { ...valid, message: marker })).status, 201);
  const candidates = (await (await fetch(`${inbox}/api/v1/messages`)).json()).messages.filter(m => !before.has(m.ID));
  let captured;
  for (const item of candidates) {
    const message = await (await fetch(`${inbox}/api/v1/message/${item.ID}`)).json();
    if (message.Text.includes(marker)) captured = message;
  }
  assert.ok(captured, "Expected a new local Mailpit message");
  assert.equal(captured.From.Address, "catalogue@catalogue-studio.test");
  assert.equal(captured.To[0].Address, "sales@catalogue-studio.test");
  assert.equal(captured.ReplyTo[0].Address, "alex@example.test");
  assert.match(captured.Text, /Product ID: 9/);
  assert.match(captured.Text, /Portable everyday drive/);
});
