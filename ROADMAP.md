# Roadmap to a Play Store testing build

Where we are: the watch app works with real Firebase delivery and a **friends
list** — each watch has a unique friend code, you add friends by code, and you
send notes (expressions, fireworks, drawings) to a chosen friend. Notes arrive
live while the app is open.

Below is what's left to reach a shareable Play Store testing build, in order,
with what each step needs from you.

## 1. Background delivery (push notifications) — needs Firebase Blaze plan
Today notes arrive only while the app is **open** (a live database listener). To
buzz like a real text when the app is closed, we add **Firebase Cloud Messaging**:

- **Client (I do):** add `firebase-messaging`, register each watch's FCM token
  into `users/{uid}.fcmToken`, and a service that shows a notification.
- **Server (I do, you deploy):** a Cloud Function that triggers when a note is
  written to `notes/{uid}/inbox` and pushes FCM to that user's token. Example:

  ```js
  // functions/index.js
  const {onDocumentCreated} = require("firebase-functions/v2/firestore");
  const {getMessaging} = require("firebase-admin/messaging");
  const {getFirestore} = require("firebase-admin/firestore");
  require("firebase-admin").initializeApp();

  exports.onNote = onDocumentCreated("notes/{uid}/inbox/{noteId}", async (e) => {
    const uid = e.params.uid;
    const user = await getFirestore().doc(`users/${uid}`).get();
    const token = user.get("fcmToken");
    if (!token) return;
    await getMessaging().send({
      token,
      notification: {title: "Cute Notes", body: "You got a note 💌"},
    });
  });
  ```

- **What you do:** upgrade the Firebase project to the **Blaze plan** (pay-as-you-go;
  Cloud Functions require it, but the free monthly allowance covers far more than
  a couple of friends). Then I wire it up and you run `firebase deploy --only functions`.

## 2. Lock down security rules — no cost
`firestore.rules` in this repo is the hardened ruleset. Right now Firestore is in
open "test mode" (fine for building, expires ~30 days). Apply the rules in
**Firebase console → Firestore → Rules → paste → Publish**, then re-test the app.

## 3. Friendlier pairing & names — no cost
- **Display names:** let each person set a name (instead of showing their code).
- **QR codes:** show your code as a QR so a friend can scan instead of typing.

## 4. A phone app — bigger piece, no extra account
A second module (`:mobile`) that shares the same Firebase backend and friend
model. You'd manage friends and send notes from your phone too. This is the
largest remaining build but reuses everything on the server side.

## 5. Play Store testing build — needs a Play Console account ($25 one-time)
- **Release signing (I do):** create a keystore + signing config, build a
  signed **App Bundle (.aab)**.
- **What you do:** create a **Google Play Console** account ($25 one-time),
  create the app, and upload the .aab to an **Internal testing** track. Add
  testers by email; they install via a link. (A simple privacy policy URL is
  required because the app handles accounts/messages — I can draft one.)

## Suggested order
1 (push) → 2 (rules) → 3 (names/QR) → 5 (signed build + internal testing) →
4 (phone app) once the watch app is solid in testers' hands.
