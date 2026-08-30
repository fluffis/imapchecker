package se.fluff.imapchecker;

public class ClaudeRunner {

    private final ClaudeClient client;
    private final String systemPrompt;

    public ClaudeRunner(ClaudeClient client, String systemPrompt) {
        this.client = client;
        this.systemPrompt = systemPrompt;
    }

    public String run(String message) {
        try {
            return client.summarize(systemPrompt, message);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
