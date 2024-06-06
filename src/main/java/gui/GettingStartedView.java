package gui;

import burp.BurpExtender;
import burp.HttpListener;
import burp.PropertiesHandler;
import burp.api.montoya.MontoyaApi;
import controller.NoiseReductionController;
import controller.QueryViewController;
import controller.SessionViewController;
import db.DBModel;
import db.DeferMatching;
import db.MatchHandler;
import db.ParameterHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

public class GettingStartedView extends JScrollPane {

    private MontoyaApi api;
    private PropertiesHandler propertiesHandler;
    private DeferMatching deferMatching;
    private ParameterHandler parameterHandler;
    private MatchHandler matchHandler;
    private NoiseReductionController noiseReductionController;
    private QueryViewController queryViewController;
    private AuditFindingView auditFindingView;
    private SessionViewController sessionViewController;
    private JEditorPane correctStateHeadline;
    public static JLabel numberOfParameters = new JLabel();
    public static JLabel numberOfParameterValues = new JLabel();
    public static JLabel numberOfParameterMatches = new JLabel();
    public static JLabel numberOfMatchValues = new JLabel();
    public static JLabel numberOfUrls = new JLabel();
    public static JCheckBox detectionActiveCheckbox;

    private JButton matchButton;

    private JButton purgeButton;

    public GettingStartedView(MontoyaApi api, PropertiesHandler propertiesHandler, DeferMatching deferMatching,
                              ParameterHandler parameterHandler, MatchHandler matchHandler, NoiseReductionController noiseReductionController,
                              QueryViewController queryViewController, AuditFindingView auditFindingView, SessionViewController sessionViewController) {
        this.api = api;
        this.propertiesHandler = propertiesHandler;
        this.deferMatching = deferMatching;
        this.parameterHandler = parameterHandler;
        this.matchHandler = matchHandler;
        this.noiseReductionController = noiseReductionController;
        this.queryViewController = queryViewController;
        this.auditFindingView = auditFindingView;
        this.sessionViewController = sessionViewController;
        JPanel mainPanel = new JPanel(new MigLayout());
        mainPanel.add(renderHowToUse(), "wrap");
        mainPanel.add(renderGettingStarted());
        this.setViewportView(mainPanel);
    }

    public JPanel renderHowToUse() {
        JPanel panel = new JPanel(new MigLayout());
        JEditorPane headline = new JEditorPane();
        headline.setEditable(false);
        headline.setOpaque(true);
        headline.setContentType("text/html");
        headline.setText("<html><h1>How To Use</h1></html>");

        JEditorPane text = new JEditorPane();
        text.setEditable(false);
        text.setOpaque(true);
        text.setContentType("text/html");

        String howToUseText = "<html>" +
                "<h2> It is recommended to use FlowMate during reconnaissance phase </h2>" +
                "<b>1.</b> Add the target application to the BurpSuite internal <i>Scope</i><br>" +
                "<b>2.</b> Activate the detection by checking both boxes in this tab <br>" +
                "<b>3.</b> Browse the application <br>" +
                "<b>&emsp;&emsp;3.1</b> Enter unique and long enough values (generally more than 6 characters) when browsing an application with FlowMate enabled <br>" +
                "<b>&emsp;&emsp;3.2</b> Do not enter payloads during this phase <br>" +
                "<b>&emsp;&emsp;3.3</b> Browse all user roles and functionality available <br>" +
                "<b>4.</b> Stop the detection before starting manual analysis to prevent payloads and duplicate values from polluting the graph <br>" +
                "</html>";

        text.setText(howToUseText);

        panel.add(headline, "wrap");
        panel.add(text);

        return panel;
    }

