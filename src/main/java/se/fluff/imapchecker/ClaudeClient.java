package se.fluff.imapchecker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClaudeClient {

    private final String apiKey;
    private final String model;
    private final String workspaceId;
    private final String baseurl = "https://api.anthropic.com/v1/";

    public ClaudeClient(String apiKey, String model, String workspaceId) {
        this.apiKey = apiKey;
        this.model = model;
        this.workspaceId = workspaceId;
    }

    public String summarize(String systemPrompt, String message) {

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("system", systemPrompt);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.put(userMessage);
        body.put("messages", messages);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(new URI(baseurl + "messages"))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json");
            if (workspaceId != null && !workspaceId.isEmpty()) {
                requestBuilder.header("anthropic-workspace-id", workspaceId);
            }
            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject result = new JSONObject(response.body());
            if (result.has("error")) {
                throw new RuntimeException("Claude API error: " + result.getJSONObject("error").getString("message"));
            }
            return result.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text");
        } catch (InterruptedException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
