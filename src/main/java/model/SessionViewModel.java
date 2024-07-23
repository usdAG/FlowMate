package model;

import burp.ContainerConverter;
import burp.HttpListener;
import burp.api.montoya.MontoyaApi;
import db.CypherQueryHandler;
import db.DBModel;
import db.MatchHandler;
import db.ParameterHandler;
import db.entities.*;
import gui.container.SessionContainer;
import org.neo4j.ogm.model.Result;
import session.IdentifiedSession;
import session.SessionHelper;
import utils.Hashing;

import java.util.*;

public class SessionViewModel {

    private MontoyaApi api;
    private static ParameterHandler parameterHandler;
    private static MatchHandler matchHandler;
    public ContainerConverter containerConverter;
    public List<InputValue> inputValuesFromSessionDefList;
    public Hashtable<String, String> parameterValuesAndNames;
    public static Hashtable<String, Session> sessionTable;
    public List<SessionHelper> helpers;
    public SessionContainer selectedSession;
    public int selectedSessionIndex;
    public static int sessionCounter;
    private static String activeSessionName;

    public SessionViewModel(MontoyaApi api, ParameterHandler pHandler, MatchHandler mHandler) {
        this.api = api;
        parameterHandler = pHandler;
        matchHandler = mHandler;
        this.containerConverter = new ContainerConverter(api, matchHandler);
        this.inputValuesFromSessionDefList = new ArrayList<>();
        this.parameterValuesAndNames = new Hashtable<>();
        sessionTable = new Hashtable<>();
        this.helpers = new ArrayList<>();
        sessionCounter = 0;
    }

    public List<IdentifiedSession> identifyExistingSessions() {
        for (var value : this.parameterValuesAndNames.keySet()) {
            String name = this.parameterValuesAndNames.get(value);
            int lowest = identifyLowestIdForValue(value);
            int highest = identifyHighestIdForValue(value);
            helpers.add(new SessionHelper(name, value, lowest, highest));
        }
        return defineSessionFromIdentified(this.helpers);
    }

    private List<IdentifiedSession> defineSessionFromIdentified(List<SessionHelper> sessionHelperList) {
        sessionHelperList.sort(Comparator.comparingInt(param -> param.getLowestId()));
        List<IdentifiedSession> sessions = new ArrayList<>();
        for (SessionHelper helper : sessionHelperList) {
            List<IdentifiedSession> activeSessions = new ArrayList<>();
            for (IdentifiedSession session : sessions) {
                if (session.inRange(helper) && !session.contains(helper)) {
                    session.sessionHelperMap.put(helper.getParamName(), helper);
                    session.updateRange(helper);
                    activeSessions.add(session);
                }
            }

            if (activeSessions.isEmpty()) {
                IdentifiedSession newSession = new IdentifiedSession(helper.getLowestId(), helper.getHighestId());
                newSession.sessionHelperMap.put(helper.getParamName(), helper);
                sessions.add(newSession);
            } else {
                for (IdentifiedSession session : activeSessions) {
                    if (!session.sessionHelperMap.containsKey(helper.getParamName())) {
                        session.sessionHelperMap.put(helper.getParamName(), helper);
                        session.updateRange(helper);
                    }
                }
            }
        }

        for (SessionHelper helper : sessionHelperList) {
            for (IdentifiedSession session : sessions) {
                if (session.inRange(helper) && !session.contains(helper)) {
                    session.sessionHelperMap.put(helper.getParamName(), helper);
                }
            }
        }
        return sessions;
    }
    private int identifyLowestIdForValue(String value) {
        return Integer.parseInt(this.inputValuesFromSessionDefList.stream()
                .filter(parameterValue -> parameterValue.getValue().equals(value))
                .toList().get(0).getMessageHash());

    }

    private int identifyHighestIdForValue(String value) {
        int size = this.inputValuesFromSessionDefList.stream().filter(v -> v.getValue().equals(value)).toList().size();
        return Integer.parseInt(this.inputValuesFromSessionDefList.stream()
                .filter(parameterValue -> parameterValue.getValue().equals(value))
                .toList().get(size - 1).getMessageHash());
    }

