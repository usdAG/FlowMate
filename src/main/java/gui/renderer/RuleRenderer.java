package gui.renderer;

import gui.components.PaddingTextPane;
import gui.container.RuleContainer;

import javax.swing.*;
import java.awt.*;

public class RuleRenderer implements ListCellRenderer<RuleContainer> {

    @Override
    public Component getListCellRendererComponent(JList<? extends RuleContainer> jList, RuleContainer ruleContainer, int i, boolean b, boolean b1) {
        var text = ruleContainer.getLabelRepresentation();
        var content = new PaddingTextPane(text, 5);
        content.setText(text);

        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(content);

        boolean isAcitve = ruleContainer.isActive();

        if (b) {
            content.setBackground(new Color(255,197,153));
            content.setForeground(new Color(0,0,0));
        } else {
            content.setBackground(new Color(255,255,255));
            content.setForeground(new Color(0,0,0));
        }

        if (isAcitve) {
            panel.setBorder(BorderFactory.createLineBorder(Color.green, 1));
        } else {
            panel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        }

        return panel;
    }


}
