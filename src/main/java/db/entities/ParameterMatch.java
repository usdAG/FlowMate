package db.entities;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NodeEntity
public class ParameterMatch {

    @Id
    private int identifier;

    private String value;

    private String name;

    private String type;

    private String messageHash;
    private String url;

    private String session;

    @Relationship(type = "MATCH", direction = Relationship.Direction.OUTGOING)
    private List<MatchValue> matchEntries = new ArrayList<MatchValue>();

    @Relationship(type="VALUE_MATCHED", direction = Relationship.Direction.OUTGOING)
    private InputValue valueMatched;
    
    public ParameterMatch(String relatedParameterName, String relatedParameterValue, String type, String messageHash, String url, InputValue inputValue) {
        this.name = relatedParameterName;
        this.value = relatedParameterValue;
        this.type = type;
        this.messageHash = messageHash;
        this.url = url;
        this.session = "not set";
        this.identifier = Objects.hash(relatedParameterName, relatedParameterValue, type, url);
        this.valueMatched = inputValue;
    }

    public ParameterMatch(String relatedParameterName, String relatedParameterValue, String type, String messageHash, String url, String session, InputValue inputValue) {
        this.name = relatedParameterName;
        this.value = relatedParameterValue;
        this.type = type;
        this.messageHash = messageHash;
        this.url = url;
        this.session = session;
        this.identifier = Objects.hash(relatedParameterName, relatedParameterValue, type, url, session);
        this.valueMatched = inputValue;
    }

    // Empty Constructor needed for neo4J
    public ParameterMatch() {}

    public void addMatchEntryEntity(MatchValue matchValueEntity) {
        matchEntries.add(matchValueEntity);
    }

    public int getIdentifier() {
        return this.identifier;
    }

    public String getUrl() {
        return this.url;
    }

    public String getSession() {
        return session;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public InputValue getInputValue(){
        return this.valueMatched;
    }

    @Override
    public String toString() {
        return "ParameterMatch{" +
                "\nidentifier=\n'" + identifier + '\'' +
                "\n value='" + value + '\'' +
                "\n name='" + name + '\'' +
                "\n messageId=" + messageHash +
                "\n url='" + url + '\'' +
                "\n session='" + session + '\'' +
                "\n matchEntries=" + matchEntries +
                "\n}";
    }
}
