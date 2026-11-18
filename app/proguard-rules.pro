# Proguard rules for Posiflow KP206 ESCPOS Thermal Printer
-keep class com.dantsu.escposprinter.** { *; }
-dontwarn com.dantsu.escposprinter.**

# Supabase and Ktor
-keep class io.github.jan.supabase.** { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Room
-keep class androidx.room.** { *; }

