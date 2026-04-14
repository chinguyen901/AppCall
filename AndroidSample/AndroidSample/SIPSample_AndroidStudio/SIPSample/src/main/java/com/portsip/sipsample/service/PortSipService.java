package com.portsip.sipsample.service;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.portsip.PortSipEnumDefine;
import com.portsip.PortSipErrorcode;
import com.portsip.PortSipSdk;
import com.portsip.OnPortSIPEvent;
import com.portsip.R;

import com.portsip.sipsample.ui.IncomingActivity;
import com.portsip.sipsample.ui.MainActivity;
import com.portsip.sipsample.ui.MyApplication;
import com.portsip.sipsample.util.CallManager;
import com.portsip.sipsample.util.Contact;
import com.portsip.sipsample.util.ContactManager;
import com.portsip.sipsample.util.NetWorkReceiver;
import com.portsip.sipsample.util.Ring;
import com.portsip.sipsample.util.Session;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.*;
import androidx.core.app.NotificationCompat;


public class PortSipService extends Service implements OnPortSIPEvent, NetWorkReceiver.NetWorkListener,OnCompleteListener {
    public static final String ACTION_SIP_REGISTER = "PortSip.AndroidSample.Test.REGISTER";
    public static final String ACTION_SIP_UNREGISTER = "PortSip.AndroidSample.Test.UNREGISTER";
    public static final String ACTION_SIP_ANSWER = "PortSip.AndroidSample.Test.ANSWER";
    public static final String ACTION_SIP_REJECT = "PortSip.AndroidSample.Test.REJECT";
    public static final String ACTION_SIP_AUDIODEVICE = "PortSip.AndroidSample.Test.AudioDeviceUpdate";

    public static final String ACTION_PUSH_MESSAGE = "PortSip.AndroidSample.Test.PushMessageIncoming";
    public static final String ACTION_PUSH_TOKEN = "PortSip.AndroidSample.Test.PushToken";
    public static final String ACTION_START_FOREGROUND = "foregroundService";
    public static final String INSTANCE_ID = "instanceid";

    public static final String USER_NAME = "user name";
    public static final String USER_PWD = "user pwd";
    public static final String SVR_HOST = "svr host";
    public static final String SVR_PORT = "svr port";

    public static final String USER_DOMAIN = "user domain";
    public static final String USER_DISPALYNAME = "user dispalay";
    public static final String USER_AUTHNAME = "user authname";
    public static final String STUN_HOST = "stun host";
    public static final String STUN_PORT = "stun port";

    public static final String TRANS = "trans type";
    public static final String SRTP = "srtp type";

    protected PowerManager.WakeLock mCpuLock;
    public static final String REGISTER_CHANGE_ACTION = "PortSip.AndroidSample.Test.RegisterStatusChange";
    public static final String CALL_CHANGE_ACTION = "PortSip.AndroidSample.Test.CallStatusChange";
    public static final String PRESENCE_CHANGE_ACTION = "PortSip.AndroidSample.Test.PRESENCEStatusChange";

    public static final String EXTRA_REGISTER_STATE = "RegisterStatus";
    public static final String EXTRA_CALL_SESSION_ID = "SessionID";
    public static final String EXTRA_CALL_DESCRIPTION = "Description";
    public static final String EXTRA_PUSH_TOKEN = "token";

