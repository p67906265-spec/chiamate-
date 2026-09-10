package com.paolo.gestionechiamate

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

class ContattoDettaglioActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_LOOKUP = "lookup"
        const val EXTRA_NOME = "nome"
        const val EXTRA_NUMERO = "numero"
    }

    private var contactId = 0L
    private var lookupKey: String? = null
    private var nomeContatto = "?"
    private var numeri: List<NumeroContatto> = emptyList()

    private val modificaContatto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { caricaContatto() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contatto_dettaglio)
        contactId = intent.getLongExtra(EXTRA_ID, 0L)
        lookupKey = intent.getStringExtra(EXTRA_LOOKUP)

        findViewById<Toolbar>(R.id.toolbar).apply {
            title = ""
            setNavigationOnClickListener { finish() }
        }
        findViewById<ImageButton>(R.id.btnAzioni).setOnClickListener { mostraAzioni(it) }
        caricaContatto()
    }

    override fun onResume() {
        super.onResume()
        caricaContatto()
    }

    private fun uriContatto(): Uri? = if (contactId > 0L && !lookupKey.isNullOrBlank()) {
        ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
    } else null

    private fun caricaContatto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val trovati = mutableListOf<NumeroContatto>()
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()), null
        )?.use { cursor ->
            val nomeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numeroIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val tipoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
            val lookupIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            while (cursor.moveToNext()) {
                nomeContatto = cursor.getString(nomeIdx) ?: "?"
                lookupKey = cursor.getString(lookupIdx) ?: lookupKey
                val numero = cursor.getString(numeroIdx) ?: continue
                val etichetta = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    resources, cursor.getInt(tipoIdx), cursor.getString(labelIdx)
                ).toString()
                if (trovati.none { normalizza(it.numero) == normalizza(numero) }) {
                    trovati += NumeroContatto(numero, etichetta)
                }
            }
        }
        numeri = trovati
        if (numeri.isEmpty()) {
            Toast.makeText(this, "Il contatto non è più disponibile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        aggiornaSchermata()
    }

    private fun aggiornaSchermata() {
        findViewById<TextView>(R.id.txtIniziale).text = nomeContatto.take(1).uppercase()
        findViewById<TextView>(R.id.txtNome).text = nomeContatto
        findViewById<TextView>(R.id.txtNumero).text = if (numeri.size == 1) {
            numeri.first().numero
        } else "${numeri.size} numeri di telefono"

        findViewById<LinearLayout>(R.id.contenitoreNumeri).apply {
            removeAllViews()
            numeri.forEachIndexed { index, voce ->
                if (index > 0) addView(creaSeparatore())
                addView(creaRigaNumero(voce))
            }
        }
    }

    private fun creaSeparatore() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            setMargins(0, dp(12), 0, dp(12))
        }
        setBackgroundColor(ContextCompat.getColor(this@ContattoDettaglioActivity, R.color.text_secondary))
        alpha = 0.22f
    }

    private fun creaRigaNumero(voce: NumeroContatto): View {
        val riga = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val testi = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        testi.addView(TextView(this).apply {
            text = voce.etichetta
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@ContattoDettaglioActivity, R.color.text_secondary))
        })
        testi.addView(TextView(this).apply {
            text = voce.numero
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ContattoDettaglioActivity, R.color.text_primary))
        })
        riga.addView(testi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        riga.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_dialog_email)
            setBackgroundResource(R.drawable.bg_dialpad_key)
            contentDescription = "Messaggio a ${voce.numero}"
            setOnClickListener {
                startActivity(Intent(this@ContattoDettaglioActivity, ComponiSmsActivity::class.java).apply {
                    putExtra(ComponiSmsActivity.EXTRA_NUMERO, voce.numero)
                })
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(10) })
        riga.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_call)
            setBackgroundResource(R.drawable.bg_circle_call)
            contentDescription = "Chiama ${voce.numero}"
            setOnClickListener { ChiamaHelper.chiama(this@ContattoDettaglioActivity, voce.numero) }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        return riga
    }

    private fun mostraAzioni(ancora: View) {
        PopupMenu(this, ancora).apply {
            menu.add(0, 1, 0, "Modifica contatto")
            menu.add(0, 2, 1, "Elimina contatto")
            setOnMenuItemClickListener {
                if (it.itemId == 1) apriModifica() else confermaEliminazione()
                true
            }
        }.show()
    }

    private fun apriModifica() {
        val uri = uriContatto() ?: return
        modificaContatto.launch(
            Intent(Intent.ACTION_EDIT).setDataAndType(
                uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE
            ).putExtra("finishActivityOnSaveCompleted", true)
        )
    }

    private fun confermaEliminazione() {
        AlertDialog.Builder(this)
            .setTitle("Eliminare il contatto?")
            .setMessage("$nomeContatto verrà eliminato dalla rubrica del telefono.")
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Elimina") { _, _ -> eliminaContatto() }
            .show()
    }

    private fun eliminaContatto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Autorizza la modifica della rubrica nelle impostazioni", Toast.LENGTH_LONG).show()
            return
        }
        val eliminati = uriContatto()?.let { contentResolver.delete(it, null, null) } ?: 0
        if (eliminati > 0) {
            Toast.makeText(this, "Contatto eliminato", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else Toast.makeText(this, "Impossibile eliminare questo contatto", Toast.LENGTH_LONG).show()
    }

    private fun normalizza(numero: String) = numero.filter(Char::isDigit).takeLast(10)
    private fun dp(valore: Int) = (valore * resources.displayMetrics.density).toInt()
}
