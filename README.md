# Bmax Super Apps

Bmax Electricity Payment Monitoring — Android project foundation wired to the live SMARTBILLER Supabase project.

## Stack

Kotlin, Jetpack Compose, Material 3, MVVM/Clean-oriented structure, Hilt, Room, DataStore, WorkManager, Ktor/Supabase Auth/PostgREST/Realtime/Storage.

## Live Supabase configuration

The Android client uses the SMARTBILLER project URL and a **publishable** key. Publishable keys are safe for mobile/public source when RLS is correctly configured; never ship a Supabase secret/service-role key in the APK.

Project URL:
`https://vgnynrzhanfnbifjedga.supabase.co`

`app/build.gradle.kts` accepts these optional Gradle properties and falls back to the live publishable configuration when they are not supplied:

- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

## Customer import compatibility

The live database uses `nama_bil` in `public.customer_import_staging`. The previous failing query referenced `nama_b11`.

Migration `supabase/migrations/016_customer_import_compatibility.sql` now:

- adds `nama_b11` only as a generated compatibility alias of `nama_bil`;
- indexes `idpel` and `import_batch_id`;
- provides `public.import_customer_staging(uuid)` for validated staging-to-customers upsert;
- maps `username`/`nama_bil` to an existing Bmax profile before importing ownership;
- creates/updates RBM A-E records when region and ULP are present;
- keeps customer ownership compatible with the live RLS model;
- uses `idpel` as the unique customer key.

For a full staging import after rows have been uploaded:

```sql
select * from public.import_customer_staging(null);
```

For one import batch:

```sql
select * from public.import_customer_staging('<IMPORT_BATCH_UUID>');
```

Do not use a Supabase service-role/secret key from Android. Server-only IAK credentials must remain in Edge Functions/backend secrets.

## GitHub Actions

The workflow at `.github/workflows/android-ci.yml` builds a debug APK on pushes and pull requests to `main`.

Optional repository secrets can override the built-in publishable configuration:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

## Local build

Use JDK 17 and run:

```bash
gradle assembleDebug
```

## Current scope

The repository now contains the live Supabase compatibility layer and build configuration. Production work still includes complete feature modules, Auth UI/session flow, Apps Script adapter, IAK Edge Functions, payment state machine/idempotency tests, printer/PDF, Maps, FCM, and release signing.

Target repository: https://github.com/Rakka99/Bmax-Super-Apps.git
