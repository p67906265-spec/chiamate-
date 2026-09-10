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
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
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
    private var numeroInAttesaFoto: String? = null

    private val scegliFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val numero = numeroInAttesaFoto
        numeroInAttesaFoto = null
        if (uri != null && !numero.isNullOrBlank()) {
            if (FotoNumeroManager.salvaFoto(this, numero, uri) != null) {
                aggiornaSchermata()
                Toast.makeText(this, "Foto associata al numero", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Impossibile salvare la foto", Toast.LENGTH_LONG).show()
            }
        }
    }

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
            orientation = LinearLayout.VERTICAL
        }
        val intestazione = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val foto = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_circle_primary)
            clipToOutline = true
            isClickable = true
            isFocusable = true
            contentDescription = "Foto per ${voce.numero}"
            setOnClickListener { gestisciFotoNumero(this, voce.numero) }
        }
        val iniziale = TextView(this).apply {
            text = nomeContatto.take(1).uppercase()
            gravity = Gravity.CENTER
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ContattoDettaglioActivity, R.color.white))
        }
        val immagine = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        foto.addView(iniziale, FrameLayout.LayoutParams(-1, -1))
        foto.addView(immagine, FrameLayout.LayoutParams(-1, -1))
        FotoNumeroManager.getFotoUri(this, voce.numero)?.let { uri ->
            try {
                immagine.setImageURI(uri)
                if (immagine.drawable != null) {
                    immagine.visibility = View.VISIBLE
                    iniziale.visibility = View.GONE
                }
            } catch (_: Exception) {
            }
        }
        intestazione.addView(foto, LinearLayout.LayoutParams(dp(58), dp(58)).apply {
            marginEnd = dp(12)
        })

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
        testi.addView(TextView(this).apply {
            text = "Tocca la foto per cambiarla"
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@ContattoDettaglioActivity, R.color.text_secondary))
        })
        intestazione.addView(testi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        riga.addView(intestazione)

        val azioni = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(8), 0, 0)
        }
        azioni.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_dialog_email)
            setBackgroundResource(R.drawable.bg_dialpad_key)
            contentDescription = "Messaggio a ${voce.numero}"
            setOnClickListener {
                startActivity(Intent(this@ContattoDettaglioActivity, ComponiSmsActivity::class.java).apply {
                    putExtra(ComponiSmsActivity.EXTRA_NUMERO, voce.numero)
                })
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(10) })
        azioni.addView(ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_call)
            setBackgroundResource(R.drawable.bg_circle_primary)
            contentDescription = "Chiama ${voce.numero}"
            setOnClickListener { ChiamaHelper.chiama(this@ContattoDettaglioActivity, voce.numero) }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        riga.addView(azioni)
        return riga
    }

    private fun gestisciFotoNumero(ancora: View, numero: String) {
        if (FotoNumeroManager.getFotoUri(this, numero) == null) {
            apriGalleria(numero)
            return
        }
        PopupMenu(this, ancora).apply {
            menu.add(0, 1, 0, "Cambia foto dalla galleria")
            menu.add(0, 2, 1, "Rimuovi foto")
            setOnMenuItemClickListener {
                if (it.itemId == 1) {
                    apriGalleria(numero)
                } else {
                    FotoNumeroManager.rimuoviFoto(this@ContattoDettaglioActivity, numero)
                    aggiornaSchermata()
                }
                true
            }
        }.show()
    }

    private fun apriGalleria(numero: String) {
        numeroInAttesaFoto = numero
        scegliFoto.launch("image/*")
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
        startActivity(Intent(this, ModificaContattoActivity::class.java).apply {
            putExtra(ModificaContattoActivity.EXTRA_ID, contactId)
        })
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
