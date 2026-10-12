# Kuromoji (Japanese readings for the lyrics) loads its dictionary and builders by name.
-keep class com.atilika.kuromoji.** { *; }
-dontwarn com.atilika.kuromoji.**
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
