package audit;

public abstract class AuditFinding {
    
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

    public enum FindingSeverity{
        INFORMATIONAL,
        LOW,
        MIDDLE,
        HIGH
    }
}
