package com.LanRhyme.geminiChatPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class GeminiChatPlugin extends JavaPlugin implements Listener {
    private ConfigurationHandler configHandler;
    private PresetHandler presetHandler;
    private GeminiAPIHandler apiHandler;
    private Map<Player, List<String>> conversationHistoryMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Initialize handlers
        configHandler = new ConfigurationHandler(this);
        presetHandler = new PresetHandler(this);
        apiHandler = new GeminiAPIHandler();

        // Load configurations and presets on plugin startup
        configHandler.loadConfig();
        presetHandler.loadPresets();

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        // Save default config and presets
        saveDefaultConfig();
        presetHandler.saveDefaultPreset();
        presetHandler.savemaomaoPreset();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith("#*")) {
            event.setCancelled(true);
            String input = message.substring(2).trim();

            Player player = event.getPlayer();
            player.sendMessage(ChatColor.AQUA + "[YOU] " + ChatColor.WHITE + input);

            List<String> conversationHistory = conversationHistoryMap.getOrDefault(player, new ArrayList<>());
            apiHandler.setConversationHistory(conversationHistory);

            apiHandler.setProxyUrl(configHandler.getProxyUrl());
            apiHandler.setApiKey(configHandler.getApiKey());

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    String reply = apiHandler.sendToGemini(input);
                    event.getPlayer().sendMessage(ChatColor.GOLD + "[◇] " + ChatColor.WHITE + reply);

                    //将 AI 的回复添加到对话历史记录
                    conversationHistory.add(input);
                    conversationHistory.add(reply);
                    conversationHistoryMap.put(player, conversationHistory);
                } catch (Exception e) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
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
            if (args.length == 1) {
                String presetName = args[0];
                if (presetHandler.switchPreset(presetName)) {
                    player.sendMessage(ChatColor.GREEN + "预设已切换为: " + presetName);
                    //重置历史对话记录
                    conversationHistoryMap.put(player, new ArrayList<>());
                } else {
                    player.sendMessage(ChatColor.RED + "预设文件不存在，请检查预设名称" );
                }
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "用法: /switchPreset <preset_name>");
                return true;
            }
        }
        return false;
    }

}
