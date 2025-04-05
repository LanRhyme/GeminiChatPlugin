package com.LanRhyme.geminiChatPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

    public PresetSelectorGUI(GeminiChatPlugin plugin, String title, int size) {
        this.plugin = plugin;
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size , title);
    }



    public static PresetSelectorGUI createDynamic(GeminiChatPlugin plugin, String title, List<String> presets) {
        int size = calculateInventorySize(presets.size());
        PresetSelectorGUI gui = new PresetSelectorGUI(plugin, title, size);
        gui.populate(presets);
        return gui;
    }

    private void populate(List<String> presets) {
        int backButtonSlot = inventory.getSize() - 1;

        // 填充预设项
        for (int i = 0; i < presets.size(); i++) {
            String presetName = presets.get(i);
            ItemStack item = createPresetItem(presetName);
            inventory.setItem(i, item);
        }

        // 添加返回按钮
        ItemStack backButton = createReturnItem();
        inventory.setItem(backButtonSlot, backButton);
    }


    private ItemStack createReturnItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "返回");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static int calculateInventorySize(int presetCount) {
        int requiredSlots = presetCount + 1; // +1 for back button
        int rows = (int) Math.ceil(requiredSlots / 9.0);
        return Math.min(rows * 9, 5 * 9); // 最多5行
    }

    public void open(Player player) {
        player.openInventory(getInventory());
    }

    public PresetSelectorGUI(GeminiChatPlugin plugin) {
        this(plugin, "预设选择菜单",27); // 默认标题
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