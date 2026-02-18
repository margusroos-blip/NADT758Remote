# NAD Remote ProGuard Rules

# Keep data classes used for JSON/XML parsing
-keep class ee.salva.nadremote.NadAvrState { *; }
-keep class ee.salva.nadremote.BluOsState { *; }
-keep class ee.salva.nadremote.BluOsTrack { *; }
-keep class ee.salva.nadremote.FoundDevice { *; }
-keep class ee.salva.nadremote.Preset { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coil
-keep class coil.** { *; }

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# XML Pull Parser
-keep class org.xmlpull.** { *; }
-keep interface org.xmlpull.** { *; }
