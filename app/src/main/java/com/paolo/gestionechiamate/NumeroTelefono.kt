package com.paolo.gestionechiamate

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/** Operazioni comuni sui numeri, senza confronti basati sulle sole ultime cifre. */
object NumeroTelefono {

    fun isStraniero(numero: String): Boolean {
        val pulito = numero.filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
        return when {
            pulito.startsWith("+39") || pulito.startsWith("0039") -> false
            pulito.startsWith("+") || pulito.startsWith("00") -> true
            else -> false
        }
    }

    fun isInRubrica(context: Context, numero: String): Boolean {
        if (numero.isBlank()) return false
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(numero)
        )
        return context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )?.use { it.moveToFirst() } == true
    }
}
