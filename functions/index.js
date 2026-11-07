/**
 * Planora – App Locker "Forgot password" OTP.
 * The 6-digit code is generated, stored (hashed), emailed, expired and verified ONLY here,
 * never inside the Android app.
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

const GMAIL_USER = defineSecret("GMAIL_USER");
const GMAIL_APP_PASSWORD = defineSecret("GMAIL_APP_PASSWORD");

const OTP_TTL_MS = 10 * 60 * 1000;   // code valid for 10 minutes
const RESEND_MS = 60 * 1000;         // 1 code per minute
const MAX_ATTEMPTS = 5;

const hash = (uid, code) =>
  crypto.createHash("sha256").update(`${uid}:${code}`).digest("hex");

exports.sendLockerOtp = onCall({ secrets: [GMAIL_USER, GMAIL_APP_PASSWORD] }, async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "Please log in first.");
  const uid = req.auth.uid;
  const email = req.auth.token.email;
  if (!email) throw new HttpsError("failed-precondition", "Your account has no email.");

  const ref = db.collection("lockerOtps").doc(uid);
  const snap = await ref.get();
  const now = Date.now();
  if (snap.exists && now - snap.data().sentAt < RESEND_MS) {
    throw new HttpsError("resource-exhausted", "Please wait a minute before requesting another code.");
  }

  const code = String(crypto.randomInt(0, 1000000)).padStart(6, "0");
  await ref.set({ hash: hash(uid, code), expiresAt: now + OTP_TTL_MS, attempts: 0, sentAt: now });

  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: { user: GMAIL_USER.value(), pass: GMAIL_APP_PASSWORD.value() },
  });
  await transporter.sendMail({
    from: `Planora <${GMAIL_USER.value()}>`,
    to: email,
    subject: "Planora App Locker – your reset code",
    text: `Your Planora App Locker reset code is ${code}\nIt expires in 10 minutes.\nIf you did not request this, ignore this email.`,
    html: `<div style="font-family:Arial,sans-serif"><h2>Planora App Locker</h2>
           <p>Your reset code is</p>
           <p style="font-size:32px;letter-spacing:6px;font-weight:bold">${code}</p>
           <p>It expires in 10 minutes. If you did not request this, ignore this email.</p></div>`,
  });
  return { ok: true };
});

exports.verifyLockerOtp = onCall(async (req) => {
  if (!req.auth) throw new HttpsError("unauthenticated", "Please log in first.");
  const uid = req.auth.uid;
  const code = String((req.data && req.data.code) || "").trim();
  if (!/^\d{6}$/.test(code)) throw new HttpsError("invalid-argument", "Enter the 6-digit code.");

  const ref = db.collection("lockerOtps").doc(uid);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError("not-found", "No code requested. Please request a new code.");
  const d = snap.data();

  if (Date.now() > d.expiresAt) {
    await ref.delete();
    throw new HttpsError("deadline-exceeded", "Code expired. Request a new one.");
  }
  if (d.attempts >= MAX_ATTEMPTS) {
    await ref.delete();
    throw new HttpsError("resource-exhausted", "Too many wrong attempts. Request a new code.");
  }
  const ok = crypto.timingSafeEqual(Buffer.from(d.hash), Buffer.from(hash(uid, code)));
  if (!ok) {
    await ref.update({ attempts: admin.firestore.FieldValue.increment(1) });
    throw new HttpsError("permission-denied", "Wrong code.");
  }
  await ref.delete();   // one-time use
  return { ok: true };
});
