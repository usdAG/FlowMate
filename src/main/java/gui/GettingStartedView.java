package gui;

import burp.HttpListener;
import burp.PropertiesHandler;
import burp.api.montoya.MontoyaApi;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GettingStartedView extends JScrollPane {

    private MontoyaApi api;

    public static JLabel numberOfParameters = new JLabel();
    public static JLabel numberOfParameterValues = new JLabel();
    public static JLabel numberOfParameterMatches = new JLabel();
    public static JLabel numberOfMatchValues = new JLabel();
    public static JLabel numberOfUrls = new JLabel();
    public static JCheckBox detectionActiveCheckbox;

    public GettingStartedView(MontoyaApi api) {
        this.api = api;
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
        JEditorPane correctStateHeadline = new JEditorPane();
        correctStateHeadline.setEditable(false);
        correctStateHeadline.setOpaque(true);
        correctStateHeadline.setContentType("text/html");

        JLabel checkBoxHeader = new JLabel("Set Scope and check Scope-CheckBox to activate the detection");
        JCheckBox scopeSet = new JCheckBox("Is Scope set?");

        detectionActiveCheckbox = new JCheckBox("Detection Activated?");
        detectionActiveCheckbox.setEnabled(false);
        detectionActiveCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                HttpListener.detectionIsActive = ((JCheckBox) actionEvent.getSource()).isSelected();
            }
        });

        PropertiesHandler propertiesHandler = new PropertiesHandler(this.api);
        if (propertiesHandler.isBurpStateMatchingWithDB()) {
            correctStateHeadline.setText("<html><h2 style=\"color:green\">Burp Project is Matching with Database Entries!</h2></html>");
        } else {
            correctStateHeadline.setText("<html><h2 style=\"color:red\">Burp Project is NOT Matching with Database Entries!</h2></html>");
        }

        if (propertiesHandler.isScopeSet()) {
            scopeSet.setSelected(true);
            detectionActiveCheckbox.setEnabled(true);
            detectionActiveCheckbox.setSelected(true);
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

        panel.add(header);
        panel.add(correctStateHeadline, "gapbottom 5");
        panel.add(checkBoxHeader, "gapleft 5");
        panel.add(scopeSet, "gapleft 5");
        panel.add(detectionActiveCheckbox, "gapleft 5");

        panel.add(renderStatistics());

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
}
