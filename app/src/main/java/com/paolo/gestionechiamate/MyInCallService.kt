package com.paolo.gestionechiamate

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

/**
 * Viene attivato dal sistema Android solo quando questa app è impostata come
 * app "Telefono" predefinita (vedi MainActivity.richiediRuoloDialer).
 * Riceve gli oggetti Call in corso e apre InCallActivity, che mostra sempre
 * il tastierino durante la chiamata.
 */
class MyInCallService : InCallService() {

    companion object {
        // Riferimento statico alla chiamata attiva, letto da InCallActivity.
        // android.telecom.Call non è Parcelable quindi non può passare in un Intent.
        var chiamataAttiva: Call? = null
            private set
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_DISCONNECTED) {
                if (chiamataAttiva == call) chiamataAttiva = null
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        chiamataAttiva = call
        call.registerCallback(callback)

        val intent = Intent(this, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        if (chiamataAttiva == call) chiamataAttiva = null
    }
}
