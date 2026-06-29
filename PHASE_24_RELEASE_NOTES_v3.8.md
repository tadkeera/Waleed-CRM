# Waleed CRM v3.8 — Phase 24

## Room stabilization and SQLite legacy decommissioning

- Confirmed that active application reads/writes now go through Room DAOs via `CrmRepository`.
- Kept `SQLiteOpenHelper` isolated to one-time legacy import only when an old `waleed_crm.db` file exists and migration has not completed.
- Added migration status metadata in SharedPreferences so the legacy database is not reopened after migration completion.

## Migration integrity verification

- Added table-count verification after SQLite-to-Room import.
- The integrity check compares core tables including clients, catalogs, pharmacies, gallery files, message templates/logs/campaigns, follow-ups, users, and saved segments.
- Migration success, mismatch, failure, or fresh Room seed is written into the audit log.

## Organized future Room migrations

- Moved official Room migrations into `RoomMigrations.kt`.
- Added a central `RoomMigrations.ALL` list used by the Room builder.
- Re-enabled Room schema export and added schema output configuration.

## Dashboard DAO optimization

- Added `DashboardDao` with optimized SQL queries for dashboard counters, grouped stats, campaign totals, recent campaigns, contacted/uncontacted doctors, and top contacted doctors.
- Replaced in-memory dashboard analytics calculations with DAO-backed queries to scale better with large databases.

## Instrumented tests

- Added migration coverage for the official `MIGRATION_1_2` path.
- Kept broad Room DAO instrumented coverage for Phase 23/24 tables.
- Confirmed Android test APK compiles successfully.

## Navigation drawer fix

- Fixed side-menu pages not appearing on smaller screens by making the drawer content vertically scrollable.

## Build

- Version code: `29`
- Version name: `3.8`
- APK: `Waleed-CRM.apk`
