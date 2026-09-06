package com.paolo.gestionechiamate

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messaggi = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messaggi.isEmpty()) return

        val mittente = messaggi[0].originatingAddress ?: ""
        val corpo = messaggi.joinToString(separator = "") { it.messageBody ?: "" }
        val data = messaggi[0].timestampMillis

        val valori = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, mittente)
            put(Telephony.Sms.BODY, corpo)
            put(Telephony.Sms.DATE, data)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, valori)
    }
}
