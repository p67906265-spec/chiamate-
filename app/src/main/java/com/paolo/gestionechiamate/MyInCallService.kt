package com.paolo.gestionechiamate

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class MyInCallService : InCallService() {

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
