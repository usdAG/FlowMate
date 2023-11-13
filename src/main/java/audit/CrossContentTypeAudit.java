package audit;

import db.MatchHelperClass;
import gui.AuditFindingView;

public class CrossContentTypeAudit {

    private AuditFindingView auditFindingView;

    public CrossContentTypeAudit(AuditFindingView auditFindingView) {
        this.auditFindingView = auditFindingView;
    }

    public void performAudit(MatchHelperClass matchHelper) {
        var inputValueType = matchHelper.getInputValueObj().getType();
        var responseContentType = matchHelper.getResponseContentType();

        // Ignore responseContentType HTML and JSON
        if (responseContentType.equals("HTML") || responseContentType.equals("JSON") || responseContentType.equals("CSS") || inputValueType.equals("COOKIE")) {
            return;
        }

        if (!responseContentType.equals(inputValueType)) {
            AuditFinding finding = buildAuditFinding(matchHelper.getName(), inputValueType, responseContentType);
            auditFindingView.addFinding(finding);
        }
    }

    private AuditFinding buildAuditFinding(String paramName, String contentTypeEntered, String contentTypeFound) {
        return new CrossContentTypeAuditFinding(paramName, contentTypeEntered, contentTypeFound);
    }


}
