# WatchPilot v2.0.0 R8 / ProGuard Commercial Release Rules
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi DTOs & Generated Adapters
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }
-keep class com.example.data.remote.tmdb.** { *; }

# Room Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }

# Firebase Firestore & AI Logic Models
-keep class com.example.data.remote.firebase.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.google.firebase.** { *; }

# Google Play Billing & AdMob
-keep class com.android.vending.billing.** { *; }
-keep class com.google.android.gms.ads.** { *; }
