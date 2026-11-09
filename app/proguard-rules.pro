# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep media3 metadata classes to allow reflection for ReplayGain
-keep class androidx.media3.extractor.metadata.** { *; }

# Room
-dontwarn androidx.room.**

# jaudiotagger (tag reading). Its classes build logger names from getPackage().getName() and
# pick readers by class, so R8 must not move or rename them: once repackaged, getPackage()
# returns null and reading the first WAV/AIFF file crashes the app.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# Coil
-keep class coil.** { *; }

-keepattributes Signature
-keepattributes *Annotation*

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX / Media3 consumer rules handle media3 internals safely

# App Models (Ensure they aren't obfuscated out of Moshi/Room reflection)
-keep class com.example.data.** { *; }

