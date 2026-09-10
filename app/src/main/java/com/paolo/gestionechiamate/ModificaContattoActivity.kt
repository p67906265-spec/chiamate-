package com.paolo.gestionechiamate

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModificaContattoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
    }

    private data class NumeroEditor(
        val dataId: Long?,
        val rawContactId: Long,
        val tipoOriginale: Int,
        val numeroOriginale: String,
        val vista: View
    )

    private val etichette = listOf("Cellulare", "Casa", "Lavoro", "Principale", "Altro")
    private val tipi = listOf(
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
    )

    private var contactId = 0L
    private var rawContactId = 0L
    private var nomeDataId: Long? = null
    private val righeNumeri = mutableListOf<NumeroEditor>()
    private val idNumeriOriginali = mutableSetOf<Long>()
    private lateinit var contenitoreNumeri: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modifica_contatto)
        contactId = intent.getLongExtra(EXTRA_ID, 0L)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        contenitoreNumeri = findViewById(R.id.contenitoreNumeri)
        findViewById<TextView>(R.id.btnAggiungiNumero).setOnClickListener {
            aggiungiRiga(null, rawContactId, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, "")
        }
        findViewById<TextView>(R.id.btnSalva).setOnClickListener { salva() }
        caricaDati()
    }

    private fun caricaDati() {
        if (contactId <= 0L || ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Contatto non disponibile", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val dati = withContext(Dispatchers.IO) { leggiContatto() }
            if (dati == null) {
                Toast.makeText(this@ModificaContattoActivity, "Impossibile leggere il contatto", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            findViewById<EditText>(R.id.editNome).setText(dati.nome)
            findViewById<EditText>(R.id.editSecondoNome).setText(dati.secondoNome)
            findViewById<EditText>(R.id.editCognome).setText(dati.cognome)
            dati.numeri.forEach { aggiungiRiga(it.dataId, it.rawId, it.tipo, it.numero) }
            if (dati.numeri.isEmpty()) {
                aggiungiRiga(null, rawContactId, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, "")
            }
        }
    }

    private data class DatiLetti(
        val nome: String,
        val secondoNome: String,
        val cognome: String,
        val numeri: List<NumeroLetto>
    )

    private data class NumeroLetto(val dataId: Long, val rawId: Long, val numero: String, val tipo: Int)

    private fun leggiContatto(): DatiLetti? {
        var nome = ""
        var secondoNome = ""
        var cognome = ""
        var nomeCompleto = ""

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME
            ),
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=? " +
                "AND ${ContactsContract.Data.IS_READ_ONLY}=0",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                nomeDataId = c.getLong(0)
                rawContactId = c.getLong(1)
                nome = c.getString(2).orEmpty()
                secondoNome = c.getString(3).orEmpty()
                cognome = c.getString(4).orEmpty()
                nomeCompleto = c.getString(5).orEmpty()
            }
        }

        val numeri = mutableListOf<NumeroLetto>()
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=? AND " +
                "${ContactsContract.Data.IS_READ_ONLY}=0",
            arrayOf(contactId.toString()), null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val rawId = c.getLong(1)
                if (rawContactId == 0L) rawContactId = rawId
                idNumeriOriginali += id
                numeri += NumeroLetto(id, rawId, c.getString(2).orEmpty(), c.getInt(3))
            }
        }
        if (rawContactId == 0L) return null
        if (nome.isBlank() && secondoNome.isBlank() && cognome.isBlank()) nome = nomeCompleto
        return DatiLetti(nome, secondoNome, cognome, numeri)
    }

    private fun aggiungiRiga(dataId: Long?, rawId: Long, tipo: Int, numero: String) {
        val vista = LayoutInflater.from(this).inflate(R.layout.item_modifica_numero, contenitoreNumeri, false)
        val spinner = vista.findViewById<Spinner>(R.id.spinnerTipo)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, etichette)
        spinner.setSelection(tipi.indexOf(tipo).takeIf { it >= 0 } ?: tipi.lastIndex)
        vista.findViewById<EditText>(R.id.editNumero).setText(numero)

        val riga = NumeroEditor(dataId, rawId, tipo, numero, vista)
        righeNumeri += riga
        vista.findViewById<ImageButton>(R.id.btnRimuovi).setOnClickListener {
            righeNumeri.remove(riga)
            contenitoreNumeri.removeView(vista)
        }
        contenitoreNumeri.addView(vista)
    }

    private fun salva() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Autorizza la modifica della rubrica", Toast.LENGTH_LONG).show()
            return
        }

        val nome = findViewById<EditText>(R.id.editNome).text.toString().trim()
        val secondoNome = findViewById<EditText>(R.id.editSecondoNome).text.toString().trim()
        val cognome = findViewById<EditText>(R.id.editCognome).text.toString().trim()
        val righeValide = righeNumeri.filter {
            it.vista.findViewById<EditText>(R.id.editNumero).text.toString().isNotBlank()
        }
        if (nome.isBlank() && secondoNome.isBlank() && cognome.isBlank()) {
            Toast.makeText(this, "Inserisci almeno il nome", Toast.LENGTH_SHORT).show()
            return
        }
        if (righeValide.isEmpty()) {
            Toast.makeText(this, "Inserisci almeno un numero", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<TextView>(R.id.btnSalva).isEnabled = false
        lifecycleScope.launch {
            val esito = withContext(Dispatchers.IO) {
                runCatching { applicaModifiche(nome, secondoNome, cognome, righeValide) }
            }
            findViewById<TextView>(R.id.btnSalva).isEnabled = true
            if (esito.isSuccess) {
                Toast.makeText(this@ModificaContattoActivity, "Contatto aggiornato", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(
                    this@ModificaContattoActivity,
                    "Impossibile salvare: il contatto potrebbe essere di sola lettura",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun applicaModifiche(
        nome: String,
        secondoNome: String,
        cognome: String,
        righeValide: List<NumeroEditor>
    ) {
        val operazioni = arrayListOf<ContentProviderOperation>()
        val valoriNome: ContentProviderOperation.Builder = if (nomeDataId != null) {
            ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, nomeDataId!!)
            )
        } else {
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
        }
        operazioni += valoriNome
            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, nome)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, secondoNome)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, cognome)
            .build()

        val idMantenuti = righeValide.mapNotNull { it.dataId }.toSet()
        (idNumeriOriginali - idMantenuti).forEach { id ->
            operazioni += ContentProviderOperation.newDelete(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id)
            ).build()
        }

        righeValide.forEach { riga ->
            val numero = riga.vista.findViewById<EditText>(R.id.editNumero).text.toString().trim()
            val indiceTipo = riga.vista.findViewById<Spinner>(R.id.spinnerTipo).selectedItemPosition
            val tipo = tipi.getOrElse(indiceTipo) { ContactsContract.CommonDataKinds.Phone.TYPE_OTHER }
            val operazione = if (riga.dataId != null) {
                ContentProviderOperation.newUpdate(
                    ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, riga.dataId)
                )
            } else {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawIdPer(riga))
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
            }
            operazioni += operazione
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, numero)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, tipo)
                .build()
        }
        contentResolver.applyBatch(ContactsContract.AUTHORITY, operazioni)
    }

    private fun rawIdPer(riga: NumeroEditor): Long =
        riga.rawContactId.takeIf { it > 0L } ?: rawContactId
}
