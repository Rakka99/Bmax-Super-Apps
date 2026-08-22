# Bmax Super Apps

Bmax Electricity Payment Monitoring — Android project skeleton prepared for cloud build on GitHub.

## Stack

Kotlin, Jetpack Compose, Material 3, MVVM/Clean-oriented structure, Hilt, Room, DataStore, WorkManager, Ktor/Supabase Auth/PostgREST/Realtime/Storage.

## Security

Only `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` are intended for Android build configuration. Never ship Supabase `service_role`, database passwords, IAK credentials, or other server secrets in the APK.

## GitHub Actions

The workflow at `.github/workflows/android-ci.yml` builds a debug APK on pushes and pull requests to `main`.

Optional repository secrets:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

The workflow passes them as Gradle project properties and the app maps them into `BuildConfig`.

## Local build

Use JDK 17 and run:

```bash
gradle assembleDebug
```

The current repository is a foundation. Production work still includes feature modules, live Supabase schema compatibility, Apps Script adapter, IAK Edge Functions, payment state machine/idempotency tests, printer/PDF, Maps, FCM, and release signing.

Target repository: https://github.com/Rakka99/Bmax-Super-Apps.git
