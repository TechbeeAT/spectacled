# ProGuard rules for androidNotesApp

# Since this is an open-source project, we disable obfuscation to keep
# stack traces readable and the code structure intact.
-dontobfuscate

# Optimization is still enabled via the default proguard-android-optimize.txt
# Shrinking is enabled via isMinifyEnabled = true

# Koin rules (if needed for reflection-based injection)
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}

# Keep Compose-specific metadata
-keepattributes Signature,Annotation,InnerClasses,EnclosingMethod

# Keep Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
