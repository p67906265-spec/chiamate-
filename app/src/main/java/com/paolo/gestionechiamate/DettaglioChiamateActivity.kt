package com.paolo.gestionechiamate

import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
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

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dettaglio_chiamate)

        val numero = intent.getStringExtra(EXTRA_NUMERO) ?: ""
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

        findViewById<ImageButton>(R.id.btnChiama).setOnClickListener {
            ChiamaHelper.chiama(this, numero)
        }

        scope.launch {
            val nomeRubrica = withContext(Dispatchers.IO) { cercaNomeInRubrica(numero) }
            if (nomeRubrica != null) {
                txtNome.text = nomeRubrica
                txtIniziale.text = nomeRubrica.take(1).uppercase()
            }

            val storico = withContext(Dispatchers.IO) { caricaStorico(numero) }
            recycler.adapter = DettaglioChiamataAdapter(storico)
        }
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
