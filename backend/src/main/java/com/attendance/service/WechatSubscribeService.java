package com.attendance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class WechatSubscribeService {

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.secret}")
    private String secret;

    @Value("${wechat.subscribe.templateIdAbnormal:}")
    private String templateIdAbnormal;

    private volatile String accessToken;
    private volatile long accessTokenExpireAtEpochSec;

    public void sendAttendanceAbnormal(String openid, String abnormalType, String abnormalTime) {
        if (openid == null || openid.isEmpty()) return;
        if (templateIdAbnormal == null || templateIdAbnormal.trim().isEmpty()) return;

        String token = getAccessToken();
        if (token == null) return;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("touser", openid);
            payload.put("template_id", templateIdAbnormal);
            payload.put("miniprogram_state", "developer");
            payload.put("lang", "zh_CN");

            Map<String, Object> data = new HashMap<>();
            data.put("thing1", kv(abnormalType));
            data.put("time2", kv(abnormalTime));
            payload.put("data", data);

            postJson("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token, payload);
        } catch (Exception ignored) {
        }
    }

    private Map<String, String> kv(String value) {
        Map<String, String> m = new HashMap<>();
        m.put("value", value == null ? "" : value);
        return m;
    }

    private String getAccessToken() {
        long now = Instant.now().getEpochSecond();
        if (accessToken != null && now < accessTokenExpireAtEpochSec - 60) return accessToken;

        synchronized (this) {
            now = Instant.now().getEpochSecond();
            if (accessToken != null && now < accessTokenExpireAtEpochSec - 60) return accessToken;

            try {
                String urlStr = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + secret;
                String resp = get(urlStr);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(resp);
                if (json.has("access_token")) {
                    accessToken = json.get("access_token").asText();
                    int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 7200;
                    accessTokenExpireAtEpochSec = Instant.now().getEpochSecond() + expiresIn;
                    return accessToken;
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private void postJson(String urlStr, Map<String, Object> body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        byte[] bytes = mapper.writeValueAsBytes(body);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
            }
        } catch (Exception ignored) {
        }
    }
}

