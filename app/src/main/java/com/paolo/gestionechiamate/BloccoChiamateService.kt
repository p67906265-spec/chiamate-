package com.paolo.gestionechiamate

import android.provider.CallLog
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

        val numeroConsentito = !nascosto && numero != null &&
            Impostazioni.isNumeroConsentito(this, numero)

        if (!daBloccare && !numeroConsentito && !nascosto && numero != null) {
            if (Impostazioni.numeroConPrefissoBloccato(this, numero)) {
                daBloccare = true
            }
            if (Impostazioni.isBloccoNumeriStranieri(this) && NumeroTelefono.isStraniero(numero)) {
                daBloccare = true
            }
            if (!daBloccare && Impostazioni.isBloccoNonInRubrica(this) &&
                !NumeroTelefono.isInRubrica(this, numero)
            ) {
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

}
