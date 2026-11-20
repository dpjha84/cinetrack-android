# WatchPilot Commercial Release R8 Optimization & Minimal Keep Rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi Codegen (all TMDB DTOs use @JsonClass(generateAdapter = true) with KSP adapters)
-keep,allowobfuscation @com.squareup.moshi.JsonClass class *
-keepclassmembers class * extends com.squareup.moshi.JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-keepclassmembers,allowobfuscation class * {
    @com.squareup.moshi.Json <fields>;
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Strip verbose/debug/info logs from release DEX to shrink bytecode and reduce allocations
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
