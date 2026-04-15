package com.portsip.sipsample.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.activity.ComponentActivity;
import androidx.annotation.*;
import androidx.core.app.ActivityCompat;

import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.widget.Toast;


import com.portsip.R;
import com.portsip.sipsample.receiver.PortMessageReceiver;
import com.portsip.sipsample.service.PortConnectionManager;
import com.portsip.sipsample.service.PortSipService;
import com.portsip.sipsample.util.CallManager;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends ComponentActivity {

    public PortMessageReceiver receiver = null;
    private Runnable backgroundRunnable =  new Runnable() {
        @Override
        public void run() {
            //If there is no call and the user is on push-online status, you could stop the SIP service, which will be initiated by the following calls through push notification.
            if(CallManager.Instance().pushOneline&& !CallManager.Instance().hasActiveSession()){
                Intent srvIntent = new Intent(MainActivity.this, PortSipService.class);
                stopService(srvIntent);
            }
        }
    };

    Handler handler = new Handler();
    private final int REQ_DANGERS_PERMISSION = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new PortMessageReceiver();
        setContentView(R.layout.main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PortSipService.REGISTER_CHANGE_ACTION);
        filter.addAction(PortSipService.CALL_CHANGE_ACTION);
        filter.addAction(PortSipService.PRESENCE_CHANGE_ACTION);
        filter.addAction(PortSipService.ACTION_SIP_AUDIODEVICE);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }else{
            registerReceiver(receiver, filter);
        }

        switchContent(R.id.login_fragment);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.postDelayed(backgroundRunnable,5*1000);

    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(backgroundRunnable);
        //to do
        requestPermissions (this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_DANGERS_PERMISSION:
                int i=0;
                for(int result:grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "you must grant the permission "+permissions[i], Toast.LENGTH_SHORT).show();
						i++;
                        stopService(new Intent(this,PortSipService.class));
                        System.exit(0);
                    }
                }
                break;
        }
    }
    public void switchToHome() {
        switchContent(R.id.numpad_fragment);
    }

    public void switchToLogin() {
        switchContent(R.id.login_fragment);
    }

    private void switchContent(@IdRes int fragmentId) {
        Fragment fragment = getFragmentManager().findFragmentById(fragmentId);
        Fragment login_fragment = getFragmentManager().findFragmentById(R.id.login_fragment);
        Fragment numpad_fragment = getFragmentManager().findFragmentById(R.id.numpad_fragment);

        FragmentTransaction fTransaction = getFragmentManager().beginTransaction();
        fTransaction.hide(login_fragment).hide(numpad_fragment);
        if(fragment!=null){
            fTransaction.show( fragment).commit();
        }
    }

    public void requestPermissions(Activity activity) {
        // Check if we have write permission
        if(	PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                ||PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO))
        {
            ArrayList<String> permissions = new ArrayList<String>();
            permissions.add(Manifest.permission.CAMERA);//for video call
            permissions.add(Manifest.permission.RECORD_AUDIO);//for audio call
            permissions.addAll(Arrays.asList(PortConnectionManager.phonePermissions));//for push call
            ActivityCompat.requestPermissions(activity,permissions.toArray(new String[0]), REQ_DANGERS_PERMISSION);
        }
    }

}
