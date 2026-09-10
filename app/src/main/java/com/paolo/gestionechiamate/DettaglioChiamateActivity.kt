package com.paolo.gestionechiamate

import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DettaglioChiamateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO = "numero"
        const val EXTRA_NOME = "nome"
    }

    private var numero: String = ""

    private val scegliFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && numero.isNotBlank()) {
            if (FotoNumeroManager.salvaFoto(this, numero, uri) != null) {
                mostraFotoAssociata()
                Toast.makeText(this, "Foto associata al numero", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Impossibile salvare la foto", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_chiamate)

        numero = intent.getStringExtra(EXTRA_NUMERO) ?: ""
        val nomeIniziale = intent.getStringExtra(EXTRA_NOME)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val txtIniziale = findViewById<TextView>(R.id.txtIniziale)
        val txtNome = findViewById<TextView>(R.id.txtNome)
        val txtNumero = findViewById<TextView>(R.id.txtNumero)
        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)

        txtNumero.text = numero
        txtNome.text = nomeIniziale ?: numero
        txtIniziale.text = (nomeIniziale ?: numero).take(1).uppercase()
        mostraFotoAssociata()

        findViewById<View>(R.id.contenitoreFotoNumero).setOnClickListener {
            gestisciFoto(it)
        }

        findViewById<ImageButton>(R.id.btnChiama).setOnClickListener {
            ChiamaHelper.chiama(this, numero)
        }

        lifecycleScope.launch {
            val nomeRubrica = withContext(Dispatchers.IO) { cercaNomeInRubrica(numero) }
            if (nomeRubrica != null) {
                txtNome.text = nomeRubrica
                txtIniziale.text = nomeRubrica.take(1).uppercase()
            }

            val storico = withContext(Dispatchers.IO) { caricaStorico(numero) }
            recycler.adapter = DettaglioChiamataAdapter(storico)
        }
    }

    private fun mostraFotoAssociata() {
        val immagine = findViewById<ImageView>(R.id.imgFotoNumero)
        val iniziale = findViewById<TextView>(R.id.txtIniziale)
        val uri = FotoNumeroManager.getFotoUri(this, numero)
        if (uri == null) {
            immagine.setImageDrawable(null)
            immagine.visibility = View.GONE
            iniziale.visibility = View.VISIBLE
            return
        }
        try {
            immagine.setImageURI(uri)
            immagine.visibility = View.VISIBLE
            iniziale.visibility = View.GONE
        } catch (_: Exception) {
            immagine.visibility = View.GONE
            iniziale.visibility = View.VISIBLE
        }
    }

    private fun gestisciFoto(ancora: View) {
        if (FotoNumeroManager.getFotoUri(this, numero) == null) {
            scegliFoto.launch("image/*")
            return
        }
        PopupMenu(this, ancora).apply {
            menu.add(0, 1, 0, "Cambia foto dalla galleria")
            menu.add(0, 2, 1, "Rimuovi foto")
            setOnMenuItemClickListener {
                if (it.itemId == 1) scegliFoto.launch("image/*")
                else {
                    FotoNumeroManager.rimuoviFoto(this@DettaglioChiamateActivity, numero)
                    mostraFotoAssociata()
                }
                true
            }
        }.show()
    }

    private fun soloCifre(numero: String): String {
        var cifre = numero.filter { it.isDigit() }
        if (cifre.length > 10) cifre = cifre.takeLast(10)
        return cifre
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

    private fun caricaStorico(numero: String): List<VoceStorico> {
        val cifreNumero = soloCifre(numero)
        val lista = mutableListOf<VoceStorico>()
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE),
            null, null,
            CallLog.Calls.DATE + " DESC"
        )
        val formatoGiornoOra = SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALY)
        cursor?.use {
            val idxNum = it.getColumnIndex(CallLog.Calls.NUMBER)
            val idxData = it.getColumnIndex(CallLog.Calls.DATE)
            val idxDurata = it.getColumnIndex(CallLog.Calls.DURATION)
            val idxTipo = it.getColumnIndex(CallLog.Calls.TYPE)
            while (it.moveToNext()) {
                val numeroVoce = if (idxNum >= 0) it.getString(idxNum) else null
                if (numeroVoce == null || soloCifre(numeroVoce) != cifreNumero) continue

                val data = if (idxData >= 0) it.getLong(idxData) else 0L
                val durataSecondi = if (idxDurata >= 0) it.getLong(idxDurata) else 0L
                val tipo = if (idxTipo >= 0) it.getInt(idxTipo) else -1

                val minuti = durataSecondi / 60
                val secondi = durataSecondi % 60
                val durataFormattata = String.format(Locale.ITALY, "%d:%02d", minuti, secondi)

                lista.add(
                    VoceStorico(
                        tipo = tipo,
                        giornoOra = formatoGiornoOra.format(Date(data)),
                        durataFormattata = durataFormattata
                    )
                )
            }
        }
        return lista
    }
}
