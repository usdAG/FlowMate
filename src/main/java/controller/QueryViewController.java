package controller;

import burp.ContainerConverter;
import burp.RegexMatcher;
import burp.api.montoya.MontoyaApi;
import db.DBModel;
import db.MatchHandler;
import db.ParameterHandler;
import db.entities.InputParameter;
import db.entities.MatchValue;
import db.entities.ParameterMatch;
import events.RuleContainerEvent;
import events.RuleContainerListener;
import gui.QueryView;
import gui.container.*;
import javafx.collections.ListChangeListener;
import model.QueryViewModel;
import utils.MessageHashToProxyId;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueryViewController implements ActionListener, ListSelectionListener, RuleContainerListener {

    private MontoyaApi api;
    private QueryView view;
    private QueryViewModel model;
    private ParameterHandler parameterHandler;
    private MatchHandler matchHandler;
    private SessionViewController sessionViewController;
    private ContainerConverter containerConverter;
    private boolean hideParamsExcludedByNoiseReduction;
    private List<ParameterContainer> excludedParams;

    public QueryViewController(MontoyaApi api, QueryView view, QueryViewModel model, ParameterHandler pHandler, MatchHandler maHandler, SessionViewController sessionViewController) {
        this.api = api;
        this.view = view;
        this.model = model;
        this.parameterHandler = pHandler;
        this.matchHandler = maHandler;
        this.sessionViewController = sessionViewController;
        this.containerConverter = new ContainerConverter(api, this.matchHandler);
        this.hideParamsExcludedByNoiseReduction = false;
        this.excludedParams = new ArrayList<>();
        registerEventHandler();
    }

    private void registerEventHandler() {
        view.searchField.addTextListener(new SearchFieldTextListener());
        view.sortByLabel.addActionListener(this);
        view.filterPicker.addActionListener(this);
        view.hideExcludedParamsCheckBox.addActionListener(this);
        view.parameterValueJList.addListSelectionListener(this);
        view.parameterMatchJList.addListSelectionListener(this);
        view.parameterJList.addListSelectionListener(this);

        // Add FocusListener so that all text gets selected on click
        view.cypherQueryField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                view.cypherQueryField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        });

        view.parameterJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                view.parameterJList.setSelectedIndex(view.parameterJList.locationToIndex(e.getPoint()));
                view.menu.show(view.parameterJList, e.getX(), e.getY());
            }
            }

        });

        view.sendToSessionDef.addActionListener(this);

        // Listener for InputParameter and Matchlist. If an item gets added to any of these list, the parameterlist needs
        // to be updated
        this.parameterHandler.observableInputParameterList.addListener(new ListChangeListener<InputParameter>() {
            @Override
            public void onChanged(Change<? extends InputParameter> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        updateParameters();
                    }
                }
            }
        });

        this.matchHandler.observableParameterMatchList.addListener(new ListChangeListener<ParameterMatch>() {
            @Override
            public void onChanged(Change<? extends ParameterMatch> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        updateParameters();
                    }
                }
            }
        });
    }

    // ActionListener for sort-label, search-field and right-click menu
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(view.sortByLabel)) { // Change text on click for Ascending/Descending order
            if (view.sortByLabel.getText().equals("Desc ↓")) {
                view.sortByLabel.setText("Asc ↑");
                filterParameterList();
            } else {
                view.sortByLabel.setText("Desc ↓");
                filterParameterList();
            }
        } else if (actionEvent.getSource().equals(view.filterPicker)) {
            JComboBox<String> cb = (JComboBox)actionEvent.getSource();
            model.setSelectedText((String) cb.getSelectedItem());
            filterParameterList();
        } else if (actionEvent.getSource().equals(view.sendToSessionDef)) {
            var param = (ParameterContainer) view.parameterJList.getSelectedValue();
            String paramName = param.getName();
            sessionViewController.addToSessionDefList(new SessionDefContainer(paramName, param.getType()));
        } else if (actionEvent.getSource().equals(view.hideExcludedParamsCheckBox)) {
            this.hideParamsExcludedByNoiseReduction = view.hideExcludedParamsCheckBox.isSelected();
            view.parameterJList.setListData(new Vector<>(filterExcludedParams(containerConverter.parameterToContainer(this.parameterHandler.observableInputParameterList))));
        }
    }

    // ListSelection Listener for ParameterList, ParameterValueList and MatchList
    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        if (!listSelectionEvent.getValueIsAdjusting()) {
            if (listSelectionEvent.getSource().equals(view.parameterMatchJList)) {
                var matchEntity = (ParameterMatchContainer) view.parameterMatchJList.getSelectedValue();
                if (matchEntity != null) {
                    var url = matchEntity.getUrl();
                    var value = matchEntity.getValue();
                    var messageId = matchEntity.getMessageId();
                    var messageHash = matchEntity.getMessageHash();
                    updateSelectedMessageId(String.valueOf(messageId));
                    int historyId = getHistoryId(messageHash);
                    view.httpRequestEditor.setRequest(this.api.proxy().history().get(historyId).finalRequest());
                    view.httpResponseEditor.setResponse(this.api.proxy().history().get(historyId).originalResponse());
                    renderMatchEntries(model.matchValueEntityList.stream().filter(e -> e.getUrl().equals(url)).filter(e -> e.getValue().equals(value)).distinct().toList());
                }
            } else if (listSelectionEvent.getSource().equals(view.parameterValueJList)) {
                var param = (InputParameterContainer) view.parameterValueJList.getSelectedValue();
                if (param != null) {
                    var messageId = param.getMessageId();
                    var messageHash = param.getMessageHash();
                    int historyId = getHistoryId(messageHash);
                    updateSelectedMessageId(String.valueOf(messageId));
                    view.httpResponseEditor.setResponse(this.api.proxy().history().get(historyId).originalResponse());
                    view.httpRequestEditor.setRequest(this.api.proxy().history().get(historyId).finalRequest());
                }
                view.parameterMatchJList.clearSelection();
                clearMatchValueContainer();
            } else if (listSelectionEvent.getSource().equals(view.parameterJList)) {
                updateSelectedMessageId("");
                view.parameterValueJList.clearSelection();
                clearParameterMatchContainer();
                view.httpRequestEditor.setRequest(null);
                view.httpResponseEditor.setResponse(null);
                var param = (ParameterContainer) view.parameterJList.getSelectedValue();
                if (param != null) {
                    parameterSelectedEvent();
                }
            }
        }
    }

    public void renderMatchEntries(List<MatchValue> entriesToDisplay) {
        view.matchValueJList.setListData(containerConverter.entryOccurrenceToContainer(entriesToDisplay));
        view.rightMidPanel.repaint();
        view.rightMidPanel.revalidate();
    }

    private void updateParameters() {
        Comparator<ParameterContainer> comparator = getSortSettings();
        List<InputParameter> parameters = this.parameterHandler.observableInputParameterList;
        Vector<ParameterContainer> parameterContainerVector;
        parameterContainerVector = new Vector<>(
                containerConverter.parameterToContainer(parameters.stream().toList())
                        .stream().sorted(comparator).toList());
        view.parameterJList.setListData(new Vector<>(filterExcludedParams(parameterContainerVector.stream().toList())));
        view.leftPanel.revalidate();
        view.leftPanel.repaint();
    }

    private void filterParameterList() {
        ListModel<ParameterContainer> model = view.parameterJList.getModel();
        List<ParameterContainer> toBeSorted = IntStream.range(0,model.getSize()).mapToObj(model::getElementAt).collect(Collectors.toList());
        Comparator<ParameterContainer> comparator = getSortSettings();
        Vector<ParameterContainer> containerList  = new Vector<>(toBeSorted.stream().sorted(comparator).toList());
        view.parameterJList.setListData(containerList);
    }

    private List<ParameterContainer> filterExcludedParams(List<ParameterContainer> containerVector) {
        if (this.hideParamsExcludedByNoiseReduction) {
            if (this.excludedParams.isEmpty()) {
                this.excludedParams = new ArrayList<>(containerVector.stream().filter(ParameterContainer::isExcludedByNoiseReduction).toList());
            }
            return new ArrayList<>(containerVector.stream().filter(parameterContainer -> !parameterContainer.isExcludedByNoiseReduction()).toList());
        } else {
            if (!this.excludedParams.isEmpty())
                containerVector.addAll(this.excludedParams);
            this.excludedParams.clear();
            return containerVector;
        }
    }

    private Comparator<ParameterContainer> getSortSettings() {
        Comparator<ParameterContainer> comparator = null;
        switch (model.getSelectedText()) {
            case "Name" -> {
                if (view.sortByLabel.getText().equals("Desc ↓")) {
                    comparator = Comparator.comparing(ParameterContainer::getName);
                } else {
                    comparator = Comparator.comparing(ParameterContainer::getName).reversed();
                }
            }
            case "Type" -> {
                if (view.sortByLabel.getText().equals("Desc ↓")) {
                    comparator = Comparator.comparing(ParameterContainer::getType);
                } else {
                    comparator = Comparator.comparing(ParameterContainer::getType).reversed();
                }
            }
            // Numbers get sorted differently so reversed needs to be the first case
            case "Number of Occurrences" -> {
                if (view.sortByLabel.getText().equals("Desc ↓")) {
                    comparator = Comparator.comparing(ParameterContainer::getOccurrences).reversed();
                } else {
                    comparator = Comparator.comparing(ParameterContainer::getOccurrences);
                }
            }
            case "Number of Matches" -> {
                if (view.sortByLabel.getText().equals("Desc ↓")) {
                    comparator = Comparator.comparing(ParameterContainer::getMatches).reversed();
                } else {
                    comparator = Comparator.comparing(ParameterContainer::getMatches);
                }
            }
        }
        return comparator;
    }

    // gets called on parameter selection from the parameterlist on the left side
    // gets called by clicking the search button as an item gets selected by this action
    private void parameterSelectedEvent() {
        model.matchValueEntityList.clear();
        String paramName = view.parameterJList.getSelectedValue().getName();
        String paramType = view.parameterJList.getSelectedValue().getType();
        ListModel<MatchValueContainer> entryContainer = view.matchValueJList.getModel();
        if (entryContainer.getSize() != 0) {
            Vector<MatchValueContainer> emptyVector = new Vector<>();
            view.matchValueJList.setListData(emptyVector);
        }

        model.loadListData(paramName, paramType);

        view.parameterValueJList.setListData(containerConverter.parameterOccurrenceToContainer(model.occurrenceEntityList));
        if (!model.parameterMatchEntityList.isEmpty()) {
            view.parameterMatchJList.setListData(containerConverter.matchOccurrenceToContainer(model.parameterMatchEntityList));
            view.parameterMatchJList.addListSelectionListener(this);
        }

        view.cypherQueryField.setText("MATCH (p1:InputParameter {name: \""+paramName+"\", type: \""+paramType+"\"})\n" +
                "OPTIONAL MATCH (p2:InputParameter {name: \""+paramName+"\", type: \""+paramType+"\"})-[OCCURS_WITH_VALUE]->" +
                "(o:InputValue)\nOPTIONAL MATCH (u1:Url)-[FOUND_PARAMETER]->" +
                "(p3:InputParameter {name: \""+paramName+"\", type: \""+paramType+"\"})\nOPTIONAL MATCH (u2:Url)-[FOUND]->" +
                "(m:ParameterMatch {name: \""+paramName+"\", type: \""+paramType+"\"})-[MATCH]->(e:MatchValue {name: \""+paramName+"\"})\n" +
                "RETURN p1,p2,o,u1,p3,u2,m,e");
        view.rightMidPanel.revalidate();
        view.rightMidPanel.repaint();
    }

    private void updateSelectedMessageId(String id) {
        view.selectedMessageId.setText("<html><b>Selected MessageId: </b>%s</html>".formatted(id));
    }

    private void clearParameterMatchContainer() {
        ListModel<ParameterMatchContainer> entryContainer = view.parameterMatchJList.getModel();
        if (entryContainer.getSize() != 0) {
            Vector<ParameterMatchContainer> emptyVector = new Vector<>();
            view.parameterMatchJList.setListData(emptyVector);
        }
    }

    private void clearMatchValueContainer() {
        ListModel<MatchValueContainer> entryContainer = view.matchValueJList.getModel();
        if (entryContainer.getSize() != 0) {
            Vector<MatchValueContainer> emptyVector = new Vector<>();
            view.matchValueJList.setListData(emptyVector);
        }
    }

    private void clearParamValueContainer() {
        ListModel<InputParameterContainer> entryContainer = view.parameterValueJList.getModel();
        if (entryContainer.getSize() != 0) {
            Vector<InputParameterContainer> emptyVector = new Vector<>();
            view.parameterValueJList.setListData(emptyVector);
        }
    }

    private int getHistoryId(String hash) {
        MessageHashToProxyId messageHashToProxyId = MessageHashToProxyId.getInstance(this.api);
        return messageHashToProxyId.calculateId(hash) - 1;
    }

    @Override
    public void onRuleChangeEvent(RuleContainerEvent event) {
        RuleContainer ruleContainer = event.getRuleContainer();
        if (event.isDeleteAction()) {
            ruleContainer.setActive(false);
        }
        this.parameterHandler.updateParameterExclusion(ruleContainer);
        DBModel.saveBulkParameters(this.parameterHandler.observableInputParameterList);
        updateParameters();
    }

    private class SearchFieldTextListener implements TextListener {

        private String oldValue = "";

        @Override
        public void textValueChanged(TextEvent event) {
            view.hideExcludedParamsCheckBox.setEnabled(false);
            var newValue = ((TextField)event.getSource()).getText();
            if (newValue == null | newValue.isEmpty()) {
                //Show all as no value entered
                view.parameterJList.setListData(new Vector<>(filterExcludedParams(containerConverter.parameterToContainer(view.parameterHandler.observableInputParameterList.stream().toList()))));
                filterParameterList();
                view.hideExcludedParamsCheckBox.setEnabled(true);
            }
            else if (newValue.length() > oldValue.length()) {
                var newParams = new Vector<ParameterContainer>();
                var model = view.parameterJList.getModel();
                List<ParameterContainer> toBeSorted = filterExcludedParams(IntStream.range(0,model.getSize()).mapToObj(model::getElementAt).collect(Collectors.toList()));
                for (var paramContainer : toBeSorted) {
                    if (paramContainer.getName().contains(newValue)) {
                        newParams.add(paramContainer);
                    }
                }
                view.parameterJList.setListData(newParams);
            }
            else {
                var newParams = new Vector<ParameterContainer>();
                var currentParams = filterExcludedParams(containerConverter.parameterToContainer(view.parameterHandler.observableInputParameterList.stream().toList()));
                for (var paramContainer : currentParams) {
                    if (paramContainer.getName().contains(newValue)) {
                        newParams.add(paramContainer);
                    }
                }
                view.parameterJList.setListData(newParams);

            }
            this.oldValue = newValue;
            view.parameterJList.revalidate();
            view.parameterJList.repaint();
        }

    }
    public void clearParameterList() {
        this.view.parameterJList.setListData(new Vector<>());
    }

    public void clearDataAndView() {
        clearParameterList();
        clearMatchValueContainer();
        clearParameterMatchContainer();
        clearParamValueContainer();
        updateSelectedMessageId("");
        model.clearAllData();
        view.cypherQueryField.setText("");
        view.searchField.setText("");
    }
}
