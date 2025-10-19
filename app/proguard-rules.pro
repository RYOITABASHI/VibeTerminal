# Add project specific ProGuard rules here.

# Keep translation data classes
-keep class com.vibeterminal.core.translator.** { *; }

# Keep Compose classes
-dontwarn androidx.compose.**

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.vibeterminal.**$$serializer { *; }
-keepclassmembers class com.vibeterminal.** {
    *** Companion;
}
-keepclasseswithmembers class com.vibeterminal.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gemini AI SDK
-keep class com.google.ai.client.generativeai.** { *; }
-keepclassmembers class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Termux Libraries
-keep class com.termux.** { *; }
-keepclassmembers class com.termux.** { *; }
-dontwarn com.termux.**

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}
