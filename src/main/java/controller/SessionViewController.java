package controller;

import audit.CrossSessionAudit;
import burp.BurpExtender;
import burp.ContainerConverter;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import db.CypherQueryHandler;
import db.DBModel;
import db.MatchHandler;
import db.ParameterHandler;
import db.entities.InputValue;
import db.entities.ParameterMatch;
import gui.SessionView;
import gui.container.*;
import model.SessionViewModel;
import org.neo4j.ogm.model.Result;
import session.IdentifiedSession;
import session.Session;
import session.SessionHelper;
import session.SessionParameter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;

import static gui.SessionView.*;
import static model.SessionViewModel.sessionCounter;

public class SessionViewController implements ActionListener, ListSelectionListener {

    private MontoyaApi api;
    private SessionView view;
    private static SessionViewModel model;
    private static MatchHandler matchHandler;
    private ParameterHandler parameterHandler;
    private ContainerConverter containerConverter;
    private CrossSessionAudit crossSessionAudit;

    public SessionViewController(MontoyaApi api, SessionView view, SessionViewModel sessionViewModel, MatchHandler mHandler, ParameterHandler pHandler, CrossSessionAudit crossSessionAudit) {
        this.api = api;
        this.view = view;
        model = sessionViewModel;
        matchHandler = mHandler;
        this.parameterHandler = pHandler;
        this.containerConverter = new ContainerConverter(api, matchHandler);
        this.crossSessionAudit = crossSessionAudit;
        registerEventHandler();
    }

    private void registerEventHandler() {
        view.removeFromSessionDefButton.addActionListener(this);
        view.saveSessionDefinitionButton.addActionListener(this);
        view.changeSessionNameButton.addActionListener(this);
        sessionJList.addListSelectionListener(this);
        sessionSpecificParameterJList.addListSelectionListener(this);
        matchJList.addListSelectionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(view.removeFromSessionDefButton)) {
            DefaultListModel<SessionDefContainer> model = (DefaultListModel<SessionDefContainer>) sessionDefJList.getModel();
            model.removeElement(sessionDefJList.getSelectedValue());
        } else if (actionEvent.getSource().equals(view.saveSessionDefinitionButton)) {
            // Check if Sessions have been defined previously, if yes delete everything related to them
            if (sessionCounter != 0) {
                sessionCounter = 0;
                ((DefaultListModel<SessionContainer>) sessionJList.getModel()).clear();
                model.inputValuesFromSessionDefList.clear();
                model.parameterValuesAndNames.clear();
                model.helpers.clear();
            }
            sessionCounter++;
            getRelevantInformationForSessionDefinition();
            List<IdentifiedSession> sessions = model.identifyExistingSessions();
            createSessionParametersFromIdentified(sessions);
        } else if (actionEvent.getSource().equals(view.changeSessionNameButton)) {
            String newName = view.sessionNameTextField.getText();
            // If the selected session that's name get changed is the last session of the list, change activeSessionName
            // in Model, ParameterHandler and MatchHandler
            if (model.getSelectedSession().getName().equals(model.getActiveSessionName())) {
                model.setActiveSessionName(newName);
                parameterHandler.setSessionName(newName);
                matchHandler.setSessionName(newName);
                model.helpers.clear();
            }
            crossSessionAudit.sessionRename(model.getSelectedSession().getName(), newName);
            changeSessionName(newName);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        if (!listSelectionEvent.getValueIsAdjusting()) {
            if (listSelectionEvent.getSource().equals(sessionJList)) {
                var sessionContainer = (SessionContainer) sessionJList.getSelectedValue();
                if (sessionContainer != null) {
                    var sessionName = sessionContainer.getName();
                    view.sessionNameTextField.setText(sessionName);
                    Session selected = SessionViewModel.sessionTable.get(sessionName);
                    model.setSelectedSession(sessionContainer);
                    model.setSelectedSessionIndex(sessionJList.getSelectedIndex());
                    addToParamMonitorContainer(selected);
                    setSessionSpecificParameters(sessionName);
                    view.clearMatchLists();
                }
            } else if (listSelectionEvent.getSource().equals(SessionView.sessionSpecificParameterJList)) {
                var paramContainer = (SessionParameterContainer) SessionView.sessionSpecificParameterJList.getSelectedValue();
                if (paramContainer != null) {
                    var paramName = paramContainer.getName();
                    var type = paramContainer.getType();
                    var value = paramContainer.getValue();
                    var session = model.getSelectedSession().getName();
                    view.clearMatchLists();
                    renderMatchEntries(type, value, session);
                    setCypherQueryTest(paramName);
                }
            } else if (listSelectionEvent.getSource().equals(matchJList)) {
                var parameterMatchContainer = (ParameterMatchContainer) matchJList.getSelectedValue();
                if (parameterMatchContainer != null) {
                    var value = parameterMatchContainer.getValue();
                    var url = parameterMatchContainer.getUrl();
                    var session = model.getSelectedSession().getName();
                    renderMatchInfo(value, url, session);
                }
            }
        }
    }

