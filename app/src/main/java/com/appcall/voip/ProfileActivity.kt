package com.appcall.voip

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class ProfileActivity : ComponentActivity() {

    private var profileWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()

        val webView = WebView(this)
        profileWebView = webView
        webView.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.addJavascriptInterface(ProfileJsBridge(), "AndroidBridge")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                    """
                    (function () {
                      var backBtn = document.querySelector('header button');
                      if (backBtn) backBtn.addEventListener('click', function(){ window.AndroidBridge.back(); });
                      var allButtons = document.querySelectorAll('button');
                      if (allButtons.length > 0) {
                        var logoutBtn = null;
                        for (var i = 0; i < allButtons.length; i++) {
                          var t = (allButtons[i].innerText || '').toLowerCase();
                          if (t.indexOf('log out') >= 0 || t.indexOf('logout') >= 0) { logoutBtn = allButtons[i]; break; }
                        }
                        if (!logoutBtn) logoutBtn = allButtons[allButtons.length - 1];
                        logoutBtn.addEventListener('click', function(){ window.AndroidBridge.logout(); });
                      }
                      var navButtons = document.querySelectorAll('nav button');
                      if (navButtons[0]) navButtons[0].addEventListener('click', function(){ window.AndroidBridge.goHome(); });
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }
        webView.loadUrl("file:///android_asset/c_nh_n/code.html")
        setContentView(webView)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    override fun onDestroy() {
        profileWebView?.let { web ->
            (web.parent as? ViewGroup)?.removeView(web)
            web.stopLoading()
            web.destroy()
        }
        profileWebView = null
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

    private inner class ProfileJsBridge {
        @JavascriptInterface
        fun back() {
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun logout() {
            SIPManager.unregister()
            runOnUiThread {
                val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        @JavascriptInterface
        fun goHome() {
            runOnUiThread {
                startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
