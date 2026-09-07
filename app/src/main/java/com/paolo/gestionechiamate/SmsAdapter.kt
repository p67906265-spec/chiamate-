package com.paolo.gestionechiamate

import android.app.AlertDialog
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, ComponiSmsActivity::class.java)
            intent.putExtra(ComponiSmsActivity.EXTRA_NUMERO, sms.numero)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Eliminare conversazione")
                .setMessage("Eliminare tutti i messaggi con ${sms.mittente}?")
                .setPositiveButton("Elimina") { _, _ ->
                    eliminaConversazione(context, sms.numero)
                    onEliminato()
                }
                .setNegativeButton("Annulla", null)
                .show()
            true
        }
    }

    private fun eliminaConversazione(context: android.content.Context, numero: String) {
        val cifreNumero = soloCifre(numero)
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS),
            null, null, null
        )
        val idsDaEliminare = mutableListOf<Long>()
        cursor?.use {
            val idxId = it.getColumnIndex(Telephony.Sms._ID)
            val idxAddr = it.getColumnIndex(Telephony.Sms.ADDRESS)
            while (it.moveToNext()) {
                val indirizzo = if (idxAddr >= 0) it.getString(idxAddr) else null
                if (indirizzo != null && soloCifre(indirizzo) == cifreNumero) {
                    if (idxId >= 0) idsDaEliminare.add(it.getLong(idxId))
                }
            }
        }
        for (id in idsDaEliminare) {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI, "${Telephony.Sms._ID}=?", arrayOf(id.toString())
            )
        }
    }

    private fun soloCifre(numero: String): String {
        var cifre = numero.filter { it.isDigit() }
        if (cifre.length > 10) cifre = cifre.takeLast(10)
        return cifre
    }

    override fun getItemCount() = dati.size
}
