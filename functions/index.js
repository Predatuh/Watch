const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

// When a note lands in notes/{uid}/inbox, push a notification to that user's
// device so it arrives even when Watchie is closed.
exports.onNoteCreated = onDocumentCreated(
    "notes/{uid}/inbox/{noteId}",
    async (event) => {
      const uid = event.params.uid;
      const note = event.data ? event.data.data() : {};

      const userSnap = await getFirestore().doc(`users/${uid}`).get();
      const token = userSnap.get("fcmToken");
      if (!token) return;

      const from = note.fromName || "a friend";
      try {
        await getMessaging().send({
          token,
          notification: {
            title: "Watchie 💌",
            body: `@${from} sent you a note`,
          },
          android: {priority: "high"},
        });
      } catch (err) {
        console.error("FCM send failed", err);
      }
    },
);