    public void changeDatabaseEntriesAccordingToSession(String sessionName, int lowestHistoryId, int highestHistoryId) {
        List<InputValue> inputValues = getInputValuesForSessionRange(sessionName, lowestHistoryId, highestHistoryId);
        List<ParameterMatch> parameterMatches = getParameterMatchesForSessionRange(sessionName, lowestHistoryId, highestHistoryId);
        List<MatchValue> matchValues = getMatchValuesForSessionRange(sessionName, lowestHistoryId, highestHistoryId);

        if (!(inputValues == null) && !(parameterMatches == null) && !(matchValues == null)) {
            List<Integer> nodesToChange = getIdentifiersFromNodes(inputValues, parameterMatches, matchValues);
            Map<String, String> values = Collections.singletonMap("sessionName", sessionName);
            String query = "UNWIND %s as X ".formatted(Arrays.toString(nodesToChange.toArray())) +
                    "MATCH (n {identifier: X}) " +
                    "SET n.session = $sessionName " +
                    "RETURN n";
            DBModel.executeCypher(query, values);
            Session sessionToSave = connectEntitiesToSessionNode(sessionName, inputValues, parameterMatches, matchValues);
            DBModel.saveEntity(sessionToSave);
            changeInStorages(nodesToChange, sessionName);
        }
    }

    public void renameSession(String oldName, String newName) {
        Session session = sessionTable.get(oldName);
        sessionTable.remove(oldName);
        session.setName(newName);
        sessionTable.put(newName, session);
        DBModel.saveEntity(session);
    }

    private void changeInStorages(List<Integer> nodesToChange, String sessionName) {
        for (InputParameter param : parameterHandler.inputParameterStorage.values()) {
            for (InputValue value : param.getOccurrenceEntities()) {
                for (Integer id : nodesToChange) {
                    if (value.getIdentifier() == id) {
                        value.setSession(sessionName);
                    }
                }
            }
        }

        for (InputValue value : parameterHandler.inputValueStorage.values()) {
            for (Integer id : nodesToChange) {
                if (value.getIdentifier() == id) {
                    value.setSession(sessionName);
                }
            }
        }

        for (Url url : parameterHandler.urlStorage.values()) {
            for (InputParameter param : url.getFoundInParameterList()) {
                for (InputValue value : param.getOccurrenceEntities()) {
                    for (Integer id : nodesToChange) {
                        if (value.getIdentifier() == id) {
                            value.setSession(sessionName);
                        }
                    }
                }
            }
        }

        for (ParameterMatch match : matchHandler.parameterMatchStorage.values()) {
            for (Integer id : nodesToChange) {
                if (match.getIdentifier() == id) {
                    match.setSession(sessionName);
                }
            }
        }

        for (MatchValue matchValue : matchHandler.matchValueStorage.values()) {
            for (Integer id : nodesToChange) {
                if (matchValue.getIdentifier() == id) {
                    matchValue.setSession(sessionName);
                }
            }
        }
    }

    private List<InputValue> getInputValuesForSessionRange(String sessionName, int lowestHistoryId, int highestHistoryId) {
        String messageHashesArray = identifyMessageHashes(lowestHistoryId, highestHistoryId);
        if (messageHashesArray.isEmpty()) {
            return null;
        }
        String query = "UNWIND %s as X ".formatted(messageHashesArray) +
                "Match (o:InputValue {messageHash: X}) " +
                "RETURN o";
        Result queryResultParameterValues = DBModel.query(query, Map.of());

        List<InputValue> inputValues = CypherQueryHandler.getOccurrencesFromQueryResult(queryResultParameterValues);
        for (InputValue inputValue : inputValues) {
            inputValue.setSession(sessionName);
        }

        return inputValues;
    }

    private List<ParameterMatch> getParameterMatchesForSessionRange(String sessionName, int lowestHistoryId, int highestHistoryId) {
        String messageHashesArray = identifyMessageHashes(lowestHistoryId, highestHistoryId);
        if (messageHashesArray.isEmpty()) {
            return null;
        }
        String query = "UNWIND %s as X ".formatted(messageHashesArray) +
                "Match (m:ParameterMatch {messageHash: X}) " +
                "RETURN m";
        Result queryResultParameterMatches = DBModel.query(query, Map.of());

        List<ParameterMatch> parameterMatches = CypherQueryHandler.getParameterMatchesFromQueryResult(queryResultParameterMatches);
        for (ParameterMatch parameterMatch : parameterMatches) {
            parameterMatch.setSession(sessionName);
        }

        return parameterMatches;
    }

