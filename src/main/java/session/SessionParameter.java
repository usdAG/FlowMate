package session;

public class SessionParameter {

    private String name;
    private String value;
    private String type;
    private String newOccurrenceId;
    private String changedTo;

    public SessionParameter(String name, String value, String type, String changedTo, String newOccurrenceId) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.changedTo = changedTo;
        this.newOccurrenceId = newOccurrenceId;
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
