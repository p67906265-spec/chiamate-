package com.paolo.gestionechiamate

import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse

class BloccoChiamateService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val numero = callDetails.handle?.schemeSpecificPart

        val nascosto = callDetails.handle == null ||
            callDetails.handlePresentation == CallLog.Calls.PRESENTATION_RESTRICTED ||
            callDetails.handlePresentation == CallLog.Calls.PRESENTATION_UNKNOWN

        var daBloccare = false

        if (nascosto && Impostazioni.isBloccoNumeriNascosti(this)) {
            daBloccare = true
        }

        if (!daBloccare && !nascosto && numero != null) {
            if (Impostazioni.numeroConPrefissoBloccato(this, numero)) {
                daBloccare = true
            }
            if (Impostazioni.isBloccoNumeriStranieri(this) && isNumeroStraniero(numero)) {
                daBloccare = true
            }
            if (!daBloccare && Impostazioni.isBloccoNonInRubrica(this) && !isNumeroInRubrica(numero)) {
                daBloccare = true
            }
        }

        val risposta = CallResponse.Builder()
        if (daBloccare) {
            risposta.setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
        }
        respondToCall(callDetails, risposta.build())
    }

    private fun isNumeroStraniero(numero: String): Boolean {
        val pulito = numero.trim()
        if (pulito.startsWith("+")) {
            return !(pulito.startsWith("+39"))
        }
        if (pulito.startsWith("0039")) return false
        if (pulito.startsWith("00")) return true
        return false
    }

    private fun isNumeroInRubrica(numero: String): Boolean {
        val cifreNumero = soloCifre(numero)
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )
        cursor?.use {
            val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val numeroRubrica = if (idx >= 0) it.getString(idx) else null
                if (numeroRubrica != null && soloCifre(numeroRubrica) == cifreNumero) {
                    return true
                }
            }
        }
        return false
    }

    private fun soloCifre(numero: String): String {
        var cifre = numero.filter { it.isDigit() }
        if (cifre.length > 10) cifre = cifre.takeLast(10)
        return cifre
    }
}
