# Waleed CRM v3.7 — Phase 23

## Full SQLite to Room migration

- Shifted the compatibility facade (`CrmRepository`) so active screen reads/writes now go through Room DAO APIs instead of direct `SQLiteOpenHelper` CRUD.
- Added one-time safe migration from the legacy `SQLiteOpenHelper` database (`waleed_crm.db`) into the Room database (`waleed_crm_room.db`).
- Fresh installs seed Room directly when no legacy SQLite database exists.
- Retained `SQLiteOpenHelper` only as a legacy import source for existing installations, not as the active application database layer.

## Room schema expansion

Room coverage now includes:

- Clients
- Specializations
- Locations
- Pharmacies
- Gallery files
- Message templates
- Message logs
- Message campaigns
- Follow ups
- Users
- Audit logs
- Saved segments

## Official Room migration

- Removed `fallbackToDestructiveMigration()`.
- Added an explicit Room `Migration(1, 2)` for the newly covered legacy tables.

## Tests

- Added Room instrumented tests for DAO read/write/search coverage.
- Added broad Phase 23 instrumented coverage across the migrated tables.
- Release unit tests compile and pass.

## Build

- Version code: `28`
- Version name: `3.7`
- APK: `Waleed-CRM.apk`
