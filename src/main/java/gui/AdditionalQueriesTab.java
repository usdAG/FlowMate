package gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class AdditionalQueriesTab extends JPanel {

    public AdditionalQueriesTab() {
        this.setLayout(new MigLayout());
        this.add(renderAdditionalParamValueQuery(), "wrap");
        this.add(renderAdditionalUrlQuery(), "wrap");
        this.add(renderAdditionalParamNameQuery());
    }

    private JPanel renderAdditionalParamValueQuery() {
        JPanel queryPanel = new JPanel(new MigLayout());
        // JEditorPane because html inside JLabel does somehow not get rendered >.>
        JEditorPane paramValueQueryHeading = new JEditorPane();
        JTextField textField = new JTextField();
        JButton enterButton = new JButton("Enter");
        JTextArea queryArea = new JTextArea();
        JCheckBox sessionsIncludedCheckBox = new JCheckBox("include sessions");

        textField.setMinimumSize(new Dimension(200, 20));
        queryArea.setMinimumSize(new Dimension(600, 100));
        queryArea.setEditable(false);

        String paramValueQuery = "MATCH (o:InputValue {value: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                "OPTIONAL MATCH (p)-[FOUND_PARAMETER]-(u:Url)\n" +
                "OPTIONAL MATCH (m1:ParameterMatch {value: \"\"})-[FOUND]-(u2:Url)\n" +
                "OPTIONAL MATCH (m2:ParameterMatch {value: \"\"})-[MATCH]-(mv:MatchValue {value: \"\"})\n" +
                "RETURN o,p,m1,m2,u,u2,mv";
        queryArea.setText(paramValueQuery);

        sessionsIncludedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String paramValueQuery = "";
                if (sessionsIncludedCheckBox.isSelected()) {
                    paramValueQuery = "MATCH (o:InputValue {value: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                            "OPTIONAL MATCH (p)-[FOUND_PARAMETER]-(u:Url)\n" +
                            "OPTIONAL MATCH (m1:ParameterMatch {value: \"\"})-[FOUND]-(u2:Url)\n" +
                            "OPTIONAL MATCH (m2:ParameterMatch {value: \"\"})-[MATCH]-(mv:MatchValue {value: \"\"})\n" +
                            "OPTIONAL MATCH (s:Session)-[]-(o)\n" +
                            "OPTIONAL MATCH (s2:Session)-[]-(m2)\n" +
                            "RETURN o,p,m1,m2,u,u2,mv,s,s2";
                } else {
                    paramValueQuery = "MATCH (o:InputValue {value: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                            "OPTIONAL MATCH (p)-[FOUND_PARAMETER]-(u:Url)\n" +
                            "OPTIONAL MATCH (m1:ParameterMatch {value: \"\"})-[FOUND]-(u2:Url)\n" +
                            "OPTIONAL MATCH (m2:ParameterMatch {value: \"\"})-[MATCH]-(mv:MatchValue {value: \"\"})\n" +
                            "RETURN o,p,m1,m2,u,u2,mv";
                }
                queryArea.setText(paramValueQuery);
                enterButton.doClick();
            }
        });


        queryArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                queryArea.selectAll();
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        });

        enterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String paramValue = textField.getText();
                String query = queryArea.getText();
                queryArea.setText(query.replaceAll("\"([^\"]*)\"", "\""+paramValue+"\""));
            }
        });


        paramValueQueryHeading.setContentType("text/html");
        paramValueQueryHeading.setText("<html><b>Query to search for specific InputValues:</b></html>");
        paramValueQueryHeading.setEditable(false);
        queryPanel.add(paramValueQueryHeading, "wrap");
        queryPanel.add(textField, "split 2");
        queryPanel.add(enterButton);
        queryPanel.add(sessionsIncludedCheckBox, "wrap");
        queryPanel.add(queryArea, "span");

        return queryPanel;

    }

    private JPanel renderAdditionalUrlQuery() {
        JPanel queryPanel = new JPanel(new MigLayout());
        // JEditorPane because html inside JLabel does somehow not get rendered >.>
        JEditorPane urlQueryHeading = new JEditorPane();
        JTextField textField = new JTextField();
        JButton enterButton = new JButton("Enter");
        JTextArea queryArea = new JTextArea();
        JCheckBox sessionsIncludedCheckBox = new JCheckBox("include sessions");

        textField.setMinimumSize(new Dimension(200, 20));
        queryArea.setEditable(false);
        String urlQuery = "MATCH (o:InputValue {url: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                "OPTIONAL MATCH (m1:ParameterMatch {url: \"\"})-[FOUND]-(u:Url)\n" +
                "OPTIONAL MATCH (m2:ParameterMatch {url: \"\"})-[MATCH]-(mv:MatchValue {url: \"\"})\n" +
                "RETURN o,p,m1,m2,u,mv";
        queryArea.setText(urlQuery);

        sessionsIncludedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String urlQuery = "";
                if (sessionsIncludedCheckBox.isSelected()) {
                    urlQuery = "MATCH (o:InputValue {url: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                            "OPTIONAL MATCH (m1:ParameterMatch {url: \"\"})-[FOUND]-(u:Url)\n" +
                            "OPTIONAL MATCH (m2:ParameterMatch {url: \"\"})-[MATCH]-(mv:MatchValue {url: \"\"})\n" +
                            "OPTIONAL MATCH (s:Session)-[]-(o)\n" +
                            "OPTIONAL MATCH (s2:Session)-[]-(m2)\n" +
                            "RETURN o,p,m1,m2,u,mv,s,s2";
                } else {
                    urlQuery = "MATCH (o:InputValue {url: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                            "OPTIONAL MATCH (m1:ParameterMatch {url: \"\"})-[FOUND]-(u:Url)\n" +
                            "OPTIONAL MATCH (m2:ParameterMatch {url: \"\"})-[MATCH]-(mv:MatchValue {url: \"\"})\n" +
                            "RETURN o,p,m1,m2,u,mv";
                }
                queryArea.setText(urlQuery);
                enterButton.doClick();
            }
        });

        queryArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                queryArea.selectAll();
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        });

        enterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String paramUrl = textField.getText();
                String query = queryArea.getText();
                queryArea.setText(query.replaceAll("\"([^\"]*)\"", "\""+paramUrl+"\""));
            }
        });

        urlQueryHeading.setContentType("text/html");
        urlQueryHeading.setOpaque(true);
        urlQueryHeading.setText("<html><b>Query to search for specific URLs:</b></html>");
        queryPanel.add(urlQueryHeading, "wrap");
        queryPanel.add(textField, "split 2");
        queryPanel.add(enterButton);
        queryPanel.add(sessionsIncludedCheckBox, "wrap");
        queryPanel.add(queryArea, "span");
        queryArea.setMinimumSize(new Dimension(600, 100));

        return queryPanel;
    }

    private JPanel renderAdditionalParamNameQuery() {
        JPanel queryPanel = new JPanel(new MigLayout());
        // JEditorPane because html inside JLabel does somehow not get rendered >.>
        JEditorPane urlQueryHeading = new JEditorPane();
        JTextField textField = new JTextField();
        JButton enterButton = new JButton("Enter");
        JTextArea queryArea = new JTextArea();
        JCheckBox sessionsIncludedCheckBox = new JCheckBox("include sessions");

        textField.setMinimumSize(new Dimension(200, 20));
        queryArea.setEditable(false);
        String nameQuery = "MATCH (p1:InputParameter {name: \"\"})\n" +
                "OPTIONAL MATCH (p2:InputParameter {name: \"\"})-[OCCURS_WITH_VALUE]->" +
                "(o:InputValue)\nOPTIONAL MATCH (u1:Url)-[FOUND_PARAMETER]->" +
                "(p3:InputParameter {name: \"\"})\nOPTIONAL MATCH (u2:Url)-[FOUND]->" +
                "(m:ParameterMatch {name: \"\"})-[MATCH]->(e:MatchValue {name: \"\"})\n" +
                "RETURN p1,p2,o,u1,p3,u2,m,e";
        queryArea.setText(nameQuery);

        sessionsIncludedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String nameQuery = "";
                if (sessionsIncludedCheckBox.isSelected()) {
                    nameQuery = "MATCH (p1:InputParameter {name: \"\"})\n" +
                            "OPTIONAL MATCH (p2:InputParameter {name: \"\"})-[OCCURS_WITH_VALUE]->" +
                            "(o:InputValue)\nOPTIONAL MATCH (u1:Url)-[FOUND_PARAMETER]->" +
                            "(p3:InputParameter {name: \"\"})\nOPTIONAL MATCH (u2:Url)-[FOUND]->" +
                            "(m:ParameterMatch {name: \"\"})-[MATCH]->(e:MatchValue {name: \"\"})\n" +
                            "OPTIONAL MATCH (s:Session)-[]-(o)\n" +
                            "OPTIONAL MATCH (s2:Session)-[]-(m)\n" +
                            "RETURN p1,p2,o,u1,p3,u2,m,e,s,s2";
                } else {
                    nameQuery = "MATCH (p1:InputParameter {name: \"\"})\n" +
                            "OPTIONAL MATCH (p2:InputParameter {name: \"\"})-[OCCURS_WITH_VALUE]->" +
                            "(o:InputValue)\nOPTIONAL MATCH (u1:Url)-[FOUND_PARAMETER]->" +
                            "(p3:InputParameter {name: \"\"})\nOPTIONAL MATCH (u2:Url)-[FOUND]->" +
                            "(m:ParameterMatch {name: \"\"})-[MATCH]->(e:MatchValue {name: \"\"})\n" +
                            "RETURN p1,p2,o,u1,p3,u2,m,e";
                }
                queryArea.setText(nameQuery);
                enterButton.doClick();
            }
        });

        queryArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                queryArea.selectAll();
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {

            }
        });

        enterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String paramName = textField.getText();
                String query = queryArea.getText();
                queryArea.setText(query.replaceAll("\"([^\"]*)\"", "\""+paramName+"\""));
            }
        });

        urlQueryHeading.setContentType("text/html");
        urlQueryHeading.setOpaque(true);
        urlQueryHeading.setText("<html><b>Query to search for specific Parameter names:</b></html>");
        queryPanel.add(urlQueryHeading, "wrap");
        queryPanel.add(textField, "split 2");
        queryPanel.add(enterButton);
        queryPanel.add(sessionsIncludedCheckBox, "wrap");
        queryPanel.add(queryArea, "span");
        queryArea.setMinimumSize(new Dimension(600, 100));

        return queryPanel;
    }
}
