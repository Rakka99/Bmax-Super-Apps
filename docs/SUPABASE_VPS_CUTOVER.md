# Supabase Cloud → VPS cutover runbook (Bmax SuperApps)

**Status:** preparation only. This document does not switch production traffic. Do not carry out the write-freeze/cutover steps until the self-hosted restore and canary tests have passed.

## Findings verified on 2026-10-18

- Current hosted database is PostgreSQL 17, project state healthy.
- Latest observed source counts (they will change while Cloud remains live): `auth.users` 108, `public.customers` 145,756, `public.billings` 191,001, `public.sync_jobs` 18,088, `public.pitu_sync_retry_queue` 5,290, `public.pitu_inquiry_audit` 20,463, `public.customer_import_staging_v2` 145,754.
- Hosted Edge Function `pln-status-engine` is active at version 145 and needs a self-hosted copy plus local secrets.
- Hosted pg_cron job `pln-status-engine-every-1m` (currently scheduled `*/5 * * * *`) uses the literal host `pcvzfthpbytrydsvrhtg.supabase.co`. A restored/copy of this schedule must not be allowed to keep calling Cloud after cutover.
- The Android Gradle build gets `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` at build time. The URL in the old README was stale. This branch removes that stale endpoint and allows HTTPS custom domains in CI; it does not change the GitHub repository secrets or install an APK.

## 1. Read-only VPS preflight

Connect to the VPS in Termius. Never paste credentials or the contents of `.env` into chat or a public repository.

```bash
set -eu
cd /opt/supabase-project
printf '\n== Disk and memory ==\n'
df -h / /opt
free -h
printf '\n== Supabase services ==\n'
docker compose ps
printf '\n== Database version ==\n'
docker exec supabase-db psql -U postgres -d postgres -Atc 'select version();'
printf '\n== Local database counts ==\n'
docker exec supabase-db psql -U postgres -d postgres -c "
select 'auth.users' as table_name, count(*) from auth.users
union all select 'public.customers', count(*) from public.customers
union all select 'public.billings', count(*) from public.billings
union all select 'public.sync_jobs', count(*) from public.sync_jobs
union all select 'public.pitu_sync_retry_queue', count(*) from public.pitu_sync_retry_queue
union all select 'public.pitu_inquiry_audit', count(*) from public.pitu_inquiry_audit
union all select 'public.customer_import_staging_v2', count(*) from public.customer_import_staging_v2;"
printf '\n== Worker and Redis status ==\n'
cd /opt/bmax-worker
docker compose ps
```

If a container name differs, inspect `docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'` and adjust the read-only command. Confirm sufficient disk capacity and that local data can be safely replaced or promoted. Do **not** restore a Cloud dump over a target database that already contains valuable data until it has been backed up and the restore path has been tested on a separate, clean staging instance.

## 2. Make a protected Cloud backup

Obtain the database connection string from the Cloud project's **Connect** panel. Use the session pooler if direct connectivity is unavailable. Run on a trusted machine/VPS with Supabase CLI, Docker and `psql` installed. This uses a hidden prompt so the connection string is not typed into the shell command history.

```bash
set -euo pipefail
umask 077
BACKUP_DIR="/opt/bmax-migration/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
read -rsp 'Cloud PostgreSQL connection string: ' CLOUD_DB_URL
printf '\n'
export CLOUD_DB_URL

supabase db dump --db-url "$CLOUD_DB_URL" -f "$BACKUP_DIR/roles.sql" --role-only
supabase db dump --db-url "$CLOUD_DB_URL" -f "$BACKUP_DIR/schema.sql"
supabase db dump --db-url "$CLOUD_DB_URL" -f "$BACKUP_DIR/data.sql" --use-copy --data-only

unset CLOUD_DB_URL
chmod 600 "$BACKUP_DIR"/*.sql
sha256sum "$BACKUP_DIR"/*.sql | tee "$BACKUP_DIR/SHA256SUMS"
du -h "$BACKUP_DIR"/*.sql
echo "Backup bundle: $BACKUP_DIR"
```

Store the bundle off the live data volume if possible and verify the checksums. Treat these files as highly sensitive because they contain customer data and authentication records. Never commit them to GitHub or send them in chat.

## 3. Restore and test in an isolated clean Supabase stack first

Use a **separate staging instance**, not the production-facing local stack, until restore compatibility is proven. Self-hosted Supabase must be on PostgreSQL 17 where possible and have the required extensions installed. Hosted Auth/Storage/Realtime versions and extensions may differ.

After confirming the staging target is fresh and backed up, use its verified PostgreSQL connection string (do not assume the port or password):

