package com.paolo.gestionechiamate

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat

object ColoriTesto {
    fun applica(root: View) {
        val colore = Impostazioni.getColoreTesto(root.context) ?: return
        applicaRicorsivo(root, colore)
    }

    private fun applicaRicorsivo(view: View, colore: Int) {
        if (view is TextView) {
            val primario = ContextCompat.getColor(view.context, R.color.text_primary)
            val secondario = ContextCompat.getColor(view.context, R.color.text_secondary)
            if (view.currentTextColor == primario || view.currentTextColor == secondario) {
                view.setTextColor(colore)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applicaRicorsivo(view.getChildAt(i), colore)
        }
    }
}
