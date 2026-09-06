package com.paolo.gestionechiamate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ContattoDettaglioActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_LOOKUP = "lookup"
        const val EXTRA_NOME = "nome"
        const val EXTRA_NUMERO = "numero"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contatto_dettaglio)

        val id = intent.getLongExtra(EXTRA_ID, 0L)
        val lookup = intent.getStringExtra(EXTRA_LOOKUP)
        val nome = intent.getStringExtra(EXTRA_NOME) ?: "?"
        val numero = intent.getStringExtra(EXTRA_NUMERO) ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnModifica).setOnClickListener {
            val uri = if (id > 0 && !lookup.isNullOrBlank()) {
                android.provider.ContactsContract.Contacts.getLookupUri(id, lookup)
            } else null

            val editIntent = if (uri != null) {
                Intent(Intent.ACTION_EDIT).setDataAndType(
                    uri, android.provider.ContactsContract.Contacts.CONTENT_ITEM_TYPE
                )
            } else {
                Intent(Intent.ACTION_INSERT, android.provider.ContactsContract.Contacts.CONTENT_URI)
                    .putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, numero)
            }
            startActivity(editIntent)
        }

        findViewById<TextView>(R.id.txtIniziale).text = nome.take(1).uppercase()
        findViewById<TextView>(R.id.txtNome).text = nome
        findViewById<TextView>(R.id.txtNumero).text = numero
        findViewById<TextView>(R.id.txtNumeroDettaglio).text = numero

        findViewById<ImageButton>(R.id.btnChiama).setOnClickListener {
            ChiamaHelper.chiama(this, numero)
        }

        findViewById<ImageButton>(R.id.btnMessaggio).setOnClickListener {
            val intent = Intent(this, ComponiSmsActivity::class.java)
            intent.putExtra(ComponiSmsActivity.EXTRA_NUMERO, numero)
            startActivity(intent)
        }
    }
}
