package com.appcall.voip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * NOTE:
 * PortSIP SDK method names can vary slightly by SDK version.
 * Update method signatures in this file to match your PortSIP AAR.
 */
object SIPManager {

    private const val TAG = "SIPManager"
    private const val SIP_PORT = 5060
    private const val STUN_SERVER = "stun.l.google.com"
    private const val STUN_PORT = 19302
    private const val INCOMING_CHANNEL_ID = "incoming_call_channel"
    private const val INCOMING_NOTIFICATION_ID = 2001

    private lateinit var appContext: Context
    private lateinit var audioManager: AudioManager

    // Replace with exact PortSIP callback interface and SDK class from your AAR.
    private var portSipSdk: com.portsip.PortSipSdk? = null
    private var currentSessionId: Long = -1
    private var currentDomain: String = ""
    private var currentUser: String = ""
    private var currentPassword: String = ""
    private var currentProxy: String = ""
    private var lastRemoteUser: String? = null
    private var registerRetryJob: Job? = null
    private val sipScope = CoroutineScope(Dispatchers.IO)

    private val _registrationState = MutableStateFlow("Not registered")
    val registrationState: StateFlow<String> = _registrationState

    private val _callState = MutableStateFlow("Idle")
    val callState: StateFlow<String> = _callState

    fun init(context: Context) {
        appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createIncomingCallChannel()
        routeAudioToEarpiece()

        if (portSipSdk == null) {
            portSipSdk = com.portsip.PortSipSdk()
            // Transport: UDP, SIP Port: 5060.
            // Keep this call aligned with your SDK initialize signature.
            val initResult = portSipSdk?.initialize(
                com.portsip.PortSipEnumDefine.ENUM_TRANSPORT_UDP,
                "0.0.0.0",
                SIP_PORT,
                "AppCall-UA",
                0,
                "logs",
                5
            ) ?: -1
            Log.d(TAG, "PortSIP initialize result=$initResult")
            configureCodecs()
            configureStun()
            setListener()
        }
    }

    fun register(username: String, password: String, domain: String, proxy: String) {
        val safeUser = username.trim()
        val safeDomain = domain.trim().ifBlank { AppConfig.DEFAULT_SIP_DOMAIN }
        val safeProxy = proxy.trim().ifBlank { AppConfig.DEFAULT_SIP_PROXY }
        if (safeUser.isBlank() || password.isBlank() || safeDomain.isBlank() || safeProxy.isBlank()) {
            _registrationState.value = "Invalid SIP credentials"
            SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, safeUser)
            Log.e(TAG, "register() rejected: empty user/pass/domain/proxy")
            return
        }

        currentUser = safeUser
        currentPassword = password
        currentDomain = safeDomain
        currentProxy = safeProxy
        _registrationState.value = "Registering..."
        SIPStateObserver.updateSelfStatus(UserSipStatus.CONNECTING, currentUser)
        Log.d(TAG, "register() start user=$currentUser domain=$currentDomain proxy=$currentProxy")
        registerRetryJob?.cancel()

        // Avoid stale registration/session on re-login.
        runCatching { portSipSdk?.unRegisterServer() }

        // Keep setUser/registerServer argument order aligned with your SDK version.
        val userOk = portSipSdk?.setUser(
            currentUser,
            "",
            "",
            currentPassword,
            currentUser,
            0,
            currentDomain,
            SIP_PORT
        ) ?: -1

        if (userOk != 0) {
            _registrationState.value = "setUser failed: $userOk"
            SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
            Log.e(TAG, "setUser failed code=$userOk user=$currentUser domain=$currentDomain")
            scheduleRegisterRetry()
            return
        }

        val registerOk = portSipSdk?.registerServer(
            currentDomain,
            SIP_PORT,
            currentProxy,
            "",
            "",
            30,
            0
        ) ?: -1