    public void addToSessionDefList(SessionDefContainer container) {
        DefaultListModel<SessionDefContainer> model = (DefaultListModel<SessionDefContainer>) sessionDefJList.getModel();
        model.addElement(container);
    }

    public void addToParamMonitorContainer(Session selected) {
        DefaultListModel<SessionParamMonitorContainer> model = (DefaultListModel<SessionParamMonitorContainer>) view.sessionParamMonitorJList.getModel();
        if (!model.isEmpty()) {
            model.clear();
        }
        for (SessionParameter param : selected.getSessionParameter()) {
            if (selected.isUnauthenticatedSession()) {
                continue;
            }
            model.addElement(new SessionParamMonitorContainer(param.getName(), param.getValue(), param.getNewOccurrenceId(), param.getChangedTo()));
        }
    }

    public static List<String> getParamNamesFromSessionDefList() {
        DefaultListModel<SessionDefContainer> listModel = (DefaultListModel<SessionDefContainer>) sessionDefJList.getModel();
        List<String> parameterNames = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            parameterNames.add(listModel.get(i).getName());
        }
        return parameterNames;
    }

    private Hashtable<String, String> getParameterNamesAndTypesFromSessionDefList() {
        DefaultListModel<SessionDefContainer> listModel = (DefaultListModel<SessionDefContainer>) sessionDefJList.getModel();
        Hashtable<String, String> parameterNamesAndTypes = new Hashtable<>();
        for (int i = 0; i < listModel.size(); i++) {
            parameterNamesAndTypes.put(listModel.get(i).getName(),listModel.get(i).getType());
        }
        return parameterNamesAndTypes;
    }

    public void createSessionParametersFromIdentified(List<IdentifiedSession> identifiedSessions) {
        sessionCounter = 0;
        int tempHighestId = 0;
        for (int i = 0; i < identifiedSessions.size(); i++) {
            List<SessionHelper> sessionHelpers = new ArrayList<>(identifiedSessions.get(i).sessionHelperMap.values());
            Hashtable<String, String> types = getParameterNamesAndTypesFromSessionDefList();
            sessionCounter++;
            List<SessionParameter> sessionParameters = new ArrayList<>();
            for (int j = 0; j < sessionHelpers.size(); j++) {
                String paramName = sessionHelpers.get(j).getParamName();
                String paramValue = sessionHelpers.get(j).getParamValue();
                String type = types.get(paramName);
                String nextParamValue = identifyNextParamValue(identifiedSessions, paramName, paramValue, sessionCounter);
                String newOccurrenceId = identifyNewOccurrenceId(identifiedSessions, paramName, paramValue, sessionCounter);
                SessionParameter sessionParameter = new SessionParameter(paramName, paramValue, type, nextParamValue, newOccurrenceId);
                sessionParameters.add(sessionParameter);
            }
            int lowestId;
            int highestId;
            try {
                Collections.sort(sessionParameters, new Comparator<SessionParameter>() {
                    @Override
                    public int compare(SessionParameter p1, SessionParameter t1) {
                        int letter = Character.compare(p1.getNewOccurrenceId().toCharArray()[0], t1.getNewOccurrenceId().toCharArray()[0]);
                        if (letter != 0) {
                            return letter;
                        }
                        return Long.compare(Long.parseLong(p1.getNewOccurrenceId()), Long.parseLong(t1.getNewOccurrenceId()));
                    }
                });
                highestId = Integer.parseInt(sessionParameters.stream().toList().get(0).getNewOccurrenceId());
                if (tempHighestId != 0) {
                    lowestId = tempHighestId + 1;
                } else {
                    lowestId = sessionHelpers.stream().sorted(Comparator.comparing(SessionHelper::getLowestId)).toList().get(0).getLowestId();
                }
                tempHighestId = highestId;
            } catch (Exception e) {
                lowestId = tempHighestId + 1;
                highestId = sessionHelpers.stream().sorted(Comparator.comparing(SessionHelper::getHighestId).reversed()).toList().get(0).getHighestId();
            }
            int realLowestId = sessionHelpers.stream().sorted(Comparator.comparing(SessionHelper::getLowestId)).toList().get(0).getLowestId();
            if (identifiedSessions.size() == 1 && realLowestId > 1) {
                createUnauthSession(lowestId, realLowestId, sessionParameters);
            }
            if (sessionCounter == 1 && lowestId > 1) {
                createUnauthSession(lowestId, highestId, sessionParameters);
                continue;
            }
            String sessionName = "Session_%s".formatted(sessionCounter);
            Session newSession = new Session(sessionName, lowestId, highestId, sessionParameters);
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            DefaultListModel<SessionContainer> listModel = (DefaultListModel<SessionContainer>) sessionJList.getModel();
            listModel.addElement(new SessionContainer(sessionName, timeStamp, lowestId, highestId));
            model.updateSessionInformation(newSession, sessionName, sessionParameters);
            model.changeDatabaseEntriesAccordingToSession(sessionName, lowestId, highestId);
            this.identifyAuditFindings(sessionName);
        }
    }

    private String identifyNextParamValue(List<IdentifiedSession> identifiedSessions, String paramName, String paramValue, int counter) {
        var iterator = identifiedSessions.iterator();
        for (int i = 0; i < counter; i++) {
            iterator.next();
        }
        if (iterator.hasNext()) {
            Collection<SessionHelper> nextOne = iterator.next().sessionHelperMap.values();
            for (SessionHelper helper : nextOne) {
                if (paramName.equals(helper.getParamName()) && !paramValue.equals(helper.getParamValue())) {
                    return helper.getParamValue();
                }
            }
        }
        return "No new Value";
    }

    private String identifyNewOccurrenceId(List<IdentifiedSession> identifiedSessions, String paramName, String paramValue, int counter) {
        var iterator = identifiedSessions.iterator();
        for (int i = 0; i < counter; i++) {
            iterator.next();
        }
        if (iterator.hasNext()) {
            Collection<SessionHelper> nextOne = iterator.next().sessionHelperMap.values();
            for (SessionHelper helper : nextOne) {
                if (paramName.equals(helper.getParamName()) && !paramValue.equals(helper.getParamValue())) {
                    if (helper.getLowestId() == 0 || helper.getLowestId() == 1) {
                        return String.valueOf(nextOne.stream().sorted(Comparator.comparing(SessionHelper::getLowestId)).filter(e -> e.getLowestId() != 0 && e.getLowestId() != 1).findFirst().get().getLowestId());
                    } else {
                        return String.valueOf(helper.getLowestId());
                    }
                }
            }
        }
        return "No new Value";
    }

    public void updateIdForLastSession(int id) {
        ListModel<SessionContainer> sessions = sessionJList.getModel();
        sessions.getElementAt(sessions.getSize()-1).updateRange(id);
        view.sessionsPanel.revalidate();
        view.sessionsPanel.repaint();
    }

    private void setCypherQueryTest(String paramName) {
        String query = "MATCH (p1:InputParameter {name: \"%s\"})\n".formatted(paramName) +
                "OPTIONAL MATCH (p2:InputParameter {name: \"%s\"})-[OCCURS_WITH_VALUE]->".formatted(paramName) +
                "(o:InputValue)\nOPTIONAL MATCH (u1:Url)-[FOUND_PARAMETER]->" +
                "(p3:InputParameter {name: \"%s\"})\nOPTIONAL MATCH (u2:Url)-[FOUND]->".formatted(paramName) +
                "(m:ParameterMatch {name: \"%s\"})-[MATCH]->(e:MatchValue {name: \"%s\"})\n".formatted(paramName, paramName) +
                "RETURN p1,p2,o,u1,p3,u2,m,e";
        view.cypherQueryField.setText(query);
        view.queryPanel.revalidate();
        view.queryPanel.repaint();
    }

    private void renderMatchEntries(String paramType, String paramValue, String session) {
        // ParameterMatches
        SessionView.matchJList.setListData(model.containerConverter.matchOccurrenceToContainer(matchHandler.parameterMatchStorage.values()
                .stream().filter(e -> e.getType().equals(paramType) && e.getValue().equals(paramValue) && e.getSession().equals(session)).toList()));
    }

    private void renderMatchInfo(String value, String url, String session) {
        // MatchValues
        matchInfoJList.setListData(model.containerConverter.entryOccurrenceToContainer(matchHandler.matchValueStorage.values()
                .stream().filter(e -> e.getUrl().equals(url) && e.getValue().equals(value) && e.getSession().equals(session)).toList()));
        parameterListPanel.revalidate();
        parameterListPanel.repaint();
    }

    public static void updateParameterListInActiveSession() {
        // Update only if the last Session in the list is selected
        if (model.getSelectedSessionIndex() + 1 == sessionJList.getModel().getSize()) {
            // get current list of parameters in sessions
            ListModel<SessionParameterContainer> parameterList = sessionSpecificParameterJList.getModel();
            Vector<SessionParameterContainer> parameterContainerVector = new Vector<>(
                    model.containerConverter.parameterToContainerSessionDef(matchHandler.parameterMatchStorage.values().stream().distinct().toList(), model.getActiveSessionName(), getParamNamesFromSessionDefList()));

            for (int i = 0; i < parameterList.getSize(); i++) {
                int x = i;
                if (parameterContainerVector.stream().noneMatch(e -> e.getName().equals(parameterList.getElementAt(x).getName()))) {
                    parameterContainerVector.add(parameterList.getElementAt(i));
                }
            }
            Vector<SessionParameterContainer> vectorSorted = new Vector<>(parameterContainerVector.stream()
                    .sorted(Comparator.comparing(SessionParameterContainer::getName)).toList());
            sessionSpecificParameterJList.setListData(vectorSorted);

            int nMatches = 0;
            for (SessionParameterContainer container: parameterContainerVector) {
                nMatches += container.getMatches();
            }

            SessionView.identMatchesNumber.setText(String.valueOf(nMatches));
            SessionView.identStatisticPanel.revalidate();
            SessionView.identStatisticPanel.repaint();
            parameterListPanel.revalidate();
            parameterListPanel.repaint();
        }
    }

    public static void createSessionFromMonitor(List<ParsedHttpParameter> parameters, int messageId) {
        var sessionsList = SessionViewModel.sessionTable.values().stream().sorted(Comparator.comparing(Session::getLowestHistoryId)).toList();
        Session lastSession = sessionsList.get(sessionsList.size() - 1);
        var lastSessionParameter = lastSession.getSessionParameter();
        Set<SessionParameter> changedParameters = new HashSet<>();
        List<SessionParameter> newParameters = new ArrayList<>();
        SessionParameter changedParam = null;
        for (ParsedHttpParameter newParam : parameters) {
            for (SessionParameter param : lastSessionParameter) {
                if (param.getName().equals(newParam.name())) {
                    param.setChangedTo(newParam.value());
                    changedParam = new SessionParameter(newParam.name(), param.getValue(), param.getType(),
                            newParam.value(), String.valueOf(messageId));
                    changedParameters.add(changedParam);
                }
            }
        }
        lastSession.setSessionParameter(changedParameters.stream().toList());
        for (ParsedHttpParameter param : parameters) {
            newParameters.add(new SessionParameter(param.name(), param.value(), param.type().name(), "No new Value", "No new Value"));
        }
        sessionCounter++;
        Session newSession = new Session("Session_%s".formatted(sessionCounter), messageId, messageId, newParameters);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        DefaultListModel<SessionContainer> listModel = (DefaultListModel<SessionContainer>) sessionJList.getModel();
        listModel.get(sessionCounter - 2).updateRange(messageId);
        listModel.addElement(new SessionContainer("Session_%s".formatted(sessionCounter), timeStamp, messageId, messageId));
        sessionJList.revalidate();
        sessionJList.repaint();
        model.updateSessionInformation(newSession, "Session_%s".formatted(sessionCounter), newParameters);
    }

    public void setSessionSpecificParameters(String sessionName) {
        Vector<SessionParameterContainer> parameterContainerList = containerConverter.parameterToContainerSessionDef(getSessionSpecificParameters(sessionName),
                sessionName, getParamNamesFromSessionDefList());
        DefaultListModel<SessionParameterContainer> listModel = (DefaultListModel<SessionParameterContainer>) sessionSpecificParameterJList.getModel();
        listModel.clear();
        listModel.addAll(parameterContainerList);

        int nMatches = 0;
        for (SessionParameterContainer container: parameterContainerList) {
            nMatches += container.getMatches();
        }

        identMatchesNumber.setText(String.valueOf(nMatches));
        parameterListPanel.revalidate();
        parameterListPanel.repaint();
    }

    public List<ParameterMatch> getSessionSpecificParameters(String sessionName) {
        List<ParameterMatch> parameters = new ArrayList<>();
        Map<String, String> values = Collections.singletonMap("sessionName", sessionName);
        String query = "MATCH (m:ParameterMatch {session: $sessionName}) RETURN m";
        Result queryResult = DBModel.query(query, values);
        Iterator<Map<String, Object>> resultIterator = queryResult.queryResults().iterator();
        while (resultIterator.hasNext()) {
            Map<?, ?> result = resultIterator.next();
            parameters.add((ParameterMatch) result.get("m"));
        }
        return parameters;
    }

    public void getRelevantInformationForSessionDefinition() {
        List<String> parameterNames = getParamNamesFromSessionDefList();
        int historySize = api.proxy().history().size();
        List<ProxyHttpRequestResponse> history = api.proxy().history().subList(BurpExtender.historyStart, historySize);
        for (String paramName: parameterNames) {
            int id = 0;
            for (int i = 0; i < history.size(); i++) {
                List<ParsedHttpParameter> params = history.get(i).finalRequest().parameters();
                for (ParsedHttpParameter parameter : params) {
                    if (parameter.name().equals(paramName)) {
                        id = i;
                        if (history.size() - id == 1) {
                            id++;
                        }
                        model.inputValuesFromSessionDefList.add(new InputValue(parameter.value(),"url", "type", String.valueOf(id + BurpExtender.historyStart)));
                        if (!model.parameterValuesAndNames.containsKey(parameter.value())) {
                            model.parameterValuesAndNames.put(parameter.value(), paramName);
                        }
                    }
                }
            }
        }
    }
    public void changeSessionName(String newName) {
        SessionContainer selectedSession = model.getSelectedSession();
        Session tempSession = SessionViewModel.sessionTable.get(model.getSelectedSession().getName());
        SessionViewModel.sessionTable.remove(selectedSession.getName());
        SessionViewModel.sessionTable.put(newName, tempSession);
        DefaultListModel<SessionContainer> listModel = (DefaultListModel<SessionContainer>) sessionJList.getModel();
        listModel.removeElement(selectedSession);
        listModel.add(model.getSelectedSessionIndex(), new SessionContainer(
                newName, selectedSession.getCreatedAt(),selectedSession.getLowId(), selectedSession.getHighId()));
        model.changeDatabaseEntriesAccordingToSession(newName, selectedSession.getLowId(), selectedSession.getHighId());
    }

    private void createUnauthSession(int lowestId, int highestId, List<SessionParameter> sessionParameters) {
        Session unauthSession = new Session("Unauthenticated", 1, highestId, sessionParameters, true);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        DefaultListModel<SessionContainer> listModel = (DefaultListModel<SessionContainer>) sessionJList.getModel();
        listModel.addElement(new SessionContainer("Unauthenticated", timeStamp, lowestId, highestId));
        model.updateSessionInformation(unauthSession, "Unauthenticated", sessionParameters);
        model.changeDatabaseEntriesAccordingToSession("Unauthenticated", lowestId, highestId);
    }

    private void identifyAuditFindings(String sessionName) {
        List<ParameterMatch> sessionSpecificParams = getSessionSpecificParameters(sessionName);
        for (ParameterMatch match : sessionSpecificParams) {
            Map<String, String> values = Map.of("name", match.getName(), "type", match.getType(), "value", match.getValue());
            String valueQuery = "MATCH (p:InputParameter {name: $name, type: $type})-[OCCURS_WITH_VALUE]-(m:InputValue {type: $type, value: $value}) RETURN m";
            Result result = DBModel.query(valueQuery, values);
            for (var parameterValue : CypherQueryHandler.getOccurrencesFromQueryResult(result).stream().distinct().toList()) {
                String sessionEntered = parameterValue.getSession();
                List<String> sessionDefParameterNames = getParamNamesFromSessionDefList();
                // Do not show Parameters defined as Session
                if (sessionDefParameterNames.stream().anyMatch(s -> s.equals(match.getName())))
                    continue;
                // Only Add Findings where Sessions are different
                if (!sessionEntered.equals(sessionName))
                    crossSessionAudit.identifyAudits(match.getName(), sessionEntered, sessionName);
            }
        }
        crossSessionAudit.renderFindings();
    }

    public void clearDataAndView() {
        model.clearAllData();
        view.clearMatchLists();
        view.cypherQueryField.setText("");
        view.sessionNameTextField.setText("");
        DefaultListModel<SessionDefContainer> listModel1 = (DefaultListModel<SessionDefContainer>) sessionDefJList.getModel();
        listModel1.clear();
        DefaultListModel<SessionContainer> listModel2 = (DefaultListModel<SessionContainer>)  sessionJList.getModel();
        listModel2.clear();
        DefaultListModel<SessionParamMonitorContainer> listModel3 = (DefaultListModel<SessionParamMonitorContainer>) view.sessionParamMonitorJList.getModel();
        listModel3.clear();
        DefaultListModel<SessionParameterContainer> listModel4 = (DefaultListModel<SessionParameterContainer>) sessionSpecificParameterJList.getModel();
        listModel4.clear();
        view.revalidate();
        view.repaint();
    }
}
