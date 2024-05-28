package gui;

import burp.ContainerConverter;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import db.MatchHandler;
import db.ParameterHandler;
import gui.container.InputParameterContainer;
import gui.container.MatchValueContainer;
import gui.container.ParameterContainer;
import gui.container.ParameterMatchContainer;
import gui.renderer.InputParameterListCellRenderer;
import gui.renderer.MatchValueListCellRenderer;
import gui.renderer.ParameterListCellRenderer;
import gui.renderer.ParameterMatchListCellRenderer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

public class QueryView extends JScrollPane {

    private JPanel panel;
    public TextField searchField;
    private JScrollPane parameterScrollPane;
    public JList<ParameterContainer> parameterJList;
    private JScrollPane parameterValueScrollpane;
    public JList<InputParameterContainer> parameterValueJList;
    private JScrollPane parameterMatchScrollpane;
    public JList<ParameterMatchContainer> parameterMatchJList;
    private JScrollPane matchValueScrollpane;
    public JList<MatchValueContainer> matchValueJList;
    public JPanel leftPanel;
    public JPanel rightMidPanel;
    private JPanel sortPanel;
    public JTextArea cypherQueryField;
    public ParameterHandler parameterHandler;
    private MatchHandler matchHandler;
    public JButton sortByLabel;
    public JComboBox<String> filterPicker;
    public JCheckBox hideExcludedParamsCheckBox;
    public JEditorPane selectedMessageId;
    public HttpRequestEditor httpRequestEditor;
    public HttpResponseEditor httpResponseEditor;
    public JPopupMenu menu;
    public JMenuItem sendToSessionDef;
    private MontoyaApi api;
    private ContainerConverter containerConverter;

    public QueryView(ParameterHandler parameterHandler, MatchHandler matchHandler, MontoyaApi api) {
        this.parameterHandler = parameterHandler;
        this.matchHandler = matchHandler;
        this.api = api;
        this.containerConverter = new ContainerConverter(api, this.matchHandler);
        initComponents();
        panel.add(initEditors(), "south");
    }

