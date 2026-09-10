package com.paolo.gestionechiamate

import android.app.Application
import android.app.Activity
import android.os.Bundle

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Impostazioni.applicaTema(Impostazioni.getTema(this))
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activity.window.decorView.post { ColoriTesto.applica(activity.window.decorView) }
                activity.window.decorView.postDelayed(
                    { ColoriTesto.applica(activity.window.decorView) }, 500
                )
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
