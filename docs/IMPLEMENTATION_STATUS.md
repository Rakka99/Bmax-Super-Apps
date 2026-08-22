# Implementation status

## Included
- Android Kotlin/Compose project skeleton
- `id.bmax.app` application id
- Material 3 glass-style UI primitives
- MVVM/Clean-oriented package layout
- Hilt application wiring
- Room/DataStore/WorkManager dependencies
- Supabase Auth/PostgREST/Realtime dependencies
- Initial relational schema
- RBM A-E ownership constraint
- Billing/payment status constraints
- Initial RLS policies and helper functions
- Security rule: no service-role or IAK secret in Android
- GitHub Actions cloud debug build workflow

## Still required before production release
- Complete feature modules and repository implementations
- Supabase schema inspection against the live SMARTBILLER project
- Existing-schema compatibility migration
- Apps Script adapter and migration service
- IAK Edge Functions (`inq-pasca`, `pay-pasca`, `checkstatus`)
- Payment state machine and idempotency tests
- Complete RLS test matrix
- Invoice PDF/thermal printer integration
- Google Maps integration
- FCM notifications
- Unit/UI/integration/security test suites
- CI signing configuration
- Real release build and signing run
