package se.fluff.imapchecker;


import java.io.*;
import java.util.*;

public class Main {

    private static Properties settings = new Properties();
    private static HashMap<String, String> accounts = new HashMap<>();
    private static HashMap<String,Thread> threads = new HashMap<>();

    public static void main(String[] args) {

        try(InputStream is = new FileInputStream("imapchecker.properties")) {
            settings.load(is);
        } catch (IOException e) {
            System.err.println("Could not find/read imapchecker.properties");
            throw new RuntimeException(e);
        }

        for(Object account : settings.keySet()) {
            accounts.put(account.toString(), settings.getProperty(account.toString()));
            ImapChecker ic = new ImapChecker(account.toString(), settings.getProperty(account.toString()));
            threads.put(account.toString(), new Thread(ic, account.toString()));
            threads.get(account.toString()).start();
        }

        while(true) {
            try {
                Thread.sleep(5 * 60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(String thread : threads.keySet()) {
                if(!threads.get(thread).isAlive()) {
                    System.out.println("Thread " + thread + " is no longer with us. Trying to restart.");
                    ImapChecker ic = new ImapChecker(thread, settings.getProperty(thread));
                    threads.put(thread, new Thread(ic, thread));
                    threads.get(thread).start();
                }
            }
        }
    }
}