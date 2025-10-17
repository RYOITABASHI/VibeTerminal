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
