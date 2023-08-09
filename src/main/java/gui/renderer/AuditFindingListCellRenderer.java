package gui.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import audit.AuditFinding;
import gui.components.PaddingTextPane;

public class AuditFindingListCellRenderer implements ListCellRenderer<AuditFinding> {

    @Override
    public Component getListCellRendererComponent(JList<? extends AuditFinding> arg0, AuditFinding auditFinding, int arg2,
            boolean b, boolean arg4) {
        var text = auditFinding.getLabelRepresentation();
        var content = new PaddingTextPane(text, 5);
        content.setText(text);

        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(content);

        if (b) {
            content.setBackground(new Color(255,197,153));
            content.setForeground(new Color(0,0,0));
        } else {
            content.setBackground(new Color(255,255,255));
            content.setForeground(new Color(0,0,0));
        }
        return panel;
    }

    
}
