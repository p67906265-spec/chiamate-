package com.paolo.gestionechiamate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.ImageButton

class ContattoAdapter(private val dati: List<Contatto>) :
    RecyclerView.Adapter<ContattoAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val txtIniziale: TextView = view.findViewById(R.id.txtIniziale)
        val txtNome: TextView = view.findViewById(R.id.txtNome)
        val txtNumero: TextView = view.findViewById(R.id.txtNumero)
        val btnChiama: ImageButton = view.findViewById(R.id.btnChiama)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contatto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contatto = dati[position]
        holder.txtIniziale.text = contatto.nome.take(1).uppercase()
        holder.txtNome.text = contatto.nome
        holder.txtNumero.text = if (contatto.numeri.size == 1) {
            contatto.numero
        } else {
            "${contatto.numero}  •  ${contatto.numeri.size} numeri"
        }
        ColoriTesto.applica(holder.itemView)
        holder.btnChiama.setOnClickListener {
            ChiamaHelper.chiama(holder.itemView.context, contatto.numero)
        }
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, ContattoDettaglioActivity::class.java)
            intent.putExtra(ContattoDettaglioActivity.EXTRA_ID, contatto.id)
            intent.putExtra(ContattoDettaglioActivity.EXTRA_LOOKUP, contatto.lookupKey)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = dati.size
}
