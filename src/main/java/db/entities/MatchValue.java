package db.entities;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import java.util.Objects;

@NodeEntity
public class MatchValue {
    @Id
    private int identifier;

    private String name;

    private String value;

    private String responseContentType;

    private String matchProof;

    private String url;

    private String session;

    private String messageHash;


    // Empty Constructor needed for neo4J
    public MatchValue() {

    }
    public MatchValue(String name, String value, String responseContentType, String matchProof, String url, String messageHash) {
        this.name = name;
        this.value = value;
        this.responseContentType = responseContentType;
        this.matchProof = matchProof;
        this.url = url;
        this.session = "not set";
        this.messageHash = messageHash;
        this.identifier = Objects.hash(name, value, matchProof, url, this.session);
    }

    public MatchValue(String name, String value, String responseContentType, String matchProof, String url, String messageHash, String session) {
        this.name = name;
        this.value = value;
        this.responseContentType = responseContentType;
        this.matchProof = matchProof;
        this.url = url;
        this.session = session;
        this.messageHash = messageHash;
        this.identifier = Objects.hash(name, value, matchProof, url, session);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getValue() {
        return value;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public String getResponseContentType() {
        return this.responseContentType;
    }

    public String getMatchProof() {
        return this.matchProof;
    }

    public int getIdentifier(){
        return this.identifier;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "MatchValue{" +
                "\nidentifier=\n'" + identifier + '\'' +
                "\nname=\n'" + name + '\'' +
                "\n value=\n'" + value + '\'' +
                "\n responseContentType=\n'" + responseContentType + '\'' +
                "\n matchProof=\n'" + matchProof + '\'' +
                "\n url=\n'" + url + '\'' +
                "\n session=\n'" + session + '\'' +
                "\n}";
    }
}
