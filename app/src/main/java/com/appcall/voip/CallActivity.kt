package com.appcall.voip

import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CallActivity : ComponentActivity() {

    private lateinit var etCallee: EditText
    private lateinit var tvCallStatus: TextView
    private lateinit var incomingLayout: LinearLayout
    private lateinit var callReceiver: CallReceiver
    private var callWebView: WebView? = null
    private var activeRemoteNumber: String = ""

    private val sipContacts = mapOf(
        "1001" to "User 1001",
        "1002" to "User 1002",
        "1003" to "User 1003"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()

        if (AppConfig.USE_PREBUILT_UI) {
            val target = intent.getStringExtra("target_user").orEmpty()
            val autoCall = intent.getBooleanExtra("auto_call", false)
            activeRemoteNumber = normalizeSipUser(target)
            val webView = WebView(this)
            callWebView = webView
            webView.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            webView.settings.javaScriptEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            webView.settings.domStorageEnabled = true
            webView.settings.loadsImagesAutomatically = true
            webView.addJavascriptInterface(CallJsBridge(), "AndroidBridge")
            webView.webChromeClient = WebChromeClient()
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        """
                        (function () {
                          var target = ${toJsString(resolveContactDisplayName(activeRemoteNumber))};
                          var title = document.querySelector('header h1, header .title-lg');
                          if (title && target) title.textContent = target;
                          var endBtn = document.getElementById('end-call-btn');
                          if (endBtn) endBtn.addEventListener('click', function(){ window.AndroidBridge.endCall(); });
                          var keypadBtn = document.getElementById('keypad-btn');
                          if (keypadBtn) keypadBtn.addEventListener('click', function(){ window.AndroidBridge.openDialPad(); });
                          var audioBtn = document.getElementById('audio-btn');
                          if (audioBtn) audioBtn.addEventListener('click', function(){ window.AndroidBridge.toggleSpeaker(); });
                          var muteBtn = document.getElementById('mute-btn');
                          if (muteBtn) muteBtn.addEventListener('click', function(){ window.AndroidBridge.toggleMute(); });
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            webView.loadUrl("file:///android_asset/ui/in_call.html")
            setContentView(webView)

            lifecycleScope.launch {
                SIPManager.callState.collect { state ->
                    val incomingNumber = extractRemoteNumberFromState(state)
                    if (incomingNumber.isNotBlank()) {
                        activeRemoteNumber = incomingNumber
                    }
                    val display = resolveContactDisplayName(activeRemoteNumber)
                    updateWebCallHeader(display)
                }
            }

            if (autoCall && target.isNotBlank()) {
                SIPManager.makeCall(target)
                if (SIPManager.callState.value.startsWith("Cannot call")) {
                    Toast.makeText(this, SIPManager.callState.value, Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

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
        enableImmersiveMode()
        SIPManager.routeAudioToEarpiece()
    }

    override fun onPause() {
        super.onPause()
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    override fun onDestroy() {
        if (!AppConfig.USE_PREBUILT_UI) {
            unregisterReceiver(callReceiver)
        } else {
            callWebView?.let { web ->
                (web.parent as? ViewGroup)?.removeView(web)
                web.stopLoading()
                web.destroy()
            }
            callWebView = null
        }
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private inner class CallJsBridge {
        @JavascriptInterface
        fun back() {
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun makeVoiceCall(target: String?) {
            val callee = target?.trim().orEmpty()
            if (callee.isBlank()) return
            SIPManager.makeCall(callee)
        }

        @JavascriptInterface
        fun endCall() {
            SIPManager.endCall()
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun toggleSpeaker() {
            runOnUiThread {
                if (SIPManager.callState.value.contains("Connected", ignoreCase = true)) {
                    SIPManager.routeAudioToSpeaker()
                } else {
                    SIPManager.routeAudioToEarpiece()
                }
            }
        }

        @JavascriptInterface
        fun toggleMute() {
            runOnUiThread {
                Toast.makeText(this@CallActivity, "Mute toggle placeholder", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun openDialPad() {
            runOnUiThread {
                Toast.makeText(this@CallActivity, "DTMF keypad coming next", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normalizeSipUser(value: String): String {
        val raw = value.trim()
        if (raw.isBlank()) return ""
        return raw.substringAfter("sip:").substringBefore("@").trim()
    }

    private fun resolveContactDisplayName(numberOrSip: String): String {
        val number = normalizeSipUser(numberOrSip)
        if (number.isBlank()) return ""
        val observerUser = SIPStateObserver.defaultUsers.value.firstOrNull { it.username == number }?.username
        return sipContacts[number] ?: observerUser ?: number
    }

    private fun extractRemoteNumberFromState(state: String): String {
        if (!state.startsWith("Incoming:", ignoreCase = true) &&
            !state.startsWith("Calling ", ignoreCase = true)
        ) {
            return ""
        }
        val raw = when {
            state.startsWith("Incoming:", ignoreCase = true) -> state.substringAfter("Incoming:")
            else -> state.substringAfter("Calling ")
        }
        return normalizeSipUser(raw)
    }

    private fun updateWebCallHeader(displayName: String) {
        val webView = callWebView ?: return
        if (displayName.isBlank()) return
        webView.evaluateJavascript(
            """
            (function() {
              var title = document.querySelector('header h1, header .title-lg');
              if (title) title.textContent = ${toJsString(displayName)};
            })();
            """.trimIndent(),
            null
        )
    }

    private fun toJsString(value: String): String {
        return "'" + value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "") + "'"
    }
}
