package model;

import burp.ContainerConverter;
import burp.HttpListener;
import burp.api.montoya.MontoyaApi;
import db.DBModel;
import db.MatchHandler;
import db.ParameterHandler;
import db.entities.*;
import gui.container.SessionContainer;
import org.neo4j.ogm.model.Result;
import session.IdentifiedSession;
import session.Session;
import session.SessionHelper;
import session.SessionParameter;
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
        List<Integer> nodesToChange = getIdentifiersForSessionNodes(lowestHistoryId, highestHistoryId);
        Map<String, String> values = Collections.singletonMap("sessionName", sessionName);
        String query = "UNWIND %s as X ".formatted(Arrays.toString(nodesToChange.toArray())) +
                "MATCH (n {identifier: X}) " +
                "SET n.session = $sessionName " +
                "RETURN n";
        DBModel.executeCypher(query, values);
        changeInStorages(nodesToChange, sessionName);
    }

    private void changeInStorages(List<Integer> nodesToChange, String sessionName) {
        for (InputParameter param : parameterHandler.parameterStorage.values()) {
            for (InputValue value : param.getOccurrenceEntities()) {
                for (Integer id : nodesToChange) {
                    if (value.getIdentifier() == id) {
                        value.setSession(sessionName);
                    }
                }
            }
        }

        for (InputValue value : parameterHandler.parameterValueStorage.values()) {
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

    private List<Integer> getIdentifiersForSessionNodes(int lowestHistoryId, int highestHistoryId) {
        int[] array = new int[(highestHistoryId - lowestHistoryId) + 1];
        int counter = lowestHistoryId;
        for (int i = 0; i < array.length; i++) {
            array[i] = counter++;
        }

        List<String> messageIds = new ArrayList<>();
        var proxyList = api.proxy().history();
        for (int i = lowestHistoryId; i <= highestHistoryId; i++) {
            if (i == proxyList.size()) {
                messageIds.add("\""+ Hashing.sha1(proxyList.get(i-1).finalRequest().toByteArray().getBytes())+"\"");
            } else {
                messageIds.add("\""+Hashing.sha1(proxyList.get(i).finalRequest().toByteArray().getBytes())+"\"");
            }
        }

        String messagesArray = Arrays.toString(messageIds.toArray());
        List<Integer> paramValuesIdentifiers = new ArrayList<>();
        String queryParameterValue = "UNWIND %s as X ".formatted(messagesArray) +
                "Match (n:InputValue {messageHash: X}) " +
                "RETURN n";
        Result queryResultParameterValues = DBModel.query(queryParameterValue, Map.of());
        Iterator<Map<String, Object>> resultIterator = queryResultParameterValues.queryResults().iterator();
        while (resultIterator.hasNext()) {
            Map<?, ?> result = resultIterator.next();
            paramValuesIdentifiers.add(((InputValue) result.get("n")).getIdentifier());
        }

        String queryParameterMatches = "UNWIND %s as X ".formatted(messagesArray) +
                "Match (n:ParameterMatch {messageHash: X}) " +
                "RETURN n";
        Result queryResultParameterMatches = DBModel.query(queryParameterMatches, Map.of());
        Iterator<Map<String, Object>> resultIterator2 = queryResultParameterMatches.queryResults().iterator();
        List<Integer> paramMatchesIdentifiers = new ArrayList<>();
        while (resultIterator2.hasNext()) {
            Map<?, ?> result = resultIterator2.next();
            paramMatchesIdentifiers.add(((ParameterMatch) result.get("n")).getIdentifier());
        }

        String queryMatchValues = "UNWIND %s as X ".formatted(messagesArray) +
                "Match (n:MatchValue {messageHash: X}) " +
                "RETURN n";
        Result queryResultMatchValues = DBModel.query(queryMatchValues, Map.of());
        Iterator<Map<String, Object>> resultIterator3 = queryResultMatchValues.queryResults().iterator();
        List<Integer> matchValuesIdentifiers = new ArrayList<>();
        while (resultIterator3.hasNext()) {
            Map<?, ?> result = resultIterator3.next();
            matchValuesIdentifiers.add(((MatchValue) result.get("n")).getIdentifier());
        }

        List<Integer> allIdentifiers = new ArrayList<>();
        allIdentifiers.addAll(paramValuesIdentifiers);
        allIdentifiers.addAll(paramMatchesIdentifiers);
        allIdentifiers.addAll(matchValuesIdentifiers);

        return allIdentifiers;
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

    public void clearAllData() {
        parameterValuesAndNames.clear();
        helpers.clear();
        inputValuesFromSessionDefList.clear();
        sessionCounter = 0;
        sessionTable.clear();
    }
}
