# Bmax Super Apps

Bmax Electricity Payment Monitoring — Android project foundation wired to the live SMARTBILLER Supabase project.

## Stack

Kotlin, Jetpack Compose, Material 3, MVVM/Clean-oriented structure, Hilt, Room, DataStore, WorkManager, Ktor/Supabase Auth/PostgREST/Realtime/Storage.

## Supabase endpoint configuration

The Android app is configured at build time. The repository does not define a production URL fallback; the build must provide these Gradle properties:

- `SUPABASE_URL`: the root HTTPS URL for the exact Supabase instance used by this build.
- `SUPABASE_PUBLISHABLE_KEY`: the publishable/anon key issued by that same instance.

GitHub Actions reads these values from repository secrets with the same names. Keep them matched to one environment (Cloud or self-hosted); do not mix a Cloud URL with a self-hosted key or vice versa.

For self-hosted production, point `SUPABASE_URL` at a custom domain with valid HTTPS that reverse-proxies to the Supabase API gateway. Do not point the app at the database port, Redis, or an unencrypted HTTP endpoint. Confirm the endpoint and Auth configuration before distributing an APK.

Publishable keys are suitable for a mobile app only when RLS is configured correctly. Never ship a Supabase secret/service-role key in the APK. After moving to a self-hosted instance with a different JWT secret, users should expect to sign in again.

## Customer import compatibility

The live database uses `nama_bil`, `tariff`, and `power_va` in `public.customer_import_staging`. Older Bmax/Sheets queries used `nama_b11`, `tarif`, `daya`, and `gardu`.

Migrations `016_customer_import_compatibility.sql` and `017_customer_import_legacy_aliases.sql` now:

- keep `nama_bil`, `tariff`, and `power_va` as canonical live fields;
- provide generated compatibility aliases `nama_b11`, `tarif`, and `daya`;
- provide a nullable `gardu` field for older import files;
- index `idpel` and `import_batch_id`;
- provide `public.import_customer_staging(uuid)` for validated staging-to-customers upsert;
- map `username`/`nama_bil` to an existing Bmax profile before importing ownership;
- create/update RBM A-E records when region and ULP are present;
- preserve `gardu`, `tiang`, `nama_bil`, `kode_petugas`, and Biller ownership in `customers`;
- keep customer ownership compatible with the live RLS model;
- use `idpel` as the unique customer key.

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

The workflow at `.github/workflows/android-ci.yml` builds a debug APK on pushes and pull requests to `main`. It uses JDK 17 and Gradle 8.9, matching the Android Gradle Plugin 8.7.3 configuration.

Optional repository secrets can override the built-in publishable configuration:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`

## Local build

Use JDK 17 and run:

```bash
gradle assembleDebug
```

## Current scope

The repository now contains the live Supabase compatibility layer, Auth/customer client integration, and CI build configuration. Production work still includes complete feature modules, Apps Script adapter, IAK Edge Functions, payment state machine/idempotency tests, printer/PDF, Maps, FCM, and release signing.

Target repository: https://github.com/Rakka99/Bmax-Super-Apps.git

CI checkpoint: 2026-08-24 — debug APK build uses Android's standard debug signing; no custom keystore is required.
