package audit;

public class CrossScopeAuditFinding extends AuditFinding {

    private String parameterName;
    private String inputDomain;
    private String matchDomain;
    private static final String name = "Cross-Scope Match";

    public CrossScopeAuditFinding(String parameterName, String inputDomain, String matchDomain) {
        super(name, AuditFinding.FindingSeverity.MIDDLE);
        this.parameterName = parameterName;
        this.inputDomain = inputDomain;
        this.matchDomain = matchDomain;
    }

    @Override
    public String getShortDescription() {
        return String.format("The parameter %s was entered in domain %s and found in domain %s", this.parameterName, this.inputDomain, this.matchDomain);
    }

    @Override
    public String getLongDescription() {
        String htmlTemplate = """
            <h1>Description</h1>
                            
            FlowMate identified a <b>data flow that crosses scope boundaries</b>, meaning that the output location is in another application or application component (domain) as the input parameter. Different applications often differ in their input and output handling, which increases the probability of related vulnerabilities.
                            
            Examples for vulnerabilities include Cross-Site Scripting (XSS), Server-Side Template Injections (SSTI), Second-Order SQL-Injections and Server-Site Request Forgery (SSRF).
                            
            As an example, consider a parameter that can be set using an API and its value is then displayed in a web application. This might allow bypassing of input filtering that the application might apply.
                            
            <h1>Details</h1>
                            
            The following data flow has been Identified:
                            
            <ul>
                <li>Parameter (Value): PARAMETER_NAME</li>
                <li>Input Location (Scope #1): INPUT_DOMAIN</li>
                <li>Output Location (Scope #2): OUTPUT_DOMAIN</li>
            </ul>
                            
            <h1>How to Test</h1>
            
            <ol>
                <li>Make sure to test the affected data flows for vulnerabilities arising specifically from different applications handling the inserted data</li>
                <li>For instance, if different programming languages or frameworks are used to build these applications, vulnerabilities may emerge due to variations in their input handling and sanitization processes</li>
                <li>This could lead to various types of injection vulnerabilities</li>
            </ol>
            """;
        return htmlTemplate.replace("PARAMETER_NAME", this.parameterName).replace("INPUT_DOMAIN", this.inputDomain).replace("OUTPUT_DOMAIN", this.matchDomain);
    }
}
