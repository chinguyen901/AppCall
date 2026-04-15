package com.portsip.sipsample.ui;

import com.portsip.PortSipSdk;
import com.portsip.sipsample.service.PortConnectionManager;

import android.app.Application;

public class MyApplication extends Application {
	public boolean mConference= false;
	public PortSipSdk mEngine;
	public boolean mUseFrontCamera= false;

	@Override
	public void onCreate() {
		super.onCreate();
		PortConnectionManager.register(this);
		mEngine = new PortSipSdk(this);
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		mEngine= null;
	}
}
