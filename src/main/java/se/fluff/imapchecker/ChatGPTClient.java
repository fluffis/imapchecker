package se.fluff.imapchecker;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class ChatGPTClient {

    private final String token;
    private final String assistant;
    private final String baseurl  =  "https://api.openai.com/v1/";


    public ChatGPTClient(String token, String assistant) {
        this.token = token;
        this.assistant = assistant;
    }

    private String post(String url, String body) {

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest
                    .newBuilder(new URI(baseurl + url))
                    .header("Authorization", "Bearer " + this.token)
                    .header("OpenAI-Beta", "assistants=v2")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();
        }
        catch (InterruptedException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String get(String url) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest
                    .newBuilder(new URI(baseurl + url))
                    .header("Authorization", "Bearer " + this.token)
                    .header("OpenAI-Beta", "assistants=v2")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();
        }
        catch (InterruptedException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject createThread() {

        String res = post("threads", "");
        return new JSONObject(res);

    }

    public JSONObject createMessage(JSONObject thread, String role, String message) {

        HashMap<String, String> request = new HashMap<>();
        request.put("role", role);

        JSONObject json = new JSONObject(request);

        HashMap<String, String> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", message);
        JSONArray contentarray = new JSONArray();
        contentarray.put(content);
        json.put("content", contentarray);

        String res = post("threads/" + thread.getString("id") + "/messages", json.toString());
        return new JSONObject(res);
    }

    public JSONObject createRun(JSONObject thread) {

        HashMap<String, String> request = new HashMap<>();
        request.put("assistant_id", this.assistant);

        JSONObject json = new JSONObject(request);

        String res = post("threads/" + thread.getString("id") + "/runs", json.toString());

        return new JSONObject(res);
    }

    public JSONObject retrieveRun(JSONObject thread, JSONObject run) {

        String res = get("threads/" + thread.getString("id") + "/runs/" + run.getString("id"));

        return new JSONObject(res);
    }

    public JSONObject retrieveRunMessages(JSONObject thread, JSONObject run) {
        String res = get("threads/" + thread.getString("id") + "/messages?run_id=" + run.getString("id"));

        return new JSONObject(res);

    }

    public JSONObject retrieveThreadMessages(JSONObject thread) {
        String res = get("threads/" + thread.getString("id")+ "/messages");

        return new JSONObject(res);
    }

}
