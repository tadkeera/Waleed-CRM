# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK.

-keep class com.pharmacomm.crm.domain.model.** { *; }
-keep class com.pharmacomm.crm.data.local.** { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }

# Keep for Compose
-keep class androidx.compose.** { *; }

# For WhatsApp intents
-keepclassmembers class * {
    public static void main(java.lang.String[]);
}

# Remove debug logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}