    private final String APPID = "com.portsip.sipsample";
    private PortSipSdk mEngine;
    private MyApplication application;
    private static final int SERVICE_NOTIFICATION  = 31414;
    public  static final int PENDINGCALL_NOTIFICATION= SERVICE_NOTIFICATION+1;
    private final String channelID = "PortSipService";
    private final String callChannelID = "Call Channel";
    private String pushToken;
    private final String PUSH_HEAD = "X-Push";
    private final String SIP_METHOD_REGISTER = "REGISTER";
    private NotificationManager mNotificationManager;
    private NetWorkReceiver mNetWorkReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        application = (MyApplication) getApplicationContext();
        mEngine = application.mEngine;

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(channelID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableLights(true);
        NotificationChannel callChannel = new NotificationChannel(callChannelID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(channel);
        mNotificationManager.createNotificationChannel(callChannel);

        registerReceiver();

        getFirebaseToken();
    }

    private int initialSDK() {
        mEngine.setOnPortSIPEvent(this);
        CallManager.Instance().online = true;
        String dataPath = getExternalFilesDir(null).getAbsolutePath();
        String certRoot = dataPath + "/certs";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Random rm = new Random();
        int localPort = 5060 + rm.nextInt(60000);
        int transType = preferences.getInt(TRANS, 0);
        int result = mEngine.initialize(getTransType(transType), "0.0.0.0", localPort,
                PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, dataPath,
                8, "PortSIP SDK for Android", 0, 0, certRoot, "", false, null);

        if (result != PortSipErrorcode.ECoreErrorNone) {
            showTipMessage("initialize failure ErrorCode = " + result);
            CallManager.Instance().resetAll();
        }else {

            result = mEngine.setLicenseKey("LicenseKey");
            if (result == PortSipErrorcode.ECoreWrongLicenseKey) {
                showTipMessage("The wrong license key was detected, please check with sales@portsip.com or support@portsip.com");
            } else if (result == PortSipErrorcode.ECoreTrialVersionLicenseKey) {
                Log.w("Trial Version", "This trial version SDK just allows short conversation, you can't hearing anything after 2-3 minutes, contact us: sales@portsip.com to buy official version.");
                showTipMessage("This Is Trial Version");
                mEngine.setInstanceId(getInstanceID());
            }

        }
        return result;
    }

    @Override
    public void onAudioDeviceChanged(PortSipEnumDefine.AudioDevice audioDevice, Set<PortSipEnumDefine.AudioDevice> set) {

        CallManager.Instance().setSelectableAudioDevice(audioDevice,  set);

        Intent intent = new Intent();
        intent.setAction(ACTION_SIP_AUDIODEVICE);
        sendBroadcast(intent);

    }

    /**
     * fired when the audio focus has been changed.
     * @param focusChange the type of focus change, one of AudioManager#AUDIOFOCUS_GAIN, AudioManager#AUDIOFOCUS_LOSS,
     *                    AudioManager#AUDIOFOCUS_LOSS_TRANSIENT and AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK.
     */
    @Override
    public void onAudioFocusChange(int focusChange){
        showTipMessage("onAudioFocusChange");
    }

    @Override
    public void onRTPPacketCallback(long sessionId,int mediaType,int enum_direction,byte[] RTPPacket, int packetSize) {

    }
    private void showServiceNotifiCation(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0/*requestCode*/, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        builder = new Notification.Builder(this, channelID);
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service Running")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent);// getNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SERVICE_NOTIFICATION, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                  );
        } else {
            startForeground(SERVICE_NOTIFICATION, builder.build());
        }
    }
    public void showPendingCallNotification(Context context, String contenTitle,String contenText,Intent intent) {
        //PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        //FullScreenIntent  Could use FullScreenIntent notification to start the answering page.
        // TODO: need Guide users to grant notification permission.
        Intent answerIntent = new Intent(this, PortSipService.class);
        answerIntent.setAction(ACTION_SIP_ANSWER);
        PendingIntent answerPendingIntent = PendingIntent.getService(context, 0, answerIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent rejectIntent = new Intent(this, PortSipService.class);
        rejectIntent.setAction(ACTION_SIP_REJECT);
        PendingIntent rejectPendingIntent = PendingIntent.getService(context, 0, rejectIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, callChannelID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(contenTitle)
                .setContentText(contenText)
                .setAutoCancel(true)
                .setShowWhen(true)
                .addAction(R.drawable.icon,"Answer",answerPendingIntent)
                .addAction(R.drawable.icon,"Reject",rejectPendingIntent)
                .setCategory(Notification.CATEGORY_CALL)
                .setOngoing(true);
                //.setContentIntent(contentIntent)
                //.setFullScreenIntent(contentIntent, true);
        mNotificationManager.notify(PENDINGCALL_NOTIFICATION, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            showServiceNotifiCation();

            if(ACTION_PUSH_MESSAGE.equals(intent.getAction())){
                if(!CallManager.Instance().online){
                    initialSDK();
                }
                if(!CallManager.Instance().isRegistered){
                    registerToServer();
                }
            }else if(ACTION_SIP_REGISTER.equals(intent.getAction())){
                if(!CallManager.Instance().online) {
                    initialSDK();
                    registerToServer();
                }
            } else if (ACTION_SIP_UNREGISTER.equals(intent.getAction())) {
                unregisterToServerClosePush();
            }else if (ACTION_PUSH_TOKEN.equals(intent.getAction())) {
                pushToken = intent.getStringExtra(EXTRA_PUSH_TOKEN);
                refreshPushToken();
            }
            else if (ACTION_SIP_REJECT.equals(intent.getAction())) {

                Session session = CallManager.Instance().findIncomingCall();
                if(session!=null){
                    mEngine.rejectCall(session.sessionID,486);
                }

                //If the app is running in the background and push notification has been enabled successfully, could choose to end up the SIP service after a call, which will be initiated by the push notification subsequently.
            }
            else if (ACTION_SIP_ANSWER.equals(intent.getAction())) {
                Session session = CallManager.Instance().findIncomingCall();
                if(session!=null){
                    mEngine.answerCall(session.sessionID,session.hasVideo);
                }
            }
        }
        return result;
    }

    private void refreshPushToken(){
        if (!TextUtils.isEmpty(pushToken))
        {
            String pushMessage = "device-os=android;device-uid=" + pushToken + ";allow-call-push=true;allow-message-push=true;app-id=" + APPID;
            //new version
            Log.d("SIP LOG",pushMessage);
            mEngine.addSipMessageHeader(-1, SIP_METHOD_REGISTER, 1, PUSH_HEAD, pushMessage);
            if (CallManager.Instance().online) {
                mEngine.refreshRegistration(0);
            }

        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver();
        if (mCpuLock != null) {
            mCpuLock.release();
        }
        mNotificationManager.cancelAll();
        mNotificationManager.deleteNotificationChannel(channelID);
        mNotificationManager.deleteNotificationChannel(callChannelID);
        mNotificationManager =null;
        unregisterToServerAndKeepPush();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void registerToServer() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int srtpType = preferences.getInt(SRTP, 0);
        String userName = preferences.getString(USER_NAME, "");
        String password = preferences.getString(USER_PWD, "");
        String displayName = preferences.getString(USER_DISPALYNAME, "");
        String authName = preferences.getString(USER_AUTHNAME, "");
        String userDomain = preferences.getString(USER_DOMAIN, "");

        String sipServer = preferences.getString(SVR_HOST, "");
        String serverPort = preferences.getString(SVR_PORT, "5060");
        String stunServer = preferences.getString(STUN_HOST, "");
        String stunPort = preferences.getString(STUN_PORT, "3478");

        int sipServerPort = Integer.parseInt(serverPort);
        int stunServerPort = Integer.parseInt(stunPort);


        if(TextUtils.isEmpty(userName)){
            showTipMessage("Please enter user name!");
            return;
        }

        if(TextUtils.isEmpty(password)){
            showTipMessage("Please enter password!");
            return;
        }

        if(TextUtils.isEmpty(sipServer)){
            showTipMessage("Please enter SIP Server!");
            return;
        }

        if(TextUtils.isEmpty(serverPort)){
            showTipMessage("Please enter Server Port!");
            return;
        }

        mEngine.removeUser();
        int result = mEngine.setUser(userName, displayName, authName, password,
                userDomain, sipServer, sipServerPort, stunServer, stunServerPort, null, 5060);

        if(result != PortSipErrorcode.ECoreErrorNone)
        {
            showTipMessage("setUser failure ErrorCode = " + result);
            CallManager.Instance().resetAll();
            return;
        }

        mEngine.enableAudioManager(true);

        mEngine.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE);
        mEngine.setVideoDeviceId(1);

        mEngine.setSrtpPolicy(srtpType, false);
        ConfigPreferences(this, preferences, mEngine);

        mEngine.enable3GppTags(false);

        if (!TextUtils.isEmpty(pushToken)) {
            String pushMessage = "device-os=android;device-uid=" + pushToken + ";allow-call-push=true;allow-message-push=true;app-id=" + APPID;
            //new version
            Log.d("SIP LOG",pushMessage);
            mEngine.addSipMessageHeader(-1, SIP_METHOD_REGISTER, 1, PUSH_HEAD, pushMessage);
        }

        result = mEngine.registerServer(90, 0);
        if(result != PortSipErrorcode.ECoreErrorNone)
        {
            showTipMessage("registerServer failure ErrorCode =" + result);
            mEngine.unRegisterServer(2000);
            CallManager.Instance().resetAll();
        }
    }

    private void showTipMessage(String tipMessage)
    {
        Intent broadIntent = new Intent(REGISTER_CHANGE_ACTION);
        broadIntent.putExtra(EXTRA_REGISTER_STATE,tipMessage);
        sendPortSipMessage(tipMessage, broadIntent);
    }

    public static void ConfigPreferences(Context context, SharedPreferences preferences, PortSipSdk sdk) {
        sdk.clearAudioCodec();
        if (preferences.getBoolean(context.getString(R.string.MEDIA_G722), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G722);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_PCMA), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_PCMU), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_G729), true)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729);
        }

        if (preferences.getBoolean(context.getString(R.string.MEDIA_GSM), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_GSM);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_ILBC), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ILBC);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_AMR), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMR);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_AMRWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMRWB);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_SPEEX), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEX);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_SPEEXWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEXWB);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_ISACWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACWB);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_ISACSWB), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACSWB);
        }
