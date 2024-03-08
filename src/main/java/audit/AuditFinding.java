package audit;

import java.io.Serializable;
import java.util.Objects;

public abstract class AuditFinding implements Serializable {
    
    public String name; 
    public FindingSeverity severity;


    public AuditFinding(String name, FindingSeverity severity){
        this.name = name;
        this.severity = severity;
    }

    public abstract String getShortDescription();
    public abstract String getLongDescription();

    public String getLabelRepresentation(){
        return String.format("Name: <b>%s</b><br>Severity: <b>%s</b><br>Description: %s", this.name, this.severity, this.getShortDescription());
    }

    public String getHash() {
        return String.valueOf(Objects.hash(this.name, this.severity, this.getShortDescription(), this.getLongDescription()));
    }

    public enum FindingSeverity{
        INFORMATIONAL,
        LOW,
        MIDDLE,
        HIGH
    }
}
