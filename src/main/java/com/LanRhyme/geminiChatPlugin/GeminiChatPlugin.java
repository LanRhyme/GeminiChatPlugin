package com.LanRhyme.geminiChatPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.Arrays;
import java.util.List;

public class GeminiChatPlugin extends JavaPlugin implements Listener {
    private ConfigurationHandler configHandler;
    private PresetHandler presetHandler;
    private GeminiAPIHandler apiHandler;
    public Map<Player, List<String>> conversationHistoryMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Initialize handlers
        configHandler = new ConfigurationHandler(this);
        presetHandler = new PresetHandler(this);
        apiHandler = new GeminiAPIHandler(this, presetHandler); // 注入依赖

        // Load configurations and presets on plugin startup
        configHandler.loadConfig();
        presetHandler.loadPresets();
        presetHandler.saveDefaultPreset();
        presetHandler.savemaomaoPreset();
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        // Save default config and presets
        saveDefaultConfig();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        apiHandler.setPreset(presetHandler.getCurrentPreset());

        if (message.startsWith("#*")) {
            // 获取当前玩家选中的preset
            String selectedPreset = presetHandler.getCurrentPreset();
            String input = message.substring(2).trim();
            apiHandler.setPreset(selectedPreset); // 关键修改：同步当前preset
            // 更新玩家的对话历史
            event.setCancelled(true);
            List<String> conversationHistory = conversationHistoryMap.getOrDefault(player, new ArrayList<>());
            Bukkit.getScheduler().runTask(this, () -> {
                conversationHistoryMap.put(player, conversationHistory);
            });
            apiHandler.setConversationHistory(conversationHistory);

            apiHandler.setProxyUrl(configHandler.getProxyUrl());
            apiHandler.setApiKey(configHandler.getApiKey());

            player.sendMessage(ChatColor.AQUA + "[YOU] " + ChatColor.WHITE + input);

            // 保存历史记录到Map（异步操作需同步到主线程）
            Bukkit.getScheduler().runTask(this, () -> {
                conversationHistoryMap.put(player, conversationHistory);
            });
            apiHandler.setConversationHistory(conversationHistory);

            apiHandler.setProxyUrl(configHandler.getProxyUrl());
            apiHandler.setApiKey(configHandler.getApiKey());

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    String reply = apiHandler.sendToGemini(input);
                    Bukkit.getScheduler().runTask(this, () -> {
                        player.sendMessage(ChatColor.GOLD + "[◇] " + ChatColor.WHITE + reply);
                    });

                    // 重新获取 conversationHistory 并更新
                    List<String> updatedHistory = conversationHistoryMap.getOrDefault(player, new ArrayList<>());
                    updatedHistory.add(input);
                    updatedHistory.add(reply);
                    conversationHistoryMap.put(player, updatedHistory);
                } catch (Exception e) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    public Map<Player, List<String>> getPlayerConversationHistoryMap() {
        return conversationHistoryMap;
    }

    public void resetPlayerHistory(Player player) {
        conversationHistoryMap.put(player, new ArrayList<>());
    }

    // 新增事件处理：GUI点击
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory != null
                && clickedItem != null) {

            event.setCancelled(true); // 防止误操作

            if (clickedItem.getType() == Material.PAPER) {
                String presetName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                if ("返回".equals(presetName)) {
                    player.closeInventory();
                    return;
                }

                // 执行预设切换
                if (presetHandler.switchPreset(presetName)) {
                    player.sendMessage(ChatColor.GREEN + "已切换至预设: " + presetName);
                    resetPlayerHistory(player); // 清空对话历史
                } else {
                    player.sendMessage(ChatColor.RED + "预设不存在！");
                }
            }
        }
    }
    // 新增公共访问方法
    public PresetHandler getPresetHandler() {
        return presetHandler;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令");
            return true;
        }

        Player player = (Player) sender;

        // Check if the player has OP permissions
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("switchPreset")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "用法: /switchPreset <preset_name>");
                return true;
            }

            String presetName = args[0];
            if (!presetHandler.switchPreset(presetName)) {
                player.sendMessage(ChatColor.RED + "预设不存在！");
                return true;
            }

            // 关键：切换后同步到API Handler
            conversationHistoryMap.put(player, new ArrayList<>());// 切换预设时清空对话历史
            player.sendMessage(ChatColor.GREEN + "已切换至预设: " + presetName);
            conversationHistoryMap.put(player, new ArrayList<>());
            return true;
        } else if (cmd.getName().equalsIgnoreCase("presetmenu")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "你没有权限使用此命令");
                return true;
            }

            openPresetSelectionGUI(player);
            return true;
        }
        return false;
    }

    private void openPresetSelectionGUI(Player player) {
        String title = ChatColor.BLUE + "预设选择菜单";
        PresetSelectorGUI gui = new PresetSelectorGUI(this, title); // 传递插件实例
        Inventory inventory = gui.getInventory(); // 获取Inventory对象
        gui.open(player);
        // 加载所有预设
        List<String> presets = presetHandler.getPresets();
        presets.add(0, "返回"); // 添加返回按钮

        for (int i = 0; i < presets.size(); i++) {
            String presetName = presets.get(i);
            ItemStack item = createPresetItem(presetName);
            inventory.setItem(i, item);
        }

        player.openInventory(inventory);
    }

    // 新增方法：创建预设展示物品
    private ItemStack createPresetItem(String presetName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + presetName);

        // 获取系统提示作为物品描述
        String systemPrompt = getPresetSystemPrompt(presetName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "系统提示：");
        lore.addAll(Arrays.asList(systemPrompt.split("\n"))); // 支持多行描述
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // 新增方法：获取预设的系统提示
    private String getPresetSystemPrompt(String presetName) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(
                new File(getDataFolder(), "presets/" + presetName + ".yml")
        );
        return config.getString("system_prompt", "无描述");
    }
}

