package com.portsip.sipsample.service;
import static com.portsip.sipsample.service.PortConnectionProxy.EXTRA_SESSION_UUID;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.RemoteConference;
import android.telecom.RemoteConnection;
import android.telecom.TelecomManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public class PortConnectionService extends ConnectionService {
    //Need to apply for this permission.   <uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
    static final String TAG ="PortConnectionService";
    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        PortConnectionProxy incomingCallCannection = null;
        incomingCallCannection = new PortConnectionProxy(this,request);

        PortConnectionManager.getInstance().addConnection(incomingCallCannection);
        incomingCallCannection.setRinging();

        return incomingCallCannection;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {

       PortConnectionProxy outgoingCallConnection = null;
       String sessionUUId = request.getExtras().getString(EXTRA_SESSION_UUID);
        if(sessionUUId == null) {
            String remoteAddress = request.getAddress().toString();
            sessionUUId = UUID.randomUUID().toString();
            // TODO: If this call comes from the system call page, a SIP call will be created in advance. Foe the outgoing calls, random UUID numbers will be used.通话来自系统通话界面，需要创建先创建SIP通话，呼叫出去的通话，使用随机UUID，
        }

        outgoingCallConnection = new PortConnectionProxy(this,request);
        outgoingCallConnection.setVideoProvider(new PortConnectionProxy.PortVideoProvide());

        outgoingCallConnection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        PortConnectionManager.getInstance().addConnection(outgoingCallConnection);
        outgoingCallConnection.setDialing();

        return outgoingCallConnection;
    }

    @Override
    public Connection onCreateIncomingHandoverConnection(PhoneAccountHandle fromPhoneAccountHandle, ConnectionRequest request) {
        return super.onCreateIncomingHandoverConnection(fromPhoneAccountHandle, request);
    }


    @Override
    public Connection onCreateOutgoingHandoverConnection(PhoneAccountHandle fromPhoneAccountHandle, ConnectionRequest request) {
        return super.onCreateOutgoingHandoverConnection(fromPhoneAccountHandle, request);
    }

    @Nullable
    @Override
    public Conference onCreateIncomingConference(@NonNull PhoneAccountHandle connectionManagerPhoneAccount, @NonNull ConnectionRequest request) {
        return super.onCreateIncomingConference(connectionManagerPhoneAccount, request);
    }



    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request);
    }


    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request);
    }

    @Override
    public void onRemoteConferenceAdded(RemoteConference conference) {
        super.onRemoteConferenceAdded(conference);
    }

    @Override
    public void onRemoteExistingConnectionAdded(RemoteConnection connection) {
        super.onRemoteExistingConnectionAdded(connection);
    }

    @Override
    public void onConference(Connection connection1, Connection connection2) {
        super.onConference(connection1, connection2);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PortConnectionManager.getInstance().clear();
    }

}
