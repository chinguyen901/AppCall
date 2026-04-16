package com.appcall.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallReceiver(private val onIncoming: (() -> Unit)? = null) : BroadcastReceiver() {
    constructor() : this(null)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_INCOMING_CALL) {
            onIncoming?.invoke()
        }
    }

    companion object {
        const val ACTION_INCOMING_CALL = "com.appcall.voip.ACTION_INCOMING_CALL"
    }
}
