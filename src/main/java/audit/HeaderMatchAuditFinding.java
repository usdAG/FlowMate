package audit;

public class HeaderMatchAuditFinding extends AuditFinding {

    private String parameterName;
    private String inputLocation;
    private String outputLocation;
    private static final String name = "Header Match";

    public HeaderMatchAuditFinding(String parameterName, String inputLocation, String outputLocation) {
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
                            
            FlowMate identified a <b>data flow where the input value is reflected in an HTTP-header</b> meaning that the application might be vulnerable to HTTP header injection.
            HTTP header injection occurs when user-supplied input, typically from web forms or URL parameters, is improperly incorporated into HTTP response headers by a web application.\s
            This security issue enables the manipulation of response headers, potentially leading to severe vulnerabilities.
            HTTP header injection may lead to attacks like HTTP Response Splitting, Cross-Site Scripting (XSS), Web Cache Poisoning and more.\s
                            
            <h1>Details</h1>
                            
            The following data flow has been Identified:
                            
            <ul>
                <li>Parameter (Value): PARAMETER_NAME</li>
                <li>Input Location: INPUT_LOCATION</li>
                <li>Output Location: OUTPUT_LOCATION</li>
            </ul>
                            
            <h1>How to Test</h1>
            
            <ol>
                <li>Try to inject newline or other meta characters into the affected parameter</li>
                <li>If this lead to a successful change of the corresponding header try to use payloads for special HTTP header related vulnerabilities, such as HTTP Response Splitting, XSS, Web Cache Poisoning etc.</li>
            </ol>
            """;
        return htmlTemplate.replace("PARAMETER_NAME", this.parameterName).replace("INPUT_LOCATION", this.inputLocation).replace("OUTPUT_LOCATION", this.outputLocation);
    }
}
