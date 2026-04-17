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
import com.portsip.OnPortSIPEvent
import com.portsip.PortSipEnumDefine
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
    private const val PORTSIP_ALLOW_ONLY_ONE_USER = -60095
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
    private var isRegistered = false
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
            portSipSdk = com.portsip.PortSipSdk(appContext)
            // Transport: UDP, SIP Port: 5060.
            // Keep this call aligned with your SDK initialize signature.
            val initResult = portSipSdk?.initialize(
                PortSipEnumDefine.ENUM_TRANSPORT_UDP,
                "0.0.0.0",
                SIP_PORT,
                0,
                "logs",
                5,
                "AppCall-UA",
                0,
                0,
                "",
                "",
                false,
                ""
            ) ?: -1
            Log.d(TAG, "PortSIP initialize result=$initResult")
            configureCodecs()
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
        isRegistered = false
        _registrationState.value = "Registering..."
        SIPStateObserver.updateSelfStatus(UserSipStatus.CONNECTING, currentUser)
        Log.d(TAG, "register() start user=$currentUser domain=$currentDomain proxy=$currentProxy")
        registerRetryJob?.cancel()

        // Avoid stale registration/session on re-login/retry.
        runCatching {
            portSipSdk?.unRegisterServer(100)
            portSipSdk?.removeUser()
        }

        val (sipServerHost, sipServerPort) = parseProxyEndpoint(currentProxy, currentDomain)

        // setUser(userName, displayName, authName, password, userDomain, SIPServer, SIPServerPort, STUNServer, STUNServerPort, outboundServer, outboundServerPort)
        var userOk = portSipSdk?.setUser(
            currentUser,
            currentUser,
            currentUser,
            currentPassword,
            currentDomain,
            sipServerHost,
            sipServerPort,
            STUN_SERVER,
            STUN_PORT,
            "",
            0
        ) ?: -1

        // PortSIP allows only one configured user in sdk state.
        if (userOk == PORTSIP_ALLOW_ONLY_ONE_USER) {
            Log.w(TAG, "setUser returned AllowOnlyOneUser, resetting previous user state")
            runCatching { portSipSdk?.removeUser() }
            userOk = portSipSdk?.setUser(
                currentUser,
                currentUser,
                currentUser,
                currentPassword,
                currentDomain,
                sipServerHost,
                sipServerPort,
                STUN_SERVER,
                STUN_PORT,
                "",
                0
            ) ?: -1
        }

        if (userOk != 0) {
            _registrationState.value = "setUser failed: $userOk"
            SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
            Log.e(
                TAG,
                "setUser failed code=$userOk user=$currentUser domain=$currentDomain sipServer=$sipServerHost:$sipServerPort"
            )
            scheduleRegisterRetry()
            return
        }

        val registerOk = portSipSdk?.registerServer(
            90,
            0
        ) ?: -1

        // registerServer()==0 only means the SDK queued/sent REGISTER — not SIP 200 OK.
        // Real success is onRegisterSuccess(); until then isRegistered stays false and makeCall is blocked.
        if (registerOk == 0) {
            _registrationState.value = "Registering... (waiting for server)"
            SIPStateObserver.updateSelfStatus(UserSipStatus.CONNECTING, currentUser)
        } else {
            _registrationState.value = "Register failed: $registerOk"
            SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
            scheduleRegisterRetry()
        }
        Log.d(
            TAG,
            "registerServer result=$registerOk user=$currentUser domain=$currentDomain sipServer=$sipServerHost:$sipServerPort"
        )
    }

    fun unregister() {
        portSipSdk?.unRegisterServer(0)
        isRegistered = false
        _registrationState.value = "Unregistered"
        _callState.value = "Idle"
        SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
        NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
        registerRetryJob?.cancel()
        Log.d(TAG, "unregister() called")
    }

    fun makeCall(targetSip: String) {
        if (!isRegistered) {
            _callState.value = "Cannot call: SIP not registered"
            Log.w(TAG, "makeCall() blocked: SIP not registered")
            return
        }
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

    private fun configureCodecs() {
        // Enable G.711 u-law (PCMU) only for this minimal sample.
        portSipSdk?.clearAudioCodec()
        portSipSdk?.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
    }

    private fun setListener() {
        portSipSdk?.setOnPortSIPEvent(object : OnPortSIPEvent {
            override fun onRegisterSuccess(statusText: String?, statusCode: Int, statusDescription: String?) {
                isRegistered = true
                registerRetryJob?.cancel()
                registerRetryJob = null
                _registrationState.value = "Registered ($statusCode)"
                SIPStateObserver.updateSelfStatus(UserSipStatus.ONLINE, currentUser)
                Log.i(TAG, "onRegisterSuccess code=$statusCode text=$statusText desc=$statusDescription")
            }

            override fun onRegisterFailure(statusText: String?, statusCode: Int, statusDescription: String?) {
                isRegistered = false
                _registrationState.value = if (statusCode == 408) {
                    "Register timeout (408): cannot reach SIP server"
                } else {
                    "Register failed ($statusCode)"
                }
                SIPStateObserver.updateSelfStatus(UserSipStatus.OFFLINE, currentUser)
                Log.e(TAG, "onRegisterFailure code=$statusCode text=$statusText desc=$statusDescription")
                scheduleRegisterRetry()
            }

            override fun onInviteIncoming(
                sessionId: Long,
                caller: String?,
                callerDisplayName: String?,
                callee: String?,
                calleeDisplayName: String?,
                audioCodecs: String?,
                videoCodecs: String?,
                existsAudio: Boolean,
                existsVideo: Boolean,
                sipMessage: String?
            ) {
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

            override fun onInviteSessionProgress(
                p0: Long,
                p1: String?,
                p2: String?,
                p3: Boolean,
                p4: Boolean,
                p5: Boolean,
                p6: String?
            ) = Unit

            override fun onInviteRinging(p0: Long, p1: String?, p2: Int, p3: String?) = Unit

            override fun onInviteAnswered(
                sessionId: Long,
                caller: String?,
                callerDisplayName: String?,
                callee: String?,
                calleeDisplayName: String?,
                audioCodecs: String?,
                videoCodecs: String?,
                existsAudio: Boolean,
                existsVideo: Boolean,
                sipMessage: String?
            ) {
                _callState.value = "Connected"
                SIPStateObserver.updateSelfStatus(UserSipStatus.BUSY, currentUser)
                SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.BUSY)
                Log.i(TAG, "onInviteAnswered session=$sessionId caller=$caller callee=$callee")
            }

            override fun onInviteFailure(
                p0: Long,
                p1: String?,
                p2: String?,
                p3: String?,
                p4: String?,
                p5: String?,
                p6: Int,
                p7: String?
            ) = Unit

            override fun onInviteUpdated(
                p0: Long,
                p1: String?,
                p2: String?,
                p3: String?,
                p4: Boolean,
                p5: Boolean,
                p6: Boolean,
                p7: String?
            ) = Unit

            override fun onInviteConnected(p0: Long) = Unit
            override fun onInviteBeginingForward(p0: String?) = Unit

            override fun onInviteClosed(sessionId: Long, reason: String?) {
                _callState.value = "Call ended"
                currentSessionId = -1
                SIPStateObserver.updateSelfStatus(UserSipStatus.ONLINE, currentUser)
                SIPStateObserver.updateUserStatus(lastRemoteUser, UserSipStatus.ONLINE)
                routeAudioToEarpiece()
                NotificationManagerCompat.from(appContext).cancel(INCOMING_NOTIFICATION_ID)
                Log.i(TAG, "onInviteClosed session=$sessionId reason=$reason")
            }

            override fun onDialogStateUpdated(p0: String?, p1: String?, p2: String?, p3: String?) = Unit
            override fun onRemoteHold(p0: Long) = Unit
            override fun onRemoteUnHold(p0: Long, p1: String?, p2: String?, p3: Boolean, p4: Boolean) = Unit
            override fun onReceivedRefer(p0: Long, p1: Long, p2: String?, p3: String?, p4: String?) = Unit
            override fun onReferAccepted(p0: Long) = Unit
            override fun onReferRejected(p0: Long, p1: String?, p2: Int) = Unit
            override fun onTransferTrying(p0: Long) = Unit
            override fun onTransferRinging(p0: Long) = Unit
            override fun onACTVTransferSuccess(p0: Long) = Unit
            override fun onACTVTransferFailure(p0: Long, p1: String?, p2: Int) = Unit
            override fun onReceivedSignaling(p0: Long, p1: String?) = Unit
            override fun onSendingSignaling(p0: Long, p1: String?) = Unit
            override fun onWaitingVoiceMessage(p0: String?, p1: Int, p2: Int, p3: Int, p4: Int) = Unit
            override fun onWaitingFaxMessage(p0: String?, p1: Int, p2: Int, p3: Int, p4: Int) = Unit
            override fun onRecvDtmfTone(p0: Long, p1: Int) = Unit
            override fun onRecvOptions(p0: String?) = Unit
            override fun onRecvInfo(p0: String?) = Unit
            override fun onRecvNotifyOfSubscription(p0: Long, p1: String?, p2: ByteArray?, p3: Int) = Unit
            override fun onPresenceRecvSubscribe(p0: Long, p1: String?, p2: String?, p3: String?) = Unit
            override fun onPresenceOnline(p0: String?, p1: String?, p2: String?) = Unit
            override fun onPresenceOffline(p0: String?, p1: String?) = Unit
            override fun onRecvMessage(p0: Long, p1: String?, p2: String?, p3: ByteArray?, p4: Int) = Unit
            override fun onRecvOutOfDialogMessage(
                p0: String?,
                p1: String?,
                p2: String?,
                p3: String?,
                p4: String?,
                p5: String?,
                p6: ByteArray?,
                p7: Int,
                p8: String?
            ) = Unit
            override fun onSendMessageSuccess(p0: Long, p1: Long, p2: String?) = Unit
            override fun onSendMessageFailure(p0: Long, p1: Long, p2: String?, p3: Int, p4: String?) = Unit
            override fun onSendOutOfDialogMessageSuccess(
                p0: Long,
                p1: String?,
                p2: String?,
                p3: String?,
                p4: String?,
                p5: String?
            ) = Unit
            override fun onSendOutOfDialogMessageFailure(
                p0: Long,
                p1: String?,
                p2: String?,
                p3: String?,
                p4: String?,
                p5: String?,
                p6: Int,
                p7: String?
            ) = Unit
            override fun onSubscriptionFailure(p0: Long, p1: Int) = Unit
            override fun onSubscriptionTerminated(p0: Long) = Unit
            override fun onPlayFileFinished(p0: Long, p1: String?) = Unit
            override fun onStatistics(p0: Long, p1: String?) = Unit
            override fun onAudioDeviceChanged(p0: PortSipEnumDefine.AudioDevice?, p1: MutableSet<PortSipEnumDefine.AudioDevice>?) = Unit
            override fun onAudioFocusChange(p0: Int) = Unit
            override fun onRTPPacketCallback(p0: Long, p1: Int, p2: Int, p3: ByteArray?, p4: Int) = Unit
            override fun onAudioRawCallback(p0: Long, p1: Int, p2: ByteArray?, p3: Int, p4: Int) = Unit
            override fun onVideoRawCallback(p0: Long, p1: Int, p2: Int, p3: Int, p4: ByteArray?, p5: Int) = Unit
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

    private fun parseProxyEndpoint(proxy: String, fallbackDomain: String): Pair<String, Int> {
        val raw = proxy.trim().removePrefix("sip:").removePrefix("sips:")
        if (raw.isBlank()) return fallbackDomain to SIP_PORT

        val hostPort = raw.substringBefore(";").substringBefore("?")
        val host = hostPort.substringBefore(":").ifBlank { fallbackDomain }
        val port = hostPort.substringAfter(":", SIP_PORT.toString()).toIntOrNull() ?: SIP_PORT
        return host to port
    }
}
