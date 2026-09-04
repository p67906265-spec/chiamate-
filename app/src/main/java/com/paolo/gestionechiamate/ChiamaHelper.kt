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
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun apriPannelloChiamata(context: Context, numero: String) {
        if (numero.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
