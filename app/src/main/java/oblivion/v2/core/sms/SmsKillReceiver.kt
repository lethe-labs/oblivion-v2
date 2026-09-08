package oblivion.v2.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import oblivion.v2.core.log.SecLog
import dagger.hilt.android.AndroidEntryPoint
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@AndroidEntryPoint
class SmsKillReceiver : BroadcastReceiver() {
    @Inject lateinit var configStore: SmsKillConfigStore
    @Inject lateinit var wipeGateway: WipeGateway

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val cfg = configStore.load()
        if (!cfg.isReady()) {
            SecLog.d(TAG, "SMS received but trigger not ready → ignore")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        for (sms in messages) {
            val sender = SmsMatcher.normalize(sms.displayOriginatingAddress ?: continue)
            val body = sms.displayMessageBody ?: continue

            SecLog.d(TAG, "SMS from=$sender body_len=${body.length}")

            if (!SmsMatcher.senderMatches(sender, cfg.senderNumber)) {
                SecLog.d(TAG, "sender mismatch → skip")
                continue
            }

            if (SmsMatcher.keywordMatches(body, cfg.keyword)) {
                SecLog.d(TAG, "KEYWORD MATCH from $sender → WIPE")
                val result = wipeGateway.wipeNow()
                SecLog.d(TAG, "wipeNow() result=$result")
                return
            } else {
                SecLog.d(TAG, "keyword not found in body → skip")
            }
        }
    }

    private companion object {
        private const val TAG = "SmsKillReceiver"
    }
}
