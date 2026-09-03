package com.paolo.gestionechiamate

import android.os.Bundle
import android.telecom.Call
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Schermata mostrata durante una chiamata reale (quando l'app è impostata come
 * dialer predefinito). Il tastierino DTMF è sempre visibile e utilizzabile
 * per tutta la durata della chiamata, insieme al pulsante per riagganciare.
 */
class InCallActivity : AppCompatActivity() {

    private val digitato = StringBuilder()
    private lateinit var txtDigitato: TextView
    private lateinit var txtNumero: TextView

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            aggiornaStato(state)
            if (state == Call.STATE_DISCONNECTED) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incall)

        txtDigitato = findViewById(R.id.txtDigitato)
        txtNumero = findViewById(R.id.txtNumeroChiamante)
        val grid = findViewById<GridLayout>(R.id.gridTasti)
        val btnRiaggancia = findViewById<ImageButton>(R.id.btnRiaggancia)

        val call = MyInCallService.chiamataAttiva
        txtNumero.text = call?.details?.handle?.schemeSpecificPart ?: ""
        call?.registerCallback(callback)
        call?.let { aggiornaStato(it.state) }

        // Il tastierino resta sempre attivo: ogni tocco invia un tono DTMF reale
        // sulla chiamata in corso, oltre a mostrarlo sul display.
        DialpadKeys.build(this, grid, keySizeDp = 68) { cifra ->
            digitato.append(cifra)
            txtDigitato.text = digitato.toString()
            val c = MyInCallService.chiamataAttiva
            c?.playDtmfTone(cifra[0])
            c?.stopDtmfTone()
        }

        btnRiaggancia.setOnClickListener {
            MyInCallService.chiamataAttiva?.disconnect()
            finish()
        }
    }

    private fun aggiornaStato(state: Int) {
        val txtStato = findViewById<TextView>(R.id.txtStato)
        txtStato.text = when (state) {
            Call.STATE_DIALING -> "Chiamata in corso…"
            Call.STATE_RINGING -> "Chiamata in arrivo…"
            Call.STATE_ACTIVE -> "In chiamata"
            Call.STATE_HOLDING -> "In attesa"
            else -> getString(R.string.chiamata_in_corso)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MyInCallService.chiamataAttiva?.unregisterCallback(callback)
    }
}
