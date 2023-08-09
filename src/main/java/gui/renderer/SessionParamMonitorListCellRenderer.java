package gui.renderer;

import gui.container.SessionParamMonitorContainer;
import gui.components.PaddingTextPane;

import javax.swing.*;
import java.awt.*;

public class SessionParamMonitorListCellRenderer implements ListCellRenderer<SessionParamMonitorContainer> {
    @Override
    public Component getListCellRendererComponent(JList<? extends SessionParamMonitorContainer> jList, SessionParamMonitorContainer sessionParamMonitorContainer, int i, boolean b, boolean b1) {
        var text = sessionParamMonitorContainer.getLabelRepresentation();
        var content = new PaddingTextPane(text, 5);
        content.setText(text);

        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(content);

        if (b) {
            panel.setBackground(new Color(255,197,153));
            panel.setForeground(new Color(0,0,0));
        } else {
            panel.setBackground(new Color(255,255,255));
            panel.setForeground(new Color(0,0,0));
        }

        return panel;
    }
}
