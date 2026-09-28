package com.paolo.gestionechiamate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "sms_ricevuti"
        private const val CHANNEL_NAME = "Messaggi SMS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messaggi = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messaggi.isEmpty()) return

        val mittente = messaggi[0].originatingAddress ?: "Numero sconosciuto"
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

        mostraNotificaSms(context, mittente, corpo)
    }

    private fun mostraNotificaSms(context: Context, mittente: String, corpo: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val suoneriaSms = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val canale = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per i nuovi SMS ricevuti"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(suoneriaSms, audioAttributes)
            }

            notificationManager.createNotificationChannel(canale)
        }

        val apriApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            apriApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifica = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(mittente)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(suoneriaSms)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notifica)
        } catch (_: SecurityException) {
            // Android 13+: nessuna notifica se POST_NOTIFICATIONS non e' autorizzato.
        }
    }
}
