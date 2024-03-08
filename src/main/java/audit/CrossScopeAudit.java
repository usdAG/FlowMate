package audit;

import db.MatchHelperClass;
import db.entities.InputValue;
import gui.AuditFindingView;
import utils.URLExtension;

public class CrossScopeAudit {

    private AuditFindingView auditFindingView;

    public CrossScopeAudit(AuditFindingView auditFindingView) {
        this.auditFindingView = auditFindingView;
    }

    public void performAudit(MatchHelperClass match) {
        String parameterName = match.getName();
        InputValue inputValue = match.getInputValueObj();
        String inputDomain = URLExtension.stringToUrl(inputValue.getUrl()).getHost();
        String matchDomain = URLExtension.stringToUrl(match.getUrl()).getHost();

        if (!inputDomain.equals(matchDomain)) {
            AuditFinding finding = buildAuditFinding(parameterName, inputDomain, matchDomain);
            auditFindingView.addFinding(finding);
        }
    }

    private AuditFinding buildAuditFinding(String paramName, String inputDomain, String matchDomain) {
        return new CrossScopeAuditFinding(paramName, inputDomain, matchDomain);
    }


}
