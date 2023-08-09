package gui.renderer;

import gui.container.SessionParameterContainer;
import gui.components.PaddingTextPane;

import javax.swing.*;
import java.awt.*;

public class SessionParameterListCellRenderer implements ListCellRenderer<SessionParameterContainer> {
    @Override
    public Component getListCellRendererComponent(JList<? extends SessionParameterContainer> jList, SessionParameterContainer sessionParameterContainer, int i, boolean b, boolean b1) {
        var text = sessionParameterContainer.getLabelRepresentation();
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
