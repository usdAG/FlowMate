package db.entities;

import org.neo4j.ogm.annotation.Id;

import java.util.Objects;

public class InputValue {

    @Id
    private int identifier;

    private String value;

    private String url;

    private String messageHash;

    private String session;

    private String type;

    // Empty Constructor needed for neo4J
    public InputValue() {

    }
    public InputValue(String value, String url, String type, String messageHash) {
        this.value = value;
        this.url = url;
        this.type = type;
        this.messageHash = messageHash;
        this.session = "not set";
        this.identifier = Objects.hash(value, url, type);
    }

    public InputValue(String value, String url, String type, String messageHash, String session) {
        this.value = value;
        this.url = url;
        this.type = type;
        this.messageHash = messageHash;
        this.session = session;
        this.identifier = Objects.hash(value, url, type, session);
    }

    public String getValue() {
        return value;
    }

    public String getUrl() {
        return url;
    }

    public String getSession() {
        return session;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public int getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "InputValue{" +
                "\nidentifier=\n'" + identifier + '\'' +
                "\nvalue='" + value + '\'' +
                "\n url='" + url + '\'' +
                "\n messageId=" + messageHash +
                "\n session='" + session + '\'' +
                '}';
    }
}
