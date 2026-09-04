package com.paolo.gestionechiamate

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.telecom.Call
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InCallActivity : AppCompatActivity() {

    private val digitato = StringBuilder()
    private lateinit var txtDigitato: TextView
    private lateinit var txtNumero: TextView
    private lateinit var audioManager: AudioManager
    private var altoparlanteAttivo = false
    private var mutoAttivo = false

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            aggiornaStato(state)
            if (state == Call.STATE_DISCONNECTED) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incall)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        txtDigitato = findViewById(R.id.txtDigitato)
        txtNumero = findViewById(R.id.txtNumeroChiamante)
        val grid = findViewById<GridLayout>(R.id.gridTasti)
        val btnRiaggancia = findViewById<ImageButton>(R.id.btnRiaggancia)
        val btnAltoparlante = findViewById<ImageButton>(R.id.btnAltoparlante)
        val btnMuto = findViewById<ImageButton>(R.id.btnMuto)

        val call = MyInCallService.chiamataAttiva
        txtNumero.text = call?.details?.handle?.schemeSpecificPart ?: ""
        call?.registerCallback(callback)
        call?.let { aggiornaStato(it.state) }

        DialpadKeys.build(this, grid, keySizeDp = 60) { cifra ->
            digitato.append(cifra)
            txtDigitato.text = digitato.toString()
            val c = MyInCallService.chiamataAttiva
            c?.playDtmfTone(cifra[0])
            c?.stopDtmfTone()
        }

        altoparlanteAttivo = audioManager.isSpeakerphoneOn
        aggiornaAspettoAltoparlante(btnAltoparlante)
        btnAltoparlante.setOnClickListener {
            altoparlanteAttivo = !altoparlanteAttivo
            audioManager.isSpeakerphoneOn = altoparlanteAttivo
            aggiornaAspettoAltoparlante(btnAltoparlante)
        }

        aggiornaAspettoMuto(btnMuto)
        btnMuto.setOnClickListener {
            mutoAttivo = !mutoAttivo
            MyInCallService.istanza?.setMuted(mutoAttivo)
            aggiornaAspettoMuto(btnMuto)
        }

        btnRiaggancia.setOnClickListener {
            MyInCallService.chiamataAttiva?.disconnect()
            finish()
        }
    }

    private fun aggiornaAspettoAltoparlante(bottone: ImageButton) {
        bottone.setBackgroundResource(
            if (altoparlanteAttivo) R.drawable.bg_circle_call else R.drawable.bg_dialpad_key
        )
    }

    private fun aggiornaAspettoMuto(bottone: ImageButton) {
        bottone.setBackgroundResource(
            if (mutoAttivo) R.drawable.bg_circle_end else R.drawable.bg_dialpad_key
        )
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
