package oblivion.v2.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import oblivion.v2.core.deadman.DeadmanConfigStore
import oblivion.v2.core.decoy.DecoyConfigStore
import oblivion.v2.core.guard.GuardConfigStore
import oblivion.v2.core.prefs.SecurePrefs
import oblivion.v2.core.scheduled.ScheduledWipeStore
import oblivion.v2.core.sms.SmsKillConfigStore
import oblivion.v2.core.usb.UsbKillConfigStore
import oblivion.v2.core.voice.VoiceKillConfigStore
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideSecurePrefs(@ApplicationContext context: Context): SecurePrefs =
        SecurePrefs.create(context)

    @Provides
    @Singleton
    fun provideGuardConfigStore(securePrefs: SecurePrefs): GuardConfigStore =
        GuardConfigStore(securePrefs)

    @Provides
    @Singleton
    fun provideUsbKillConfigStore(securePrefs: SecurePrefs): UsbKillConfigStore =
        UsbKillConfigStore(securePrefs)

    @Provides
    @Singleton
    fun provideVoiceKillConfigStore(securePrefs: SecurePrefs): VoiceKillConfigStore =
        VoiceKillConfigStore(securePrefs)

    @Provides
    @Singleton
    fun provideSmsKillConfigStore(securePrefs: SecurePrefs): SmsKillConfigStore =
        SmsKillConfigStore(securePrefs)

    @Provides
    @Singleton
    fun provideWipeGateway(@ApplicationContext context: Context): WipeGateway =
        WipeGateway(context)

    @Provides
    @Singleton
    fun provideDeadmanConfigStore(securePrefs: SecurePrefs): DeadmanConfigStore =
        DeadmanConfigStore(securePrefs)

    @Provides
    @Singleton
    fun provideScheduledWipeStore(securePrefs: SecurePrefs): ScheduledWipeStore =
        ScheduledWipeStore(securePrefs)

    @Provides
    @Singleton
    fun provideDecoyConfigStore(securePrefs: SecurePrefs): DecoyConfigStore =
        DecoyConfigStore(securePrefs)
}
