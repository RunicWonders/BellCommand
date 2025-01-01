package cn.ningmo.bellcommand.update;

import cn.ningmo.bellcommand.BellCommand;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CustomUpdateSource implements UpdateSource {
    private final BellCommand plugin;
    private final String customUrl;
    private static final String USER_AGENT = "BellCommand UpdateChecker";

    public CustomUpdateSource(BellCommand plugin) {
        this.plugin = plugin;
        this.customUrl = plugin.getConfig().getString("update-source.custom-url", "");
        if (customUrl.isEmpty() && plugin.isDebugEnabled()) {
            plugin.getLogger().warning("自定义更新URL未配置");
        }
    }

    @Override
    public UpdateInfo checkUpdate() throws Exception {
        if (customUrl.isEmpty()) {
            throw new IllegalStateException("Custom update URL is not configured");
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(customUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
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
                    String downloadUrl = (String) json.get("download_url");

                    if (version == null || downloadUrl == null) {
                        throw new IllegalStateException("Invalid response format");
                    }

                    return new UpdateInfo(
                        plugin.getDescription().getVersion(),
                        version,
                        downloadUrl
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