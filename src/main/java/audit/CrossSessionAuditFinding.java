package audit;

public class CrossSessionAuditFinding extends AuditFinding {

    public String parameterName;
    public String sessionEntered;
    public String sessionFound;

    private static final String name = "Cross-Session Parameter Match";



    public CrossSessionAuditFinding(String parameterName, String sessionEntered, String sessionFound) {
        super(name, FindingSeverity.MIDDLE);
        this.parameterName = parameterName;
        this.sessionEntered = sessionEntered;
        this.sessionFound = sessionFound;
        
    }


    public void renameSession(String oldName, String newName){
        if(sessionEntered.equals(oldName)){
            this.sessionEntered = newName;
        }
        else if(sessionFound.equals(oldName)){
            this.sessionFound = newName;
        }
    }

    @Override
    public String getShortDescription() {
        return String.format("The parameter %s was entered in session %s and found in session %s", this.parameterName, this.sessionEntered, this.sessionFound);
    }

    @Override
    public String getLongDescription() {
        String htmlTemplate = """
        <h1>Description</h1>

        FlowMate identified a <b>data flow that crosses session-boundaries</b>. When the parameter is injectable for Cross-Site Scripting, this may allow vertical or horizontal privilege escalation within the application.
        
        This is particularly interesting if the parameter value can be modified by a low-privileged user and is subsequently displayed to a higher-privileged user.
        
        As an example, consider the username of an application that can be set by a regular user and is displayed to an administrator in context of user management.
        
        Note that this might be a false positive, if there are multiple sessions for the same user created within FlowMate.
        
        <h1>Details</h1>
        
        The following cross-session data flow has been identified:
        
        <ul>
            <li>Parameter (Value): PARAMETER_NAME</li>
            <li>Input Location (Session): SESSION_ENTERED</li>
            <li>Output Location (Session): SESSION_FOUND</li>
        </ul>
        
        <h1>How to Test</h1>
        
        <ol>
            <li>Create a XSS payload based on the output context of the parameter. You can use the match preview or search for the value in the response</li>
            <li>Inject the payload in the input location of the parameter</li>
            <li>Check the output location whether the payload triggers or not. Refine your payload</li>
            <li>If the vulnerability is exploitable, you have likely identified a path for privilege escalation</li>
        </ol>
        """;

        return htmlTemplate.replace("PARAMETER_NAME", this.parameterName).replace("SESSION_ENTERED", this.sessionEntered).replace("SESSION_FOUND", this.sessionFound);
    }
}
