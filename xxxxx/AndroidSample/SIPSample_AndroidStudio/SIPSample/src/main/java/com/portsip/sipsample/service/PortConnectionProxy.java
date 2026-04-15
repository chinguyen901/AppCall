package com.portsip.sipsample.service;

import static android.telecom.TelecomManager.PRESENTATION_ALLOWED;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.view.Surface;

/**
 * When the user operate a call from system page or hardware button, call the relevant SIP interface by proxy call and perform the corresponding operations(pick up, hung up, mute...).当用户通过车机界面，或者系统界面，硬件按钮操作通话时，通过代理通话回掉SIP接口，执行对应(接听，挂断，静音...)操作
 */
public class PortConnectionProxy extends Connection {
    /// Foe the outgoing calls, use random UUID numbers. For the incoming calls, obtain the UUID number from SIP or notification message.
    public static String EXTRA_SESSION_UUID ="SESSION_UUID";
    public static String EXTRA_SESSION_DISPLAYNAME ="SESSION_DISPLAYNAME";
    final protected String mSessionUUID;//UUID number for proxy call, which is the same as SIP call's.
    final private Context mContext;

    static class PortVideoProvide extends VideoProvider{

        @Override
        public void onSetCamera(String cameraId) {

        }

        @Override
        public void onSetPreviewSurface(Surface surface) {

        }

        @Override
        public void onSetDisplaySurface(Surface surface) {

        }

        @Override
        public void onSetDeviceOrientation(int rotation) {

        }

        @Override
        public void onSetZoom(float value) {

        }

        @Override
        public void onSendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile) {

        }

        @Override
        public void onSendSessionModifyResponse(VideoProfile responseProfile) {

        }

        @Override
        public void onRequestCameraCapabilities() {

        }

        @Override
        public void onRequestConnectionDataUsage() {

        }

        @Override
        public void onSetPauseImage(Uri uri) {

        }
    }

    PortConnectionProxy(Context context, ConnectionRequest request)
    {
        this(context,request,request.getExtras().getString(EXTRA_SESSION_UUID));
    }


    PortConnectionProxy(Context context, ConnectionRequest request,String uuid)
    {
        setInitializing();
        mContext = context;
        mSessionUUID = uuid;
        setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        String displayName = request.getExtras().getString(EXTRA_SESSION_DISPLAYNAME);
        setCallerDisplayName(displayName,PRESENTATION_ALLOWED);
        setConnectionProperties(CAPABILITY_HOLD | CAPABILITY_SUPPORT_HOLD | CAPABILITY_MUTE | PROPERTY_SELF_MANAGED);
        setInitialized();
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);
        if(state == STATE_ACTIVE){
            setAudioModeIsVoip(true);
        }

    }

    @Override
    public void onCallEvent(String event, Bundle extras) {
        super.onCallEvent(event, extras);

    }


    @Override
    public void onPostDialContinue(boolean proceed) {
        super.onPostDialContinue(proceed);
    }

    @Override
    public void onSilence() {
        super.onSilence();

        // TODO: Find the SIP call corresponding to the proxy call, and mute it.
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        super.onCallAudioStateChanged(state);

        // TODO: Find the SIP call corresponding to the proxy call, and switch audio devices.
    }


    @Override
    public void onShowIncomingCallUi() {
        super.onShowIncomingCallUi();
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();

        destroy();
        // TODO: Find the SIP call corresponding to the proxy call, and hung up it.
    }


    @Override
    public void onSeparate() {
        super.onSeparate();
    }


    @Override
    public void onAbort() {
        super.onAbort();

        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        destroy();
        // TODO: Find the SIP call corresponding to the proxy call, and cancel it.
    }

    @Override
    public void onHold() {
        super.onHold();
        // TODO: Find the SIP call corresponding to the proxy call, and hold it.
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
        // TODO: Find the SIP call corresponding to the proxy call, and unhold it.
    }

    @Override
    public void onPlayDtmfTone(char c) {
        super.onPlayDtmfTone(c);
        // TODO: Find the SIP call corresponding to the proxy call, and send the dtmf code.
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        // TODO: Find the SIP call corresponding to the proxy call, and pick it up.

    }

    @Override
    public void onReject() {
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));

        destroy();
        // TODO: Find the SIP call corresponding to the proxy call, and reject it.
    }

}
