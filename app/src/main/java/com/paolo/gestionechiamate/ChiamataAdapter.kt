package com.paolo.gestionechiamate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.provider.CallLog
import androidx.recyclerview.widget.RecyclerView

class ChiamataAdapter(
    private val dati: List<VoceChiamata>,
    private val onSelezioneCambiata: (List<VoceChiamata>) -> Unit
) :
    RecyclerView.Adapter<ChiamataAdapter.ViewHolder>() {

    private val selezionate = linkedSetOf<Long>()
    private var modalitaSelezione = false

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
        val tipo = when (voce.tipo) {
            CallLog.Calls.OUTGOING_TYPE -> "Effettuata"
            CallLog.Calls.MISSED_TYPE -> "Persa"
            CallLog.Calls.REJECTED_TYPE -> "Rifiutata"
            CallLog.Calls.BLOCKED_TYPE -> "Bloccata"
            else -> "Ricevuta"
        }
        holder.txtData.text = "$tipo  •  ${voce.dataFormattata}\n${voce.numeroVisualizzabile()}"
        holder.txtData.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                holder.itemView.context,
                if (voce.tipo == CallLog.Calls.MISSED_TYPE) R.color.end_call else R.color.text_secondary
            )
        )
        val richiamabile = voce.numero.isNotBlank() && !voce.numero.startsWith("-")
        holder.btnChiama.visibility = if (richiamabile) View.VISIBLE else View.INVISIBLE
        holder.btnChiama.isEnabled = !modalitaSelezione
        val sfondo = if (voce.id in selezionate) {
            R.drawable.bg_chiamata_selezionata
        } else {
            val valore = android.util.TypedValue()
            holder.itemView.context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground, valore, true
            )
            valore.resourceId
        }
        holder.itemView.setBackgroundResource(sfondo)
        ColoriTesto.applica(holder.itemView)
        holder.btnChiama.setOnClickListener {
            ChiamaHelper.chiama(holder.itemView.context, voce.numero)
        }
        holder.itemView.setOnClickListener {
            if (modalitaSelezione) {
                cambiaSelezione(voce.id)
                return@setOnClickListener
            }
            val context = holder.itemView.context
            val intent = android.content.Intent(context, DettaglioChiamateActivity::class.java)
            intent.putExtra(DettaglioChiamateActivity.EXTRA_NUMERO, voce.numero)
            intent.putExtra(DettaglioChiamateActivity.EXTRA_NOME, voce.nome)
            context.startActivity(intent)
        }
        holder.itemView.setOnLongClickListener {
            modalitaSelezione = true
            cambiaSelezione(voce.id)
            true
        }
    }

    override fun getItemCount() = dati.size

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

    private fun VoceChiamata.numeroVisualizzabile(): String = when (numero) {
        "-1", "-2", "-3", "" -> "Numero non disponibile"
        else -> numero
    }
}
