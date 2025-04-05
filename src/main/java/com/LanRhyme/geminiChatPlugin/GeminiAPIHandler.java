package com.LanRhyme.geminiChatPlugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.File;

import com.google.gson.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

public class GeminiAPIHandler {
    private final PresetHandler presetHandler;
    private final GeminiChatPlugin plugin;
    private String proxyUrl;
    private String apiKey;
    private String preset;
    private List<String> conversationHistory;  //存储对话历史记录
    private static final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    public GeminiAPIHandler(GeminiChatPlugin plugin, PresetHandler presetHandler, String proxyUrl, String apiKey) {
        this.plugin = plugin;
        this.presetHandler = presetHandler;
        this.proxyUrl = proxyUrl;
        this.apiKey = apiKey;
    }


    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setConversationHistory(List<String> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    private static final int MAX_HISTORY_LENGTH = 100;

    public void addMessageToHistory(String message) {
        if (conversationHistory.size() >= MAX_HISTORY_LENGTH) {
            conversationHistory.subList(0, MAX_HISTORY_LENGTH / 2).clear(); // 保留最新半数记录
        }
        conversationHistory.add(message);
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String sendToGemini(List<String> conversationHistory, String currentInput) throws Exception {
        // 在发送请求前验证系统提示有效性
        String activePreset = presetHandler.getCurrentPreset();
        String systemPrompt = presetHandler.getSystemPrompt(activePreset);
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            throw new IllegalStateException("系统提示未配置");
        }
        // 1. 构建 JSON 请求体
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();

        // 2. 添加历史对话
        for (int i = 0; i < conversationHistory.size(); i += 2) {
            if (i + 1 >= conversationHistory.size()) break; // 避免越界
            String userMsg = conversationHistory.get(i);
            String aiMsg = conversationHistory.get(i + 1);
            // 添加用户和AI消息到请求体

            // 用户消息
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("text", userMsg);
            JsonArray userParts = new JsonArray();
            userParts.add(userMessage);
            JsonObject userRole = new JsonObject();
            userRole.addProperty("role", "user");
            userRole.add("parts", userParts);
            contents.add(userRole);

            // AI 消息
            if (!aiMsg.isEmpty()) {
                JsonObject aiMessage = new JsonObject();
                aiMessage.addProperty("text", aiMsg);
                JsonArray aiParts = new JsonArray();
                aiParts.add(aiMessage);
                JsonObject aiRole = new JsonObject();
                aiRole.addProperty("role", "model");
                aiRole.add("parts", aiParts);
                contents.add(aiRole);
            }
        }


        requestBody.add("contents", contents);

        // 添加当前用户输入
        if (!conversationHistory.isEmpty()) {
            JsonObject currentUser = new JsonObject();
            currentUser.addProperty("text", currentInput);
            JsonArray currentParts = new JsonArray();
            currentParts.add(currentUser);
            JsonObject currentRole = new JsonObject();
            currentRole.addProperty("role", "user");
            currentRole.add("parts", currentParts);
            contents.add(currentRole);
        }


        // 添加系统提示
        JsonObject systemInstruction = new JsonObject();
        JsonObject part = new JsonObject();
        part.addProperty("text", systemPrompt);
        JsonArray partsArray = new JsonArray();
        partsArray.add(part);
        systemInstruction.add("parts", partsArray);
        requestBody.add("system_instruction", systemInstruction);

        // 4. 补充必需参数
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("max_output_tokens", 2048);
        requestBody.add("generation_config", generationConfig);

        // 序列化为字符串并发送请求
        String requestBodyString = gson.toJson(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(proxyUrl + "/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build();
        // 直接使用传入的 conversationHistory;
        FileConfiguration presetConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "presets/" + activePreset + ".yml")
        );


        // 动态构建并校验 URI
        String endpoint = "/v1beta/models/gemini-1.5-flash:generateContent?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String fullUrl = proxyUrl.endsWith("/") ? proxyUrl + endpoint : proxyUrl + "/" + endpoint;
        URI uri = URI.create(fullUrl);


        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("API 错误: " + response.statusCode());
            }

            // 解析响应
            JsonElement jsonResponse = JsonParser.parseString(response.body());
            JsonObject result = jsonResponse.getAsJsonObject().get("candidates").getAsJsonArray().get(0).getAsJsonObject();
            return result.get("content").getAsJsonObject().get("parts").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString();
        } catch (IOException | InterruptedException e) {
            Bukkit.getLogger().severe("API 请求失败: " + e.getMessage());
            return "请求超时或网络异常，请稍后重试"; // 返回兜底回复
        }
    }
}