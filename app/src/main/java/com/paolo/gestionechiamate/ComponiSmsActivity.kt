package com.paolo.gestionechiamate

import android.content.ContentValues
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton

class ComponiSmsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_componi_sms)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val editNumero = findViewById<EditText>(R.id.editNumero)
        val editMessaggio = findViewById<EditText>(R.id.editMessaggio)

        val numeroDaIntent = intent.data?.schemeSpecificPart ?: intent.getStringExtra(EXTRA_NUMERO)
        if (!numeroDaIntent.isNullOrBlank()) {
            editNumero.setText(numeroDaIntent)
        }

        findViewById<MaterialButton>(R.id.btnInvia).setOnClickListener {
            val numero = editNumero.text.toString().trim()
            val messaggio = editMessaggio.text.toString().trim()
            if (numero.isBlank() || messaggio.isBlank()) {
                Toast.makeText(this, "Inserisci numero e messaggio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            inviaSms(numero, messaggio)
        }
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

            Toast.makeText(this, "Messaggio inviato", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Invio non riuscito: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_NUMERO = "numero"
    }
}
