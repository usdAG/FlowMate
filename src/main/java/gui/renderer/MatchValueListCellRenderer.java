package gui.renderer;

import gui.container.MatchValueContainer;
import gui.components.PaddingTextPane;

import javax.swing.*;
import java.awt.*;

public class MatchValueListCellRenderer implements ListCellRenderer<MatchValueContainer> {
    @Override
    public Component getListCellRendererComponent(JList<? extends MatchValueContainer> jList, MatchValueContainer occurrenceContainer, int i, boolean b, boolean b1) {
        var text = occurrenceContainer.getLabelRepresentation();
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
