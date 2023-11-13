package audit;

import burp.api.montoya.MontoyaApi;
import db.MatchHandler;
import db.MatchHelperClass;
import db.entities.InputValue;
import db.entities.MatchValue;
import db.entities.Url;
import gui.AuditFindingView;
import utils.MessageHashToProxyId;

import java.util.HashMap;
import java.util.Objects;

public class LongDistanceMatchAudit {

    private AuditFindingView auditFindingView;
    private MontoyaApi api;
    private HashMap<Integer, String> tempMatchStorage;

    public LongDistanceMatchAudit(AuditFindingView auditFindingView, MontoyaApi api) {
        this.auditFindingView = auditFindingView;
        this.api = api;
        this.tempMatchStorage = new HashMap<>();
    }

    public void performAudit(MatchHelperClass match, MatchValue matchValue, MatchHandler matchHandler) {
        // Check if it is the first time the match appears
        Url matchingUrlEntity = matchHandler.getMatchingUrlEntity(match.getUrl());
        if (this.tempMatchStorage.containsKey(Objects.hash(match.getName()))) {
            return;
        }

        this.tempMatchStorage.put(Objects.hash(match.getName()), matchValue.getValue());

        if (matchHandler.matchEntityExistsInUrlEntity(matchingUrlEntity, match.getName(), match.getValue()))
            return;

        MessageHashToProxyId messageHashToProxyId = MessageHashToProxyId.getInstance(this.api);
        InputValue inputValue = match.getInputValueObj();
        String inputMessageHash = inputValue.getMessageHash();
        String matchMessageHash = match.getMessageHash();
        int inputId = messageHashToProxyId.calculateId(inputMessageHash);
        int matchId = messageHashToProxyId.calculateId(matchMessageHash);
        if (matchId - inputId >= 100) {
            String paramName = match.getName();
            String inputLocation = inputValue.getUrl();
            String outputLocation = match.getUrl();
            AuditFinding auditFinding = buildAuditFinding(paramName, inputLocation, outputLocation);
            this.auditFindingView.addFinding(auditFinding);
            int a = 0;
        }
    }

    private AuditFinding buildAuditFinding(String paramName, String inputLocation, String outputLocation) {
        return new LongDistanceMatchAuditFinding(paramName, inputLocation, outputLocation);
    }
}
