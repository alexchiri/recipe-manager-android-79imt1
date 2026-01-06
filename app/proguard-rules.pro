# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.kroslabs.recipemanager.**$$serializer { *; }
-keepclassmembers class com.kroslabs.recipemanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.kroslabs.recipemanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}
