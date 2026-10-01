package com.paolo.gestionechiamate

import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import java.util.Locale

object LocalitaNumero {

    fun cerca(numero: String): String? {
        if (!InfoNumero.isFissoItaliano(numero)) return null
        return try {
            val analizzatore = PhoneNumberUtil.getInstance()
            val parsed = analizzatore.parse(numero, "IT")
            if (!analizzatore.isValidNumberForRegion(parsed, "IT")) return null
            PhoneNumberOfflineGeocoder.getInstance()
                .getDescriptionForNumber(parsed, Locale.ITALIAN)
                .trim()
                .ifBlank { null }
        } catch (e: Exception) {
            Log.w("LocalitaNumero", "Località offline non disponibile", e)
            null
        }
    }
}
