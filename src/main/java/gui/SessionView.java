package gui;

import burp.ContainerConverter;
import burp.api.montoya.MontoyaApi;
import db.MatchHandler;
import db.ParameterHandler;
import gui.container.*;
import gui.renderer.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.*;

public class SessionView extends JScrollPane {

    private MontoyaApi api;
    private static ParameterHandler parameterHandler;
    private static MatchHandler matchHandler;
    private static ContainerConverter containerConverter;
    private JPanel mainPanel;
    private JPanel sessionDefinitionPanel;
    public JPanel sessionsPanel;
    private JPanel sessionInfoPanel;
    public JPanel queryPanel;
    public JTextArea cypherQueryField;

    private JScrollPane sessionDefScrollPane;
    public static JList<SessionDefContainer> sessionDefJList;

    private JScrollPane sessionScrollPane;
    public static JList<SessionContainer> sessionJList;
    private JScrollPane sessionParamMonitorScrollPane;
    public JList<SessionParamMonitorContainer> sessionParamMonitorJList;
    public static JList<MatchValueContainer> matchInfoJList;
    public static JList<ParameterMatchContainer> matchJList;
    private JScrollPane parameterScrollPane;
    public static JList<SessionParameterContainer> sessionSpecificParameterJList;

    public JButton removeFromSessionDefButton;
    public JButton saveSessionDefinitionButton;

    public JButton changeSessionNameButton;

    public JTextField sessionNameTextField;

    public static JPanel identStatisticPanel;
    public static JPanel parameterListPanel;
    public static JLabel identMatchesNumber;

    public SessionView(MontoyaApi api, ParameterHandler pHandler, MatchHandler mHandler) {
        this.api = api;
        parameterHandler = pHandler;
        matchHandler = mHandler;
        containerConverter = new ContainerConverter(api, matchHandler);
        initPanel();
        initSessionDefinitionPanelComponents();
        initSessionsPanelComponents();
        initSessionInformationPanelComponents();

    }

