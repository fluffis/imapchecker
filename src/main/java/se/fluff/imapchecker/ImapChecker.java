package se.fluff.imapchecker;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.eclipse.angus.mail.imap.IMAPFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.TimeZone;

public class ImapChecker implements Runnable {

    private String threadname;
    private Properties settings = new Properties();


    public ImapChecker(String threadname, String filename) {

        this.threadname = threadname;

        try(InputStream is = new FileInputStream(filename)) {
            settings.load(is);
        } catch (IOException e) {
            System.err.println("Could not find/read " + filename);
            throw new RuntimeException(e);
        }

    }

    @Override
    public void run() {
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utcTimeZone);

        String username = settings.getProperty("username");
        String password = settings.getProperty("password");
        String server = settings.getProperty("hostname");
        String ntfytoken = settings.getProperty("ntfytoken");
        String ntfyurl = settings.getProperty("ntfyurl");
        String junkpath = settings.getProperty("junkpath");

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(props, null);


        try {
            Store store = session.getStore("imaps");
            store.connect(server, username, password);

            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_WRITE);
            inbox.setSubscribed(true);

            Folder junk = store.getFolder(junkpath);
            junk.open(Folder.READ_WRITE);

            CheckMessageCountListener cmcl = new CheckMessageCountListener(
                    this.threadname,
                    junk,
                    ntfytoken,
                    ntfyurl,
                    settings.getProperty("filterfile")
            );

            if(!settings.getProperty("openaikey", "").isEmpty()) {
                cmcl.loadAI(settings.getProperty("openaikey"), settings.getProperty("assistantid"));
            }

            inbox.addMessageCountListener(cmcl);
            log("Unread count: " + inbox.getUnreadMessageCount());
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for(Message m : messages) {
                log("=====");
                log("ID: " + m.getMessageNumber());
                log("Date: " + m.getSentDate());
                log("Subject: " + m.getSubject());

                m.setFlag(Flags.Flag.SEEN, false);

            }
            while(true) {
                ((IMAPFolder)inbox).idle();
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void log(String msg) {
        System.out.println(this.threadname + ": " + msg);

    }
}
