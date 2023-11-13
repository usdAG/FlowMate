package audit;

public class LongDistanceMatchAuditFinding extends AuditFinding {

    private String parameterName;
    private String inputLocation;
    private String outputLocation;
    private static final String name = "Long Distance Match";

    public LongDistanceMatchAuditFinding(String parameterName, String inputLocation, String outputLocation) {
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
                            
            FlowMate identified a <b>match over a long distance among 100 HTTP messages</b>.
            Depending on how the application was browsed during the reconnaissance phase, the input location of the parameter as well as the outputs may be in different functions of the application.
            This suggests that the input value is logically decoupled from its actual input location. This heuristic match serves as a reminder not to overlook this specific data flow during manual testing.
                            
            <h1>Details</h1>
                            
            The following data flow has been Identified:
                            
            <ul>
                <li>Parameter (Value): PARAMETER_NAME</li>
                <li>Input Location: INPUT_LOCATION</li>
                <li>Output Location: OUTPUT_LOCATION</li>
            </ul>
            
            <h1>How to Test</h1>
            
            <ol>
                <li>Test the highlighted data flow for potential injection, access control and further vulnerability categories</li>
            </ol>
            """;
        return htmlTemplate.replace("PARAMETER_NAME", this.parameterName).replace("INPUT_LOCATION", this.inputLocation).replace("OUTPUT_LOCATION", this.outputLocation);
    }
}