//        if (preferences.getBoolean(context.getString(R.string.MEDIA_G7221), false)) {
//            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G7221);
//        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_OPUS), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_OPUS);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_DTMF), false)) {
            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_DTMF);
        }

        sdk.clearVideoCodec();
        if (preferences.getBoolean(context.getString(R.string.MEDIA_H264), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_H264);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_VP8), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP8);
        }
        if (preferences.getBoolean(context.getString(R.string.MEDIA_VP9), true)) {
            sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP9);
        }

        sdk.setVideoNackStatus(preferences.getBoolean(context.getString(R.string.VIDEO_NACK), true));

		sdk.enableAEC(preferences.getBoolean(context.getString(R.string.MEDIA_AEC), true));
        sdk.enableAGC(preferences.getBoolean(context.getString(R.string.MEDIA_AGC), true));
        sdk.enableCNG(preferences.getBoolean(context.getString(R.string.MEDIA_CNG), true));
        sdk.enableVAD(preferences.getBoolean(context.getString(R.string.MEDIA_VAD), true));
        sdk.enableANS(preferences.getBoolean(context.getString(R.string.MEDIA_ANS), false));

        boolean foward = preferences.getBoolean(context.getString(R.string.str_fwopenkey), false);
        boolean fowardBusy = preferences.getBoolean(context.getString(R.string.str_fwbusykey), false);
        String fowardto = preferences.getString(context.getString(R.string.str_fwtokey), null);
        if (foward && !TextUtils.isEmpty(fowardto)) {
            sdk.enableCallForward(fowardBusy, fowardto);
        }

        sdk.setReliableProvisional(preferences.getBoolean(context.getString(R.string.str_pracktitle), false)?2:0);

        String resolution = preferences.getString((context.getString(R.string.str_resolution)), "CIF");
        int width = 352;
        int height = 288;
        if (resolution.equals("QCIF")) {
            width = 176;
            height = 144;
        } else if (resolution.equals("CIF")) {
            width = 352;
            height = 288;
        } else if (resolution.equals("VGA")) {
            width = 640;
            height = 480;
        } else if (resolution.equals("720P")) {
            width = 1280;
            height = 720;
        } else if (resolution.equals("1080P")) {
            width = 1920;
            height = 1080;
        }

        sdk.setVideoResolution(width, height);
    }

    private int getTransType(int select) {
        switch (select) {
            case 0:
                return PortSipEnumDefine.ENUM_TRANSPORT_UDP;
            case 1:
                return PortSipEnumDefine.ENUM_TRANSPORT_TLS;
            case 2:
                return PortSipEnumDefine.ENUM_TRANSPORT_TCP;
        }
        return PortSipEnumDefine.ENUM_TRANSPORT_UDP;
    }

    String getInstanceID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String insanceid = preferences.getString(INSTANCE_ID, "");
        if (TextUtils.isEmpty(insanceid)) {
            insanceid = UUID.randomUUID().toString();
            preferences.edit().putString(INSTANCE_ID, insanceid).apply();
        }
        return insanceid;
    }

    private void unregisterToServerAndKeepPush(){
        unregisterToServer(true);
    }

    private void unregisterToServerClosePush() {
        unregisterToServer(false);
    }

    private void unregisterToServer(boolean keepPush) {
        if(CallManager.Instance().online) {

            if (!TextUtils.isEmpty(pushToken)) {
                mEngine.clearAddedSipMessageHeaders();
                String pushMessage = "device-os=android;device-uid=" + pushToken + ";allow-call-push="+keepPush+";allow-message-push="+keepPush+";app-id=" + APPID;
                //new version
                Log.d("SIP LOG",pushMessage);
                mEngine.addSipMessageHeader(-1, SIP_METHOD_REGISTER, 1, PUSH_HEAD, pushMessage);
                CallManager.Instance().pushOneline = keepPush && CallManager.Instance().pushOneline;
            }

            mEngine.unRegisterServer(2000);
            mEngine.removeUser();
            mEngine.unInitialize();
            CallManager.Instance().online = false;

            CallManager.Instance().isRegistered = false;
        }
    }

    //--------------------
    @Override
    public void onRegisterSuccess(String statusText, int statusCode, String sipMessage) {
        CallManager.Instance().isRegistered = true;
        String pushEnabled = mEngine.getSipMessageHeaderValue(sipMessage,PUSH_HEAD);
        CallManager.Instance().pushOneline=pushEnabled.contains("enabled=true");
        Intent broadIntent = new Intent(REGISTER_CHANGE_ACTION);
        broadIntent.putExtra(EXTRA_REGISTER_STATE,statusText);
        sendPortSipMessage("onRegisterSuccess", broadIntent);
        keepCpuRun(true);
    }

    @Override
    public void onRegisterFailure(String statusText, int statusCode, String sipMessage) {
        Intent broadIntent = new Intent(REGISTER_CHANGE_ACTION);
        broadIntent.putExtra(EXTRA_REGISTER_STATE, statusText);
        sendPortSipMessage("onRegisterFailure" + statusCode, broadIntent);
        CallManager.Instance().isRegistered=false;
        CallManager.Instance().resetAll();

        keepCpuRun(false);
    }
    @Override
    public void onInviteIncoming(long sessionId,
                                 String callerDisplayName,
                                 String caller,
                                 String calleeDisplayName,
                                 String callee,
                                 String audioCodecNames,
                                 String videoCodecNames,
                                 boolean existsAudio,
                                 boolean existsVideo,
                                 String sipMessage) {

        String sessionUUID = mEngine.getSipMessageHeaderValue(sipMessage,Session.PUSH_ID);

        if(TextUtils.isEmpty(sessionUUID)){
            // Non-PortSIP server or don't support PortSIP push notification
            sessionUUID = UUID.randomUUID().toString();
        }
        Session callSession = CallManager.Instance().findCallByUUID(sessionUUID);

        if(callSession==null) {
            callSession = CallManager.Instance().findIdleSession();
        }

        callSession.state = Session.CALL_STATE_FLAG.INCOMING;
        callSession.hasVideo = existsVideo;
        callSession.sessionID = sessionId;
        callSession.remote = caller;
        callSession.displayName = callerDisplayName;
        callSession.sessionUUID = sessionUUID;
        if(!callSession.bReportSystem){
            callSession.bReportSystem  = true;
            PortConnectionManager.inComingCall(this,caller,calleeDisplayName,sessionUUID);
        }

        Intent activityIntent = new Intent(this, IncomingActivity.class);
        activityIntent.putExtra(EXTRA_CALL_SESSION_ID,sessionId);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if(isForeground()){
            startActivity(activityIntent);
        }else{
            showPendingCallNotification(this, callerDisplayName, caller, activityIntent);
        }
        Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
		String description = callSession.lineName + " onInviteIncoming";
		
        broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
        broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

        sendPortSipMessage(description, broadIntent);
        Ring.getInstance(this).startRingTone();
    }

    @Override
    public void onInviteTrying(long sessionId) {
    }

    @Override
    public void onInviteSessionProgress(
            long sessionId,
            String audioCodecNames,
            String videoCodecNames,
            boolean existsEarlyMedia,
            boolean existsAudio,
            boolean existsVideo,
            String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);

        if (session != null) {
            session.bEarlyMedia = existsEarlyMedia;
        }
    }

    @Override
    public void onInviteRinging(long sessionId, String statusText, int statusCode, String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);

        if (session != null && !session.bEarlyMedia) {
            Ring.getInstance(this).startRingBackTone();
        }
    }

    @Override
    public void onInviteAnswered(long sessionId,
                                 String callerDisplayName,
                                 String caller,
                                 String calleeDisplayName,
                                 String callee,
                                 String audioCodecNames,
                                 String videoCodecNames,
                                 boolean existsAudio,
                                 boolean existsVideo,
                                 String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);

        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED;
            session.hasVideo = existsVideo;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);

            String description = session.lineName + " onInviteAnswered";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }

        Ring.getInstance(this).stopRingBackTone();
    }

    @Override
    public void onInviteFailure(long sessionId, String callerDisplayName,
                                String caller,
                                String calleeDisplayName,
                                String callee,
                                String reason,
                                int code,
                                String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);

        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.FAILED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " onInviteFailure";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }

        Ring.getInstance(this).stopRingBackTone();
    }

    @Override
    public void onInviteUpdated(long sessionId, String audioCodecs, String videoCodecs, String screenCodecs, boolean existsAudio, boolean existsVideo, boolean existsScreen, String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);

        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED;
            session.hasVideo = existsVideo;
            session.bScreenShare = existsScreen;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " OnInviteUpdated";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
    }

    @Override
    public void onInviteConnected(long sessionId) {

        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED;
            session.sessionID = sessionId;

            if (application.mConference)
            {
                mEngine.joinToConference(session.sessionID);
                mEngine.sendVideo(session.sessionID, true);
            }

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " OnInviteConnected";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
    }

    @Override
    public void onInviteBeginingForward(String forwardTo) {
    }

    @Override
    public void onInviteClosed(long sessionId, String sipMessage) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " OnInviteClosed";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
        Ring.getInstance(this).stopRingTone();
        mNotificationManager.cancel(PENDINGCALL_NOTIFICATION);
    }

    @Override
    public void onDialogStateUpdated(String BLFMonitoredUri,
                                     String BLFDialogState,
                                     String BLFDialogId,
                                     String BLFDialogDirection) {
        String text = "The user ";
        text += BLFMonitoredUri;
        text += " dialog state is updated: ";
        text += BLFDialogState;
        text += ", dialog id: ";
        text += BLFDialogId;
        text += ", direction: ";
        text += BLFDialogDirection;
    }

    @Override
    public void onRemoteUnHold(
            long sessionId,
            String audioCodecNames,
            String videoCodecNames,
            boolean existsAudio,
            boolean existsVideo) {
    }

    @Override
    public void onRemoteHold(long sessionId) {
    }

    @Override
    public void onReceivedRefer(
            long sessionId,
            long referId,
            String to,
            String referFrom,
            String referSipMessage) {
    }

    @Override
    public void onReferAccepted(long sessionId) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " onReferAccepted";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
        }
        Ring.getInstance(this).stopRingTone();
    }

    @Override
    public void onReferRejected(long sessionId, String reason, int code) {
    }

    @Override
    public void onTransferTrying(long sessionId) {
    }

    @Override
    public void onTransferRinging(long sessionId) {
    }

    @Override
    public void onACTVTransferSuccess(long sessionId) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED;
            session.sessionID = sessionId;

            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " Transfer succeeded, call closed";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);
            // Close the call after succeeded transfer the call
            mEngine.hangUp(sessionId);
        }
    }

    @Override
    public void onACTVTransferFailure(long sessionId, String reason, int code) {
        Session session = CallManager.Instance().findSessionBySessionID(sessionId);
        if (session != null) {
            Intent broadIntent = new Intent(CALL_CHANGE_ACTION);
            broadIntent.putExtra(EXTRA_CALL_SESSION_ID, sessionId);
            String description = session.lineName + " Transfer failure!";
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description);

            sendPortSipMessage(description, broadIntent);

        }
    }

    @Override
    public void onReceivedSignaling(long sessionId, String signaling) {
    }

    @Override
    public void onSendingSignaling(long sessionId, String signaling) {
    }

    @Override
    public void onWaitingVoiceMessage(
            String messageAccount,
            int urgentNewMessageCount,
            int urgentOldMessageCount,
            int newMessageCount,
            int oldMessageCount) {
    }

    @Override
    public void onWaitingFaxMessage(
            String messageAccount,
            int urgentNewMessageCount,
            int urgentOldMessageCount,
            int newMessageCount,
            int oldMessageCount) {
    }

    @Override
    public void onRecvDtmfTone(long sessionId, int tone) {
    }

    @Override
    public void onRecvOptions(String optionsMessage) {
    }

    @Override
    public void onRecvInfo(String infoMessage) {
    }

    @Override
    public void onRecvNotifyOfSubscription(long sessionId, String notifyMessage, byte[] messageData, int messageDataLength) {
    }

    //Receive a new subscribe
    @Override
    public void onPresenceRecvSubscribe(
            long subscribeId,
            String fromDisplayName,
            String from,
            String subject) {
        Contact contact = ContactManager.Instance().findContactBySipAddr(from);
        if (contact == null) {
            contact = new Contact();
            contact.sipAddr = from;
            ContactManager.Instance().addContact(contact);
        }

        contact.subRequestDescription = subject;
        contact.subId = subscribeId;
        switch (contact.state) {
            case ACCEPTED://This subscribe has accepted
                mEngine.presenceAcceptSubscribe(subscribeId);
                break;
            case REJECTED://This subscribe has rejected
                mEngine.presenceRejectSubscribe(subscribeId);
                break;
            case UNSETTLLED:
                break;
            case UNSUBSCRIBE:
                contact.state = Contact.SUBSCRIBE_STATE_FLAG.UNSETTLLED;
                break;
        }
        Intent broadIntent = new Intent(PRESENCE_CHANGE_ACTION);
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent);
    }

    //update online status
    @Override
    public void onPresenceOnline(String fromDisplayName, String from, String stateText) {
        Contact contact = ContactManager.Instance().findContactBySipAddr(from);
        if (contact == null) {

        } else {
            contact.subDescription = stateText;
        }

        Intent broadIntent = new Intent(PRESENCE_CHANGE_ACTION);
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent);
    }

    //update offline status
    @Override
    public void onPresenceOffline(String fromDisplayName, String from) {
        Contact contact = ContactManager.Instance().findContactBySipAddr(from);
        if (contact == null) {

        } else {
            contact.subDescription = "Offline";
        }

        Intent broadIntent = new Intent(PRESENCE_CHANGE_ACTION);
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent);
    }

    @Override
    public void onRecvMessage(
            long sessionId,
            String mimeType,
            String subMimeType,
            byte[] messageData,
            int messageDataLength) {
    }

    @Override
    public void onRecvOutOfDialogMessage(
            String fromDisplayName,
            String from,
            String toDisplayName,
            String to,
            String mimeType,
            String subMimeType,
            byte[] messageData,
            int messageDataLength,
            String sipMessage) {
        if ("text".equals(mimeType) && "plain".equals(subMimeType)) {
            Toast.makeText(this,"you have a message from: "+from+ "  "+new String(messageData),Toast.LENGTH_SHORT).show();
        }else{
        }
    }

    @Override
    public void onSendMessageSuccess(long sessionId, long messageId, String sipMessage) {
    }

    @Override
    public void onSendMessageFailure(long sessionId, long messageId, String reason, int code, String sipMessage) {
    }

    @Override
    public void onSendOutOfDialogMessageSuccess(long messageId,
                                                String fromDisplayName,
                                                String from,
                                                String toDisplayName,
                                                String to,
                                                String sipMessage) {
    }

    @Override
    public void onSendOutOfDialogMessageFailure(
            long messageId,
            String fromDisplayName,
            String from,
            String toDisplayName,
            String to,
            String reason,
            int code,
            String sipMessage) {
    }

    @Override
    public void onSubscriptionFailure(long subscribeId, int statusCode) {
    }

    @Override
    public void onSubscriptionTerminated(long subscribeId) {
    }

    @Override
    public void onPlayFileFinished(long sessionId, String fileName) {
    }

    @Override
    public void onStatistics(long sessionId,String statistics){

    }

    @Override
    public void onAudioRawCallback(
            long sessionId,
            int callbackType,
            byte[] data,
            int dataLength,
            int samplingFreqHz) {
    }

    @Override
    public void onVideoRawCallback(long l, int i, int i1, int i2, byte[] bytes, int i3) {

    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        unregisterToServerAndKeepPush();
    }
    //--------------------
    public void sendPortSipMessage(String message, Intent broadIntent) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        builder = new Notification.Builder(this, channelID);
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle("Sip Notify")
                .setContentText(message)
                .setContentIntent(contentIntent)
                .build();// getNotification()

        mNotificationManager.notify(1, builder.build());

        sendBroadcast(broadIntent);
    }

    public int outOfDialogRefer(int replaceSessionId, String replaceMethod, String target, String referTo) {
        return 0;
    }

    public void keepCpuRun(boolean keepRun) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (keepRun == true) { //open
            if (mCpuLock == null) {
                if ((mCpuLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SipSample:CpuLock.")) == null) {
                    return;
                }
                mCpuLock.setReferenceCounted(false);
            }

            synchronized (mCpuLock) {
                if (!mCpuLock.isHeld()) {
                    mCpuLock.acquire();
                }
            }
        } else {//close
            if (mCpuLock != null) {
                synchronized (mCpuLock) {
                    if (mCpuLock.isHeld()) {
                        mCpuLock.release();
                    }
                }
            }
        }
    }

    @Override
    public void onNetworkChange(int netMobile) {
        if(netMobile==-1){
            //invaluable
        }else{
            if(CallManager.Instance().online) {
                mEngine.refreshRegistration(0);
            }else{
                //
            }
        }
    }
    private void registerReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mNetWorkReceiver = new NetWorkReceiver();
        mNetWorkReceiver.setListener(this);


        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mNetWorkReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }else{
            registerReceiver(mNetWorkReceiver, filter);
        }
    }

    private void unregisterReceiver(){
        if(mNetWorkReceiver!=null){
            unregisterReceiver(mNetWorkReceiver);
        }
    }

    private boolean isForeground(){
        String[] alActivity = getActivePackages(this);
        if(alActivity.length>0){
            String packageName = getPackageName();
            //String processName= getProcessName();||activityName.contains(processName)
            for(String activityName:alActivity){
                if(activityName.contains(packageName)){
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private String[] getActivePackagesCompat(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
        final ComponentName componentName = taskInfo.get(0).topActivity;
        final String[] activePackages = new String[1];
        activePackages[0] = componentName.getPackageName();
        return activePackages;
    }

    private String[] getActivePackages(Context context) {
        final Set<String> activePackages = new HashSet<String>();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                activePackages.addAll(Arrays.asList(processInfo.pkgList));
            }
        }
        return activePackages.toArray(new String[activePackages.size()]);
    }


    void getFirebaseToken() {
        try {
            FirebaseApp.initializeApp(this);
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(this);
        } catch (Exception e) {
        }
    }

    @Override
    public void onComplete(@NonNull Task task) {
        if (task.isSuccessful()) {
            pushToken = (String)task.getResult();
            refreshPushToken();
        } else {

        }
    }
}

