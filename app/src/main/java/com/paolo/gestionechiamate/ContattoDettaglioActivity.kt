package com.paolo.gestionechiamate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ContattoDettaglioActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOME = "nome"
        const val EXTRA_NUMERO = "numero"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contatto_dettaglio)

        val nome = intent.getStringExtra(EXTRA_NOME) ?: "?"
        val numero = intent.getStringExtra(EXTRA_NUMERO) ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.txtIniziale).text = nome.take(1).uppercase()
        findViewById<TextView>(R.id.txtNome).text = nome
        findViewById<TextView>(R.id.txtNumero).text = numero
        findViewById<TextView>(R.id.txtNumeroDettaglio).text = numero

        findViewById<ImageButton>(R.id.btnChiama).setOnClickListener {
            ChiamaHelper.chiama(this, numero)
        }

        findViewById<ImageButton>(R.id.btnMessaggio).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$numero"))
            startActivity(intent)
        }
    }
}
