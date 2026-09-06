package com.paolo.gestionechiamate

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager

class RespondViaMessageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val numero = intent.data?.schemeSpecificPart
            val messaggio = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!numero.isNullOrBlank() && !messaggio.isNullOrBlank()) {
                inviaSms(numero, messaggio)
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun inviaSms(numero: String, messaggio: String) {
        try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(numero, null, messaggio, null, null)

            val valori = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, numero)
                put(Telephony.Sms.BODY, messaggio)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, valori)
        } catch (e: Exception) {
        }
    }
}
