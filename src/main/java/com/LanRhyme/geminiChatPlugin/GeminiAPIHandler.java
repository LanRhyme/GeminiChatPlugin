package com.LanRhyme.geminiChatPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import com.google.gson.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;

public class GeminiAPIHandler {
    private final PresetHandler presetHandler;
    private final GeminiChatPlugin plugin;
    private String proxyUrl;
    private String apiKey;
    private String preset;
    private List<String> conversationHistory;  //存储对话历史记录

    public GeminiAPIHandler(GeminiChatPlugin plugin, PresetHandler presetHandler) {
        this.plugin = plugin;
        this.presetHandler = presetHandler;
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

    public void addMessageToHistory(String message) {
        if (conversationHistory == null) {
            conversationHistory = new ArrayList<>();
        }
        conversationHistory.add(message);
        while (conversationHistory.size() > 30) {
            conversationHistory.remove(0);
        }
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String sendToGemini(String message) throws Exception {
        // 获取当前激活的preset名称
        String activePreset = presetHandler.getCurrentPreset();
        if (activePreset == null) {
            throw new IllegalStateException("No active preset found!");
        }

        // 加载对应preset的系统提示
        FileConfiguration presetConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "presets/" + activePreset + ".yml")
        );
        String systemPrompt = presetConfig.getString("system_prompt");
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            throw new IllegalArgumentException("System prompt missing in preset: " + activePreset);
        }

        // 构建包含系统提示的JSON请求体
        String requestBody = String.format(
                "{\"contents\": [{\"parts\":[{\"text\":\"%s\"}],\"role\":\"user\"}],\"system_instruction\":{\"parts\":[{\"text\":\"%s\"}]}}",
                escapeJson(message),
                escapeJson(systemPrompt)
        );


        if (conversationHistory == null) {
            conversationHistory = new ArrayList<>();
        }
        conversationHistory.add(message);
        //限制历史对话记录长度
        if (conversationHistory.size() > 30) {
            conversationHistory = conversationHistory.subList(conversationHistory.size() - 30, conversationHistory.size());
        }

        HttpClient client = HttpClient.newHttpClient();


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(proxyUrl + "/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("API返回非200状态码: " + response.statusCode());
        }
        // Parse the JSON response using lenient mode
        try (JsonReader jsonReader = new JsonReader(new StringReader(response.body()))) {
            jsonReader.setLenient(true); // 允许解析格式不太严格的 JSON
            JsonElement jsonResponse = JsonParser.parseReader(jsonReader);

            // 继续解析 JSON 数据
            if (jsonResponse.isJsonObject()) {
                JsonObject jsonObject = jsonResponse.getAsJsonObject();
                JsonElement candidatesElement = jsonObject.get("candidates");
                if (candidatesElement != null && candidatesElement.isJsonArray()) {
                    JsonArray candidatesArray = candidatesElement.getAsJsonArray();
                    if (candidatesArray.size() > 0) {
                        JsonElement firstCandidate = candidatesArray.get(0);
                        if (firstCandidate.isJsonObject()) {
                            JsonObject firstCandidateObject = firstCandidate.getAsJsonObject();
                            JsonElement contentElement = firstCandidateObject.get("content");
                            if (contentElement.isJsonObject()) {
                                JsonObject contentObject = contentElement.getAsJsonObject();
                                JsonElement partsElement = contentObject.get("parts");
                                if (partsElement.isJsonArray()) {
                                    JsonArray partsArray = partsElement.getAsJsonArray();
                                    if (partsArray.size() > 0) {
                                        JsonElement firstPart = partsArray.get(0);
                                        if (firstPart.isJsonObject()) {
                                            JsonObject firstPartObject = firstPart.getAsJsonObject();
                                            return firstPartObject.get("text").getAsString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Sending request to Gemini API with preset: " + preset);
        } catch (Exception e) {
            System.err.println("Gemini API请求失败: " + e.getMessage());
            e.printStackTrace();
        }
        return "§4API请求失败，请重试！";
    }

    private String conversationHistoryToString() {
        StringBuilder sb = new StringBuilder();
        for (String entry : conversationHistory) {
            sb.append(entry).append("\n");
        }
        return sb.toString();
    }
    // 辅助方法：转义JSON特殊字符
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}