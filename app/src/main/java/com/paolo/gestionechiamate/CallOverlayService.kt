package com.paolo.gestionechiamate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * Mostra un piccolo tastierino flottante sopra le altre app mentre una chiamata
 * è in corso. Serve come soluzione di riserva quando l'app non è impostata come
 * Telefono predefinito (in quel caso è InCallActivity a gestire il tastierino).
 * Se una Call di sistema è disponibile (MyInCallService attivo), i tasti inviano
 * anche il tono DTMF reale; altrimenti il tastierino resta comunque a portata di
 * mano per consultare rapidamente i numeri da digitare.
 */
class CallOverlayService : Service() {

    companion object {
        const val AZIONE_MOSTRA = "mostra"
        const val AZIONE_NASCONDI = "nascondi"
        private const val CANALE_ID = "overlay_chiamata"
        private const val NOTIFICA_ID = 501
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val digitato = StringBuilder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AZIONE_MOSTRA -> {
                avviaComeForeground()
                mostraOverlay()
            }
            AZIONE_NASCONDI -> {
                rimuoviOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun avviaComeForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canale = NotificationChannel(
                CANALE_ID, "Tastierino durante la chiamata",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(canale)
        }
        val notifica: Notification = NotificationCompat.Builder(this, CANALE_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.chiamata_in_corso))
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICA_ID, notifica)
    }

    private fun mostraOverlay() {
        if (overlayView != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_dialpad, null)
        val txtDigitato = view.findViewById<TextView>(R.id.txtDigitatoOverlay)
        val grid = view.findViewById<GridLayout>(R.id.gridTastiOverlay)
        val btnChiudi = view.findViewById<ImageButton>(R.id.btnChiudiOverlay)

        DialpadKeys.build(this, grid, keySizeDp = 48, textSizeSp = 16f) { cifra ->
            digitato.append(cifra)
            txtDigitato.text = digitato.toString()
            val call = MyInCallService.chiamataAttiva
            call?.playDtmfTone(cifra[0])
            call?.stopDtmfTone()
        }

        btnChiudi.setOnClickListener { rimuoviOverlay() }

        val tipoFinestra =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoFinestra,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 24
        params.y = 200

        windowManager?.addView(view, params)
        overlayView = view
    }

    private fun rimuoviOverlay() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        digitato.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        rimuoviOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
