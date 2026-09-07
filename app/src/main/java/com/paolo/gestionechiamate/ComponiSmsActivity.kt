package com.paolo.gestionechiamate

import android.content.ContentValues
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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

class ComponiSmsActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var editNumero: EditText
    private lateinit var recyclerThread: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_componi_sms)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        editNumero = findViewById(R.id.editNumero)
        recyclerThread = findViewById(R.id.recyclerThread)
        recyclerThread.layoutManager = LinearLayoutManager(this)
        val editMessaggio = findViewById<EditText>(R.id.editMessaggio)

        val numeroDaIntent = intent.data?.schemeSpecificPart ?: intent.getStringExtra(EXTRA_NUMERO)
        if (!numeroDaIntent.isNullOrBlank()) {
            editNumero.setText(numeroDaIntent)
            caricaConversazione(numeroDaIntent)
        }

        findViewById<ImageButton>(R.id.btnInvia).setOnClickListener {
            val numero = editNumero.text.toString().trim()
            val messaggio = editMessaggio.text.toString().trim()
            if (numero.isBlank() || messaggio.isBlank()) {
                Toast.makeText(this, "Inserisci numero e messaggio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            inviaSms(numero, messaggio)
            editMessaggio.setText("")
        }
    }

    private fun soloCifre(numero: String): String {
        var cifre = numero.filter { it.isDigit() }
        if (cifre.length > 10) cifre = cifre.takeLast(10)
        return cifre
    }

    private fun caricaConversazione(numero: String) {
        scope.launch {
            val messaggi = withContext(Dispatchers.IO) { leggiConversazione(numero) }
            recyclerThread.adapter = SmsThreadAdapter(messaggi)
            if (messaggi.isNotEmpty()) {
                recyclerThread.scrollToPosition(messaggi.size - 1)
            }
        }
    }

    private fun leggiConversazione(numero: String): List<MessaggioThread> {
        val cifreNumero = soloCifre(numero)
        val lista = mutableListOf<MessaggioThread>()
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null, null,
            Telephony.Sms.DATE + " ASC"
        )
        val formatoOra = SimpleDateFormat("dd/MM HH:mm", Locale.ITALY)
        cursor?.use {
            val idxAddr = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
            val idxData = it.getColumnIndex(Telephony.Sms.DATE)
            val idxTipo = it.getColumnIndex(Telephony.Sms.TYPE)
            while (it.moveToNext()) {
                val indirizzo = if (idxAddr >= 0) it.getString(idxAddr) else null
                if (indirizzo == null || soloCifre(indirizzo) != cifreNumero) continue

                val corpo = if (idxBody >= 0) it.getString(idxBody) else ""
                val data = if (idxData >= 0) it.getLong(idxData) else 0L
                val tipo = if (idxTipo >= 0) it.getInt(idxTipo) else Telephony.Sms.MESSAGE_TYPE_INBOX
                val inviato = tipo == Telephony.Sms.MESSAGE_TYPE_SENT || tipo == Telephony.Sms.MESSAGE_TYPE_OUTBOX

                lista.add(MessaggioThread(corpo ?: "", formatoOra.format(Date(data)), inviato))
            }
        }
        return lista
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

            caricaConversazione(numero)
        } catch (e: Exception) {
            Toast.makeText(this, "Invio non riuscito: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_NUMERO = "numero"
    }
}
