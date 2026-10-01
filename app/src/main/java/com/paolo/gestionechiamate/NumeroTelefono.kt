package com.paolo.gestionechiamate

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils

/** Operazioni comuni sui numeri, senza confronti basati sulle sole ultime cifre. */
object NumeroTelefono {

    data class Contatto(val nome: String?, val fotoUri: String?)

    @Suppress("DEPRECATION")
    fun equivalenti(primo: String?, secondo: String?): Boolean {
        if (primo.isNullOrBlank() || secondo.isNullOrBlank()) return false
        return PhoneNumberUtils.compare(primo, secondo)
    }

    fun isStraniero(numero: String): Boolean {
        val pulito = numero.filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
        return when {
            pulito.startsWith("+39") || pulito.startsWith("0039") -> false
            pulito.startsWith("+") || pulito.startsWith("00") -> true
            else -> false
        }
    }

    fun isInRubrica(context: Context, numero: String): Boolean {
        return cercaContatto(context, numero) != null
    }

    fun cercaContatto(context: Context, numero: String): Contatto? {
        if (numero.isBlank()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(numero)
        )
        return context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val indiceNome = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val indiceFoto = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
            val nome = if (indiceNome >= 0) cursor.getString(indiceNome) else null
            val foto = if (indiceFoto >= 0) cursor.getString(indiceFoto) else null
            Contatto(nome, foto)
        }
    }
}
