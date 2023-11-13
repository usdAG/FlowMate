package audit;

public class KeywordMatchAuditFinding extends AuditFinding {

    private String parameterName;
    private String inputLocation;
    private String outputLocation;
    private static final String name = "Keyword Match";

    public KeywordMatchAuditFinding(String parameterName, String inputLocation, String outputLocation) {
        super(name, AuditFinding.FindingSeverity.MIDDLE);
        this.parameterName = parameterName;
        this.inputLocation = inputLocation;
        this.outputLocation = outputLocation;
    }

    @Override
    public String getShortDescription() {
        return String.format("The parameter %s was entered in %s and found in %s", this.parameterName, this.inputLocation, this.outputLocation);
    }

    @Override
    public String getLongDescription() {
        String htmlTemplate = """
            <h1>Description</h1>
                            
            FlowMate identified a <b>match of a potentially sensitive parameter, based on its name</b>.
            This includes parameters with names such as "secret," "password," or variations and alternate spellings of these parameter names.
            Appearances of such parameters in the outputs of an application should be closely monitored.
            Depending on the context in which the value is returned to the user, this could potentially lead to an information disclosure vulnerability.
                            
            <h1>Details</h1>
                            
            The following data flow has been Identified:
                            
            <ul>
                <li>Parameter (Value): PARAMETER_NAME</li>
                <li>Input Location: INPUT_LOCATION</li>
                <li>Output Location: OUTPUT_LOCATION</li>
            </ul>
            
            <h1>How to Test</h1>
            
            <ol>
                <li>Check each instance of affected parameters to determine if the corresponding output context is safe for returning these values</li>
                <li>To do this, trace the input from its input location to the identified output location</li>
                <li>Some helpful questions to ask include whether the user entering the value is the same as the one receiving the value in the application's outputs</li>
                <li>If not, verify if the current user is allowed to access this value</li>
                <li>Additionally, assess whether the value should be kept secret at all</li>
            </ol>
            """;
        return htmlTemplate.replace("PARAMETER_NAME", this.parameterName).replace("INPUT_LOCATION", this.inputLocation).replace("OUTPUT_LOCATION", this.outputLocation);
    }
}
