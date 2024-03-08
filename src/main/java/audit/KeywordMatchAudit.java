package audit;

import db.MatchHelperClass;
import gui.AuditFindingView;
import utils.StringUtils;

import java.util.Arrays;
import java.util.List;

public class KeywordMatchAudit {

    private AuditFindingView auditFindingView;

    private final List<String> PASSWORD_PARAM_NAMES = Arrays.asList("password", "passwd", "secret", "pwd");

    public KeywordMatchAudit(AuditFindingView auditFindingView) {
        this.auditFindingView = auditFindingView;
    }

    public void performAudit(MatchHelperClass match) {
        String paramName = match.getName();
        if (!StringUtils.containsListIgnoreCase(PASSWORD_PARAM_NAMES, paramName)) {
            return;
        }
        String inputLocation = match.getInputValueObj().getUrl();
        String outputLocation = match.getUrl();
        AuditFinding auditFinding = buildAuditFinding(paramName, inputLocation, outputLocation);
        this.auditFindingView.addFinding(auditFinding);
    }

    private AuditFinding buildAuditFinding(String paramName, String inputLocation, String outputLocation) {
        return new KeywordMatchAuditFinding(paramName, inputLocation, outputLocation);
    }
}