    // Initialize Components for QueryView Tab
    private void initComponents() {
        this.panel = new JPanel(new MigLayout("wrap 2"));
        this.getVerticalScrollBar().setUnitIncrement(16);
        this.setViewportView(panel);
        leftPanel = new JPanel(new MigLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        leftPanel.setMaximumSize(new Dimension(400, 1000));

        this.searchField = new TextField();
        this.searchField.setMinimumSize(new Dimension(157, 20));

        this.rightMidPanel = new JPanel(new MigLayout());

        JLabel parameterOccurrenceLabel = new JLabel("Parameter Value occurred at:");
        this.parameterValueJList = new JList<>();
        this.parameterValueJList.setCellRenderer(new InputParameterListCellRenderer());
        this.parameterValueScrollpane = new JScrollPane(parameterValueJList);
        this.parameterValueScrollpane.setMinimumSize(new Dimension(250, 600));

        JLabel matchEntityOccurrenceLabel = new JLabel("Parameter Match occurred at:");
        this.parameterMatchJList = new JList<>();
        this.parameterMatchJList.setCellRenderer(new ParameterMatchListCellRenderer());
        this.parameterMatchScrollpane = new JScrollPane(parameterMatchJList);
        this.parameterMatchScrollpane.setMinimumSize(new Dimension(250, 600));

        JLabel entryOccurrenceLabel = new JLabel("ParameterMatch Info");
        this.matchValueJList = new JList<>();
        this.matchValueJList.setCellRenderer(new MatchValueListCellRenderer());
        this.matchValueScrollpane = new JScrollPane(matchValueJList);
        this.matchValueScrollpane.setMinimumSize(new Dimension(250, 600));

        this.rightMidPanel.add(parameterOccurrenceLabel);
        this.rightMidPanel.add(matchEntityOccurrenceLabel);
        this.rightMidPanel.add(entryOccurrenceLabel, "wrap");
        this.rightMidPanel.add(parameterValueScrollpane);
        this.rightMidPanel.add(parameterMatchScrollpane);
        this.rightMidPanel.add(matchValueScrollpane);

        JLabel cypherQueryFieldLabel = new JLabel("Query to View in Neo4J Browser:");
        this.cypherQueryField = new JTextArea();
        this.cypherQueryField.setMinimumSize(new Dimension(785, 200));
        this.cypherQueryField.setBorder(BorderFactory.createLineBorder(new Color(209, 209, 209)));
        this.cypherQueryField.setEditable(false);

        // panel for cypher query field
        JPanel rightBotPanel = new JPanel(new MigLayout());
        rightBotPanel.setMinimumSize(new Dimension(785, 200));
        rightBotPanel.add(cypherQueryFieldLabel, "wrap");
        rightBotPanel.add(this.cypherQueryField);

        // panel for all components on the left side
        leftPanel.add(initFilterAndSortPanel(), "wrap");
        leftPanel.add(initParameterList());

        panel.add(leftPanel);

        // Helper panel to achieve the right layout
        JPanel bigPanel =  new JPanel(new MigLayout());
        bigPanel.add(rightMidPanel, "cell 0 0");
        bigPanel.add(rightBotPanel, "cell 0 1");

        panel.add(bigPanel, "growy");
        // I don't know if that's necessary
        panel.revalidate();
        panel.repaint();
    }

    private JPanel initParameterList() {
        JPanel parameterListPanel = new JPanel(new MigLayout());
        JLabel parameterListLabel = new JLabel("List of Parameters:");
        this.parameterJList = new JList<>(containerConverter.parameterToContainer(this.parameterHandler.observableInputParameterList.stream().toList()));
        this.parameterJList.setCellRenderer(new ParameterListCellRenderer());
        this.parameterScrollPane = new JScrollPane(parameterJList);

        parameterScrollPane.setMinimumSize(new Dimension(sortPanel.getWidth(), 670));

        parameterListPanel.add(parameterListLabel, "wrap");
        parameterListPanel.add(parameterScrollPane);

        this.menu = new JPopupMenu();
        this.sendToSessionDef = new JMenuItem("Send to Session Definition");
        this.menu.add(this.sendToSessionDef);

        return parameterListPanel;
    }

    private JPanel initFilterAndSortPanel() {
        this.sortPanel = new JPanel(new MigLayout());
        String[] items = {"Name", "Type", "Number of Occurrences", "Number of Matches"};
        this.filterPicker = new JComboBox<String>(items);
        this.sortByLabel = new JButton("Desc ↓");

        this.sortPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        this.sortByLabel.setBorderPainted(false);
        this.filterPicker.setSelectedItem("Name");

        this.hideExcludedParamsCheckBox = new JCheckBox("Hide Parameter excluded by Noise Reduction");
        this.hideExcludedParamsCheckBox.setSelected(false);

        this.sortPanel.add(new JLabel("Sort By "));
        this.sortPanel.add(this.filterPicker);
        this.sortPanel.add(this.sortByLabel, "wrap");
        this.sortPanel.add(new JLabel("Search "));
        this.sortPanel.add(this.searchField, "wrap");
        this.sortPanel.add(this.hideExcludedParamsCheckBox, "span");

       return this.sortPanel;
    }

    private JPanel initEditors() {
        JPanel jPanel = new JPanel(new MigLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.httpRequestEditor = this.api.userInterface().createHttpRequestEditor();
        this.httpResponseEditor = this.api.userInterface().createHttpResponseEditor();
        splitPane.setResizeWeight(0.5);

        Component requestEditor = this.httpRequestEditor.uiComponent();
        Component responseEditor = this.httpResponseEditor.uiComponent();
        requestEditor.setMinimumSize(new Dimension(650, 400));
        responseEditor.setMinimumSize(new Dimension(650, 400));
        splitPane.setTopComponent(this.httpRequestEditor.uiComponent());
        splitPane.setBottomComponent(this.httpResponseEditor.uiComponent());
        splitPane.setPreferredSize(new Dimension(1300, 800));

        jPanel.add(initSelectedMessageIdLabel(), "wrap");
        jPanel.add(splitPane, "span");
        return jPanel;
    }

    // JEditorPane because JLabels are somehow unable to render HTML
    private JEditorPane initSelectedMessageIdLabel() {
        selectedMessageId = new JEditorPane();
        DefaultCaret caret = (DefaultCaret)selectedMessageId.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        selectedMessageId.setContentType("text/html");
        selectedMessageId.setEditable(false);
        selectedMessageId.setOpaque(true);
        selectedMessageId.setMinimumSize(new Dimension(200, 20));
        selectedMessageId.setText("<html><b>Selected MessageId: </b></html>");
        return selectedMessageId;
    }

}
