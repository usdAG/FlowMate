package gui;

import audit.AuditFinding;
import burp.PropertiesHandler;
import burp.api.montoya.MontoyaApi;
import gui.renderer.AuditFindingListCellRenderer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class AuditFindingView extends JScrollPane {
    
    private MontoyaApi api;
    private PropertiesHandler propertiesHandler;
    private JPanel panel;
    private JList<AuditFinding> listOfFindings;
    private JEditorPane desccriptionPane;
    private JScrollPane findingsOverviewScrollPane;
    private JScrollPane findingDescriptionScrollPane;
    private Vector<AuditFinding> findings;
    private ArrayList<Integer> duplicates;

    public AuditFindingView(MontoyaApi api, PropertiesHandler propertiesHandler){
        this.api = api;
        this.propertiesHandler = propertiesHandler;
        this.findings = new Vector<AuditFinding>();
        this.duplicates = new ArrayList<>();
        this.initialize();
        this.registerListener();
        loadAuditFindings();
    }

    private void initialize(){
        this.panel = new JPanel(new MigLayout());
        this.setViewportView(panel);

        this.listOfFindings = new JList<>(new Vector<AuditFinding>());
        this.listOfFindings.setCellRenderer(new AuditFindingListCellRenderer());
        this.findingsOverviewScrollPane = new JScrollPane(listOfFindings);
        this.findingsOverviewScrollPane.setMinimumSize(new Dimension(700, 800));
        this.findingsOverviewScrollPane.setMaximumSize(new Dimension(700, 800));

        this.desccriptionPane = new JEditorPane();
        this.desccriptionPane.setEditable(false);
        this.desccriptionPane.setContentType("text/html");
        this.findingDescriptionScrollPane = new JScrollPane(desccriptionPane);
        this.findingDescriptionScrollPane.setMinimumSize(new Dimension(500, 800));
        this.findingDescriptionScrollPane.setMaximumSize(new Dimension(500, 800));

        this.panel.add(this.findingsOverviewScrollPane);
        this.panel.add(this.findingDescriptionScrollPane);
    }

    private void registerListener() {
        this.listOfFindings.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    var finding = listOfFindings.getSelectedValue();
                    if (finding != null)
                        desccriptionPane.setText(finding.getLongDescription());
                }
            }
        });
    }

    public void addFinding(AuditFinding finding){
        if (!this.duplicates.contains(Objects.hash(finding.getShortDescription(), finding.getLongDescription()))) {
            this.duplicates.add(Objects.hash(finding.getShortDescription(), finding.getLongDescription()));
            this.findings.add(finding);
            this.propertiesHandler.saveAuditFinding(finding);
            this.renderFindings();
        }
    }

    public void addFindings(List<AuditFinding> newFindings){
        for (AuditFinding finding : newFindings) {
            this.addFinding(finding);
        }
        this.renderFindings();
    }

    public Vector<AuditFinding> getAuditFindings(){
        return this.findings;
    }

    public void renderFindings() {
        this.listOfFindings.setListData(this.findings);
        this.panel.revalidate();
        this.panel.repaint();
    }

    public void setAuditFindings(List<AuditFinding> list) {
        this.findings.clear();
        this.propertiesHandler.deleteAuditFindings();
        this.duplicates.clear();
        this.addFindings(list);
        this.renderFindings();
    }

    public void loadAuditFindings() {
        List<AuditFinding> auditFindings = this.propertiesHandler.loadAuditFindings();
        for (AuditFinding finding : auditFindings) {
            this.duplicates.add(Objects.hash(finding.getShortDescription(), finding.getLongDescription()));
            this.findings.add(finding);
        }
        renderFindings();
    }

    private void clearDescriptionPane() {
        this.desccriptionPane.setText("");
        this.panel.revalidate();
        this.panel.repaint();
    }

    public void clearDataAndFields() {
        this.setAuditFindings(new ArrayList<>());
        this.clearDescriptionPane();
        this.duplicates.clear();
        this.propertiesHandler.deleteAuditFindings();
    }
}
