package com.paolo.gestionechiamate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat

object AutoRichiamo {
    private const val CHANNEL_ID = "richiamo_automatico"
    private const val NOTIFICATION_ID = 117
    private const val ATTESA_SECONDI = 10
    private const val MAX_TENTATIVI = 5

    private val handler = Handler(Looper.getMainLooper())
    private var numero: String? = null
    private var tentativo = 0
    private var operazione: Runnable? = null

    @Synchronized
    fun numeroOccupato(context: Context, nuovoNumero: String) {
        if (!Impostazioni.isRichiamoAutomatico(context) || nuovoNumero.isBlank()) return
        if (numero != nuovoNumero) {
            annullaInterno(context, cancellaNotifica = true)
            numero = nuovoNumero
            tentativo = 0
        }
        if (tentativo >= MAX_TENTATIVI) {
            mostraNotifica(context, "Richiamo terminato", "Raggiunti 5 tentativi senza risposta")
            numero = null
            return
        }
        tentativo++
        pianifica(context.applicationContext, ATTESA_SECONDI)
    }

    @Synchronized
    fun chiamataTerminataNonOccupata(context: Context, numeroTerminato: String) {
        if (numero == numeroTerminato) annullaInterno(context, cancellaNotifica = true)
    }

    @Synchronized
    fun chiamataConnessa(context: Context) {
        annullaInterno(context, cancellaNotifica = true)
    }

    @Synchronized
    fun annulla(context: Context) {
        annullaInterno(context, cancellaNotifica = true)
    }

    private fun pianifica(context: Context, secondi: Int) {
        operazione?.let(handler::removeCallbacks)
        val corrente = numero ?: return
        var rimasti = secondi
        val runnable = object : Runnable {
            override fun run() {
                if (numero != corrente) return
                if (rimasti <= 0) {
                    mostraNotifica(context, "Richiamo in corso", "Tentativo $tentativo di $MAX_TENTATIVI")
                    operazione = null
                    ChiamaHelper.richiamoAutomatico(context, corrente)
                    return
                }
                mostraNotifica(
                    context,
                    "Numero occupato",
                    "Richiamo $tentativo di $MAX_TENTATIVI tra $rimasti secondi"
                )
                rimasti--
                handler.postDelayed(this, 1000)
            }
        }
        operazione = runnable
        handler.post(runnable)
    }

    private fun mostraNotifica(context: Context, titolo: String, testo: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Richiamo automatico", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val annullaIntent = Intent(context, AnnullaRichiamoReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 117, annullaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notifica = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(titolo)
            .setContentText(testo)
            .setOnlyAlertOnce(true)
            .setOngoing(operazione != null)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Annulla", pending)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notifica)
        } catch (_: SecurityException) {
        }
    }

    private fun annullaInterno(context: Context, cancellaNotifica: Boolean) {
        operazione?.let(handler::removeCallbacks)
        operazione = null
        numero = null
        tentativo = 0
        if (cancellaNotifica) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(NOTIFICATION_ID)
        }
    }
}

class AnnullaRichiamoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AutoRichiamo.annulla(context)
    }
}