    private JPanel renderGettingStarted() {

        JPanel panel = new JPanel(new MigLayout("wrap"));

        JEditorPane header = new JEditorPane();
        header.setEditable(false);
        header.setOpaque(true);
        header.setContentType("text/html");
        header.setText("<html><h1>Getting Started</h1></html>");

        // JEditorPane because HTML in JLabels does not get rendered
        correctStateHeadline = new JEditorPane();
        correctStateHeadline.setEditable(false);
        correctStateHeadline.setOpaque(true);
        correctStateHeadline.setContentType("text/html");

        JLabel checkBoxHeader = new JLabel("Set Scope and check Scope-CheckBox to activate the detection");
        JCheckBox scopeSet = new JCheckBox("Is Scope set?");

        detectionActiveCheckbox = new JCheckBox("Parameter Detection Activated?");
        detectionActiveCheckbox.setEnabled(false);
        detectionActiveCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                HttpListener.detectionIsActive = ((JCheckBox) actionEvent.getSource()).isSelected();
            }
        });
        
        if (this.propertiesHandler.isMatching) {
            correctStateHeadline.setText("<html><h2 style=\"color:green\">Burp Project is Matching with Database Entries!</h2></html>");
        } else {
            correctStateHeadline.setText("<html><h2 style=\"color:red\">Burp Project is NOT Matching with Database Entries!</h2></html>");
        }

        if (this.propertiesHandler.isScopeSet()) {
            scopeSet.setSelected(true);
            detectionActiveCheckbox.setEnabled(true);
        } else {
            scopeSet.setSelected(false);
            detectionActiveCheckbox.setSelected(false);
        }
        HttpListener.detectionIsActive = detectionActiveCheckbox.isSelected();

        scopeSet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JCheckBox checkBox = (JCheckBox) actionEvent.getSource();
                if (checkBox.isSelected()) {
                    propertiesHandler.setScopeKeyInBurpState("true");
                    detectionActiveCheckbox.setEnabled(true);
                } else {
                    propertiesHandler.setScopeKeyInBurpState("false");
                    detectionActiveCheckbox.setSelected(false);
                    detectionActiveCheckbox.setEnabled(false);
                    HttpListener.detectionIsActive = false;
                }
            }
        });

        JEditorPane infoLabel = new JEditorPane();
        infoLabel.setEditable(false);
        infoLabel.setOpaque(true);
        infoLabel.setContentType("text/html");
        infoLabel.setText("<html><b>Click the \"Match Now\" Button to identify Matches</b></html>");
        matchButton = new JButton("Match Now");
        matchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deferMatching.init();
            }
        });

        panel.add(header);
        panel.add(correctStateHeadline, "gapbottom 5");
        panel.add(checkBoxHeader, "gapleft 5");
        panel.add(scopeSet, "gapleft 5");
        panel.add(detectionActiveCheckbox, "gapleft 5");

        panel.add(renderTogglePane());

        panel.add(infoLabel, "gaptop 5");
        panel.add(matchButton, "gapleft 5");

        panel.add(renderStatistics(), "wrap");

        panel.add(renderDbPurgePane());

        return panel;

    }

    public static JPanel renderStatistics() {
        JPanel panel = new JPanel(new MigLayout());
        JEditorPane statisticHeadLine = new JEditorPane();
        statisticHeadLine.setEditable(false);
        statisticHeadLine.setOpaque(true);
        statisticHeadLine.setContentType("text/html");
        statisticHeadLine.setText("<html><h2>Database Statistics</h2</html>");

        JLabel textParameterValuesFound = new JLabel("InputValues found: ");
        JLabel textParameterFound = new JLabel("InputParameter found: ");
        JLabel textParameterMatchesFound = new JLabel("ParameterMatches found: ");
        JLabel textMatchValuesFound = new JLabel("MatchValues found: ");
        JLabel textUrlsFound = new JLabel("URLs found: ");

        panel.add(statisticHeadLine, "wrap");
        panel.add(textParameterValuesFound, "gapleft 5");
        panel.add(numberOfParameterValues, "wrap");
        panel.add(textParameterFound, "gapleft 5");
        panel.add(numberOfParameters, "wrap");
        panel.add(textParameterMatchesFound, "gapleft 5");
        panel.add(numberOfParameterMatches, "wrap");
        panel.add(textMatchValuesFound, "gapleft 5");
        panel.add(numberOfMatchValues, "wrap");
        panel.add(textUrlsFound, "gapleft 5");
        panel.add(numberOfUrls);

        return panel;
    }

    public JPanel renderTogglePane() {
        JPanel togglePanel = new JPanel(new MigLayout("gap rel 20"));
        JLabel label = new JLabel("Choose whether you want to match Parameters while browsing, or manually later:");
        JToggleButton liveMatchingToggle = new JToggleButton("Live Matching");
        JToggleButton deferredMatchingToggle = new JToggleButton("Deferred Matching");

        ButtonGroup toggleButtonGroup = new ButtonGroup();
        toggleButtonGroup.add(liveMatchingToggle);
        toggleButtonGroup.add(deferredMatchingToggle);

        toggleButtonGroup.setSelected(liveMatchingToggle.getModel(), true);
        HttpListener.setLiveMatchingIsActive(true);

        liveMatchingToggle.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                HttpListener.setLiveMatchingIsActive(itemEvent.getStateChange() == ItemEvent.SELECTED);
                matchButton.setEnabled(!(itemEvent.getStateChange() == ItemEvent.SELECTED));
            }
        });

        deferredMatchingToggle.setSelected(false);
        matchButton.setEnabled(false);


        togglePanel.add(label, "north, gapbottom 5");
        togglePanel.add(liveMatchingToggle, "west, gapright 5");
        togglePanel.add(deferredMatchingToggle, "west");
        return togglePanel;
    }

    public JPanel renderDbPurgePane() {
        JPanel panel = new JPanel(new MigLayout());

        JEditorPane header = new JEditorPane();
        header.setEditable(false);
        header.setOpaque(true);
        header.setContentType("text/html");
        header.setText("<html><h2>Purge Database</h2></html>");

        JEditorPane infoPane = new JEditorPane();
        infoPane.setEditable(false);
        infoPane.setOpaque(true);
        infoPane.setContentType("text/html");
        infoPane.setText("""
                <html>
                <b>Note:</b> This will delete all stored data in the database as well as all associated plugin data!
                </html>
                """);

        this.purgeButton = new JButton("Purge Database");

        this.purgeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int selectedOption = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to purge the current FlowMate database and all associated plugin data?",
                        "Are you sure?",
                        JOptionPane.YES_NO_OPTION);
                if (selectedOption == JOptionPane.YES_OPTION) {
                    parameterHandler.clearAllStorages();
                    matchHandler.clearAllStorages();
                    resetStatistics();
                    DBModel.purgeDatabase();
                    propertiesHandler.makeBurpStateMatchWithDB();
                    propertiesHandler.restoreDefaultNoiseReductionRules();
                    noiseReductionController.loadRules();
                    queryViewController.clearParameterList();
                    correctStateHeadline.setText("<html><h2 style=\"color:green\">Burp Project is Matching with Database Entries!</h2></html>");
                    BurpExtender.historyStart = api.proxy().history().size() + 1;
                    api.persistence().extensionData().setInteger("historyStart", api.proxy().history().size() + 1);
                    auditFindingView.clearDataAndFields();
                    sessionViewController.clearDataAndView();
                    queryViewController.clearDataAndView();
                    HttpListener.monitoredParameter.clear();
                    HttpListener.hasActiveSession = false;
                }
            }
        });

        panel.add(header, "wrap");
        panel.add(infoPane, "wrap");
        panel.add(this.purgeButton);


        return panel;
    }

    private void resetStatistics() {
        numberOfParameters.setText("0");
        numberOfParameterValues.setText("0");
        numberOfParameterMatches.setText("0");
        numberOfMatchValues.setText("0");
        numberOfUrls.setText("0");
    }
}
