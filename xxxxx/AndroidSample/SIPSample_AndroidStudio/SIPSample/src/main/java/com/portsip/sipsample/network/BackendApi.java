package com.portsip.sipsample.network;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BackendApi {
    public static class BackendException extends Exception {
        public BackendException(String message) {
            super(message);
        }
    }

    public static class StreamConfig {
        public String apiKey;
        public String userId;
        public String userName;
        public String token;
    }

    public static class AuthResult {
        public String accessToken;
        public StreamConfig streamConfig;
    }

    private final String baseUrl;

    public BackendApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void register(
            String email,
            String username,
            String password,
            String displayName
    ) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("username", username);
        payload.put("password", password);
        payload.put("displayName", displayName);
        request("POST", "/api/auth/register", null, payload);
    }

    public AuthResult login(String identifier, String password, String deviceName) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("identifier", identifier);
        payload.put("password", password);
        payload.put("deviceName", deviceName);

        JSONObject json = request("POST", "/api/auth/login", null, payload);
        AuthResult result = new AuthResult();
        result.accessToken = json.optString("accessToken", "");

        JSONObject stream = json.getJSONObject("stream");
        StreamConfig cfg = new StreamConfig();
        cfg.apiKey = stream.optString("apiKey", "");
        cfg.userId = stream.optString("userId", "");
        cfg.userName = stream.optString("userName", "");
        cfg.token = stream.optString("token", "");
        result.streamConfig = cfg;
        return result;
    }

    public void logout(String accessToken) throws Exception {
        request("POST", "/api/auth/logout", accessToken, new JSONObject());
    }

    private JSONObject request(String method, String path, String bearerToken, JSONObject payload) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (bearerToken != null && !bearerToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        if (payload != null) {
            conn.setDoOutput(true);
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)
            );
            writer.write(payload.toString());
            writer.flush();
            writer.close();
        }

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8
                )
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        String body = sb.toString().isEmpty() ? "{}" : sb.toString();
        JSONObject json = new JSONObject(body);
        if (code < 200 || code >= 300) {
            throw new BackendException(json.optString("error", "Backend error: " + code));
        }
        return json;
    }
}
