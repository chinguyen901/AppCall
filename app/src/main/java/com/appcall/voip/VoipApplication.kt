package com.appcall.voip

import android.app.Application

class VoipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SIPManager.init(this)
    }
}
