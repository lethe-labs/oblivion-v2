-keep class oblivion.v2.core.admin.DeviceAdminReceiver { *; }

-keep class oblivion.v2.core.guard.GuardAccessibilityService { *; }
-keep class oblivion.v2.core.usb.UsbKillService { *; }
-keep class oblivion.v2.core.usb.UsbKillBootReceiver { *; }
-keep class oblivion.v2.core.voice.VoiceKillService { *; }
-keep class oblivion.v2.core.voice.VoiceKillBootReceiver { *; }
-keep class oblivion.v2.core.sms.SmsKillReceiver { *; }

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

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