```bash
set -euo pipefail
read -rsp 'Staging PostgreSQL connection string: ' TARGET_DB_URL
printf '\n'
psql --single-transaction --variable ON_ERROR_STOP=1 \\
  --file "$BACKUP_DIR/roles.sql" \\
  --file "$BACKUP_DIR/schema.sql" \\
  --command 'SET session_replication_role = replica' \\
  --file "$BACKUP_DIR/data.sql" \\
  --dbname "$TARGET_DB_URL"
unset TARGET_DB_URL
```

If any restore step fails, stop and resolve the version/schema/role error before trying to cut over. Do not blindly re-run against a partially populated database.

Compare source and restored table counts from the same freeze-time baseline, verify primary keys/foreign keys and expected extensions, and inspect RLS status:

```sql
select extname, extversion from pg_extension order by extname;
select schemaname, tablename, rowsecurity
from pg_tables
where schemaname = 'public'
order by tablename;
```

Test authentication using a test Biller account, per-biller customer search, Dashboard counts, billing statuses, Stand Meter, invoice totals, Admin reset-password functionality, and the role/RBM scope rules. Also test Realtime and Storage if the app depends on them.

## 4. Prepare every non-database dependency

Database restore alone does **not** finish a Supabase migration. Before any production switch:

1. Provision the self-hosted HTTPS domain and reverse proxy to the Supabase API gateway. Keep PostgreSQL and Redis ports private; expose only the intended HTTPS ingress. Test REST, Auth, Realtime, Storage and Functions through the public HTTPS endpoint.
2. Reconfigure self-hosted JWT/API keys. Use the **self-hosted publishable key** for Android, and the self-hosted service-role/secret key only in backend containers/functions. Do not reuse a Cloud service-role key in the APK.
3. Copy every Edge Function actually used by the app. At minimum, `pln-status-engine` is Cloud version 145. Configure its local environment with the self-hosted URL, self-hosted service-role key, and `PITU_API_KEY`. Edge Function source and secrets are not automatically carried by the database dump.
4. Transfer Storage objects/buckets separately if used. Configure SMTP/email, Auth site URL/redirect URLs, rate limits, Realtime publication settings, and any external webhook URLs.
5. Inspect local `cron.job` and all worker/scheduler settings. Replace any hard-coded hosted URL with the correct local internal/API endpoint. Confirm only one production PituCode scheduler will be active after cutover.
6. Verify the worker at `/opt/bmax-worker` against the actual config source. The last previously captured VPS state had the worker in `dry-run`; confirm this again locally. Keep dry-run until the restored database and function secrets have been verified, then change only the documented mode setting for the cutover.

## 5. Controlled production cutover

Schedule a maintenance window. Without logical replication, a final dump/restore requires a short write freeze so data created after the dump is not lost.

1. Announce a short write freeze; stop new import/write jobs and stop the hosted PituCode cron job. Verify the hosted cron job is inactive. Keep the Cloud project available for recovery; do not pause/delete it.
2. Record a final baseline of row counts and critical billing totals on Cloud; create the final protected roles/schema/data dump and restore it to the tested target. Re-run the count, RLS, extension, Auth and application checks.
3. Configure the Android build secrets in GitHub repository settings: `SUPABASE_URL=https://<your-self-hosted-HTTPS-domain>` and the matching self-hosted `SUPABASE_PUBLISHABLE_KEY`. This branch only adjusts CI validation; it does not change secrets automatically. Build and install a canary APK; users should expect to authenticate again because the self-hosted JWT secret differs.
4. Point backend clients and operational tooling to the self-hosted URL/keys. Configure the worker's database URL to the local Supabase API/database as required by its implementation. Keep its production mode disabled until its connection tests pass.
5. Enable the self-hosted Edge Function/cron/worker in a single-writer arrangement. Run one controlled batch, verify successful DB updates and no writes to Cloud, then watch logs, connections, locks, timeouts and retry queue before widening traffic.
6. End the write freeze only after the Android canary and worker tests pass. Keep the Cloud project intact but with its production scheduler disabled until the observation window has passed.

## 6. Rollback guardrail

Do not simply point clients back to Cloud after local writes have started: those new writes would be missing from Cloud. To roll back, first stop the self-hosted worker/scheduler, record local writes since cutover, reconcile or export them to Cloud, then switch client configuration back and re-enable exactly one Cloud scheduler. Keep both schedulers from querying PituCode at the same time.

## Current blocker

This repository branch prepares Android CI/docs for a custom HTTPS Supabase endpoint. Actual production cutover still requires a successful clean-staging restore and verification on the VPS, the public HTTPS hostname, updating GitHub repository secrets, and coordinated scheduler/worker switching. No traffic is switched by this runbook.
