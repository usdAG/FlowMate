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

    private boolean excludedByNoiseReduction;

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
        this.excludedByNoiseReduction = false;
    }

    public InputValue(String value, String url, String type, String messageHash, String session) {
        this.value = value;
        this.url = url;
        this.type = type;
        this.messageHash = messageHash;
        this.session = session;
        this.identifier = Objects.hash(value, url, type, session);
        this.excludedByNoiseReduction = false;
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

    public boolean isExcludedByNoiseReduction() {
        return excludedByNoiseReduction;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public void setExcludedByNoiseReduction(boolean excludedByNoiseReduction) {
        this.excludedByNoiseReduction = excludedByNoiseReduction;
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