    private void initPanel() {
        this.mainPanel = new JPanel(new MigLayout());
        this.getVerticalScrollBar().setUnitIncrement(16);
        this.setViewportView(mainPanel);

        this.sessionDefinitionPanel = new JPanel(new MigLayout("wrap 1"));
        this.sessionDefinitionPanel.setBorder(BorderFactory.createTitledBorder("Session Definition"));
        this.sessionDefinitionPanel.setMinimumSize(new Dimension(400, 400));

        this.sessionsPanel = new JPanel(new MigLayout());
        this.sessionsPanel.setBorder(BorderFactory.createTitledBorder("Sessions"));
        this.sessionsPanel.setMinimumSize(new Dimension(400, 600));

        this.sessionInfoPanel = new JPanel(new MigLayout());
        this.sessionInfoPanel.setBorder(BorderFactory.createTitledBorder("Session Information"));
        this.sessionInfoPanel.setMinimumSize(new Dimension(400, 1000));

        this.queryPanel = new JPanel(new MigLayout());
        this.queryPanel.setBorder(BorderFactory.createTitledBorder("Query to view Parameters in Neo4J Browser:"));
        this.queryPanel.setMinimumSize(new Dimension(800, 400));
        this.cypherQueryField = new JTextArea();
        this.cypherQueryField.setMinimumSize(new Dimension(800, 400));
        this.cypherQueryField.setEditable(false);
        // Add FocusListener so that all text gets selected on click
        this.cypherQueryField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                cypherQueryField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        });
        this.queryPanel.add(cypherQueryField);

        JPanel leftPanel = new JPanel(new MigLayout());
        leftPanel.add(sessionDefinitionPanel, "wrap");
        leftPanel.add(sessionsPanel);

        this.mainPanel.add(leftPanel);
        this.mainPanel.add(sessionInfoPanel, "wrap");
        this.mainPanel.add(queryPanel, "span");
    }

    private void initSessionDefinitionPanelComponents() {
        DefaultListModel<SessionDefContainer> model = new DefaultListModel<>();
        this.sessionDefJList = new JList<>();
        this.sessionDefJList.setModel(model);
        this.sessionDefJList.setCellRenderer(new SessionDefListCellRenderer());
        this.sessionDefScrollPane = new JScrollPane(sessionDefJList);
        this.sessionDefScrollPane.setMinimumSize(new Dimension(390, 400));
        this.removeFromSessionDefButton = new JButton("Remove");
        this.removeFromSessionDefButton.setMinimumSize(new Dimension(390, 20));

        this.saveSessionDefinitionButton = new JButton("Apply Session Definition");
        this.saveSessionDefinitionButton.setMinimumSize(new Dimension(390, 20));


        this.sessionDefinitionPanel.add(sessionDefScrollPane);
        this.sessionDefinitionPanel.add(removeFromSessionDefButton);
        this.sessionDefinitionPanel.add(saveSessionDefinitionButton);
    }

    private void initSessionsPanelComponents() {
        DefaultListModel<SessionContainer> model = new DefaultListModel<>();
        sessionJList = new JList<>();
        sessionJList.setModel(model);
        sessionJList.setCellRenderer(new SessionListCellRenderer());
        this.sessionScrollPane = new JScrollPane(sessionJList);
        this.sessionScrollPane.setMinimumSize(new Dimension(390, 580));
        this.sessionsPanel.add(sessionScrollPane);
    }

    private void initSessionInformationPanelComponents() {
        JPanel sessionNamePanel = new JPanel(new MigLayout());
        JLabel sessionNameLabel = new JLabel("Name:");
        this.sessionNameTextField = new JTextField();
        this.sessionNameTextField.setMinimumSize(new Dimension(300, 20));
        changeSessionNameButton = new JButton("Change");

        sessionNamePanel.add(sessionNameLabel);
        sessionNamePanel.add(this.sessionNameTextField);
        sessionNamePanel.add(changeSessionNameButton);

        DefaultListModel<SessionParamMonitorContainer> model = new DefaultListModel<>();
        this.sessionParamMonitorJList = new JList<>();
        this.sessionParamMonitorJList.setModel(model);
        this.sessionParamMonitorJList.setCellRenderer(new SessionParamMonitorListCellRenderer());
        this.sessionParamMonitorScrollPane = new JScrollPane(sessionParamMonitorJList);
        this.sessionParamMonitorScrollPane.setMinimumSize(new Dimension(390, 450));
        this.sessionParamMonitorScrollPane.setMaximumSize(new Dimension(390, 450));

        JPanel sessionParamMonitoringPanel = new JPanel(new MigLayout());
        JLabel monitoringLabel = new JLabel("Session-Parameter Monitoring:");
        sessionParamMonitoringPanel.add(monitoringLabel, "wrap");
        sessionParamMonitoringPanel.add(sessionParamMonitorScrollPane);

        identStatisticPanel = new JPanel(new MigLayout());
        JLabel identMatchesLabel = new JLabel("Matches found in this Session:");
        identMatchesNumber = new JLabel();
        identStatisticPanel.add(identMatchesLabel);
        identStatisticPanel.add(identMatchesNumber);

        parameterListPanel = new JPanel(new MigLayout());
        JLabel parameterListLabel = new JLabel("List of Matches during Session:");
        DefaultListModel<SessionParameterContainer> model2 = new DefaultListModel<>();
        sessionSpecificParameterJList = new JList<>();
        sessionSpecificParameterJList.setModel(model2);
        sessionSpecificParameterJList.setCellRenderer(new SessionParameterListCellRenderer());

        this.parameterScrollPane = new JScrollPane(sessionSpecificParameterJList);
        this.parameterScrollPane.setMinimumSize(new Dimension(390, 450));
        this.parameterScrollPane.setMaximumSize(new Dimension(390, 450));

        JLabel matchListLabel = new JLabel("ParameterMatches Details:");
        DefaultListModel<ParameterMatchContainer> model3 = new DefaultListModel<>();
        matchJList = new JList<>();
        matchJList.setModel(model3);
        matchJList.setCellRenderer(new ParameterMatchListCellRenderer());

        JScrollPane matchListScrollPane = new JScrollPane(matchJList);
        matchListScrollPane.setMinimumSize(new Dimension(390, 450));
        matchListScrollPane.setMaximumSize(new Dimension(390, 450));

        JLabel matchInfoLabel = new JLabel("ParameterMatch Info:");
        DefaultListModel<MatchValueContainer> model4 = new DefaultListModel<>();
        matchInfoJList = new JList<>();
        matchInfoJList.setModel(model4);
        matchInfoJList.setCellRenderer(new MatchValueListCellRenderer());
        JScrollPane matchInfoScrollPane = new JScrollPane(matchInfoJList);
        matchInfoScrollPane.setMinimumSize(new Dimension(390, 450));
        matchInfoScrollPane.setMaximumSize(new Dimension(390, 450));

        parameterListPanel.add(parameterListLabel);
        parameterListPanel.add(matchListLabel);
        parameterListPanel.add(matchInfoLabel, "wrap");
        parameterListPanel.add(parameterScrollPane);
        parameterListPanel.add(matchListScrollPane);
        parameterListPanel.add(matchInfoScrollPane);

        this.sessionInfoPanel.add(sessionNamePanel, "wrap");
        this.sessionInfoPanel.add(parameterListPanel, "wrap");
        this.sessionInfoPanel.add(identStatisticPanel, "wrap");
        this.sessionInfoPanel.add(sessionParamMonitoringPanel);
    }

    public void clearMatchLists() {
        ListModel<ParameterMatchContainer> matchContainer = matchJList.getModel();
        if (matchContainer.getSize() != 0) {
            Vector<ParameterMatchContainer> emptyVector = new Vector<>();
            matchJList.setListData(emptyVector);
        }
        ListModel<MatchValueContainer> entryContainer = matchInfoJList.getModel();
        if (entryContainer.getSize() != 0) {
            Vector<MatchValueContainer> emptyVector = new Vector<>();
            matchInfoJList.setListData(emptyVector);
        }
        parameterListPanel.revalidate();
        parameterListPanel.repaint();
    }
} 