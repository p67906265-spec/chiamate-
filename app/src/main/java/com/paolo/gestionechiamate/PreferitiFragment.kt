package com.paolo.gestionechiamate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class PreferitiFragment : Fragment() {

    private var slotInAttesaScelta = -1

    private val sceltaContatto = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { risultato ->
        if (risultato.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = risultato.data?.data
            if (uri != null && slotInAttesaScelta >= 0) {
                leggiContattoESalva(uri, slotInAttesaScelta)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_preferiti, container, false)
        disegnaSlot(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        view?.let { disegnaSlot(it) }
    }

    private fun disegnaSlot(root: View) {
        val grid = root.findViewById<GridLayout>(R.id.gridPreferiti)
        grid.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (slot in 0 until FavoritesManager.NUM_SLOT) {
            val itemView = inflater.inflate(R.layout.item_favorite_slot, grid, false)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            itemView.layoutParams = params

            val preferito = FavoritesManager.get(requireContext(), slot)
            val txtIniziale = itemView.findViewById<TextView>(R.id.txtIniziale)
            val txtNome = itemView.findViewById<TextView>(R.id.txtNome)
            val txtNumero = itemView.findViewById<TextView>(R.id.txtNumero)

            if (preferito.isVuoto) {
                txtIniziale.text = "+"
                txtNome.text = getString(R.string.nessun_preferito)
                txtNumero.text = ""
            } else {
                txtIniziale.text = preferito.nome?.take(1)?.uppercase() ?: "?"
                txtNome.text = preferito.nome
                txtNumero.text = preferito.numero
            }

            itemView.setOnClickListener {
                if (preferito.isVuoto) {
                    apriScelta(slot)
                } else {
                    ChiamaHelper.chiama(requireContext(), preferito.numero ?: "")
                }
            }
            itemView.setOnLongClickListener {
                mostraMenuSlot(slot, preferito.isVuoto)
                true
            }

            grid.addView(itemView)
        }
    }

    private fun mostraMenuSlot(slot: Int, vuoto: Boolean) {
        val opzioni = if (vuoto)
            arrayOf(getString(R.string.scegli_contatto, slot + 1))
        else
            arrayOf(getString(R.string.scegli_contatto, slot + 1), getString(R.string.elimina_preferito))

        AlertDialog.Builder(requireContext())
            .setItems(opzioni) { _, which ->
                if (which == 0) apriScelta(slot)
                else {
                    FavoritesManager.clear(requireContext(), slot)
                    view?.let { disegnaSlot(it) }
                }
            }
            .show()
    }

    private fun apriScelta(slot: Int) {
        slotInAttesaScelta = slot
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        sceltaContatto.launch(intent)
    }

    private fun leggiContattoESalva(uri: Uri, slot: Int) {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nomeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numeroIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nome = if (nomeIdx >= 0) it.getString(nomeIdx) else "?"
                val numero = if (numeroIdx >= 0) it.getString(numeroIdx) else ""
                FavoritesManager.set(requireContext(), slot, nome ?: "?", numero ?: "")
                view?.let { v -> disegnaSlot(v) }
            } else {
                Toast.makeText(requireContext(), "Contatto non valido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
