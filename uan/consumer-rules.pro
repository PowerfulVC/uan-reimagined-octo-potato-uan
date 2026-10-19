# Ad unit config is deserialized with Gson by field name; keep it intact in minified host apps.
-keep class uan.mod.configs.AdUnit { *; }
-keepattributes Signature, *Annotation*
