package com.paolo.gestionechiamate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChiamataAdapter(private val dati: List<VoceChiamata>) :
    RecyclerView.Adapter<ChiamataAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNome: TextView = view.findViewById(R.id.txtNome)
        val txtData: TextView = view.findViewById(R.id.txtData)
        val btnChiama: ImageButton = view.findViewById(R.id.btnChiama)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chiamata, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val voce = dati[position]
        holder.txtNome.text = voce.nome
        holder.txtData.text = voce.dataFormattata
        val chiama = { ChiamaHelper.chiama(holder.itemView.context, voce.numero) }
        holder.btnChiama.setOnClickListener { chiama() }
        holder.itemView.setOnClickListener { chiama() }
    }

    override fun getItemCount() = dati.size
}
