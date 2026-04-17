package com.appcall.voip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etDomain: EditText
    private lateinit var etProxy: EditText
    private lateinit var tvStatus: TextView
    private var shouldOpenMainOnRegister = false
    private var loginWebView: WebView? = null
    private var webLoginNavigatedToMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()

        if (AppConfig.USE_PREBUILT_UI) {
            requestPermissionsIfNeeded()
            val webView = WebView(this)
            loginWebView = webView
            webView.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            webView.settings.javaScriptEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            webView.settings.domStorageEnabled = true
            webView.settings.loadsImagesAutomatically = true
            webView.addJavascriptInterface(LoginJsBridge(), "AndroidBridge")
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Wire HTML login actions to native SIP login reliably.
                    view?.evaluateJavascript(
                        """
                        (function () {
                          function showBanner(message, ok) {
                            var id = 'appcall-login-banner';
                            var old = document.getElementById(id);
                            if (old) old.remove();
                            var div = document.createElement('div');
                            div.id = id;
                            div.textContent = message;
                            div.style.position = 'fixed';
                            div.style.top = '16px';
                            div.style.left = '50%';
                            div.style.transform = 'translateX(-50%)';
                            div.style.padding = '10px 14px';
                            div.style.borderRadius = '999px';
                            div.style.fontWeight = '600';
                            div.style.zIndex = '99999';
                            div.style.color = '#fff';
                            div.style.background = ok ? '#16a34a' : '#dc2626';
                            div.style.boxShadow = '0 8px 24px rgba(0,0,0,0.18)';
                            document.body.appendChild(div);
                            setTimeout(function () { if (div && div.parentNode) div.parentNode.removeChild(div); }, 2800);
                          }
                          window.__appcallShowLoginResult = showBanner;

                          function submitNativeLogin() {
                            var form = document.querySelector('form');
                            var inputs = form ? form.querySelectorAll('input') : document.querySelectorAll('input');
                            var user = (inputs[0] && inputs[0].value || '').trim();
                            var pass = (inputs[1] && inputs[1].value || '');
                            if (!user || !pass) {
                              showBanner('Please enter username and password', false);
                              return;
                            }
                            window.AndroidBridge.login(user, pass);
                          }

                          var form = document.querySelector('form');
                          if (form && form.dataset.androidHooked !== '1') {
                            form.dataset.androidHooked = '1';
                            form.addEventListener('submit', function (e) {
                              e.preventDefault();
                              submitNativeLogin();
                            });
                          }

                          var btns = document.querySelectorAll('button');
                          for (var i = 0; i < btns.length; i++) {
                            var t = (btns[i].innerText || '').toLowerCase();
                            if (t.indexOf('log in') >= 0 || t.indexOf('login') >= 0) {
                              if (!btns[i].dataset.androidHooked) {
                                btns[i].dataset.androidHooked = '1';
                                btns[i].addEventListener('click', function (e) {
                                  e.preventDefault();
                                  submitNativeLogin();
                                });
                              }
                            }
                          }

                          var allInputs = document.querySelectorAll('input');
                          for (var j = 0; j < allInputs.length; j++) {
                            allInputs[j].addEventListener('keydown', function (e) {
                              if (e.key === 'Enter') {
                                e.preventDefault();
                                submitNativeLogin();
                              }
                            });
                          }
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }
            webView.loadUrl("file:///android_asset/ng_nh_p/code.html")
            setContentView(webView)
            // SIP register runs in background; home opens after credentials are submitted (see LoginJsBridge).
            return
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etDomain = findViewById(R.id.etDomain)
        etProxy = findViewById(R.id.etProxy)
        tvStatus = findViewById(R.id.tvStatus)
        val btnLogin: Button = findViewById(R.id.btnLogin)

        etDomain.setText(AppConfig.DEFAULT_SIP_DOMAIN)
        etProxy.setText(AppConfig.DEFAULT_SIP_PROXY)

        requestPermissionsIfNeeded()

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            val domainInput = etDomain.text.toString().trim()
            val proxyInput = etProxy.text.toString().trim()
            val domain = domainInput.ifBlank { AppConfig.DEFAULT_SIP_DOMAIN }
            val proxy = proxyInput.ifBlank { AppConfig.buildProxyFromDomain(domain) }

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill username/password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            shouldOpenMainOnRegister = true
            SIPManager.register(username, password, domain, proxy)
        }

        lifecycleScope.launch {
            SIPManager.registrationState.collect { state ->
                tvStatus.text = "SIP Register: $state"
                if (shouldOpenMainOnRegister && state.startsWith("Registered")) {
                    shouldOpenMainOnRegister = false
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val required = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
        val mutablePermissions = required.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutablePermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val denied = mutablePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (denied.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, denied.toTypedArray(), 100)
        }
    }

    override fun onDestroy() {
        loginWebView?.let { web ->
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            web.stopLoading()
            web.destroy()
        }
        loginWebView = null
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private inner class LoginJsBridge {
        @JavascriptInterface
        fun login(username: String?, password: String?) {
            val safeUser = username?.trim().orEmpty()
            val safePass = password.orEmpty()
            Log.d(TAG, "Web login requested user=$safeUser")
            if (safeUser.isBlank() || safePass.isBlank()) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Please fill username/password", Toast.LENGTH_SHORT).show()
                }
                return
            }
            if (webLoginNavigatedToMain) return
            webLoginNavigatedToMain = true
            val domain = AppConfig.DEFAULT_SIP_DOMAIN
            val proxy = AppConfig.DEFAULT_SIP_PROXY
            SIPManager.register(safeUser, safePass, domain, proxy)
            runOnUiThread {
                showLoginBanner("Signing in…", true)
                startActivity(
                    Intent(this@LoginActivity, MainActivity::class.java)
                        .putExtra("logged_in_user", safeUser)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                finish()
            }
        }
    }

    private fun showLoginBanner(message: String, success: Boolean) {
        loginWebView?.post {
            loginWebView?.evaluateJavascript(
                "window.__appcallShowLoginResult && window.__appcallShowLoginResult(${message.asJsString()}, ${if (success) "true" else "false"});",
                null
            )
        }
    }

    private fun String.asJsString(): String {
        return "'" + this
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n") + "'"
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
