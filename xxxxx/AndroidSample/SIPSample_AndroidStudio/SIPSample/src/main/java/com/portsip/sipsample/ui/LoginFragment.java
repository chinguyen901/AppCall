package com.portsip.sipsample.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.portsip.R;
import com.portsip.sipsample.network.BackendApi;
import com.portsip.sipsample.receiver.PortMessageReceiver;

public class LoginFragment extends BaseFragment implements PortMessageReceiver.BroadcastListener {
    private MainActivity activity;
    private WebView webView;
    private TextView tvStatus;
    private SharedPreferences sharedPreferences;

    private static final String KEY_BACKEND_URL = "backend_url_demo";
    private static final String KEY_ACCESS_TOKEN = "backend_access_token";
    private static final String KEY_STREAM_API_KEY = "stream_api_key";
    private static final String KEY_STREAM_USER_ID = "stream_user_id";
    private static final String KEY_STREAM_USER_NAME = "stream_user_name";
    private static final String KEY_STREAM_TOKEN = "stream_token";
    private static final String DEFAULT_BACKEND_URL = "https://your-app.vercel.app";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return inflater.inflate(R.layout.login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvStatus = view.findViewById(R.id.txtips);
        webView = view.findViewById(R.id.wv_login);
        setupWebView();
        setOnlineStatus(null);
        activity.receiver.broadcastReceiver = this;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            activity.receiver.broadcastReceiver = this;
            setOnlineStatus(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activity.receiver.broadcastReceiver = null;
    }

    private BackendApi createBackendApi() throws Exception {
        String backendUrl = sharedPreferences.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL);
        if (TextUtils.isEmpty(backendUrl)) throw new Exception("Missing backend URL.");
        return new BackendApi(backendUrl);
    }

    private void applyStreamFromBackend(BackendApi.AuthResult authResult) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_STREAM_API_KEY, authResult.streamConfig.apiKey);
        editor.putString(KEY_STREAM_USER_ID, authResult.streamConfig.userId);
        editor.putString(KEY_STREAM_USER_NAME, authResult.streamConfig.userName);
        editor.putString(KEY_STREAM_TOKEN, authResult.streamConfig.token);
        editor.apply();
    }

    private void doRegisterAccount(String identifier, String password) {
        new Thread(() -> {
            try {
                BackendApi api = createBackendApi();
                String username = identifier.trim();
                String email = identifier.contains("@") ? identifier : (identifier + "@appcall.local");
                api.register(email, username, password, username);
                activity.runOnUiThread(() -> Toast.makeText(getActivity(), "Register success", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                activity.runOnUiThread(() -> Toast.makeText(getActivity(), "Register failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doBackendLogin(String identifier, String password) {
        new Thread(() -> {
            try {
                BackendApi api = createBackendApi();
                BackendApi.AuthResult authResult = api.login(identifier, password, Build.MODEL);
                activity.runOnUiThread(() -> {
                    applyStreamFromBackend(authResult);
                    sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, authResult.accessToken).apply();
                    tvStatus.setText("Connected Stream");
                    activity.switchToHome();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> Toast.makeText(getActivity(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void doLogout() {
        String token = sharedPreferences.getString(KEY_ACCESS_TOKEN, "");
        if (!TextUtils.isEmpty(token)) {
            new Thread(() -> {
                try {
                    createBackendApi().logout(token);
                } catch (Exception ignored) {
                }
            }).start();
        }
        sharedPreferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_STREAM_API_KEY)
                .remove(KEY_STREAM_USER_ID)
                .remove(KEY_STREAM_USER_NAME)
                .remove(KEY_STREAM_TOKEN)
                .apply();
        tvStatus.setText("Disconnected");
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new LoginBridge(), "AppBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectLoginBindings();
            }
        });
        webView.loadUrl("file:///android_asset/ui/ng_nh_p.html");
    }

    private void injectLoginBindings() {
        String js =
                "(function(){" +
                        "var form=document.querySelector('form');" +
                        "var id=document.querySelector('input[placeholder=\"Email or Phone\"]');" +
                        "var pw=document.querySelector('input[type=\"password\"]');" +
                        "var btns=document.querySelectorAll('button');" +
                        "if(form){form.addEventListener('submit',function(e){e.preventDefault();AppBridge.login((id&&id.value)||'',(pw&&pw.value)||'');});}" +
                        "if(btns&&btns.length>1){btns[1].addEventListener('click',function(e){e.preventDefault();AppBridge.register((id&&id.value)||'',(pw&&pw.value)||'');});}" +
                        "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    public void onBroadcastReceiver(Intent intent) {
        // Stream flow does not depend on PortSIP broadcast.
    }

    private void setOnlineStatus(String tips) {
        boolean connected = !TextUtils.isEmpty(sharedPreferences.getString(KEY_ACCESS_TOKEN, ""));
        tvStatus.setText(connected ? "Connected Stream" : "Disconnected");
    }

    private class LoginBridge {
        @JavascriptInterface
        public void login(String identifier, String password) {
            activity.runOnUiThread(() -> {
                if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(password)) {
                    Toast.makeText(getActivity(), "Nhap tai khoan/mat khau", Toast.LENGTH_SHORT).show();
                    return;
                }
                doBackendLogin(identifier, password);
            });
        }

        @JavascriptInterface
        public void register(String identifier, String password) {
            activity.runOnUiThread(() -> {
                if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(password)) {
                    Toast.makeText(getActivity(), "Nhap tai khoan/mat khau", Toast.LENGTH_SHORT).show();
                    return;
                }
                doRegisterAccount(identifier, password);
            });
        }

        @JavascriptInterface
        public void logout() {
            activity.runOnUiThread(() -> {
                doLogout();
                activity.switchToLogin();
            });
        }
    }
}
