package com.LanRhyme.geminiChatPlugin;

import org.bukkit.Bukkit;
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
        proxyUrl = config.getString("proxyUrl", "");
        apiKey = config.getString("apiKey", "");

        // 添加日志验证
        Bukkit.getLogger().info("Proxy URL: " + proxyUrl);
        Bukkit.getLogger().info("API Key: " + apiKey);
        if (proxyUrl.isEmpty() || apiKey.isEmpty()) {
            throw new IllegalStateException("Proxy URL或API Key未配置");
        }
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getApiKey() {
        return apiKey;
    }
}
