package com.paolo.gestionechiamate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

object ChiamaHelper {

    fun chiama(context: Context, numero: String) {
        if (numero.isBlank()) return
        val intent = if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$numero"))
        } else {
            // Fallback: apre il compositore di sistema se manca il permesso CALL_PHONE
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Apre il pannello di chiamata con il numero già inserito, senza chiamare subito. */
    fun apriPannelloChiamata(context: Context, numero: String) {
        if (numero.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Apre la scheda del contatto (nome, numeri, foto, ecc.) senza avviare la chiamata. */
    fun apriSchedaContatto(context: Context, id: Long, lookupKey: String?, numero: String) {
        val uri = if (id > 0 && !lookupKey.isNullOrBlank()) {
            android.provider.ContactsContract.Contacts.getLookupUri(id, lookupKey)
        } else null

        val intent = if (uri != null) {
            Intent(Intent.ACTION_VIEW, uri)
        } else {
            // Fallback se non abbiamo un id valido: cerca il numero nella rubrica di sistema
            Intent(Intent.ACTION_VIEW, Uri.parse("tel:$numero"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