        _registrationState.value = if (registerOk == 0) "Registered" else "Register failed: $registerOk"
        SIPStateObserver.updateSelfStatus(
            if (registerOk == 0) UserSipStatus.ONLINE else UserSipStatus.OFFLINE,
            currentUser
        )
        Log.d(TAG, "registerServer result=$registerOk user=$currentUser domain=$currentDomain")
        if (registerOk != 0) scheduleRegisterRetry()
    }

    fun unregister() {
        portSipSdk?.unRegisterServer()
        _registrationState.value = "Unregistered"
        _callState.value = "Idle"
        SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
        NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
        registerRetryJob?.cancel()
        Log.d(TAG, "unregister() called")
    }

    fun makeCall(targetSip: String) {
        if (currentDomain.isBlank()) return
        routeAudioToSpeaker()
        val destination = if (targetSip.contains("@")) targetSip else "$targetSip@$currentDomain"
        val callee = "sip:$destination"
        currentSessionId = portSipSdk?.call(callee, true, false) ?: -1
        lastRemoteUser = destination.substringBefore("@")
        _callState.value = if (currentSessionId > 0) "Calling $callee" else "Call failed"
        if (currentSessionId > 0) {
            SIPStateObserver.updateSelfStatus(UserSipStatus.BUSY, currentUser)
            SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.CONNECTING)
        }
        Log.d(TAG, "makeCall() callee=$callee session=$currentSessionId")
    }

    fun endCall() {
        if (currentSessionId > 0) {
            portSipSdk?.hangUp(currentSessionId)
        }
        currentSessionId = -1
        _callState.value = "Call ended"
        SIPStateObserver.updateSelfStatus(UserSipStatus.ONLINE, currentUser)
        SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.ONLINE)
        routeAudioToEarpiece()
        NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
        Log.d(TAG, "endCall()")
    }

    fun handleIncomingCall() {
        val intent = Intent(appContext, CallReceiver::class.java).apply {
            action = CallReceiver.ACTION_INCOMING_CALL
        }
        appContext.sendBroadcast(intent)
        showIncomingCallNotification()
        Log.d(TAG, "handleIncomingCall() broadcast + notification")
    }

    fun answerCall() {
        if (currentSessionId > 0) {
            routeAudioToSpeaker()
            portSipSdk?.answerCall(currentSessionId, true)
            _callState.value = "Connected"
            SIPStateObserver.updateSelfStatus(UserSipStatus.BUSY, currentUser)
            SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.BUSY)
            NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
            Log.d(TAG, "answerCall() session=$currentSessionId")
        }
    }

    fun rejectCall() {
        if (currentSessionId > 0) {
            portSipSdk?.rejectCall(currentSessionId, 486)
            _callState.value = "Rejected"
            currentSessionId = -1
            SIPStateObserver.updateSelfStatus(UserSipStatus.ONLINE, currentUser)
            SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.ONLINE)
            routeAudioToEarpiece()
            NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
            Log.d(TAG, "rejectCall()")
        }
    }

    fun routeAudioToSpeaker() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        Log.d(TAG, "Audio routed to speaker")
    }

    fun routeAudioToEarpiece() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        Log.d(TAG, "Audio routed to earpiece")
    }

    private fun configureStun() {
        // Keep aligned with your SDK method name/signature.
        portSipSdk?.setLocalServerAddress(
            STUN_SERVER,
            STUN_PORT,
            "",
            "",
            0
        )
    }

    private fun configureCodecs() {
        // Enable G.711 u-law (PCMU) only for this minimal sample.
        portSipSdk?.clearAudioCodec()
        portSipSdk?.addAudioCodec(com.portsip.PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
    }

    private fun setListener() {
        portSipSdk?.setListener(object : com.portsip.OnPortSIPEvent {
            override fun onRegisterSuccess(statusText: String?, statusCode: Int) {
                _registrationState.value = "Registered ($statusCode)"
                SIPStateObserver.updateSelfStatus(UserSipStatus.ONLINE, currentUser)
                Log.i(TAG, "onRegisterSuccess code=$statusCode text=$statusText")
            }

            override fun onRegisterFailure(statusText: String?, statusCode: Int) {
                _registrationState.value = "Register failed ($statusCode)"
                SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
                Log.e(TAG, "onRegisterFailure code=$statusCode text=$statusText")
                scheduleRegisterRetry()
            }

            override fun onInviteIncoming(sessionId: Long, caller: String?, callerDisplayName: String?) {
                currentSessionId = sessionId
                _callState.value = "Incoming: ${caller ?: "Unknown"}"
                lastRemoteUser = caller?.substringAfter("sip:")?.substringBefore("@")
                SIPStateObserver.updateSelfStatus(UserSipStatus.CONNECTING, currentUser)
                SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.CONNECTING)
                Log.i(TAG, "onInviteIncoming session=$sessionId caller=$caller display=$callerDisplayName")
                handleIncomingCall()
            }

            override fun onInviteTrying(sessionId: Long) {
                _callState.value = "Ringing..."
                SIPStateObserver.updateSelfStatus(UserSipStatus.CONNECTING, currentUser)
                Log.d(TAG, "onInviteTrying session=$sessionId")
            }

            override fun onInviteAnswered(sessionId: Long, statusText: String?, statusCode: Int) {
                _callState.value = "Connected"
                SIPStateObserver.updateSelfStatus(UserSipStatus.BUSY, currentUser)
                SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.BUSY)
                Log.i(TAG, "onInviteAnswered session=$sessionId code=$statusCode text=$statusText")
            }

            override fun onInviteClosed(sessionId: Long) {
                _callState.value = "Call ended"
                currentSessionId = -1
                SIPStateObserver.updateSelfStatus(UserSipStatus.ONLINE, currentUser)
                SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.ONLINE)
                routeAudioToEarpiece()
                NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
                Log.i(TAG, "onInviteClosed session=$sessionId")
            }
        })
    }

    private fun showIncomingCallNotification() {
        val openCallIntent = Intent(appContext, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("incoming", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            appContext,
            100,
            openCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, INCOMING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("Incoming SIP call")
            .setContentText("Tap to open call screen")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        NotificationManagerCompat.from(appContext).notify(INCOMING_NOTIFICATION_ID, notification)
    }

    private fun createIncomingCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INCOMING_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming SIP call alerts"
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleRegisterRetry() {
        if (currentUser.isBlank() || currentPassword.isBlank() || currentDomain.isBlank() || currentProxy.isBlank()) {
            return
        }
        if (registerRetryJob?.isActive == true) return

        registerRetryJob = sipScope.launch {
            delay(3000)
            Log.w(TAG, "Retrying SIP register...")
            register(currentUser, currentPassword, currentDomain, currentProxy)
        }
    }
}
