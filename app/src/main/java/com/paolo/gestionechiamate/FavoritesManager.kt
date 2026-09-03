package com.paolo.gestionechiamate

import android.content.Context

data class Preferito(val nome: String?, val numero: String?) {
    val isVuoto get() = numero.isNullOrBlank()
}

/**
 * Salva i 6 slot preferiti in SharedPreferences: nome e numero per ogni slot (0..5).
 */
object FavoritesManager {

    private const val PREFS = "preferiti_prefs"
    const val NUM_SLOT = 6

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context, slot: Int): Preferito {
        val p = prefs(context)
        return Preferito(
            p.getString("nome_$slot", null),
            p.getString("numero_$slot", null)
        )
    }

    fun set(context: Context, slot: Int, nome: String, numero: String) {
        prefs(context).edit()
            .putString("nome_$slot", nome)
            .putString("numero_$slot", numero)
            .apply()
    }

    fun clear(context: Context, slot: Int) {
        prefs(context).edit()
            .remove("nome_$slot")
            .remove("numero_$slot")
            .apply()
    }

    fun tutti(context: Context): List<Preferito> =
        (0 until NUM_SLOT).map { get(context, it) }
}
