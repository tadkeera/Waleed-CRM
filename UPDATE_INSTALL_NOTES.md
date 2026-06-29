# Waleed CRM update signing notes

Starting from version 2.1, debug and release APKs are signed with a stable project keystore: `app/waleed-crm-update.keystore`.
This keeps the Android package signature consistent for future in-place updates.

Important: Android only allows updating an installed app when the new APK has the same `applicationId` and signing certificate, and a higher `versionCode`.
Version 2.1 uses:
- applicationId: `com.waleed.crm`
- versionCode: 12
- versionName: 2.1

If a device already has an older APK that was signed with a different temporary debug key, Android may still require uninstalling that old copy once. After installing v2.1, future versions signed with this keystore should update normally without deleting app data.
