package com.portsip.sipsample.service;

import static android.content.Context.TELECOM_SERVICE;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.util.Log;

import androidx.annotation.*;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manager proxy call. When the status of SIP call changes, notify system proxy call simultaneously.
 */
public class PortConnectionManager {
    private static final String TAG="PortConnectionManager";
    public static PortConnectionManager instance = new PortConnectionManager();
    public static final String[] phonePermissions = new String[] {
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            // Manifest.permission.MANAGE_OWN_CALLS, Protection level: normal
            // Manifest.permission.BIND_TELECOM_CONNECTION_SERVICE
    };
    // TODO: Pls enter the account
    private static String PHONE_ACCOUNT_ID="myaccount";
    static public PortConnectionManager getInstance() {
        return instance;
    }

    public void hangUpCall(String sessionUUID, int resean) {
        PortConnectionProxy proxy = getConnection(sessionUUID);
        if (proxy != null ) {
            proxy.setDisconnected(new DisconnectCause(resean));
            proxy.destroy();
        }
    }


    /**
     * When hold the call through app, notify system proxy call simultaneously.
     * @param sessionUUID
     */
    public void hold(String sessionUUID) {
        PortConnectionProxy proxy = getConnection(sessionUUID);
        if (proxy != null &&proxy.getState()==Connection.STATE_ACTIVE) {
            proxy.setOnHold();
        }
    }

    /**
     * When unhold the call through app, notify system proxy call simultaneously.
     * @param sessionUUID
     */
    void unhold(String sessionUUID) {
        PortConnectionProxy proxy = getConnection(sessionUUID);
        if (proxy != null &&proxy.getState()==Connection.STATE_HOLDING) {
            proxy.setActive();
        }
    }

    /**
     * When answer the call through SIP call, notify system proxy call simultaneously.
     * @param sessionUUID
     */
    public void answered(String sessionUUID) {
        PortConnectionProxy proxy = getConnection(sessionUUID);
        if (proxy != null) {
            if (proxy.getState() != Connection.STATE_ACTIVE && proxy.getState() != Connection.STATE_HOLDING){
                proxy.setActive();
            }
        }
    }

    /// mConnectionList For managing proxy call
    private final
    HashMap<String,PortConnectionProxy> mConnectionList = new HashMap<>();


