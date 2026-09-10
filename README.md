# Bmax Super Apps

Bmax Electricity Payment Monitoring — Android project foundation wired to the live Bmax Super Apps Supabase project.

## Stack

Kotlin, Jetpack Compose, Material 3, MVVM/Clean-oriented structure, Hilt, Room, DataStore, WorkManager, Ktor/Supabase Auth/PostgREST/Realtime/Storage.

## Live Supabase configuration

The Android client uses the Bmax Supabase project URL and a **publishable** key. Publishable keys are intended for client applications; never ship a Supabase secret/service-role key or a DDTN API key in the APK.

Live Bmax Supabase project URL:
`https://pcvzfthpbytrydsvrhtg.supabase.co`

`app/build.gradle.kts` accepts these optional Gradle properties:

- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `MAPS_API_KEY`

When `SUPABASE_URL` is not supplied, the build falls back to the live Bmax Supabase project URL above.

## DDTN PLN integration

Bmax now calls the Supabase Edge Function `ddtn-pln-sync` from the Android customer screen. The Android client sends the signed-in user's Supabase session JWT plus the publishable key; the DDTN API key remains server-side in the Supabase Edge Function secret `DDTN_API_KEY`.

Flow:

```text
Android Bmax
  -> Supabase Edge Function: ddtn-pln-sync
  -> DDTN POST /v1/pln/check-bulk
  -> match customer_no + period
  -> update public.billings
```

Status handling:

- `is_paid = true` -> `billings.status = PAID`
- `is_paid = false` -> `billings.status = UNPAID`
- `is_paid = null` -> existing billing status is preserved as unverified
- DDTN/provider HTTP errors never force a PAID/UNPAID value
- `checked_at` is stored in the existing `billings.synced_at` field
- DDTN-driven updates use the existing `billings.source` value `API`

The existing database schema is reused; no new billing/invoice table is required for this integration.

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

## GitHub Actions

The workflow at `.github/workflows/android-ci.yml` builds a debug APK on pushes and pull requests to `main`. It uses JDK 17 and Gradle 8.9.

Required repository secrets:
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `MAPS_API_KEY`

The DDTN API key is **not** a GitHub/Android secret. Keep it only in Supabase Edge Function Secrets as `DDTN_API_KEY`.

## Local build

Use JDK 17 and run:

```bash
gradle assembleDebug
```

## Current scope

The repository contains the live Supabase Auth/customer client, DDTN PLN status integration, customer-level DDTN checking, CI build configuration, and Maps foundation. Mass DDTN synchronization/scheduling should only be enabled after the customer-level integration is validated.

Target repository: https://github.com/Rakka99/Bmax-Super-Apps.git
