package com.paolo.gestionechiamate

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class MessaggioThread(
    val corpo: String,
    val oraFormattata: String,
    val inviato: Boolean
)

class SmsThreadAdapter(private val dati: List<MessaggioThread>) :
    RecyclerView.Adapter<SmsThreadAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: LinearLayout = view as LinearLayout
        val txtFumetto: TextView = view.findViewById(R.id.txtFumetto)
        val txtOra: TextView = view.findViewById(R.id.txtOra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_messaggio_thread, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messaggio = dati[position]
        holder.txtFumetto.text = messaggio.corpo
        holder.txtOra.text = messaggio.oraFormattata

        if (messaggio.inviato) {
            holder.root.gravity = Gravity.END
            holder.txtFumetto.setBackgroundResource(R.drawable.bg_fumetto_inviato)
            holder.txtFumetto.setTextColor(holder.root.resources.getColor(R.color.white, null))
        } else {
            holder.root.gravity = Gravity.START
            holder.txtFumetto.setBackgroundResource(R.drawable.bg_fumetto_ricevuto)
            holder.txtFumetto.setTextColor(holder.root.resources.getColor(R.color.text_primary, null))
        }
    }

    override fun getItemCount() = dati.size
}
