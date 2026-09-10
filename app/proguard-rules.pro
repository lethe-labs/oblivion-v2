# Everything below is bound by class name from AndroidManifest.xml. R8 has no
# way to see those references, so without these rules the release build strips
# or renames them and every trigger silently stops firing -- with no crash and
# no log to point at it. Debug builds are unaffected, which makes it worse.
-keep class oblivion.v2.core.admin.DeviceAdminReceiver { *; }

-keep class oblivion.v2.core.guard.GuardAccessibilityService { *; }
-keep class oblivion.v2.core.usb.UsbKillService { *; }
-keep class oblivion.v2.core.usb.UsbKillBootReceiver { *; }
-keep class oblivion.v2.core.voice.VoiceKillService { *; }
-keep class oblivion.v2.core.voice.VoiceKillBootReceiver { *; }
-keep class oblivion.v2.core.sms.SmsKillReceiver { *; }

# Vosk reaches its native libraries through JNA, which resolves classes and
# fields reflectively at runtime.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn com.sun.jna.**

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Second line of defence behind SecLog: strips debug/verbose logging from the
# release binary even if a raw Log call slips in. Forensic value of an empty
# logcat is the whole point.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
