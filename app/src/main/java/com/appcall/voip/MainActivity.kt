package com.appcall.voip

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tvSelfStatus: TextView
    private lateinit var listUsers: ListView
    private lateinit var userAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
