package com.paolo.gestionechiamate

import android.content.Context
import android.net.Uri
import java.io.File

/** Conserva una foto distinta per ogni numero nello spazio privato dell'app. */
object FotoNumeroManager {

    private const val PREFS = "foto_numeri_prefs"

    fun getFotoUri(context: Context, numero: String): Uri? {
        val chiave = normalizza(numero)
        if (chiave.isBlank()) return null
        val nomeFile = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(chiave, null) ?: return null
        val file = File(context.filesDir, "foto_numeri/$nomeFile")
        if (!file.exists()) return null
        return Uri.fromFile(file)
    }

    fun salvaFoto(context: Context, numero: String, origine: Uri): Uri? {
        val chiave = normalizza(numero)
        if (chiave.isBlank()) return null
        val cartella = File(context.filesDir, "foto_numeri").apply { mkdirs() }
        val destinazione = File(cartella, "foto_$chiave.img")
        val temporaneo = File(cartella, "foto_$chiave.tmp")
        return try {
            context.contentResolver.openInputStream(origine)?.use { input ->
                temporaneo.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (destinazione.exists()) destinazione.delete()
            if (!temporaneo.renameTo(destinazione)) {
                temporaneo.copyTo(destinazione, overwrite = true)
                temporaneo.delete()
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(chiave, destinazione.name).apply()
            Uri.fromFile(destinazione)
        } catch (_: Exception) {
            temporaneo.delete()
            null
        }
    }

    fun rimuoviFoto(context: Context, numero: String) {
        val chiave = normalizza(numero)
        if (chiave.isBlank()) return
        getFotoUri(context, numero)?.path?.let { File(it).delete() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(chiave).apply()
    }

    fun normalizza(numero: String): String {
        var cifre = numero.filter(Char::isDigit)
        if (cifre.startsWith("0039") && cifre.length > 10) cifre = cifre.drop(4)
        else if (cifre.startsWith("39") && cifre.length > 10) cifre = cifre.drop(2)
        return cifre.takeLast(10)
    }
}
