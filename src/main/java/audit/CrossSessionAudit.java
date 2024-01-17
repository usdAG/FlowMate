package audit;

import db.MatchHelperClass;
import db.entities.ParameterMatch;
import gui.AuditFindingView;

import java.util.Vector;

public class CrossSessionAudit {
    
    private AuditFindingView auditFindingView;

    public CrossSessionAudit(AuditFindingView auditFindingView){
        this.auditFindingView = auditFindingView;
    }

    public void performAudit(ParameterMatch match, String sessionName, MatchHelperClass matchHelper){
        var inputValueSession = match.getInputValue().getSession();

        if(sessionName == null || sessionName.equals("not set")
           || inputValueSession == null || inputValueSession.equals("not set")
           || inputValueSession.equals(sessionName) ){
            return;
        }

        AuditFinding finding = buildAuditFinding(matchHelper.getInputParameterObj().getName(), inputValueSession, sessionName);
        this.auditFindingView.addFinding(finding);
    }

    public void identifyAudits(String paramName, String sessionEntered, String sessionMatched) {
        AuditFinding finding = buildAuditFinding(paramName, sessionEntered, sessionMatched);
        this.auditFindingView.addFinding(finding);
    }
//
//    public void performAudit(ParameterMatch match, String sessionName, MatchHelperClass matchHelper, Session session){
//        InputValue inputValue = match.getInputValue();
//
//        boolean inputValueContainedInSession = false;
//        for (InputValue value : session.getInputValuesRelatedToSession()) {
//            if (inputValue.getIdentifier() == value.getIdentifier())
//                inputValueContainedInSession = true;
//        }
//
//        if (inputValueContainedInSession) {
//            return;
//        }
//
//        Session sessionRelatedToInputValue = null;
//        Map<String, String> values = Collections.singletonMap("valueIdentifier", String.valueOf(inputValue.getIdentifier()));
//        String query = "MATCH (s:Session)-[INPUT_VALUE_FOUND_IN_SESSION]-(i:InputValue {identifier:$valueIdentifier}) RETURN s";
//        Result queryResult = DBModel.query(query, values);
//        Iterator<Map<String, Object>> resultIterator = queryResult.queryResults().iterator();
//        while (resultIterator.hasNext()) {
//            Map<?, ?> result = resultIterator.next();
//            sessionRelatedToInputValue = (Session) result.get("s");
//        }
//
//        if (sessionRelatedToInputValue != null) {
//            AuditFinding finding = buildAuditFinding(matchHelper.getInputParameterObj().getName(), sessionRelatedToInputValue.getName(), sessionName);
//            this.auditFindingView.addFinding(finding);
//        }
//    }

    private AuditFinding buildAuditFinding(String paramName, String sessionEntered, String sessionMatched){
        return new CrossSessionAuditFinding(paramName, sessionEntered, sessionMatched);
    }

    public void sessionRename(String oldName, String newName){
        var allFindings = this.auditFindingView.getAuditFindings();
        var newFindings = new Vector<AuditFinding>();
        for(var finding : allFindings){
            if(!finding.getClass().getName().equals(CrossSessionAuditFinding.class.getName())){
                //No rename if not CrossSessionAuditFinding
                newFindings.add(finding);
                continue;
            }
            var crossSessionFinding = (CrossSessionAuditFinding)finding;
            crossSessionFinding.renameSession(oldName, newName);
            newFindings.add(crossSessionFinding);
        }
        this.auditFindingView.setAuditFindings(newFindings);
    }
    public void renderFindings() {
        this.auditFindingView.renderFindings();
    }

}
