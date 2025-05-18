package se.fluff.imapchecker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ChatGPTRunner {

    private ChatGPTClient gptClient;
    private String[] donestatuses = new String[] { "completed", "expired", "failed", "incomplete", "cancelled"};


    public ChatGPTRunner(ChatGPTClient gptClient) {
        this.gptClient = gptClient;
    }

    public String run(String message) {
        try {
            JSONObject msgthread = gptClient.createThread();
            gptClient.createMessage(msgthread, "user", message);
            JSONObject run = gptClient.createRun(msgthread);
            int keeprunning = 0;
            String laststatus = run.getString("status");
            while (keeprunning < 10) {
                keeprunning++;
                JSONObject runstatus = gptClient.retrieveRun(msgthread, run);
                if (Arrays.stream(donestatuses).anyMatch(p -> p.equals(runstatus.getString("status")))) {
                    keeprunning = 100;
                }
                laststatus = runstatus.getString("status");
                TimeUnit.SECONDS.sleep(5);
            }

            if(!"completed".equals(laststatus)) {
                return "Run failed: " + laststatus;
            }
            JSONObject afterrunmsg = gptClient.retrieveRunMessages(msgthread, run);
            JSONArray data = afterrunmsg.getJSONArray("data");
            String firstmsg = data.getJSONObject(0)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getJSONObject("text")
                    .getString("value");
            return firstmsg;
        }
        catch(Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
