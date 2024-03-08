package gui.renderer;

import gui.components.PaddingTextPane;
import gui.container.ParameterContainer;

import javax.swing.*;
import java.awt.*;

public class ParameterListCellRenderer implements ListCellRenderer<ParameterContainer> {
    @Override
    public Component getListCellRendererComponent(JList<? extends ParameterContainer> jList, ParameterContainer parameterContainer, int i, boolean b, boolean b1) {
        var text = parameterContainer.getLabelRepresentation();
        var content = new PaddingTextPane(text, 5);
        content.setText(text);

        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(content);

        if (b) {
            content.setBorder(BorderFactory.createLineBorder(new Color(255, 197, 153), 5));
        } else {
            content.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 5));
        }

        if(parameterContainer.isExcludedByNoiseReduction()) {
            content.markAsExcluded();
            panel.setToolTipText("Excluded by Noise Reduction");
        }

        return panel;
    }
}
