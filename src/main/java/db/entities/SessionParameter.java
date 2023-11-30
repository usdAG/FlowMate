package db.entities;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import java.util.Objects;

@NodeEntity
public class SessionParameter {

    @Id
    private int identifier;
    private String name;
    private String value;
    private String type;
    private String newOccurrenceId;
    private String changedTo;

    public SessionParameter() {}

    public SessionParameter(String name, String value, String type, String changedTo, String newOccurrenceId) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.changedTo = changedTo;
        this.newOccurrenceId = newOccurrenceId;
        this.identifier = Objects.hash(this.name, this.value, this.type, this.changedTo, this.newOccurrenceId);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getNewOccurrenceId() {
        return newOccurrenceId;
    }

    public String getChangedTo() {
        return changedTo;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setChangedTo(String changedTo) {
        this.changedTo = changedTo;
    }

    @Override
    public String toString() {
        return "SessionParameter{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type='" + type + '\'' +
                ", newOccurrenceId='" + newOccurrenceId + '\'' +
                ", changedTo='" + changedTo + '\'' +
                '}';
    }
}
