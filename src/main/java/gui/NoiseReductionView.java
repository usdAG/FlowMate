package gui;

import gui.container.RuleContainer;
import gui.renderer.RuleRenderer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class NoiseReductionView extends JScrollPane {

    public JPanel mainPanel;
    public JPanel listPanel;
    public JPanel editorPanel;

    public JPanel infoBoxPanel;
    public JScrollPane listScrollPane;
    public JList<RuleContainer> ruleList;
    public JTextField nameTextField;
    public JTextField regexTextField;

    public JLabel regexInvalidLabel;
    public JEditorPane regexChangeInfoPane;
    public JCheckBox namesCheckBox;
    public JCheckBox valuesCheckBox;
    public JCheckBox headerCheckBox;
    public JCheckBox bodyCheckBox;
    public JCheckBox cookieCheckBox;
    public JCheckBox caseInsensitiveCheckBox;
    public JCheckBox activatedCheckBox;
    public JButton newRuleButton;
    public JButton saveButton;
    public JButton deleteButton;
    public JButton purgeAndRematchButton;
    public TitledBorder editorTileBorder;

    public NoiseReductionView() {
        initComponents();
    }

    private void initComponents() {
        this.mainPanel = new JPanel(new MigLayout());
        this.getVerticalScrollBar().setUnitIncrement(16);
        this.setViewportView(mainPanel);

        this.listPanel = new JPanel(new MigLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("List of Rules"));
        listPanel.setMaximumSize(new Dimension(400, 800));

        this.ruleList = new JList<>();
        DefaultListModel<RuleContainer> model = new DefaultListModel<>();
        this.ruleList.setModel(model);
        this.ruleList.setCellRenderer(new RuleRenderer());
        this.listScrollPane = new JScrollPane(ruleList);
        this.listScrollPane.setMinimumSize(new Dimension(380, 720));

        this.editorPanel = new JPanel(new MigLayout());
        this.editorTileBorder = BorderFactory.createTitledBorder("Rule editor");
        this.editorPanel.setBorder(editorTileBorder);
        this.editorPanel.setMaximumSize(new Dimension(600, 800));

        this.infoBoxPanel = new JPanel(new MigLayout());
        this.infoBoxPanel.setBorder(BorderFactory.createTitledBorder("Info"));
        this.infoBoxPanel.setMinimumSize(new Dimension(450, 180));
        this.infoBoxPanel.setMaximumSize(new Dimension(450, 180));

        JLabel nameLabel = new JLabel("Name:");
        JLabel affectsParamsLabel = new JLabel("Apply on Parameter:");
        JLabel affectsTypeLabel = new JLabel("Affects Type:");
        JLabel regexLabel = new JLabel("Regex:");

        this.regexInvalidLabel = new JLabel("Regex is invalid!");
        this.regexInvalidLabel.setForeground(Color.red);
        this.regexInvalidLabel.setVisible(false);

        this.regexChangeInfoPane = new JEditorPane();
        this.regexChangeInfoPane.setEditable(false);
        this.regexChangeInfoPane.setOpaque(true);
        this.regexChangeInfoPane.setContentType("text/html");
        this.regexChangeInfoPane.setText("""
                <html>
                <b>Note:</b>
                Rule changes do not apply on already saved matches. <br>
                If you wish to apply them, you can purge existing matches from the database and start deferred matching
                by clicking on the button below. <br>
                <b>Depending on your burp history size, this may take a while.</b>
                </html>
                """);

        this.nameTextField = new JTextField();
        this.nameTextField.setMinimumSize(new Dimension(300, 20));
        this.regexTextField = new JTextField();
        this.regexTextField.setMinimumSize(new Dimension(300, 20));

        this.namesCheckBox = new JCheckBox("Names");
        this.valuesCheckBox = new JCheckBox("Values");
        this.headerCheckBox = new JCheckBox("Query String");
        this.bodyCheckBox = new JCheckBox("Body");
        this.cookieCheckBox = new JCheckBox("Cookie");
        this.caseInsensitiveCheckBox = new JCheckBox("Case-insensitive");
        this.activatedCheckBox = new JCheckBox("Activated");

        this.newRuleButton = new JButton("New Rule");
        this.saveButton = new JButton("Save");
        this.deleteButton = new JButton("Delete");
        this.purgeAndRematchButton = new JButton("Purge Matches and Rematch");

        JPanel paramCheckBoxPanel = new JPanel(new MigLayout("insets 0"));
        JPanel typesCheckBoxPanel = new JPanel(new MigLayout("insets 0"));
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0"));

        paramCheckBoxPanel.add(namesCheckBox);
        paramCheckBoxPanel.add(valuesCheckBox);

        typesCheckBoxPanel.add(headerCheckBox);
        typesCheckBoxPanel.add(bodyCheckBox);
        typesCheckBoxPanel.add(cookieCheckBox);

        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);

        this.listPanel.add(listScrollPane, "wrap");
        this.listPanel.add(newRuleButton, "gapleft 296");

        this.editorPanel.add(nameLabel);
        this.editorPanel.add(nameTextField, "wrap");
        this.editorPanel.add(affectsParamsLabel);
        this.editorPanel.add(paramCheckBoxPanel, "wrap");
        this.editorPanel.add(affectsTypeLabel);
        this.editorPanel.add(typesCheckBoxPanel, "wrap");
        this.editorPanel.add(regexLabel);
        this.editorPanel.add(regexTextField, "wrap");
        this.editorPanel.add(caseInsensitiveCheckBox, "cell 1 4");
        this.editorPanel.add(regexInvalidLabel, "cell 1 4, wrap");
        this.editorPanel.add(activatedCheckBox);
        this.editorPanel.add(buttonPanel, "span, gapleft 150");

        this.infoBoxPanel.add(regexChangeInfoPane, "wrap");
        this.infoBoxPanel.add(purgeAndRematchButton);

        JPanel rightSidePanel = new JPanel(new MigLayout("insets 0"));
        rightSidePanel.setMinimumSize(new Dimension(600, 800));
        rightSidePanel.add(editorPanel, "wrap");
        rightSidePanel.add(infoBoxPanel);

        this.mainPanel.add(listPanel);
        this.mainPanel.add(rightSidePanel);

    }

    public void setValuesInEditor(String name, String regex, boolean affectsParameterNames, boolean affectsParameterValues,
                                  boolean affectsHeader, boolean affectsBody, boolean affectsCookie, boolean isActive, boolean isCaseInsensitive) {
        this.nameTextField.setText(name);
        this.regexTextField.setText(regex);
        this.namesCheckBox.setSelected(affectsParameterNames);
        this.valuesCheckBox.setSelected(affectsParameterValues);
        this.headerCheckBox.setSelected(affectsHeader);
        this.bodyCheckBox.setSelected(affectsBody);
        this.cookieCheckBox.setSelected(affectsCookie);
        this.activatedCheckBox.setSelected(isActive);
        this.caseInsensitiveCheckBox.setSelected(isCaseInsensitive);
    }
}
