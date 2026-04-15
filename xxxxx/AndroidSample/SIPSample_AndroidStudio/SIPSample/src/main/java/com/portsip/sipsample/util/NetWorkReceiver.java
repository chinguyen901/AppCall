package com.portsip.sipsample.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NetWorkReceiver extends BroadcastReceiver{
        private static NetWorkListener mListener;

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && null != context) {
                    int netWorkState = getNetWorkState(context);
                    // When the network environment has changed, determine the current network status and callback it through NetEvent.
                    if (mListener != null) {
                        mListener.onNetworkChange(netWorkState);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public interface NetWorkListener {
            public void onNetworkChange(int netMobile);
        }

        private int getNetWorkState(@NonNull Context context) {

            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            // Get all network connection information
            Network[] networks = connMgr.getAllNetworks();
            // Extract the network one by one through a loop
            for (Network network : networks) {
                // Get NetworkInfo object corresponding to ConnectivityManager object
                NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                if (networkInfo!=null&&networkInfo.isConnected()) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        return ConnectivityManager.TYPE_MOBILE;
                    } else {
                        return ConnectivityManager.TYPE_WIFI;
                    }
                }
            }
            return -1;
    }

    public static void setListener(NetWorkListener listener) {
        mListener = listener;
    }
}
