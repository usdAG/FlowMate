package controller;

import events.RuleContainerEvent;
import events.RuleContainerListener;
import gui.AuditFindingView;
import gui.NoiseReductionView;
import gui.container.RuleContainer;
import model.NoiseReductionModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NoiseReductionController implements ActionListener, ListSelectionListener {

    private NoiseReductionView view;
    private NoiseReductionModel model;
    private AuditFindingView auditFindingView;
    private boolean isEditing;
    private List<RuleContainerListener> listeners;

    public NoiseReductionController(NoiseReductionView view, NoiseReductionModel model, AuditFindingView auditFindingView) {
        this.view = view;
        this.model = model;
        this.auditFindingView = auditFindingView;
        this.isEditing = false;
        this.listeners = new ArrayList<>();
        registerEventListeners();
        loadRules();
    }
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(view.newRuleButton)) {
           setEditorInNewRuleMode();
        } else if (actionEvent.getSource().equals(view.saveButton)) {
            saveEditorContent();
        } else if (actionEvent.getSource().equals(view.deleteButton)) {
            deleteSelectedRule();
        } else if (actionEvent.getSource().equals(view.activatedCheckBox)) {
            changeActivatedStatus();
        } else if (actionEvent.getSource().equals(view.purgeAndRematchButton)) {
            purgeAndRematch();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        if (!listSelectionEvent.getValueIsAdjusting()) {
            if (view.ruleList.getSelectedValue() == null)
                return;
            view.editorPanel.setBorder(BorderFactory.createTitledBorder("Rule editor - *Editing rule*"));
            this.isEditing = true;
            setValuesInEditor();
        }
    }

    private void registerEventListeners() {
        view.activatedCheckBox.addActionListener(this);
        view.saveButton.addActionListener(this);
        view.deleteButton.addActionListener(this);
        view.newRuleButton.addActionListener(this);
        view.ruleList.addListSelectionListener(this);
        view.purgeAndRematchButton.addActionListener(this);
    }

    private void clearAllTextFields() {
        view.nameTextField.setText("");
        view.regexTextField.setText("");
    }

    private void clearAllCheckBoxes() {
        view.namesCheckBox.setSelected(false);
        view.valuesCheckBox.setSelected(false);
        view.headerCheckBox.setSelected(false);
        view.bodyCheckBox.setSelected(false);
        view.cookieCheckBox.setSelected(false);
        view.activatedCheckBox.setSelected(false);
    }

    private void setEditorInNewRuleMode() {
        this.isEditing = false;
        view.ruleList.getSelectionModel().clearSelection();
        view.nameTextField.requestFocus();
        clearAllTextFields();
        clearAllCheckBoxes();
        view.editorPanel.setBorder(BorderFactory.createTitledBorder("Rule editor - *Adding new rule*"));
    }

    private void saveEditorContent() {
        RuleContainer rule = getSelectedValuesFromView();
        view.regexInvalidLabel.setVisible(false);
        if (!isRegexValid(rule.getRegex())) {
            view.regexInvalidLabel.setVisible(true);
            return;
        }
        if (isEditing) {
            saveEditedRule(rule.getName(), rule.getRegex(), rule.affectsParameterNames(), rule.affectsParameterValues(),
                    rule.affectsQueryString(), rule.affectsBody(), rule.affectsCookie(), rule.isActive(), rule.isCaseInsensitive());
        } else {
            saveNewRule(rule.getName(), rule.getRegex(), rule.affectsParameterNames(), rule.affectsParameterValues(),
                    rule.affectsQueryString(), rule.affectsBody(), rule.affectsCookie(), rule.isActive(), rule.isCaseInsensitive());
        }
    }

    private void deleteSelectedRule() {
        int index = view.ruleList.getSelectedIndex();
        if (index != -1) {
            DefaultListModel<RuleContainer> listModel = (DefaultListModel<RuleContainer>) view.ruleList.getModel();
            RuleContainer selectedRuleContainer = view.ruleList.getSelectedValue();
            listModel.removeElement(selectedRuleContainer);
            model.deleteRuleInState(selectedRuleContainer);
            fireRuleContainerChanged(selectedRuleContainer, true);
            clearAllTextFields();
            clearAllCheckBoxes();
        }
    }

    private void changeActivatedStatus() {
        if (isEditing) {
            RuleContainer rule = getSelectedValuesFromView();
            updateActiveStatus(rule.isActive());
        }
    }

    private void purgeAndRematch() {
        model.purgeDbAndStartRematch();
        auditFindingView.clearDataAndFields();
    }

    private void setValuesInEditor() {
        RuleContainer ruleContainer = view.ruleList.getSelectedValue();
        if (ruleContainer != null) {
            String name = ruleContainer.getName();
            String regex = ruleContainer.getRegex();
            boolean affectsParameterNames = ruleContainer.affectsParameterNames();
            boolean affectsParameterValues = ruleContainer.affectsParameterValues();
            boolean affectsHeader = ruleContainer.affectsQueryString();
            boolean affectsBody = ruleContainer.affectsBody();
            boolean affectsCookie = ruleContainer.affectsCookie();
            boolean isActive = ruleContainer.isActive();
            boolean isCaseInsensitive = ruleContainer.isCaseInsensitive();
            view.setValuesInEditor(name, regex, affectsParameterNames, affectsParameterValues, affectsHeader,
                    affectsBody, affectsCookie, isActive, isCaseInsensitive);
        }
    }

    public void loadRules() {
        DefaultListModel<RuleContainer> ruleListModel = (DefaultListModel<RuleContainer>) view.ruleList.getModel();
        Vector<RuleContainer> ruleContainers = this.model.loadRules();
        ruleListModel.clear();
        ruleListModel.addAll(ruleContainers);
    }

    private boolean isRegexValid(String regex) {
        boolean isValid;
        try {
            Pattern.compile(regex);
            isValid = true;
        } catch (PatternSyntaxException exception) {
           isValid = false;
        }
        return isValid;
    }

    private RuleContainer getSelectedValuesFromView() {
        String name = view.nameTextField.getText();
        String regex = view.regexTextField.getText();
        boolean affectsParamName = view.namesCheckBox.isSelected();
        boolean affectsParamValue = view.valuesCheckBox.isSelected();
        boolean affectsHeader = view.headerCheckBox.isSelected();
        boolean affectsBody = view.bodyCheckBox.isSelected();
        boolean affectsCookie = view.cookieCheckBox.isSelected();
        boolean active = view.activatedCheckBox.isSelected();
        boolean caseInsensitive = view.caseInsensitiveCheckBox.isSelected();
        return new RuleContainer(name, regex, affectsParamName, affectsParamValue, affectsHeader, affectsBody, affectsCookie, active, caseInsensitive);
    }

    private void saveEditedRule(String name, String regex, boolean affectsParamName, boolean affectsParamValue,
                                boolean affectsHeader, boolean affectsBody, boolean affectsCookie, boolean active, boolean caseInsensitive) {
        RuleContainer ruleContainer = view.ruleList.getSelectedValue();
        int index = view.ruleList.getSelectedIndex();
        if (ruleContainer != null && index != -1) {
            String oldRuleHash = String.valueOf(ruleContainer.getHash());
            // Deactivate the old rule and apply deactivation
            ruleContainer.setActive(false);
            fireRuleContainerChanged(ruleContainer, false);
            // Set properties of new rule
            ruleContainer.updateValues(name, regex, affectsParamName, affectsParamValue, affectsHeader, affectsBody, affectsCookie, active, caseInsensitive);
            // Apply changes
            model.updateRuleInState(ruleContainer, oldRuleHash);
            updateListPanel();
            fireRuleContainerChanged(ruleContainer, false);
        }
    }

    private void saveNewRule(String name, String regex, boolean affectsParamName, boolean affectsParamValue,
                             boolean affectsHeader, boolean affectsBody, boolean affectsCookie, boolean active, boolean caseInsensitive) {
        DefaultListModel<RuleContainer> ruleListModel = (DefaultListModel<RuleContainer>) view.ruleList.getModel();
        RuleContainer ruleContainer = model.addRule(name, regex, affectsParamName, affectsParamValue, affectsHeader, affectsBody, affectsCookie, active, caseInsensitive);
        ruleListModel.addElement(ruleContainer);
        model.saveRuleInState(ruleContainer);
        fireRuleContainerChanged(ruleContainer, false);
    }

    private void updateActiveStatus(boolean active) {
        RuleContainer ruleContainer = view.ruleList.getSelectedValue();
        int index = view.ruleList.getSelectedIndex();
        if (ruleContainer != null && index != -1) {
            String oldRuleHash = String.valueOf(ruleContainer.getHash());
            ruleContainer.setActive(active);
            model.updateRuleInState(ruleContainer, oldRuleHash);
            updateListPanel();
            fireRuleContainerChanged(ruleContainer, false);
        }
    }

    private void updateListPanel() {
        view.listPanel.repaint();
        view.listPanel.repaint();
    }

    public void addRuleContainerListener(RuleContainerListener listener) {
        this.listeners.add(listener);
    }

    public void removeRuleContainerListener(RuleContainerListener listener) {
        this.listeners.remove(listener);
    }

    private void fireRuleContainerChanged(RuleContainer ruleContainer, boolean deleteAction) {
        RuleContainerEvent event = new RuleContainerEvent(this, ruleContainer, deleteAction);
        for (RuleContainerListener listener : this.listeners) {
            listener.onRuleChangeEvent(event);
        }
    }

}
