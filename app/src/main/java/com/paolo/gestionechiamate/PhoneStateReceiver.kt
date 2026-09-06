package com.paolo.gestionechiamate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val stato = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        when (stato) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (siamoGiaDialerPredefinito(context)) return
                if (puoDisegnareOverlay(context)) {
                    val service = Intent(context, CallOverlayService::class.java)
                        .setAction(CallOverlayService.AZIONE_MOSTRA)
                    context.startForegroundService(service)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                context.startService(
                    Intent(context, CallOverlayService::class.java)
                        .setAction(CallOverlayService.AZIONE_NASCONDI)
                )
            }
        }
    }

    private fun siamoGiaDialerPredefinito(context: Context): Boolean {
        val telecomManager = context.getSystemService(android.telecom.TelecomManager::class.java)
        return telecomManager?.defaultDialerPackage == context.packageName
    }

    private fun puoDisegnareOverlay(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
}
