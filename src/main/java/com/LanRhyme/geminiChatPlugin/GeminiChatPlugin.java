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
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GeminiChatPlugin extends JavaPlugin implements Listener {
    private ConfigurationHandler configHandler;
    private PresetHandler presetHandler;
    private GeminiAPIHandler apiHandler;
    private static final String PRESET_MENU_TITLE = ChatColor.BLUE + "预设选择菜单";
    public Map<Player, List<String>> conversationHistoryMap = new ConcurrentHashMap<>();


    @Override
    public void onEnable() {
        // Initialize handlers
        configHandler = new ConfigurationHandler(this);
        configHandler.loadConfig();
        presetHandler = new PresetHandler(this);
        apiHandler = new GeminiAPIHandler(this, presetHandler,
                configHandler.getProxyUrl(),
                configHandler.getApiKey());
        apiHandler.setProxyUrl(configHandler.getProxyUrl());  // 新增
        apiHandler.setApiKey(configHandler.getApiKey());

        // Load configurations and presets on plugin startup

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

        if (message.startsWith("#*")) {
            event.setCancelled(true);
            Player playerRef = player; // 保留Player引用（避免异步失效）
            // 初始化对话历史
            List<String> history = conversationHistoryMap.getOrDefault(playerRef, new ArrayList<>());
            if (history.isEmpty()) {
                conversationHistoryMap.putIfAbsent(playerRef, history);
            }
            // 提取并存储输入内容
            String input = message.substring(2).trim();
            conversationHistoryMap.get(player).add(input);
            player.sendMessage(ChatColor.AQUA + "[YOU] " + ChatColor.WHITE + input);

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    String reply = apiHandler.sendToGemini(history, input); // 传递两个参数

                    Bukkit.getScheduler().runTask(this, () -> {
                        if (!reply.trim().isEmpty()) {
                            history.add(input);
                            history.add(reply);
                            player.sendMessage(ChatColor.GOLD + "[◇] " + ChatColor.WHITE + reply);
                        } else {
                            player.sendMessage(ChatColor.RED + "AI 未返回有效回复");
                        }
                    });
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
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

        if (isPresetMenuOpen(player, clickedInventory)) {
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

    private boolean isPresetMenuOpen(Player player, Inventory inventory) {
        if (inventory == null || !(inventory.getHolder() instanceof PresetSelectorGUI)) {
            return false;
        }

        // 直接通过InventoryHolder获取标题
        InventoryView openInventory = player.getOpenInventory();
        return true;
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
            resetPlayerHistory(player);// 切换预设时清空对话历史
            player.sendMessage(ChatColor.GREEN + "已切换至预设: " + presetName);
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
        PresetHandler presetHandler = this.getPresetHandler();
        List<String> presets = presetHandler.getPresets();

        if (presets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "没有可用的预设！");
            return;
        }

        PresetSelectorGUI gui = PresetSelectorGUI.createDynamic(
                this,
                ChatColor.BLUE + "预设选择菜单",
                presets
        );
        gui.open(player);
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

