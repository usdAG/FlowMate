package audit;

import db.MatchHelperClass;
import gui.AuditFindingView;

public class HeaderMatchAudit {

    private AuditFindingView auditFindingView;

    public HeaderMatchAudit(AuditFindingView auditFindingView) {
        this.auditFindingView = auditFindingView;
    }

    public void performAudit(MatchHelperClass match) {
        String matchProof = match.getMatchProof();
        String paramName = match.getName();
        String inputLocation = match.getInputValueObj().getUrl();
        String outputLocation = match.getUrl();
        if (matchProof.startsWith("Header:")) {
            AuditFinding auditFinding = buildAuditFinding(paramName, inputLocation, outputLocation);
            auditFindingView.addFinding(auditFinding);
        }
    }

    private AuditFinding buildAuditFinding(String paramName, String inputLocation, String outputLocation) {
        return new HeaderMatchAuditFinding(paramName, inputLocation, outputLocation);
    }
}
