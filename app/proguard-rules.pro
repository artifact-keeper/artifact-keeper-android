# Artifact Keeper Android - ProGuard / R8 rules

# Keep kotlinx.serialization classes and their generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes and their generated serializers
-keep,includedescriptorclasses class com.artifactkeeper.android.data.models.**$$serializer { *; }
-keepclassmembers class com.artifactkeeper.android.data.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.artifactkeeper.android.data.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep SDK model classes
-keep,includedescriptorclasses class com.artifactkeeper.client.models.**$$serializer { *; }
-keepclassmembers class com.artifactkeeper.client.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.artifactkeeper.client.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Compose
-dontwarn androidx.compose.**

# ZXing
-keep class com.google.zxing.** { *; }
