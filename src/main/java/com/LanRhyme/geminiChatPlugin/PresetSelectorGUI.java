package com.LanRhyme.geminiChatPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PresetSelectorGUI implements InventoryHolder {
    private final GeminiChatPlugin plugin;
    private final String title;
    private Inventory inventory;

    public PresetSelectorGUI(GeminiChatPlugin plugin, String title) {
        this.plugin = plugin;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, 9 * 3, title);
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, 9 * 3, title);
        }
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(getInventory());
    }

    public PresetSelectorGUI(GeminiChatPlugin plugin) {
        this(plugin, "预设选择菜单"); // 默认标题
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9 * 3, "预设选择菜单");
        // 通过插件实例获取presetHandler
        PresetHandler presetHandler = plugin.getPresetHandler();
        // 动态加载预设
        List<String> presets = plugin.getPresetHandler().getPresets();
        for (int i = 0; i < presets.size(); i++) {
            String presetName = presets.get(i);
            ItemStack item = createPresetItem(presetName);
            gui.setItem(i, item);
        }

        player.openInventory(gui);
    }

    private ItemStack createPresetItem(String presetName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + presetName);

        // 获取系统提示作为描述
        String systemPrompt = getPresetSystemPrompt(presetName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "系统提示：");
        lore.addAll(Arrays.asList(systemPrompt.split("\n")));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String getPresetSystemPrompt(String presetName) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "presets/" + presetName + ".yml")
        );
        return config.getString("system_prompt", "无描述");
    }

}