package se.fluff.imapchecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Junkfilter {

    private final MailField field;
    private final Pattern pattern;

    public Junkfilter(MailField field, String text) {
        this.field = field;
        this.pattern = Pattern.compile(text);
    }

    public Junkfilter(String fieldtext, String text) {
        this(getMailField(fieldtext), text);
    }

    public MailField getMailField() {
        return field;
    }

    private static MailField getMailField(String fieldtext) {
        return switch (fieldtext) {
            case "subject":
                yield MailField.SUBJECT;
            case "sender":
                yield MailField.SENDER;
            case "receiver":
                yield MailField.RECEIVER;
            default:
                yield MailField.NOFIELD;

        };
    }

    public boolean match(MailField targetfield, String targettext) {
        if(!targetfield.equals(field) || field.equals(MailField.NOFIELD)) {
            return false;
        }

        return match(targettext);
    }

    public boolean match(String targettext) {
        Matcher m = pattern.matcher(targettext);
        return m.matches();
    }

    @Override
    public String toString() {
        return "Junkfilter{" +
                "field=" + field +
                ", pattern=" + pattern +
                '}';
    }
}
