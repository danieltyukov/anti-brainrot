# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class io.github.danieltyukov.antibrainrot.**$$serializer { *; }
-keepclassmembers class io.github.danieltyukov.antibrainrot.** { *** Companion; }
-keepclasseswithmembers class io.github.danieltyukov.antibrainrot.** { kotlinx.serialization.KSerializer serializer(...); }
