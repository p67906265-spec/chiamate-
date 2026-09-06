package com.paolo.gestionechiamate

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.Call
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InCallActivity : AppCompatActivity() {

    private val digitato = StringBuilder()
    private lateinit var txtDigitato: TextView
    private lateinit var txtNumero: TextView
    private lateinit var audioManager: AudioManager
    private var altoparlanteAttivo = false
    private var mutoAttivo = false
    private var attesaAttiva = false
    private var tastierinoVisibile = false
    private var numeroChiamante: String = ""
    private val scope = CoroutineScope(Dispatchers.Main)

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
        val btnTelefono = findViewById<ImageButton>(R.id.btnTelefono)
        val btnMuto = findViewById<ImageButton>(R.id.btnMuto)
        val btnAttesa = findViewById<ImageButton>(R.id.btnAttesa)
        val btnTastierino = findViewById<ImageButton>(R.id.btnTastierino)
        val btnAltro = findViewById<ImageButton>(R.id.btnAltro)

        val call = MyInCallService.chiamataAttiva
        numeroChiamante = call?.details?.handle?.schemeSpecificPart ?: ""
        txtNumero.text = numeroChiamante
        call?.registerCallback(callback)
        call?.let { aggiornaStato(it.state) }

        if (numeroChiamante.isNotBlank()) {
            scope.launch {
                val nome = withContext(Dispatchers.IO) { cercaNomeInRubrica(numeroChiamante) }
                if (nome != null) {
                    txtNumero.text = nome
                }
            }
        }

        DialpadKeys.build(this, grid, keySizeDp = 52, stileScuro = true) { cifra ->
            digitato.append(cifra)
            txtDigitato.text = digitato.toString()
            val c = MyInCallService.chiamataAttiva
            c?.playDtmfTone(cifra[0])
            c?.stopDtmfTone()
        }

        btnTastierino.setOnClickListener {
            tastierinoVisibile = !tastierinoVisibile
            grid.visibility = if (tastierinoVisibile) View.VISIBLE else View.GONE
        }

        altoparlanteAttivo = audioManager.isSpeakerphoneOn
        aggiornaAspettoTelefono(btnTelefono)
        btnTelefono.setOnClickListener {
            mostraMenuAudio(btnTelefono)
        }

        aggiornaAspettoMuto(btnMuto)
        btnMuto.setOnClickListener {
            mutoAttivo = !mutoAttivo
            MyInCallService.istanza?.setMuted(mutoAttivo)
            aggiornaAspettoMuto(btnMuto)
        }

        aggiornaAspettoAttesa(btnAttesa)
        btnAttesa.setOnClickListener {
            attesaAttiva = !attesaAttiva
            val c = MyInCallService.chiamataAttiva
            if (attesaAttiva) c?.hold() else c?.unhold()
            aggiornaAspettoAttesa(btnAttesa)
        }

        btnAltro.setOnClickListener {
            mostraMenuAltro(btnAltro)
        }

        btnRiaggancia.setOnClickListener {
            MyInCallService.chiamataAttiva?.disconnect()
            finish()
        }
    }

    private fun mostraMenuAudio(ancora: View) {
        val popup = PopupMenu(this, ancora)
        popup.menu.add(0, 1, 0, "Telefono")
        popup.menu.add(0, 2, 1, "Vivavoce")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    altoparlanteAttivo = false
                    audioManager.isSpeakerphoneOn = false
                }
                2 -> {
                    altoparlanteAttivo = true
                    audioManager.isSpeakerphoneOn = true
                }
            }
            aggiornaAspettoTelefono(findViewById(R.id.btnTelefono))
            true
        }
        popup.show()
    }

    private fun mostraMenuAltro(ancora: View) {
        val popup = PopupMenu(this, ancora)
        popup.menu.add(0, 1, 0, "Messaggio")
        popup.menu.add(0, 2, 1, "Aggiungi chiamata")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> apriMessaggio()
                2 -> apriAggiungiChiamata()
            }
            true
        }
        popup.show()
    }

    private fun apriMessaggio() {
        if (numeroChiamante.isNotBlank()) {
            val intent = Intent(this, ComponiSmsActivity::class.java)
            intent.putExtra(ComponiSmsActivity.EXTRA_NUMERO, numeroChiamante)
            startActivity(intent)
        }
    }

    private fun apriAggiungiChiamata() {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun cercaNomeInRubrica(numero: String): String? {
        val cifreNumero = soloCifre(numero)
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )
        cursor?.use {
            val idxNome = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val idxNum = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val numeroRubrica = if (idxNum >= 0) it.getString(idxNum) else null
                if (numeroRubrica != null && soloCifre(numeroRubrica) == cifreNumero) {
                    return if (idxNome >= 0) it.getString(idxNome) else null
                }
            }
        }
        return null
    }

    private fun soloCifre(numero: String): String {
        var cifre = numero.filter { it.isDigit() }
        if (cifre.length > 10) cifre = cifre.takeLast(10)
        return cifre
    }

    private fun aggiornaAspettoTelefono(bottone: ImageButton) {
        bottone.setBackgroundResource(
            if (altoparlanteAttivo) R.drawable.bg_circle_end else R.drawable.bg_circle_call
        )
    }

    private fun aggiornaAspettoMuto(bottone: ImageButton) {
        bottone.setBackgroundResource(
            if (mutoAttivo) R.drawable.bg_circle_end else R.drawable.bg_dialpad_key
        )
    }

    private fun aggiornaAspettoAttesa(bottone: ImageButton) {
        bottone.setBackgroundResource(
            if (attesaAttiva) R.drawable.bg_circle_call else R.drawable.bg_dialpad_key
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