    private List<MatchValue> getMatchValuesForSessionRange(String sessionName, int lowestHistoryId, int highestHistoryId) {
        String messageHashesArray = identifyMessageHashes(lowestHistoryId, highestHistoryId);
        if (messageHashesArray.isEmpty()) {
            return null;
        }
        String query = "UNWIND %s as X ".formatted(messageHashesArray) +
                "Match (mv:MatchValue {messageHash: X}) " +
                "RETURN mv";
        Result queryResultMatchValues = DBModel.query(query, Map.of());

        List<MatchValue> matchValues = CypherQueryHandler.getMatchValuesFromQueryResult(queryResultMatchValues);
        for (MatchValue matchValue : matchValues) {
            matchValue.setSession(sessionName);
        }

        return matchValues;
    }

    private List<Integer> getIdentifiersFromNodes(List<InputValue> inputValues, List<ParameterMatch> parameterMatches, List<MatchValue> matchValues) {
        List<Integer> returnList = new ArrayList<>();

        returnList.addAll(inputValues.stream().map(InputValue::getIdentifier).toList());
        returnList.addAll(parameterMatches.stream().map(ParameterMatch::getIdentifier).toList());
        returnList.addAll(matchValues.stream().map(MatchValue::getIdentifier).toList());

        return returnList;
    }

    private Session connectEntitiesToSessionNode(String sessionName, List<InputValue> inputValues, List<ParameterMatch> parameterMatches, List<MatchValue> matchValues) {
        Session session = sessionTable.get(sessionName);
        session.setInputValuesRelatedToSession(inputValues);
        session.setParameterMatchesRelatedToSession(parameterMatches);
        session.setMatchValuesRelatedToSession(matchValues);
        sessionTable.put(sessionName, session);
        return session;
    }

    private String identifyMessageHashes(int lowestHistoryId, int highestHistoryId) {
        List<String> messageIds = new ArrayList<>();
        var proxyList = api.proxy().history();
        if (!(proxyList.size() < lowestHistoryId)) {
            for (int i = lowestHistoryId -1; i < highestHistoryId; i++) {
                if (i == proxyList.size()) {
                    messageIds.add("\"" + Hashing.sha1(proxyList.get(i - 1).finalRequest().toByteArray().getBytes()) + "\"");
                } else {
                    messageIds.add("\"" + Hashing.sha1(proxyList.get(i).finalRequest().toByteArray().getBytes()) + "\"");
                }
            }
            return Arrays.toString(messageIds.toArray());
        }

       return "";
    }

    public void updateSessionInformation(Session newSession, String sessionName, List<SessionParameter> newParameters) {
        sessionTable.put(sessionName, newSession);
        HttpListener.setMonitoredParameter(newParameters);
        HttpListener.hasActiveSession = true;
        parameterHandler.setSessionName(sessionName);
        parameterHandler.setHasActiveSession(true);
        matchHandler.setSessionName(sessionName);
        matchHandler.setHasActiveSession(true);
        activeSessionName = sessionName;
    }

    public String getActiveSessionName() {
        return activeSessionName;
    }

    public void setActiveSessionName(String sessionName) {
        activeSessionName = sessionName;
    }

    public SessionContainer getSelectedSession() {
        return selectedSession;
    }

    public void setSelectedSession(SessionContainer selectedSession) {
        this.selectedSession = selectedSession;
    }

    public int getSelectedSessionIndex() {
        return this.selectedSessionIndex;
    }

    public void setSelectedSessionIndex(int index) {
        this.selectedSessionIndex = index;
    }

    public static void setSessionCounter(int sessionCounter) {
        SessionViewModel.sessionCounter = sessionCounter;
    }

    public void clearAllData() {
        parameterValuesAndNames.clear();
        helpers.clear();
        inputValuesFromSessionDefList.clear();
        sessionCounter = 0;
        activeSessionName = "not set";
        sessionTable.clear();
    }

    public void deleteSessionsInDB() {
        String query1 = "Match (n:Session) detach delete n";
        String query2 = "Match (n:SessionParameter) detach delete n";
        DBModel.executeCypher(query1, Map.of());
        DBModel.executeCypher(query2, Map.of());
    }

    public static void deleteMatchesFromSession() {
        var keys = sessionTable.keys().asIterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Session session = sessionTable.get(key);
            session.setParameterMatchesRelatedToSession(new ArrayList<>());
            session.setMatchValuesRelatedToSession(new ArrayList<>());
            SessionViewModel.sessionTable.put(key, session);
        }
    }
}
