package com.paolo.gestionechiamate

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Impostazioni {

    private const val PREFS = "impostazioni_prefs"

    const val TEMA_CHIARO = 0
    const val TEMA_SCURO = 1
    const val TEMA_SISTEMA = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getTema(context: Context): Int = prefs(context).getInt("tema", TEMA_SISTEMA)

    fun setTema(context: Context, tema: Int) {
        prefs(context).edit().putInt("tema", tema).apply()
        applicaTema(tema)
    }

    fun applicaTema(tema: Int) {
        val modalita = when (tema) {
            TEMA_CHIARO -> AppCompatDelegate.MODE_NIGHT_NO
            TEMA_SCURO -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(modalita)
    }

    fun isBloccoNumeriNascosti(context: Context): Boolean =
        prefs(context).getBoolean("blocco_nascosti", false)

    fun setBloccoNumeriNascosti(context: Context, valore: Boolean) {
        prefs(context).edit().putBoolean("blocco_nascosti", valore).apply()
    }

    fun isBloccoNumeriStranieri(context: Context): Boolean =
        prefs(context).getBoolean("blocco_stranieri", false)

    fun setBloccoNumeriStranieri(context: Context, valore: Boolean) {
        prefs(context).edit().putBoolean("blocco_stranieri", valore).apply()
    }

    fun isBloccoNonInRubrica(context: Context): Boolean =
        prefs(context).getBoolean("blocco_non_in_rubrica", false)

    fun setBloccoNonInRubrica(context: Context, valore: Boolean) {
        prefs(context).edit().putBoolean("blocco_non_in_rubrica", valore).apply()
    }
}
