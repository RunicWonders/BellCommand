package cn.ningmo.bellcommand.update;

import cn.ningmo.bellcommand.BellCommand;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class CustomUpdateSource implements UpdateSource {
    private final BellCommand plugin;
    private final String customUrl;
    private final boolean verifySSL;
    private final Map<String, String> customHeaders;
    private static final String USER_AGENT = "BellCommand UpdateChecker";

    public CustomUpdateSource(BellCommand plugin) {
        this.plugin = plugin;
        this.customUrl = plugin.getConfig().getString("update-source.custom.check-url", "");
        this.verifySSL = plugin.getConfig().getBoolean("update-source.custom.verify-ssl", true);
        
        // 加载自定义请求头
        this.customHeaders = new HashMap<>();
        if (plugin.getConfig().contains("update-source.custom.headers")) {
            for (String key : plugin.getConfig().getConfigurationSection("update-source.custom.headers").getKeys(false)) {
                String value = plugin.getConfig().getString("update-source.custom.headers." + key);
                if (value != null) {
                    customHeaders.put(key, value);
                }
            }
        }
        
        if (customUrl.isEmpty() && plugin.isDebugEnabled()) {
            plugin.getLogger().warning("自定义更新URL未配置");
        }
    }

    private void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().warning("禁用SSL验证时发生错误: " + e.getMessage());
            }
        }
    }

    @Override
    public UpdateInfo checkUpdate() throws Exception {
        if (customUrl.isEmpty()) {
            throw new IllegalStateException("Custom update URL is not configured");
        }

        if (!verifySSL && customUrl.toLowerCase().startsWith("https")) {
            disableSSLVerification();
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(customUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            
            // 添加自定义请求头
            for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    if (plugin.isDebugEnabled()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("response", response.toString());
                        plugin.getLogger().info(plugin.getLanguageManager()
                            .getMessage("messages.debug.update.response", placeholders));
                    }

                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(response.toString());
                    String version = (String) json.get("version");
                    String downloadUrl = (String) json.get("download");
                    String description = (String) json.get("description");

                    if (version == null) {
                        throw new IllegalStateException("Invalid response format: missing version field");
                    }

                    return new UpdateInfo(
                        plugin.getDescription().getVersion(),
                        version,
                        downloadUrl != null ? downloadUrl : customUrl,
                        description
                    );
                }
            } else {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().warning("检查更新失败: HTTP " + connection.getResponseCode());
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        plugin.getLogger().warning("错误信息: " + response.toString());
                    }
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
} 