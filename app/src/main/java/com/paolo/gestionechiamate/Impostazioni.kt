package com.paolo.gestionechiamate

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Impostazioni {

    data class PrefissoBloccato(val prefisso: String, val descrizione: String)

    private const val PREFS = "impostazioni_prefs"

    const val TEMA_CHIARO = 0
    const val TEMA_SCURO = 1
    private const val KEY_COLORE_TESTO = "colore_testo"
    private const val KEY_RICHIAMO_AUTOMATICO = "richiamo_automatico"
    private const val KEY_MESSAGGIO_RIFIUTO = "messaggio_rifiuto"
    private const val KEY_BLOCCO_PREFISSI_ATTIVO = "blocco_prefissi_attivo"
    private const val KEY_PREFISSI_BLOCCATI = "prefissi_bloccati"
    const val MESSAGGIO_RIFIUTO_PREDEFINITO = "Sono occupato, ti richiamo dopo"
    const val TEMA_SISTEMA = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getTema(context: Context): Int = prefs(context).getInt("tema", TEMA_SISTEMA)

    fun setTema(context: Context, tema: Int) {
        prefs(context).edit().putInt("tema", tema).apply()
        applicaTema(tema)
    }

    fun getColoreTesto(context: Context): Int? {
        val valore = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_COLORE_TESTO, Int.MIN_VALUE)
        return valore.takeIf { it != Int.MIN_VALUE }
    }

    fun setColoreTesto(context: Context, colore: Int?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (colore == null) remove(KEY_COLORE_TESTO) else putInt(KEY_COLORE_TESTO, colore)
        }.apply()
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

    fun isBloccoPrefissiAttivo(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCCO_PREFISSI_ATTIVO, true)

    fun setBloccoPrefissiAttivo(context: Context, valore: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCCO_PREFISSI_ATTIVO, valore).apply()
    }

    fun getPrefissiBloccati(context: Context): List<PrefissoBloccato> =
        prefs(context).getStringSet(KEY_PREFISSI_BLOCCATI, emptySet()).orEmpty()
            .mapNotNull { voce ->
                val parti = voce.split('\t', limit = 2)
                val prefisso = parti.firstOrNull()?.filter(Char::isDigit).orEmpty()
                if (prefisso.isBlank()) null
                else PrefissoBloccato(prefisso, parti.getOrElse(1) { "" })
            }
            .distinctBy { it.prefisso }
            .sortedBy { it.prefisso }

    fun aggiungiPrefissoBloccato(context: Context, prefissoInserito: String, descrizione: String): Boolean {
        val prefisso = normalizzaPrefissoItaliano(prefissoInserito) ?: return false
        val lista = getPrefissiBloccati(context).filterNot { it.prefisso == prefisso }.toMutableList()
        lista += PrefissoBloccato(prefisso, descrizione.trim().replace("\t", " "))
        salvaPrefissi(context, lista)
        return true
    }

    fun eliminaPrefissoBloccato(context: Context, prefisso: String) {
        salvaPrefissi(context, getPrefissiBloccati(context).filterNot { it.prefisso == prefisso })
    }

    fun numeroConPrefissoBloccato(context: Context, numero: String): Boolean {
        if (!isBloccoPrefissiAttivo(context)) return false
        val nazionale = normalizzaNumeroItaliano(numero) ?: return false
        return getPrefissiBloccati(context).any { nazionale.startsWith(it.prefisso) }
    }

    private fun salvaPrefissi(context: Context, lista: List<PrefissoBloccato>) {
        val valori = lista.map { "${it.prefisso}\t${it.descrizione}" }.toSet()
        prefs(context).edit().putStringSet(KEY_PREFISSI_BLOCCATI, valori).apply()
    }

    private fun normalizzaPrefissoItaliano(valore: String): String? {
        val originale = valore.trim()
        var cifre = originale.filter(Char::isDigit)
        if (originale.startsWith("+39")) cifre = cifre.removePrefix("39")
        else if (cifre.startsWith("0039")) cifre = cifre.removePrefix("0039")
        return cifre.takeIf { it.length in 2..10 }
    }

    private fun normalizzaNumeroItaliano(numero: String): String? {
        val originale = numero.trim()
        var cifre = originale.filter(Char::isDigit)
        if (originale.startsWith("+") && !originale.startsWith("+39")) return null
        if (cifre.startsWith("00") && !cifre.startsWith("0039")) return null
        if (cifre.startsWith("0039")) cifre = cifre.removePrefix("0039")
        else if (originale.startsWith("+39")) cifre = cifre.removePrefix("39")
        else if (cifre.startsWith("39") && cifre.length > 10) cifre = cifre.removePrefix("39")
        return cifre.takeIf { it.isNotBlank() }
    }

    fun isRichiamoAutomatico(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RICHIAMO_AUTOMATICO, true)

    fun setRichiamoAutomatico(context: Context, valore: Boolean) {
        prefs(context).edit().putBoolean(KEY_RICHIAMO_AUTOMATICO, valore).apply()
    }

    fun getMessaggioRifiuto(context: Context): String =
        prefs(context).getString(KEY_MESSAGGIO_RIFIUTO, MESSAGGIO_RIFIUTO_PREDEFINITO)
            ?.takeIf { it.isNotBlank() } ?: MESSAGGIO_RIFIUTO_PREDEFINITO

    fun setMessaggioRifiuto(context: Context, valore: String) {
        prefs(context).edit().putString(
            KEY_MESSAGGIO_RIFIUTO,
            valore.trim().ifBlank { MESSAGGIO_RIFIUTO_PREDEFINITO }
        ).apply()
    }
}
