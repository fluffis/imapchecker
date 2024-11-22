package se.fluff.imapchecker;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.eclipse.angus.mail.imap.IMAPFolder;

import java.io.*;
import java.util.*;

public class Main {

    private static Properties settings = new Properties();
    private static FilterLoader filterLoader;

    public static void main(String[] args) {

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utcTimeZone);

        try(InputStream is = new FileInputStream("imapchecker.properties")) {
            settings.load(is);
        } catch (IOException e) {
            System.err.println("Could not find/read imapchecker.properties");
            throw new RuntimeException(e);
        }

        String username = settings.getProperty("username");
        String password = settings.getProperty("password");
        String server = settings.getProperty("hostname");
        String ntfytoken = settings.getProperty("ntfytoken");
        String ntfyurl = settings.getProperty("ntfyurl");

        filterLoader = new FilterLoader(settings.getProperty("filterfile"), 600);

        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(props, null);


        try {
            Store store = session.getStore("imaps");
            store.connect(server, username, password);

            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_WRITE);
            inbox.setSubscribed(true);

            Folder junk = store.getFolder("inbox.Junk");
            junk.open(Folder.READ_WRITE);

            inbox.addMessageCountListener(new CheckMessageCountListener(junk, ntfytoken, ntfyurl));
            System.out.println("Unread count: " + inbox.getUnreadMessageCount());
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for(Message m : messages) {
                System.out.println("=====");
                System.out.println("ID: " + m.getMessageNumber());
                System.out.println("Date: " + m.getSentDate());
                System.out.println("Subject: " + m.getSubject());

                m.setFlag(Flags.Flag.SEEN, false);

            }
            while(true) {
                ((IMAPFolder)inbox).idle();
                filterLoader.tryLoad();
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    public static List<Junkfilter> getFilters() {
        return filterLoader.getFilters();
    }
}