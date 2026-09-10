package com.paolo.gestionechiamate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class PreferitiFragment : Fragment() {

    private var slotInAttesaScelta = -1
    private var slotInAttesaFoto = -1

    private val sceltaContatto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { risultato ->
        if (risultato.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = risultato.data?.data
            if (uri != null && slotInAttesaScelta >= 0) {
                leggiContattoESalva(uri, slotInAttesaScelta)
            }
        }
    }

    private val sceltaFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && slotInAttesaFoto >= 0) {
            val preferito = FavoritesManager.get(requireContext(), slotInAttesaFoto)
            val fotoSalvata = preferito.numero?.let {
                FotoNumeroManager.salvaFoto(requireContext(), it, uri)
            }
            if (fotoSalvata != null) {
                FavoritesManager.setFoto(
                    requireContext(),
                    slotInAttesaFoto,
                    fotoSalvata.toString()
                )
                view?.let { disegnaSlot(it) }
            } else {
                Toast.makeText(requireContext(), "Impossibile salvare la foto", Toast.LENGTH_LONG).show()
            }
        }
        slotInAttesaFoto = -1
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
            val imgFoto = itemView.findViewById<ImageView>(R.id.imgFotoPreferito)
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
                val fotoUri = preferito.numero?.let {
                    FotoNumeroManager.getFotoUri(requireContext(), it)?.toString()
                } ?: preferito.fotoUri
                if (!fotoUri.isNullOrBlank()) {
                    try {
                        imgFoto.setImageURI(Uri.parse(fotoUri))
                        if (imgFoto.drawable != null) {
                            imgFoto.visibility = View.VISIBLE
                            txtIniziale.visibility = View.GONE
                        } else {
                            imgFoto.visibility = View.GONE
                            txtIniziale.visibility = View.VISIBLE
                        }
                    } catch (_: Exception) {
                        imgFoto.visibility = View.GONE
                        txtIniziale.visibility = View.VISIBLE
                    }
                }
            }

            ColoriTesto.applica(itemView)

            itemView.setOnClickListener {
                if (preferito.isVuoto) {
                    apriScelta(slot)
                } else {
                    ChiamaHelper.chiama(requireContext(), preferito.numero ?: "")
                }
            }
            itemView.setOnLongClickListener {
                mostraMenuSlot(itemView, slot, preferito)
                true
            }

            grid.addView(itemView)
        }
    }

    private fun mostraMenuSlot(ancora: View, slot: Int, preferito: Preferito) {
        val menu = PopupMenu(requireContext(), ancora)
        menu.menu.add(0, 1, 0, if (preferito.isVuoto) "Scegli contatto" else "Cambia contatto")
        if (!preferito.isVuoto) {
            menu.menu.add(0, 2, 1, "Scegli foto dalla galleria")
            val haFoto = preferito.numero?.let {
                FotoNumeroManager.getFotoUri(requireContext(), it) != null
            } == true || !preferito.fotoUri.isNullOrBlank()
            if (haFoto) menu.menu.add(0, 3, 2, "Rimuovi foto")
            menu.menu.add(0, 4, 3, getString(R.string.elimina_preferito))
        }
        menu.setOnMenuItemClickListener { voce ->
            when (voce.itemId) {
                1 -> apriScelta(slot)
                2 -> apriSceltaFoto(slot)
                3 -> {
                    preferito.numero?.let { FotoNumeroManager.rimuoviFoto(requireContext(), it) }
                    FavoritesManager.setFoto(requireContext(), slot, null)
                    view?.let { disegnaSlot(it) }
                }
                4 -> {
                    FavoritesManager.clear(requireContext(), slot)
                    view?.let { disegnaSlot(it) }
                }
            }
            true
        }
        menu.show()
    }

    private fun apriSceltaFoto(slot: Int) {
        slotInAttesaFoto = slot
        sceltaFoto.launch("image/*")
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
                val fotoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val nome = if (nomeIdx >= 0) it.getString(nomeIdx) else "?"
                val numero = if (numeroIdx >= 0) it.getString(numeroIdx) else ""
                val foto = if (fotoIdx >= 0) it.getString(fotoIdx) else null
                FavoritesManager.set(requireContext(), slot, nome ?: "?", numero ?: "", foto)
                view?.let { v -> disegnaSlot(v) }
            } else {
                Toast.makeText(requireContext(), "Contatto non valido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
