package com.paolo.gestionechiamate

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.InCallService

class MyInCallService : InCallService() {

    private val chiamateInUscita = mutableSetOf<Call>()

    companion object {
        var chiamataAttiva: Call? = null
            private set

        var istanza: MyInCallService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        istanza = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (istanza == this) istanza = null
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_DIALING || state == Call.STATE_CONNECTING) {
                chiamateInUscita += call
            }
            if (state == Call.STATE_ACTIVE && call in chiamateInUscita) {
                AutoRichiamo.chiamataConnessa(this@MyInCallService)
            }
            if (state == Call.STATE_DISCONNECTED) {
                val numero = call.details.handle?.schemeSpecificPart.orEmpty()
                if (call in chiamateInUscita) {
                    if (call.details.disconnectCause.code == DisconnectCause.BUSY) {
                        AutoRichiamo.numeroOccupato(this@MyInCallService, numero)
                    } else {
                        AutoRichiamo.chiamataTerminataNonOccupata(this@MyInCallService, numero)
                    }
                }
                if (chiamataAttiva == call) chiamataAttiva = null
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        chiamataAttiva = call
        val uscita = call.state == Call.STATE_DIALING || call.state == Call.STATE_CONNECTING ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                call.details.callDirection == Call.Details.DIRECTION_OUTGOING)
        if (uscita) chiamateInUscita += call
        call.registerCallback(callback)

        val intent = Intent(this, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callback)
        chiamateInUscita -= call
        if (chiamataAttiva == call) chiamataAttiva = null
    }
}
