package cn.ningmo.bellcommand.update;

import cn.ningmo.bellcommand.BellCommand;
import org.bukkit.configuration.ConfigurationSection;
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

    public CustomUpdateSource(BellCommand plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.customUrl = plugin.getConfig().getString("update-source.custom-url", "");
    }

    @Override
    public UpdateInfo checkUpdate() throws Exception {
        if (customUrl.isEmpty()) {
            throw new IllegalStateException("Custom update URL is not configured");
        }

        URL url = new URL(customUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
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

                return new UpdateInfo(
                    plugin.getDescription().getVersion(),
                    version,
                    downloadUrl
                );
            }
        }
        return null;
    }
} 