    static public void register(Context context) {

        TelecomManager manager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);
        PhoneAccountHandle phoneAccountHandle = getPhoneAccountHandle(context);
        PhoneAccount phoneAccount = PhoneAccount.builder(phoneAccountHandle, PHONE_ACCOUNT_ID)
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED
                        | PhoneAccount.CAPABILITY_VIDEO_CALLING
                        | PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING)
                .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
                .addSupportedUriScheme(PhoneAccount.SCHEME_TEL).build();
        manager.registerPhoneAccount(phoneAccount);
    }

    /**
     * When making a outgoing call, notify system to create a proxy call simultaneously. @see {@link com.portsip.sipsample.service.PortConnectionService#onCreateOutgoingConnection}
     * @param context
     * @param caller
     * @param disPlayName
     * @param sessionUUID
     * @return
     */
    static public boolean outGoingCall(Context context, String caller, String disPlayName, String sessionUUID) {
        boolean createSuccess = false;
        final TelecomManager telecomManager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);

        PhoneAccountHandle phoneAccountHandle = getPhoneAccountHandle(context);
        boolean permitted = false;
        try {
            permitted = telecomManager.isOutgoingCallPermitted(phoneAccountHandle);
        } catch (Exception e){
        }

        if (permitted) {
            Bundle callInExtras = createCallIntentExtras(context, false, sessionUUID, disPlayName);
            if (!caller.startsWith("sip:")) {
                caller = "sip:" + caller;
            }
            Uri uri = Uri.parse(caller);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                telecomManager.placeCall(uri, callInExtras);
                createSuccess = true;
            }

        }
        return createSuccess;
    }

    /**
     * When receiving a SIP call. notify system to create a proxy call simultaneously. @see {@link com.portsip.sipsample.service.PortConnectionService#onCreateIncomingConnection}
     * @param context
     * @param caller
     * @param disPlayName
     * @param sessionUUID
     * @return
     */
    static public boolean inComingCall(Context context, String caller, String disPlayName, String sessionUUID) {
        boolean createSuccess = false;
         final TelecomManager telecomManager =
                (TelecomManager) context.getSystemService(TELECOM_SERVICE);
        PhoneAccountHandle phoneAccountHandle = getPhoneAccountHandle(context);
        Log.i(TAG, phoneAccountHandle.toString());
        boolean permitted;
        try {
            permitted = telecomManager.isIncomingCallPermitted(phoneAccountHandle);
        }catch (Exception e){
            Log.e(TAG, "isIncomingCallPermitted failed", e);
            permitted = false;
        }

        if (permitted) {
            PortConnectionProxy proxyCall =getInstance().getConnection(sessionUUID);
            if(proxyCall == null) {
                //addConnection()
                Bundle extras = new Bundle();

                if (!caller.startsWith("sip:")) caller = "sip:" + caller;
                Uri uri = Uri.fromParts(PhoneAccount.SCHEME_SIP, caller.substring(4), null);


                //Uri uri = Uri.fromParts(PhoneAccount.SCHEME_SIP, disPlayName, null);
                extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri);
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    extras.putBoolean(
                            PhoneAccount.EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE, true);
                }

                extras.putString(PortConnectionProxy.EXTRA_SESSION_UUID, sessionUUID);
                extras.putString(PortConnectionProxy.EXTRA_SESSION_DISPLAYNAME, disPlayName);
                try {
                    telecomManager.addNewIncomingCall(phoneAccountHandle, extras);
                } catch (Exception e) {
                    Log.e(TAG, "addNewIncomingCall failed", e);
                }
            }
            createSuccess = true;

        }

        return createSuccess;
    }

    static public PhoneAccountHandle getPhoneAccountHandle(@NonNull Context context) {
        PhoneAccountHandle phoneAccountHandle = new PhoneAccountHandle(
                new ComponentName(context.getPackageName(), PortConnectionService.class.getName()), PHONE_ACCOUNT_ID);

        TelecomManager manager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            Log.i(TAG, "SelfManaged accounts: " + manager.getSelfManagedPhoneAccounts());
            Log.i(TAG, "All call-capable accounts: " + manager.getCallCapablePhoneAccounts());
        }

        return phoneAccountHandle;//
    }
    static private Bundle createCallIntentExtras(@NonNull Context context, boolean hasvideo, String sessionUUID, String disPlayName) {
        Bundle extras = new Bundle();
        PhoneAccountHandle phoneAccountHandle = getPhoneAccountHandle(context);

        Bundle callIn = new Bundle();
        callIn.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,phoneAccountHandle);
        if(hasvideo) {
            callIn.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_BIDIRECTIONAL);
        }
        extras.putString(PortConnectionProxy.EXTRA_SESSION_UUID,sessionUUID);
        extras.putString(PortConnectionProxy.EXTRA_SESSION_DISPLAYNAME, disPlayName);
        extras.putBoolean(PhoneAccount.EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE, true);
        callIn.putParcelable(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS,extras);

        return callIn;
    }

    public void addConnection(PortConnectionProxy connection){
        synchronized (this){
            mConnectionList.put(connection.mSessionUUID,connection);
        }
    }

    /**
     * find the PortConnectionProxy by Sip Call UUID
     * @param sessionUUID
     * @return
     */
    public @Nullable PortConnectionProxy getConnection(@NonNull String sessionUUID){
        return mConnectionList.get(sessionUUID);

    }
    public void removeConnection(PortConnectionProxy connection){
           mConnectionList.remove(connection.mSessionUUID);
    }

    public void clear(){
        Iterator<Map.Entry<String, PortConnectionProxy>> iterator = mConnectionList.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String ,PortConnectionProxy> entry = (Map.Entry<String, PortConnectionProxy>) iterator.next();
            PortConnectionProxy proxy = entry.getValue();
            if(entry.getValue()!=null&&proxy.getState()!=Connection.STATE_DISCONNECTED){
                proxy.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                proxy.destroy();
            }
        }
        mConnectionList.clear();

    }
}
