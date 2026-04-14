package com.portsip.sipsample.service;
import android.content.Intent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.portsip.sipsample.util.CallManager;
import com.portsip.sipsample.util.Session;

import java.util.Map;
import static com.portsip.sipsample.service.PortSipService.ACTION_PUSH_MESSAGE;
import static com.portsip.sipsample.util.Session.PUSH_ID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String KEY_SEND_FROM = "send_from";
    private static final String KEY_SEND_TO   = "send_to";
    private static final String KEY_MESSAGE_TYPE = "msg_type";
    private static final String KEY_MESSAGE_CONTENT = "msg_content";
    private static final String MESSAGE_TYPE_IM = "im";
    private static final String MESSAGE_TYPE_CALL = "call";
    private static final String MESSAGE_TYPE_AUDIO_CALL = "audio";
    private static final String MESSAGE_TYPE_VIDEO_CALL = "video";
    @Override
    public void onCreate() {
        super.onCreate();
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        String content = data.get(KEY_MESSAGE_CONTENT);
        String from = data.get(KEY_SEND_FROM);
        String to = data.get(KEY_SEND_TO);
        String pushType = data.get(KEY_MESSAGE_TYPE);
        String sessionUUID = data.get(PUSH_ID);

        if (MESSAGE_TYPE_CALL.equals(pushType)||MESSAGE_TYPE_AUDIO_CALL.equals(pushType)||MESSAGE_TYPE_VIDEO_CALL.equals(pushType)) {
            if(sessionUUID!=null){
                Session session = CallManager.Instance().findCallByUUID(sessionUUID);
                if (session == null) {
                    //sip The conversation has been established. And the service has been started, no need to start it again.
                    PortConnectionManager.inComingCall(this, from, from, sessionUUID);
                    Intent srvIntent = new Intent(this, PortSipService.class);
                    srvIntent.setAction(ACTION_PUSH_MESSAGE);
                    startService(srvIntent);
                }

            }
        }else if(MESSAGE_TYPE_IM.equals(pushType))
        {
            // TODO: Display new message notification.
        }

    }

    @Override
    public void onNewToken(String s) {
        sendRegistrationToServer(s);
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }

    private void sendRegistrationToServer(String token) {
        //TO DO

    }
}
