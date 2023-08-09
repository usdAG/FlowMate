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
        this.add(renderAdditionalUrlQuery());
    }

    private JPanel renderAdditionalParamValueQuery() {
        JPanel queryPanel = new JPanel(new MigLayout());
        // JEditorPane because html inside JLabel does somehow not get rendered >.>
        JEditorPane paramValueQueryHeading = new JEditorPane();
        JTextField textField = new JTextField();
        JButton enterButton = new JButton("Enter");
        JTextArea queryArea = new JTextArea();

        textField.setMinimumSize(new Dimension(200, 20));
        queryArea.setMinimumSize(new Dimension(600, 100));
        queryArea.setEditable(false);
        String paramValueQuery = "MATCH (o:InputValue {value: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                "OPTIONAL MATCH (m1:ParameterMatch {value: \"\"})-[FOUND]-(u:Url)\n" +
                "OPTIONAL MATCH (m2:ParameterMatch {value: \"\"})-[MATCH]-(mv:MatchValue {value: \"\"})\n" +
                "RETURN o,p,m1,m2,u,mv";
        queryArea.setText(paramValueQuery);
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
                String paramValueQuery = "MATCH (o:InputValue {value: \"%s\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n".formatted(paramValue) +
                        "OPTIONAL MATCH (m1:ParameterMatch {value: \"%s\"})-[FOUND]-(u:Url)\n".formatted(paramValue) +
                        "OPTIONAL MATCH (m2:ParameterMatch {value: \"%s\"})-[MATCH]-(mv:MatchValue {value: \"%s\"})\n".formatted(paramValue, paramValue) +
                        "RETURN o,p,m1,m2,u,mv";
                queryArea.setText(paramValueQuery);
            }
        });


        paramValueQueryHeading.setContentType("text/html");
        paramValueQueryHeading.setText("<html><b>Query to search for specific InputValues:</b></html>");
        paramValueQueryHeading.setEditable(false);
        queryPanel.add(paramValueQueryHeading, "wrap");
        queryPanel.add(textField, "split 2");
        queryPanel.add(enterButton, "wrap");
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

        textField.setMinimumSize(new Dimension(200, 20));
        queryArea.setEditable(false);
        String urlQuery = "MATCH (o:InputValue {url: \"\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n" +
                "OPTIONAL MATCH (m1:ParameterMatch {url: \"\"})-[FOUND]-(u:Url)\n" +
                "OPTIONAL MATCH (m2:ParameterMatch {url: \"\"})-[MATCH]-(mv:MatchValue {url: \"\"})\n" +
                "RETURN o,p,m1,m2,u,mv";
        queryArea.setText(urlQuery);
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
                String paramValueQuery = "MATCH (o:InputValue {url: \"%s\"})-[OCCURS_WITH_VALUE]-(p:InputParameter)\n".formatted(paramValue) +
                        "OPTIONAL MATCH (m1:ParameterMatch {url: \"%s\"})-[FOUND]-(u:Url)\n".formatted(paramValue) +
                        "OPTIONAL MATCH (m2:ParameterMatch {url: \"%s\"})-[MATCH]-(mv:MatchValue {url: \"%s\"})\n".formatted(paramValue, paramValue) +
                        "RETURN o,p,m1,m2,u,mv";
                queryArea.setText(paramValueQuery);
            }
        });

        urlQueryHeading.setContentType("text/html");
        urlQueryHeading.setOpaque(true);
        urlQueryHeading.setText("<html><b>Query to search for specific URLs:</b></html>");
        queryPanel.add(urlQueryHeading, "wrap");
        queryPanel.add(textField, "split 2");
        queryPanel.add(enterButton, "wrap");
        queryPanel.add(queryArea, "span");
        queryArea.setMinimumSize(new Dimension(600, 100));

        return queryPanel;
    }
}
