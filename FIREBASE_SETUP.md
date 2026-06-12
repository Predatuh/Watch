# Turning on real phone-to-phone delivery (Firebase)

Right now the app runs in **Demo mode**: a note you send echoes back to you a few
seconds later so you can see the whole receive experience on one watch. The code
is written against a small `NoteTransport` interface (see `Note.kt`), so switching
to real delivery between two people is a contained change.

To go live we use **Firebase** (free tier is plenty). Only *you* can do the
console steps below because they're tied to your Google account.

## What you do (about 5–10 minutes, all in the browser)

1. Go to https://console.firebase.google.com and click **Add project**. Name it
   something like `cute-watch-notes`. You can turn Google Analytics off.
2. In the project, click the **Android** icon to add an Android app.
   - **Android package name:** `com.cutenotes.watch`  (must match exactly)
   - Nickname/anything else: optional. Click **Register app**.
3. **Download `google-services.json`** when it offers it. Put that file here:
   `app/google-services.json`  (next to `app/build.gradle.kts`)
4. In the left sidebar:
   - **Build → Firestore Database → Create database** → start in **Test mode**.
   - **Build → Authentication → Get started →** enable **Anonymous** sign-in.
   - (Later, for notifications when the app is closed: **Build → Cloud Messaging** —
     no action needed now.)

That's it. Tell me when `app/google-services.json` is in place.

## What I do next (the code)

- Add the Firebase Gradle plugin + dependencies (Firestore + Auth).
- Write `FirebaseNoteTransport` implementing the same `NoteTransport` interface
  and point `transport` (in `Note.kt`) at it instead of `LoopbackTransport`.
- Add **pairing**: each person gets a short code (or QR); entering your partner's
  code links the two accounts so notes route to the right watch.
- Notes are written to Firestore under the recipient; the app listens in real time
  and shows them exactly like the demo does today (banner, buzz, full-screen play).

## Notes / privacy

- Test-mode Firestore rules are open for ~30 days — fine for building. Before
  sharing the app with anyone, we'll lock the rules down so only paired users can
  read each other's notes.
- The free Spark plan covers far more than two people sending cute notes.
