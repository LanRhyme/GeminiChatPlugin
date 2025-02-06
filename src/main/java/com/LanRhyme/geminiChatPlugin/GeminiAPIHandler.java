package com.LanRhyme.geminiChatPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;

public class GeminiAPIHandler {
    private String proxyUrl;
    private String apiKey;
    private String preset;
    private List<String> conversationHistory;  //存储对话历史记录

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setConversationHistory(List<String> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String sendToGemini(String message) throws Exception {
        if (conversationHistory == null) {
            conversationHistory = new ArrayList<>();
        }
        conversationHistory.add(message);
        //限制历史对话记录长度
        if (conversationHistory.size() > 30) {
            conversationHistory = conversationHistory.subList(conversationHistory.size() - 30, conversationHistory.size());
        }

        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format(
                "{ \"contents\": [{ \"parts\": [{ \"text\": \"%s\" }] }] }",
                conversationHistoryToString()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(proxyUrl + "/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "§4解析失败，请重试！"; // 如果解析失败，返回默认回复
    }

    private String conversationHistoryToString() {
        StringBuilder sb = new StringBuilder();
        for (String entry : conversationHistory) {
            sb.append(entry).append("\n");
        }
        return sb.toString();
    }
}