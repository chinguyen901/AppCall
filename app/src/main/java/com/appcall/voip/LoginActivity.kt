package com.appcall.voip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etDomain: EditText
    private lateinit var etProxy: EditText
    private lateinit var tvStatus: TextView
    private var shouldOpenMainOnRegister = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
