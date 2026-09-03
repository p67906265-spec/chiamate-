package com.paolo.gestionechiamate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SmsAdapter(private val dati: List<Sms>) :
    RecyclerView.Adapter<SmsAdapter.ViewHolder>() {

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
    }

    override fun getItemCount() = dati.size
}
