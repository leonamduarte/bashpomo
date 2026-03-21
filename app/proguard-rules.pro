# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in $ANDROID_HOME/tools/proguard/proguard-android.txt

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class dev.bashln.bashpomo.data.db.entity.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.bashln.bashpomo.data.model.**$$serializer { *; }
-keepclassmembers class dev.bashln.bashpomo.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class dev.bashln.bashpomo.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
