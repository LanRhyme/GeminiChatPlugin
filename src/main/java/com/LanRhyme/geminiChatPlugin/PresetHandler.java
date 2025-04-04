package com.LanRhyme.geminiChatPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PresetHandler {
    private final GeminiChatPlugin plugin;
    private String currentPreset;
    private List<String> presets = new ArrayList<>();

    public PresetHandler(GeminiChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPresets() {
        Path presetsFolder = Path.of(plugin.getDataFolder().toString(), "presets");
        if (!Files.exists(presetsFolder)) {
            try {
                Files.createDirectories(presetsFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.list(presetsFolder).forEach(path -> {
                if (path.toFile().isFile() && path.toString().endsWith(".yml")) {
                    String presetName = path.getFileName().toString().replace(".yml", "");
                    presets.add(presetName);
                }
            });

            Path currentPresetFile = Path.of(presetsFolder.toString(), "current_preset.txt");
            if (Files.exists(currentPresetFile)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(currentPresetFile.toFile()))) {
                    currentPreset = reader.readLine(); // 读取第一行
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                currentPreset = "default";
                saveCurrentPreset();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean switchPreset(String presetName) {
        Path presetsFolder = Path.of(plugin.getDataFolder().toString(), "presets");
        Path presetFile = Path.of(presetsFolder.toString(), presetName + ".yml");

        if (presetName == null || presetName.isEmpty()) {
            System.err.println("尝试切换到空预设名称");
            return false;
        }

        if (Files.exists(presetFile)) {
            currentPreset = presetName;
            saveCurrentPreset();
            return true;
        } else {
            return false;
        }
    }

    public void resetPlayerConversationHistory(Player player) {
        this.plugin.conversationHistoryMap.put(player, new ArrayList<>()); // 通过 plugin 访问
        Bukkit.getLogger().info("Reset conversation history for " + player.getName());
    }

    public void reloadPresets(Player player) {
        loadPresets();
        this.plugin.getPlayerConversationHistoryMap().clear(); // 通过 plugin 访问
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    private void saveCurrentPreset() {
        Path presetsFolder = Path.of(plugin.getDataFolder().toString(), "presets");
        Path currentPresetFile = Path.of(presetsFolder.toString(), "current_preset.txt");
        try {
            Files.write(currentPresetFile, currentPreset.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saveDefaultPreset() {
        Path presetsFolder = Path.of(plugin.getDataFolder().toString(), "presets");
        Path defaultPresetFile = Path.of(presetsFolder.toString(), "default.yml");

        if (!Files.exists(defaultPresetFile)) {
            try {
                Files.createFile(defaultPresetFile);
                // 写入默认预设内容
                String defaultPresetContent = "system_prompt: 用户正在游玩Minecraft，请根据用户的行为和对话内容，为用户提供帮助和指导。";
                Files.write(defaultPresetFile, defaultPresetContent.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void savemaomaoPreset() {
        Path presetsFolder = Path.of(plugin.getDataFolder().toString(), "presets");
        Path defaultPresetFile = Path.of(presetsFolder.toString(), "猫猫.yml");

        if (!Files.exists(defaultPresetFile)) {
            try {
                Files.createFile(defaultPresetFile);
                // 写入默认预设内容
                String defaultPresetContent = "system_prompt: 你将扮演一只猫猫,在对话时要使用颜文字和动作，用户正在游玩MInecraft，请根据用户的行为和对话内容，陪用户聊天";
                Files.write(defaultPresetFile, defaultPresetContent.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
