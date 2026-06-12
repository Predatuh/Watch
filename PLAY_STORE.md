# Uploading Cute Notes to Play Store (Internal testing)

The signed App Bundle to upload is built at:

```
app/build/outputs/bundle/release/app-release.aab
```

Rebuild it any time with:

```
gradlew.bat bundleRelease
```

## ⚠️ Back up your signing key first

Release signing uses `release-keystore.jks` (password in `keystore.properties`).
**Both are gitignored and exist only on this machine.** Copy them somewhere safe
(password manager / cloud drive). This is your *upload key*. Play also offers
**Play App Signing** (recommended, on by default) which manages the real app
signing key for you, so an upload key can be reset via Play support if lost — but
back it up anyway.

Key details:
- Keystore: `release-keystore.jks`
- Alias: `cutenotes`
- Store/key password: see `keystore.properties`

## Steps in Play Console

1. **Create the app** — Play Console → *Create app*. Name: **Cute Notes**.
   App, Free. Declare it's an app (not a game).
2. **Form factor: Wear OS** — in the app's setup, indicate it targets Wear OS.
   (The bundle declares `android.hardware.type.watch`, so it's a watch-only app.)
3. **Internal testing track** — Testing → *Internal testing* → *Create new release*.
   - Upload `app-release.aab`.
   - Play App Signing: accept (let Google manage the app signing key).
   - Release name: `1.0 (1)`. Add brief release notes.
4. **Add testers** — create an email list and add yourself + anyone with a Galaxy
   Watch. Save, then share the **opt-in link** with them; they install via the
   Play Store on the watch (or the paired phone).
5. **Required declarations** (Play won't publish without these):
   - **Privacy policy URL** — use the public `PRIVACY.md` in the repo, e.g.
     `https://github.com/Predatuh/Watch/blob/main/PRIVACY.md`
     (or host it anywhere public).
   - **Data safety form** — declare: collects an app-generated user ID + the
     usernames/messages described in PRIVACY.md; data encrypted in transit;
     messages deleted after delivery; no data sold; no ads.
   - **Content rating questionnaire** — it's a messaging app; answer truthfully
     (users can send free-text drawings to friends → user-generated content).
   - **Target audience** — 13+.

## Notes

- This first build is `versionCode 1`, `versionName 1.0`. For each new upload,
  bump `versionCode` in `app/build.gradle.kts` (2, 3, …).
- Internal testing usually goes live within minutes; no full review wait.
- `app/google-services.json` is baked into the build, so testers' watches talk to
  the same Firebase project automatically.
