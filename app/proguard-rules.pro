# Hilt / Dagger — preserve generated components and injectors
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class io.github.moxisuki.blockprint.cat.*_HiltComponents { *; }
-keep class io.github.moxisuki.blockprint.cat.*_MembersInjector { *; }
-keep class io.github.moxisuki.blockprint.cat.*_Factory { *; }
-keep class io.github.moxisuki.blockprint.cat.**_HiltModules$* { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room — preserve entities, DAOs, and database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class io.github.moxisuki.blockprint.cat.data.AppDatabase { *; }
# DAO _Impl classes are generated for every @Dao across these subpackages:
-keep class io.github.moxisuki.blockprint.cat.data.blueprint.*_Impl { *; }
-keep class io.github.moxisuki.blockprint.cat.data.bridge.*_Impl { *; }
-keep class io.github.moxisuki.blockprint.cat.data.community.*_Impl { *; }
-keep class io.github.moxisuki.blockprint.cat.data.render.*_Impl { *; }
-keep class io.github.moxisuki.blockprint.cat.data.vanilla.*_Impl { *; }
-keep class io.github.moxisuki.blockprint.cat.data.AppDatabase_Impl { *; }

# Coil
-keep class coil.** { *; }
-keep class coil.compose.** { *; }
-keep class * implements coil.ImageLoaderFactory { *; }

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# SceneView (Filament-based 3D)
-keep class io.github.sceneview.** { *; }
-keep class com.google.android.filament.** { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Kotlinx Serialization / Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Data classes — keep all fields for JSON/Hilt/Room
-keep class io.github.moxisuki.blockprint.cat.data.** { *; }
-keep class io.github.moxisuki.blockprint.cat.di.** { *; }
-keep class io.github.moxisuki.blockprint.cat.ui.** { *; }

# WebView — preserve JavaScript interface if used
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Bugly — keep all classes and warnings suppressed
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.** { *; }
