const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();
const DAY = 24 * 60 * 60 * 1000;

// When a note lands in notes/{uid}/inbox: update the friendship streak, then
// push a notification so it arrives even when Watchie is closed.
exports.onNoteCreated = onDocumentCreated(
    "notes/{uid}/inbox/{noteId}",
    async (event) => {
      const recipient = event.params.uid;
      const note = event.data ? event.data.data() : {};
      const sender = note.fromUid;
      const now = Date.now();

      // ---- streak ----
      // A streak counts up once per day while BOTH friends keep messaging each
      // other within a 24h window; missing ~a day resets it.
      if (sender && sender !== recipient) {
        const [a, b] = [sender, recipient].sort();
        const senderIsA = sender === a;
        const ref = db.doc(`streaks/${a}_${b}`);
        try {
          const newCount = await db.runTransaction(async (tx) => {
            const snap = await tx.get(ref);
            const d = snap.exists ? snap.data() : {};
            let lastA = d.lastA || 0;
            let lastB = d.lastB || 0;
            let count = d.count || 0;
            let lastTick = d.lastTick || 0;

            if (senderIsA) lastA = now; else lastB = now;

            const reciprocated =
              lastA > 0 && lastB > 0 &&
              (now - lastA <= DAY) && (now - lastB <= DAY);

            if (reciprocated) {
              if (count === 0 || (now - lastTick) > 2 * DAY) {
                count = 1;
                lastTick = now;
              } else if ((now - lastTick) >= DAY) {
                count = count + 1;
                lastTick = now;
              }
            }

            tx.set(ref, {uidA: a, uidB: b, lastA, lastB, count, lastTick}, {merge: true});
            return count;
          });

          // Denormalize the count into both friend docs for easy display.
          await Promise.all([
            db.doc(`users/${a}/friends/${b}`).set({streak: newCount}, {merge: true}),
            db.doc(`users/${b}/friends/${a}`).set({streak: newCount}, {merge: true}),
          ]);
        } catch (err) {
          console.error("streak update failed", err);
        }
      }

      // ---- push ----
      const userSnap = await db.doc(`users/${recipient}`).get();
      const token = userSnap.get("fcmToken");
      if (!token) return;
      const from = note.fromName || "a friend";
      const summary = note.summary || "sent you a note";
      try {
        // A notification message so the system shows it even when the watch app
        // is closed — buzzes, surfaces on wrist-raise, and shows the content
        // like a text. Tapping opens the animated note.
        await getMessaging().send({
          token,
          notification: {
            title: `@${from}`,
            body: summary,
          },
          data: {fromName: String(from)},
          android: {
            priority: "high",
            notification: {channelId: "notes", priority: "high"},
          },
        });
      } catch (err) {
        console.error("FCM send failed", err);
      }
    },
);
