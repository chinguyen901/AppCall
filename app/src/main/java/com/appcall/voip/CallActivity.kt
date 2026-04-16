package com.appcall.voip

import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CallActivity : ComponentActivity() {

    private lateinit var etCallee: EditText
    private lateinit var tvCallStatus: TextView
    private lateinit var incomingLayout: LinearLayout
    private lateinit var callReceiver: CallReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        etCallee = findViewById(R.id.etCallee)
        tvCallStatus = findViewById(R.id.tvCallStatus)
        incomingLayout = findViewById(R.id.incomingLayout)
        intent.getStringExtra("target_user")?.let { etCallee.setText(it) }

        val btnCall: Button = findViewById(R.id.btnCall)
        val btnEndCall: Button = findViewById(R.id.btnEndCall)
        val btnAccept: Button = findViewById(R.id.btnAccept)
        val btnReject: Button = findViewById(R.id.btnReject)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        callReceiver = CallReceiver {
            incomingLayout.visibility = View.VISIBLE
        }
        ContextCompat.registerReceiver(
            this,
            callReceiver,
            IntentFilter(CallReceiver.ACTION_INCOMING_CALL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        btnCall.setOnClickListener {
            val callee = etCallee.text.toString().trim()
            if (callee.isNotBlank()) SIPManager.makeCall(callee)
        }

        btnEndCall.setOnClickListener {
            SIPManager.endCall()
            incomingLayout.visibility = View.GONE
        }

        btnAccept.setOnClickListener {
            SIPManager.answerCall()
            incomingLayout.visibility = View.GONE
        }

        btnReject.setOnClickListener {
            SIPManager.rejectCall()
            incomingLayout.visibility = View.GONE
        }

        btnLogout.setOnClickListener {
            SIPManager.unregister()
            finishAffinity()
        }

        lifecycleScope.launch {
            SIPManager.callState.collect { state ->
                val selfStatus = SIPStateObserver.selfStatus.value
                tvCallStatus.text = "Call: $state | SIP: ${selfStatus.emoji} ${selfStatus.label}"
                if (!state.startsWith("Incoming")) {
                    incomingLayout.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SIPManager.routeAudioToEarpiece()
    }

    override fun onPause() {
        super.onPause()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    override fun onDestroy() {
        unregisterReceiver(callReceiver)
        super.onDestroy()
    }
}
