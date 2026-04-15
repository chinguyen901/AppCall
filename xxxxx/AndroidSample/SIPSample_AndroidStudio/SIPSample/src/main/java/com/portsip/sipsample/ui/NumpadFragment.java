package com.portsip.sipsample.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.portsip.R;
import com.portsip.sipsample.receiver.PortMessageReceiver;

public class NumpadFragment extends BaseFragment implements PortMessageReceiver.BroadcastListener {
    private MainActivity activity;
    private TextView tvTips;
    private WebView webView;
    private String currentTarget = "";
    private SharedPreferences sharedPreferences;

    private static final String KEY_STREAM_API_KEY = "stream_api_key";
    private static final String KEY_STREAM_USER_ID = "stream_user_id";
    private static final String KEY_STREAM_USER_NAME = "stream_user_name";
    private static final String KEY_STREAM_TOKEN = "stream_token";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return inflater.inflate(R.layout.numpad, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvTips = view.findViewById(R.id.txtips);
        webView = view.findViewById(R.id.wv_home);
        setupWebView();
        activity.receiver.broadcastReceiver = this;
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new HomeBridge(), "AppBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("tin_nh_n")) injectHomeBindings();
                if (url.contains("h_i_tho_i")) injectCallBindings();
                if (url.contains("h_i_tho_i")) injectStreamSession();
            }
        });
        webView.loadUrl("file:///android_asset/ui/tin_nh_n.html");
    }

    private void injectHomeBindings() {
        String js =
                "(function(){var items=document.querySelectorAll('section.space-y-1 > div');" +
                        "for(var i=0;i<items.length;i++){(function(idx){items[idx].addEventListener('click',function(){AppBridge.openChat('1002');});})(i);} })();";
        webView.evaluateJavascript(js, null);
    }

    private void injectCallBindings() {
        String js =
                "(function(){var btns=document.querySelectorAll('button');" +
                        "if(btns.length>0){btns[0].addEventListener('click',function(e){e.preventDefault();AppBridge.backToChats();});}" +
                        "if(btns.length>1){btns[1].addEventListener('click',function(e){e.preventDefault();AppBridge.call();});}" +
                        "})();";
        webView.evaluateJavascript(js, null);
    }

    private void injectStreamSession() {
        String apiKey = sharedPreferences.getString(KEY_STREAM_API_KEY, "");
        String userId = sharedPreferences.getString(KEY_STREAM_USER_ID, "");
        String userName = sharedPreferences.getString(KEY_STREAM_USER_NAME, "");
        String token = sharedPreferences.getString(KEY_STREAM_TOKEN, "");
        String escaped = "window.__streamConfig={apiKey:'" + escapeJs(apiKey) + "',userId:'" + escapeJs(userId) + "',userName:'" + escapeJs(userName) + "',token:'" + escapeJs(token) + "'};";
        webView.evaluateJavascript(escaped, null);
    }

    private String escapeJs(String input) {
        return input == null ? "" : input.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void showTips(String text) {
        if (text == null) text = "";
        tvTips.setText(text);
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    private void callTo(String callTo) {
        if (TextUtils.isEmpty(callTo)) {
            showTips("Target user rong.");
            return;
        }
        showTips("Starting Stream call with " + callTo);
    }

    private void hangupCurrent() {
        showTips("Hangup");
    }

    @Override
    public void onBroadcastReceiver(Intent intent) {
        // Stream flow does not depend on PortSIP broadcast.
    }

    private class HomeBridge {
        @JavascriptInterface
        public void openChat(String target) {
            activity.runOnUiThread(() -> {
                currentTarget = target;
                webView.loadUrl("file:///android_asset/ui/h_i_tho_i.html");
            });
        }

        @JavascriptInterface
        public void backToChats() {
            activity.runOnUiThread(() -> webView.loadUrl("file:///android_asset/ui/tin_nh_n.html"));
        }

        @JavascriptInterface
        public void call() {
            activity.runOnUiThread(() -> {
                if (TextUtils.isEmpty(currentTarget)) {
                    final EditText input = new EditText(getActivity());
                    input.setHint("1002 hoac sip:1002@domain");
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Nhap SIP target")
                            .setView(input)
                            .setPositiveButton("Call", (d, w) -> callTo(input.getText().toString().trim()))
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    callTo(currentTarget);
                }
            });
        }

        @JavascriptInterface
        public void hangup() {
            activity.runOnUiThread(() -> hangupCurrent());
        }
    }
}
