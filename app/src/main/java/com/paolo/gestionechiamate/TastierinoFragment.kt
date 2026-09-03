package com.paolo.gestionechiamate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment

class TastierinoFragment : Fragment() {

    private val numero = StringBuilder()
    private lateinit var txtNumero: TextView
    private lateinit var btnBackspace: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_tastierino, container, false)

        txtNumero = view.findViewById(R.id.txtNumeroDigitato)
        btnBackspace = view.findViewById(R.id.btnBackspace)
        val grid = view.findViewById<GridLayout>(R.id.gridTasti)
        val btnChiama = view.findViewById<ImageButton>(R.id.btnChiama)

        DialpadKeys.build(requireContext(), grid) { cifra ->
            numero.append(cifra)
            aggiornaDisplay()
        }

        btnBackspace.setOnClickListener {
            if (numero.isNotEmpty()) {
                numero.deleteCharAt(numero.length - 1)
                aggiornaDisplay()
            }
        }
        btnBackspace.setOnLongClickListener {
            numero.clear()
            aggiornaDisplay()
            true
        }

        btnChiama.setOnClickListener {
            if (numero.isNotEmpty()) {
                ChiamaHelper.chiama(requireContext(), numero.toString())
            }
        }

        aggiornaDisplay()
        return view
    }

    private fun aggiornaDisplay() {
        txtNumero.text = numero.toString()
        btnBackspace.visibility = if (numero.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }
}
