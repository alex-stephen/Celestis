# Keep app entry points referenced from AndroidManifest.xml.
-keep class com.alexstephen.celestis80085.CelestisApp { *; }
-keep class com.alexstephen.celestis80085.MainActivity { *; }
-keep class com.alexstephen.celestis80085.notifications.CelestisFcmService { *; }
-keep class com.alexstephen.celestis80085.notifications.ApodNotificationReceiver { *; }
-keep class com.alexstephen.celestis80085.widget.ApodWidgetReceiver { *; }
-keep class com.alexstephen.celestis80085.widget.ApodWidget { *; }

# Preserve Kotlinx Serialization generated serializers and metadata used by Ktor.
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
