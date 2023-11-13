package gui.renderer;

import gui.components.PaddingTextPane;
import gui.container.SessionContainer;

import javax.swing.*;
import java.awt.*;

public class SessionListCellRenderer implements ListCellRenderer<SessionContainer> {
    @Override
    public Component getListCellRendererComponent(JList<? extends SessionContainer> jList, SessionContainer sessionContainer, int i, boolean b, boolean b1) {
        var text = sessionContainer.getLabelRepresentation();
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
