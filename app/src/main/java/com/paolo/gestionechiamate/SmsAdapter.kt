package com.paolo.gestionechiamate

import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class SmsAdapter(
    private val dati: List<Sms>,
    private val onEliminato: () -> Unit,
    private val onSelezioneCambiata: (List<Sms>) -> Unit = {}
) : RecyclerView.Adapter<SmsAdapter.ViewHolder>() {

    private val selezionate = linkedSetOf<Long>()
    private var modalitaSelezione = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMittente: TextView = view.findViewById(R.id.txtMittente)
        val txtCorpo: TextView = view.findViewById(R.id.txtCorpo)
        val txtData: TextView = view.findViewById(R.id.txtData)
        val txtNuovo: TextView = view.findViewById(R.id.txtNuovo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sms = dati[position]
        holder.txtMittente.text = sms.mittente
        holder.txtCorpo.text = sms.corpo
        holder.txtData.text = sms.dataFormattata
        holder.txtNuovo.visibility = if (sms.nuovo) View.VISIBLE else View.GONE
        holder.itemView.setBackgroundResource(
            when {
                sms.id in selezionate -> R.drawable.bg_chiamata_selezionata
                sms.nuovo -> R.drawable.bg_item_uscita
                else -> R.drawable.bg_item_letto
            }
        )
        holder.txtMittente.setTypeface(
            null, if (sms.nuovo) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
        ColoriTesto.applica(holder.itemView)

        holder.itemView.setOnClickListener {
            if (modalitaSelezione) {
                cambiaSelezione(sms.id)
                return@setOnClickListener
            }
            val context = holder.itemView.context
            val intent = android.content.Intent(context, ComponiSmsActivity::class.java)
            intent.putExtra(ComponiSmsActivity.EXTRA_NUMERO, sms.numero)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            modalitaSelezione = true
            cambiaSelezione(sms.id)
            true
        }
    }

    fun annullaSelezione() {
        modalitaSelezione = false
        selezionate.clear()
        notifyDataSetChanged()
        onSelezioneCambiata(emptyList())
    }

    private fun cambiaSelezione(id: Long) {
        if (id in selezionate) selezionate.remove(id) else selezionate.add(id)
        if (selezionate.isEmpty()) modalitaSelezione = false
        notifyDataSetChanged()
        onSelezioneCambiata(dati.filter { it.id in selezionate })
    }

    fun chiediEliminaSelezionati(context: Context) {
        val scelti = dati.filter { it.id in selezionate }
        if (scelti.isEmpty()) return
        val ids = scelti.map { it.id }
        mostraConfermaEliminazione(
            context,
            if (ids.size == 1) "Eliminare il messaggio?" else "Eliminare i messaggi?",
            if (ids.size == 1) "Verrà eliminato il messaggio selezionato."
            else "Verranno eliminati i ${ids.size} messaggi selezionati."
        ) {
            val eliminati = eliminaMessaggi(context, ids)
            if (eliminati > 0) {
                Toast.makeText(
                    context,
                    if (eliminati == 1) "1 messaggio eliminato" else "$eliminati messaggi eliminati",
                    Toast.LENGTH_SHORT
                ).show()
                modalitaSelezione = false
                selezionate.clear()
                onSelezioneCambiata(emptyList())
                onEliminato()
            } else if (eliminati == ERRORE_PERMESSO) {
                mostraRichiestaSmsPredefinita(context)
            } else {
                Toast.makeText(context, "Nessun messaggio eliminato.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun eliminaMessaggi(context: Context, ids: List<Long>): Int {
        var eliminati = 0
        try {
            for (id in ids) {
                eliminati += context.contentResolver.delete(
                    ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id), null, null
                )
            }
        } catch (_: SecurityException) {
            return if (eliminati > 0) eliminati else ERRORE_PERMESSO
        } catch (_: Exception) {
            return eliminati
        }
        return eliminati
    }

    private fun mostraConfermaEliminazione(
        context: Context,
        titolo: String,
        messaggio: String,
        conferma: () -> Unit
    ) {
        val dialog = android.app.Dialog(context)
        dialog.setContentView(R.layout.dialog_conferma_eliminazione)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.txtTitoloConferma).text = titolo
        dialog.findViewById<TextView>(R.id.txtMessaggioConferma).text = messaggio
        dialog.findViewById<TextView>(R.id.btnConfermaElimina).apply {
            text = "Elimina"
            setOnClickListener {
                dialog.dismiss()
                conferma()
            }
        }
        dialog.findViewById<TextView>(R.id.btnConfermaAnnulla).setOnClickListener {
            dialog.dismiss()
        }
        ColoriTesto.applica(dialog.findViewById(R.id.pannelloConferma))
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88f).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun mostraRichiestaSmsPredefinita(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Autorizzazione necessaria")
            .setMessage(
                "Android permette di eliminare i messaggi soltanto all'app SMS predefinita. " +
                    "Imposta temporaneamente Gestione Chiamate come app SMS predefinita e poi ripeti l'eliminazione."
            )
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Imposta ora") { _, _ ->
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    roleManager?.createRequestRoleIntent(RoleManager.ROLE_SMS)
                } else {
                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                        .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                }
                if (intent != null) context.startActivity(intent)
            }
            .show()
    }

    override fun getItemCount() = dati.size

    companion object {
        private const val ERRORE_PERMESSO = -1
    }
}
