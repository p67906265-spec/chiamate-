package com.paolo.gestionechiamate

import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class VoceStorico(
    val tipo: Int,
    val giornoOra: String,
    val durataFormattata: String
)

class DettaglioChiamataAdapter(private val dati: List<VoceStorico>) :
    RecyclerView.Adapter<DettaglioChiamataAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgTipo: ImageView = view.findViewById(R.id.imgTipo)
        val txtTipo: TextView = view.findViewById(R.id.txtTipo)
        val txtGiornoOra: TextView = view.findViewById(R.id.txtGiornoOra)
        val txtDurata: TextView = view.findViewById(R.id.txtDurata)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dettaglio_chiamata, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voce = dati[position]
        holder.txtGiornoOra.text = voce.giornoOra
        holder.txtDurata.text = voce.durataFormattata

        val (etichetta, icona) = when (voce.tipo) {
            CallLog.Calls.INCOMING_TYPE -> "Ricevuta" to android.R.drawable.sym_call_incoming
            CallLog.Calls.OUTGOING_TYPE -> "Effettuata" to android.R.drawable.sym_call_outgoing
            CallLog.Calls.MISSED_TYPE -> "Persa" to android.R.drawable.sym_call_missed
            CallLog.Calls.REJECTED_TYPE -> "Rifiutata" to android.R.drawable.sym_call_missed
            CallLog.Calls.BLOCKED_TYPE -> "Bloccata" to android.R.drawable.sym_call_missed
            else -> "Chiamata" to android.R.drawable.sym_call_incoming
        }
        holder.txtTipo.text = etichetta
        holder.imgTipo.setImageResource(icona)
    }

    override fun getItemCount() = dati.size
}
