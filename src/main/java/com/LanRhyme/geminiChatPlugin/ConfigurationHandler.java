package com.LanRhyme.geminiChatPlugin;

import org.bukkit.configuration.file.FileConfiguration;


import java.io.File;


public class ConfigurationHandler {
    private final GeminiChatPlugin plugin;
    private String proxyUrl;
    private String apiKey;

    public ConfigurationHandler(GeminiChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        FileConfiguration config = plugin.getConfig();
        proxyUrl = config.getString("proxyUrl", "http://default-proxy.com");
        apiKey = config.getString("apiKey", "DEFAULT_API_KEY");
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getApiKey() {
        return apiKey;
    }
}
