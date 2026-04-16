package com.appcall.voip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserSipStatus(val emoji: String, val label: String) {
    ONLINE("🟢", "Online"),
    BUSY("🔴", "Busy"),
    CONNECTING("🟡", "Connecting"),
    OFFLINE("⚫", "Offline")
}

data class SipUserPresence(
    val username: String,
    val status: UserSipStatus
)

object SIPStateObserver {
    private val _selfStatus = MutableStateFlow(UserSipStatus.OFFLINE)
    val selfStatus: StateFlow<UserSipStatus> = _selfStatus.asStateFlow()

    private val _defaultUsers = MutableStateFlow(
        listOf(
            SipUserPresence("1001", UserSipStatus.OFFLINE),
            SipUserPresence("1002", UserSipStatus.OFFLINE),
            SipUserPresence("1003", UserSipStatus.OFFLINE)
        )
    )
    val defaultUsers: StateFlow<List<SipUserPresence>> = _defaultUsers.asStateFlow()

    fun updateSelfStatus(status: UserSipStatus, selfUser: String?) {
        _selfStatus.value = status
        updateUserStatus(selfUser, status)
    }

    fun updateUserStatus(username: String?, status: UserSipStatus) {
        if (username.isNullOrBlank()) return
        _defaultUsers.value = _defaultUsers.value.map { user ->
            if (user.username == username) user.copy(status = status) else user
        }
    }
}
