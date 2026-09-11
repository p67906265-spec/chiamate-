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
    private val onEliminato: () -> Unit
) : RecyclerView.Adapter<SmsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMittente: TextView = view.findViewById(R.id.txtMittente)
        val txtCorpo: TextView = view.findViewById(R.id.txtCorpo)
        val txtData: TextView = view.findViewById(R.id.txtData)
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
        ColoriTesto.applica(holder.itemView)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, ComponiSmsActivity::class.java)
            intent.putExtra(ComponiSmsActivity.EXTRA_NUMERO, sms.numero)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            mostraConfermaEliminazione(context, sms.mittente) {
                val eliminati = eliminaConversazione(context, sms.numero)
                if (eliminati > 0) {
                    Toast.makeText(context, "Conversazione eliminata", Toast.LENGTH_SHORT).show()
                    onEliminato()
                } else if (eliminati == ERRORE_PERMESSO) {
                    mostraRichiestaSmsPredefinita(context)
                } else {
                    Toast.makeText(
                        context,
                        "Nessun messaggio trovato da eliminare.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            true
        }
    }

    private fun mostraConfermaEliminazione(
        context: Context,
        mittente: String,
        conferma: () -> Unit
    ) {
        val dialog = android.app.Dialog(context)
        dialog.setContentView(R.layout.dialog_conferma_eliminazione)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.txtTitoloConferma).text = "Eliminare conversazione?"
        dialog.findViewById<TextView>(R.id.txtMessaggioConferma).text =
            "Saranno eliminati tutti i messaggi con $mittente."
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

    private fun eliminaConversazione(context: Context, numero: String): Int {
        val cifreNumero = soloCifre(numero)
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.THREAD_ID),
            null, null, null
        )
        val idsDaEliminare = mutableListOf<Long>()
        val threadDaEliminare = mutableSetOf<Long>()
        cursor?.use {
            val idxId = it.getColumnIndex(Telephony.Sms._ID)
            val idxAddr = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val idxThread = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            while (it.moveToNext()) {
                val indirizzo = if (idxAddr >= 0) it.getString(idxAddr) else null
                if (indirizzo != null && soloCifre(indirizzo) == cifreNumero) {
                    if (idxId >= 0) idsDaEliminare.add(it.getLong(idxId))
                    if (idxThread >= 0) threadDaEliminare.add(it.getLong(idxThread))
                }
            }
        }
        var eliminati = 0
        try {
            for (threadId in threadDaEliminare) {
                eliminati += context.contentResolver.delete(
                    Uri.parse("content://mms-sms/conversations/$threadId"), null, null
                )
            }
            if (eliminati > 0) return eliminati
            for (id in idsDaEliminare) {
                eliminati += context.contentResolver.delete(
                    ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id), null, null
                )
            }
        } catch (_: SecurityException) {
            return ERRORE_PERMESSO
        } catch (_: Exception) {
            return 0
        }
        return eliminati
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

    private fun soloCifre(numero: String): String {
        var cifre = numero.filter { it.isDigit() }
        if (cifre.length > 10) cifre = cifre.takeLast(10)
        return cifre
    }

    override fun getItemCount() = dati.size

    companion object {
        private const val ERRORE_PERMESSO = -1
    }
}
