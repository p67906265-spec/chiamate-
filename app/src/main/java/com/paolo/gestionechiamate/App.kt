package com.paolo.gestionechiamate

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Impostazioni.applicaTema(Impostazioni.getTema(this))
    }
}
