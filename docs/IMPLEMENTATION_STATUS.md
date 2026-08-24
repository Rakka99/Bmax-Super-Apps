# Implementation status

## Included
- Android Kotlin/Compose project skeleton
- `id.bmax.app` application id
- Material 3 glass-style UI primitives
- MVVM/Clean-oriented package layout
- Hilt application wiring
- Room/DataStore/WorkManager dependencies
- Supabase Auth/PostgREST/Realtime/Storage dependencies
- Live SMARTBILLER Supabase URL + publishable-key build fallback
- Existing-schema customer import compatibility migration
- `nama_bil` / legacy `nama_b11` compatibility handling
- Validated staging-to-customers import function with Biller/RBM ownership mapping
- Initial relational schema
- RBM A-E ownership constraint in the project schema
- Billing/payment status constraints
- Initial RLS policies and helper functions
- Security rule: no service-role or IAK secret in Android
- GitHub Actions cloud debug build workflow

## Verified against live SMARTBILLER project
- `public.customer_import_staging` exists and uses `nama_bil`.
- `public.customers` already contains `nama_bil`, `username`, `kode_petugas`, and `tiang`.
- Live `customers.idpel` is unique.
- Live customer ownership uses `assigned_biller` / `bmax_biller_id` and `bmax_rbm_id`.
- Live RLS restricts customer reads to authenticated users and ownership/supervisor scope.
- Compatibility migration applied successfully.
- `public.import_customer_staging(null)` executes successfully with the current empty staging table.

## Still required before production release
- Complete feature modules and repository implementations
- Supabase Auth login/session UI and authenticated data flow
- Apps Script adapter and migration service
- IAK Edge Functions (`inq-pasca`, `pay-pasca`, `checkstatus`)
- Payment state machine and idempotency tests
- Complete RLS test matrix
- Invoice PDF/thermal printer integration
- Google Maps integration
- FCM notifications
- Unit/UI/integration/security test suites
- CI release signing configuration
- Real release build and signing run
