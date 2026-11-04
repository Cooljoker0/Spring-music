# ProGuard & R8 Optimization Rules for Spring Music

# Basic keep attributes for reflection, annotations, and stack traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.**

# Keep Data Models & DTOs for Moshi serialization and reflection
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep Room Database and DAOs
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep Retrofit and OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*,Signature
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Firebase & Play Services Credentials
-keepclassmembers class * {
    @com.google.firebase.firestore.* *;
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep AndroidX Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Jetpack Compose runtime
-keep class androidx.compose.** { *; }

# Strip logging/debug messages in release APK
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
