package se.fluff.imapchecker;

import jakarta.mail.*;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Optional;

public class CheckMessageCountListener implements MessageCountListener {

    private Folder junkfolder;
    private String ntfytoken;
    private String ntfyurl;

    public CheckMessageCountListener(Folder junkfolder, String ntfytoken, String ntfyurl) {
        this.junkfolder = junkfolder;
        this.ntfytoken = ntfytoken;
        this.ntfyurl = ntfyurl;
    }

    @Override
    public void messagesAdded(MessageCountEvent messageCountEvent) {
        Message[] messages = messageCountEvent.getMessages();

        try {
            for (Message m : messages) {
                System.out.println("=====");
                System.out.println("ID: " + m.getMessageNumber());
                System.out.println("Date: " + m.getSentDate());
                System.out.println("Subject: " + m.getSubject());

                if(messageMatches(m)) {
                    System.out.println("Spam detected!");
                    m.getFolder().copyMessages(new Message[]{ m }, junkfolder);
                    m.setFlag(Flags.Flag.DELETED, true);
                    m.getFolder().expunge();
                }
                else {
                    System.out.println("No spam detected!");

                    Optional<Address> from = Arrays.stream(m.getFrom()).findFirst();
                    String sender = from.isPresent() ? from.get().toString() : "Unknown sender";
                    String body = getBodyFromMessageContent(m.getContent());
                    body = m.getSubject() + "\n\n" + body.substring(0, Math.min(body.length(), 100));

                    this.sendNotification(sender, body);
                    m.setFlag(Flags.Flag.SEEN, false);
                }

            }
        }
        catch(MessagingException | IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void messagesRemoved(MessageCountEvent messageCountEvent) {

    }

    private void sendNotification(String sender, String body) {

        try {
            System.out.println("Sending notification");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest
                    .newBuilder(new URI("https://ntfy.sh/fluff-will-notify-you"))
                    .header("Authorization", "Bearer " + ntfytoken)
                    .header("Title", sender)
                    .header("Tags", "incoming_envelope")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch(InterruptedException | URISyntaxException | IOException e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }

    private static String getBodyFromMessageContent(Object content) {

        StringBuilder sb = new StringBuilder();

        try {

            if (content instanceof Multipart multipart) {

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.getContentType().toLowerCase().startsWith("text/plain")) {
                        sb.append((String) bodyPart.getContent());
                    } else if (bodyPart.getContentType().toLowerCase().startsWith("text/html")) {
                        Document doc = Jsoup.parse((String) bodyPart.getContent());
                        sb.append(doc);
                    }
                }
            } else {
                sb.append((String) content);
            }
        } catch (IOException | MessagingException e) {
            // handle exception
        }

        return sb.toString();
    }

    private boolean messageMatches(Message m) {

        try {
            Address[] froms = m.getFrom();
            Address[] tos = m.getAllRecipients();
            String subject = m.getSubject().toLowerCase();

            for(Junkfilter f : Main.getFilters()) {
                if(f.getMailField().equals(MailField.SUBJECT)) {
                    if(f.match(subject)) {
                        return true;
                    }
                }
                else if(f.getMailField().equals(MailField.RECEIVER)) {
                    for(Address to : tos) {
                        if(f.match(to.toString())) {
                            return true;
                        }
                    }
                }
                else if(f.getMailField().equals(MailField.SENDER)) {
                    for(Address from : froms) {
                        if(f.match(from.toString())) {
                            return true;
                        }
                    }
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return false;
    }
}
