package gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PaddingTextPane extends JPanel {

    private JTextPane pane;

    public PaddingTextPane(String content){
        pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText(content);
        this.setLayout(new BorderLayout());
        this.add(pane, BorderLayout.PAGE_START);
    }

    public PaddingTextPane(String content, int margin){
        this(content);
        this.setMargin(margin);
    }

    public void setText(String text){
        pane.setText(text);
    }

    public void setMargin(int allSides){
        this.setBorder(new EmptyBorder(allSides, allSides, allSides, allSides));
    }

    public void markAsExcluded() {
        this.pane.setForeground(Color.darkGray);
        this.pane.setToolTipText("Excluded by Noise Reduction");
    }


}
