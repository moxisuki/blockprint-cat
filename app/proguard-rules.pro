# Hilt / Dagger — preserve generated components and injectors
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class com.github.moxisuki.litematiccat.*_HiltComponents { *; }
-keep class com.github.moxisuki.litematiccat.*_MembersInjector { *; }
-keep class com.github.moxisuki.litematiccat.*_Factory { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room — preserve entities, DAOs, and database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.github.moxisuki.litematiccat.data.BlueprintEntity { *; }
-keep class com.github.moxisuki.litematiccat.data.AppDatabase { *; }
-keep class com.github.moxisuki.litematiccat.data.BlueprintDao { *; }

# Room — preserve generated impls
-keep class com.github.moxisuki.litematiccat.data.AppDatabase_Impl { *; }
-keep class com.github.moxisuki.litematiccat.data.BlueprintDao_Impl { *; }

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
-keep class com.github.moxisuki.litematiccat.data.** { *; }
-keep class com.github.moxisuki.litematiccat.data.bridge.** { *; }
-keep class com.github.moxisuki.litematiccat.data.community.** { *; }
-keep class com.github.moxisuki.litematiccat.data.repository.** { *; }
-keep class com.github.moxisuki.litematiccat.di.** { *; }
-keep class com.github.moxisuki.litematiccat.ui.** { *; }

# WebView — preserve JavaScript interface if used
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
