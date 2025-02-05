package com.LanRhyme.geminiChatPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PresetHandler {
    private final GeminiChatPlugin plugin;
    private String currentPreset;

    public PresetHandler(GeminiChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPresets() {
        File presetsFolder = new File(plugin.getDataFolder(), "presets");
        if (!presetsFolder.exists()) {
            presetsFolder.mkdirs();
        }

        File currentPresetFile = new File(presetsFolder, "current_preset.txt");
        if (!currentPresetFile.exists()) {
            try {
                currentPresetFile.createNewFile();
                currentPreset = "default";
                Files.write(currentPresetFile.toPath(), currentPreset.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                currentPreset = Files.readAllLines(currentPresetFile.toPath()).get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveDefaultPreset() {
        File presetsFolder = new File(plugin.getDataFolder(), "presets");
        File defaultPreset = new File(presetsFolder, "default.yml");
        File defaultPreset0 = new File(presetsFolder,"猫娘.yml");

        if (!defaultPreset.exists()) {
            try {
                defaultPreset.createNewFile();
                FileConfiguration config = YamlConfiguration.loadConfiguration(defaultPreset);
                config.set("system_prompt", "用户正在游玩Minecraft，请扮演一个友善的AI助手，尽可能提供有用的信息。");
                config.save(defaultPreset);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public boolean switchPreset(String presetName) {
        File presetsFolder = new File(plugin.getDataFolder(), "presets");
        File presetFile = new File(presetsFolder, presetName + ".yml");

        if (presetFile.exists()) {
            return false;
        }

        currentPreset = presetName;
        try {
            Files.write(new File(presetsFolder, "current_preset.txt").toPath(), currentPreset.getBytes());
        return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
