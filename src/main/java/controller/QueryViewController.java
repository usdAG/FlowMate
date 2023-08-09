package controller;

import burp.ContainerConverter;
import burp.api.montoya.MontoyaApi;
import db.MatchHandler;
import db.ParameterHandler;
import db.entities.InputParameter;
import db.entities.MatchValue;
import db.entities.ParameterMatch;
import gui.*;
import gui.container.*;
import javafx.collections.ListChangeListener;
import model.QueryViewModel;
import utils.Hashing;
import utils.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.TextField;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueryViewController implements ActionListener, ListSelectionListener {

    private MontoyaApi api;
    private QueryView view;
    private QueryViewModel model;
    private ParameterHandler parameterHandler;
    private MatchHandler matchHandler;
    private SessionViewController sessionViewController;
    private ContainerConverter containerConverter;

    public QueryViewController(MontoyaApi api, QueryView view, QueryViewModel model, ParameterHandler pHandler, MatchHandler maHandler, SessionViewController sessionViewController) {
        this.api = api;
        this.view = view;
        this.model = model;
        this.parameterHandler = pHandler;
        this.matchHandler = maHandler;
        this.sessionViewController = sessionViewController;
        this.containerConverter = new ContainerConverter(api, this.matchHandler);
        registerEventHandler();
    }

    private void registerEventHandler() {
        view.searchField.addTextListener(new SearchFieldTextListener());
        view.sortByLabel.addActionListener(this);
        view.filterPicker.addActionListener(this);
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
        Vector<ParameterContainer> parameterContainerVector = new Vector<>(
                containerConverter.parameterToContainer(this.parameterHandler.observableInputParameterList.stream().toList())
                        .stream().sorted(comparator).toList());
        view.parameterJList.setListData(parameterContainerVector);
        view.leftPanel.revalidate();
        view.leftPanel.repaint();
    }

    private void filterParameterList() {
        Vector<ParameterContainer> containerList = new Vector<>();
        ListModel<ParameterContainer> model = view.parameterJList.getModel();
        List<ParameterContainer> toBeSorted = IntStream.range(0,model.getSize()).mapToObj(model::getElementAt).collect(Collectors.toList());
        Comparator<ParameterContainer> comparator = getSortSettings();

        containerList = new Vector<>(toBeSorted.stream().sorted(comparator).toList());
        view.parameterJList.setListData(containerList);
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

        view.cypherQueryField.setText(("MATCH (p1:InputParameter {name: \"%s\", type: \"%s\"})\n".formatted(paramName, paramType) +
                "OPTIONAL MATCH (p2:InputParameter {name: \"%s\", type: \"%s\"})-[OCCURS_WITH_VALUE]->").formatted(paramName, paramType) +
                "(o:InputValue)\nOPTIONAL MATCH (u1:Url)-[FOUND_PARAMETER]->" +
                "(p3:InputParameter {name: \"%s\", type: \"%s\"})\nOPTIONAL MATCH (u2:Url)-[FOUND]->".formatted(paramName, paramType) +
                "(m:ParameterMatch {name: \"%s\", type: \"%s\"})-[MATCH]->(e:MatchValue {name: \"%s\"})\n".formatted(paramName, paramType, paramName) +
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

    private int getHistoryId(String hash) {
        var proxyList = api.proxy().history();
        for (int i = 0; i < proxyList.size(); i++) {
            String proxyHash = Hashing.sha1(proxyList.get(i).finalRequest().toByteArray().getBytes());
            if (hash.equals(proxyHash)) {
                return i;
            }
        }
        return -1;
    }

    private class SearchFieldTextListener implements TextListener {

        private String oldValue = "";

        @Override
        public void textValueChanged(TextEvent event) {
            var newValue = ((TextField)event.getSource()).getText();
            if (newValue == null | newValue.isEmpty()) {
                //Show all as no value entered
                view.parameterJList.setListData(containerConverter.parameterToContainer(view.parameterHandler.observableInputParameterList.stream().toList()));
                filterParameterList();
            }
            else if (newValue.length() > oldValue.length()) {
                var newParams = new Vector<ParameterContainer>();
                var currentParams = view.parameterJList.getModel();
                for (int i = 0; i < currentParams.getSize(); i++) {
                    var paramContainer = currentParams.getElementAt(i);
                    if (paramContainer.getName().contains(newValue)) {
                        newParams.add(paramContainer);
                    }
                }
                view.parameterJList.setListData(newParams);
            }
            else {
                var newParams = new Vector<ParameterContainer>();
                var currentParams = containerConverter.parameterToContainer(view.parameterHandler.observableInputParameterList.stream().toList());
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
}
