package com.appcall.voip

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var tvSelfStatus: TextView
    private lateinit var listUsers: ListView
    private lateinit var userAdapter: ArrayAdapter<String>
    private var mainWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()

        if (AppConfig.USE_PREBUILT_UI) {
            val webView = WebView(this)
            mainWebView = webView
            webView.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            webView.settings.javaScriptEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            webView.settings.domStorageEnabled = true
            webView.settings.loadsImagesAutomatically = true
            webView.addJavascriptInterface(MainJsBridge(), "AndroidBridge")
            webView.webChromeClient = WebChromeClient()
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        """
                        (function () {
                          window.AndroidBridge.logEvent('inject-start');
                          function firstText(el, selector) {
                            var n = el && el.querySelector(selector);
                            return (n && n.innerText || '').trim();
                          }
                          // Open chat screen via event delegation for every chat card.
                          document.addEventListener('click', function (e) {
                            var row = e.target.closest('section .cursor-pointer, section .rounded-2xl');
                            if (!row) return;
                            var name = firstText(row, 'h3');
                            if (!name || name.toLowerCase().indexOf('archived') >= 0) return;
                            window.AndroidBridge.openChat(name);
                          }, true);

                          // Top-left avatar also opens profile.
                          var avatar = document.querySelector('header img');
                          if (avatar && !avatar.dataset.androidHooked) {
                            avatar.dataset.androidHooked = '1';
                            avatar.style.cursor = 'pointer';
                            avatar.addEventListener('click', function (e) {
                              e.preventDefault();
                              window.AndroidBridge.openProfile();
                            });
                          }

                          // Add keypad/call button on header.
                          var headerActions = document.querySelector('header .flex.items-center.gap-2');
                          if (headerActions && !document.getElementById('android-dial-btn')) {
                            var dialBtn = document.createElement('button');
                            dialBtn.id = 'android-dial-btn';
                            dialBtn.className = 'w-10 h-10 flex items-center justify-center hover:bg-slate-200/50 rounded-full transition-colors';
                            dialBtn.innerHTML = '<span class="material-symbols-outlined">dialpad</span>';
                            dialBtn.addEventListener('click', function () { window.AndroidBridge.openDialPad(); });
                            headerActions.appendChild(dialBtn);
                          }
                          // Add profile button in bottom nav.
                          var nav = document.querySelector('nav .flex.justify-around');
                          if (nav && !document.getElementById('android-profile-btn')) {
                            var profileBtn = document.createElement('button');
                            profileBtn.id = 'android-profile-btn';
                            profileBtn.className = 'flex flex-col items-center justify-center text-slate-500 w-12 h-12 hover:bg-slate-200/50 rounded-full transition-colors';
                            profileBtn.innerHTML = '<span class="material-symbols-outlined">person</span>';
                            profileBtn.addEventListener('click', function () { window.AndroidBridge.openProfile(); });
                            nav.appendChild(profileBtn);
                          }
                          window.AndroidBridge.logEvent('inject-done');
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            webView.loadUrl("file:///android_asset/tin_nh_n/code.html")
            setContentView(webView)
            return
        }

        setContentView(R.layout.activity_main)

        tvSelfStatus = findViewById(R.id.tvSelfStatus)
        listUsers = findViewById(R.id.listUsers)
        val btnOpenCallScreen: Button = findViewById(R.id.btnOpenCallScreen)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        userAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listUsers.adapter = userAdapter

        listUsers.setOnItemClickListener { _, _, position, _ ->
            val users = SIPStateObserver.defaultUsers.value
            val user = users.getOrNull(position) ?: return@setOnItemClickListener
            startActivity(Intent(this, CallActivity::class.java).putExtra("target_user", user.username))
        }

        btnOpenCallScreen.setOnClickListener {
            startActivity(Intent(this, CallActivity::class.java))
        }

        btnLogout.setOnClickListener {
            SIPManager.unregister()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        lifecycleScope.launch {
            SIPStateObserver.selfStatus.collect { status ->
                tvSelfStatus.text = "My SIP status: ${status.emoji} ${status.label}"
            }
        }

        lifecycleScope.launch {
            SIPStateObserver.defaultUsers.collect { users ->
                userAdapter.clear()
                userAdapter.addAll(users.map { "${it.status.emoji} ${it.username}" })
                userAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    override fun onDestroy() {
        mainWebView?.let { web ->
            (web.parent as? ViewGroup)?.removeView(web)
            web.stopLoading()
            web.destroy()
        }
        mainWebView = null
        super.onDestroy()
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun openDialPadDialog() {
        val input = EditText(this).apply {
            hint = "Enter SIP number (e.g. 1002)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(this)
            .setTitle("Quick Call")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Call") { _, _ ->
                val target = input.text.toString().trim()
                if (target.isNotBlank()) {
                    startActivity(
                        Intent(this, CallActivity::class.java)
                            .putExtra("target_user", target)
                            .putExtra("auto_call", true)
                    )
                }
            }
            .show()
    }

    private inner class MainJsBridge {
        @JavascriptInterface
        fun logEvent(name: String?) {
            Log.d(TAG, "Web event: ${name.orEmpty()}")
        }

        @JavascriptInterface
        fun openProfile() {
            runOnUiThread {
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
            }
        }

        @JavascriptInterface
        fun openChat(username: String?) {
            val target = username?.trim().orEmpty()
            runOnUiThread {
                startActivity(Intent(this@MainActivity, CallActivity::class.java).putExtra("target_user", target))
            }
        }

        @JavascriptInterface
        fun openDialPad() {
            runOnUiThread { openDialPadDialog() }
        }
    }
}
