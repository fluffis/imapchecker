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

    private String threadname;
    private Folder junkfolder;
    private String ntfytoken;
    private String ntfyurl;
    private FilterLoader filterLoader;

    public CheckMessageCountListener(String threadname, Folder junkfolder, String ntfytoken, String ntfyurl, String filterfilename) {
        this.threadname = threadname;
        this.junkfolder = junkfolder;
        this.ntfytoken = ntfytoken;
        this.ntfyurl = ntfyurl;

        this.filterLoader = new FilterLoader(filterfilename, 600);
    }

    @Override
    public void messagesAdded(MessageCountEvent messageCountEvent) {
        Message[] messages = messageCountEvent.getMessages();

        try {
            filterLoader.tryLoad();
            for (Message m : messages) {
                log("=====");
                log("ID: " + m.getMessageNumber());
                log("Date: " + m.getSentDate());
                log("Subject: " + m.getSubject());

                if(messageMatches(m)) {
                    log("Spam detected!");
                    m.getFolder().copyMessages(new Message[]{ m }, junkfolder);
                    m.setFlag(Flags.Flag.DELETED, true);
                    m.getFolder().expunge();
                }
                else {
                    log("No spam detected!");

                    if(!this.ntfytoken.isEmpty()) {
                        Optional<Address> from = Arrays.stream(m.getFrom()).findFirst();
                        String sender = from.isPresent() ? from.get().toString() : "Unknown sender";
                        String body = getBodyFromMessageContent(m.getContent());
                        body = m.getSubject() + "\n\n" + body.substring(0, Math.min(body.length(), 100));

                        this.sendNotification(sender, body);
                    }
                    m.setFlag(Flags.Flag.SEEN, false);
                }

            }
        }
        catch(MessagingException | IOException e) {
            log(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void messagesRemoved(MessageCountEvent messageCountEvent) {

    }

    private void sendNotification(String sender, String body) {

        try {
            log("Sending notification");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest
                    .newBuilder(new URI(ntfyurl))
                    .header("Authorization", "Bearer " + ntfytoken)
                    .header("Title", sender)
                    .header("Tags", "incoming_envelope")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch(InterruptedException | URISyntaxException | IOException e) {
            log("Failed to send notification: " + e.getMessage());
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

            for(Junkfilter f : this.filterLoader.getFilters()) {
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

    private void log(String msg) {
        System.out.println(this.threadname + ": " + msg);

    }
}
