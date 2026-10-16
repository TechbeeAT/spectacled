# ProGuard rules for androidJournalsApp

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

# Glance instantiates widget ActionCallbacks reflectively by class name
# (RunCallbackAction.run → getDeclaredConstructor().newInstance()). R8 full mode
# otherwise strips their unused no-arg constructor and onAction, so the callback
# fails silently (e.g. the widget checkbox never persists the task progress).
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
    *** onAction(...);
}
