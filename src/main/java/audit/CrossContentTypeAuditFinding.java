package audit;

public class CrossContentTypeAuditFinding extends AuditFinding {

    private String parameterName;
    private String contentTypeEntered;
    private String contentTypeFound;
    private static final String name = "Cross-Content-Type Parameter Match";

    public CrossContentTypeAuditFinding(String parameterName, String contentTypeEntered, String contentTypeFound) {
        super(name, AuditFinding.FindingSeverity.MIDDLE);
        this.parameterName = parameterName;
        this.contentTypeEntered = contentTypeEntered;
        this.contentTypeFound = contentTypeFound;
    }


    @Override
    public String getShortDescription() {
        return String.format("The parameter %s was entered with content type %s and found in content type %s", this.parameterName, this.contentTypeEntered, this.contentTypeFound);
    }

    @Override
    public String getLongDescription() {
        String htmlTemplate = """
            <h1>Description</h1>
                            
            FlowMate identified a <b>data flow where the input value is reflected in a different content type</b>. If the data is not properly sanitized or validated, this might allow vulnerabilities exploiting the specific syntax or processing rule for that content type.
                            
            For instance, if input values are contained in an XML-response, an attacker might be able to inject XML tags that manipulate data. Another example are CSV files, where attackers can inject formula expressions that might allow code execution when imported in Microsoft Excel.
                            
            <h1>Details</h1>
                            
            The following data flow has been identified:
                            
            <ul>
                <li>Parameter (Value): PARAMETER_NAME</li>
                <li>Input Location (Content Type): ENTERED_IN</li>
                <li>Output Location (Content Type): FOUND_IN</li>
            </ul>
                            
            <h1>How to Test</h1>
            
            <ol>
                <li>Try to inject meta sequences corresponding to the output file format of the application</li>
                <li>Check if the parser is still able to process the resulting file format, if you break the expected format</li>
                <li>Furthermore, you could use pingback URLs such as the Burp collaborator, to see if your input is processed insecurely (e.g. in case of XXE)</li>
            </ol>
            """;
        return htmlTemplate.replace("PARAMETER_NAME", this.parameterName).replace("ENTERED_IN", this.contentTypeEntered).replace("FOUND_IN", this.contentTypeFound);
    }
}
