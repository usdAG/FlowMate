package gui;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProgressDialog {

    private JProgressBar progressBar;
    private JDialog progressDialog;
    private JTextArea taskOutput;
    private JButton closeButton;
    private String title;

    public ProgressDialog(String title) {
        this.title = title;
    }

    public void init() {
        progressDialog = new JDialog((Frame) null, title, true);
        progressDialog.setResizable(false);
        progressDialog.getContentPane().setLayout(new MigLayout("fill"));
        progressDialog.setLocationRelativeTo(null);
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setSize(280, 20);
        progressBar.setStringPainted(true);
        taskOutput = new JTextArea();
        taskOutput.setMargin(new Insets(5,5,5,5));
        taskOutput.setEditable(false);
        closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                progressDialog.setVisible(false);
            }
        });
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setMinimumSize(new Dimension(300, 150));
        progressDialog.setResizable(false);
        progressDialog.add(new JLabel("Progress..."), "north");
        progressDialog.add(new JScrollPane(taskOutput), "dock center, grow");
        progressDialog.add(closeButton, "south");
        progressDialog.add(progressBar,"south");
        progressDialog.setVisible(true);
    }

    public void updateProgressBarValue(int value) {
        this.progressBar.setValue(value);
    }

    public void appendTaskOutput(String str) {
        this.taskOutput.append(str);
    }

    public void setTaskOutputText(String text) {
        this.taskOutput.setText(text);
    }

    public void updateDialogDefaultCloseOperation(int operation) {
        this.progressDialog.setDefaultCloseOperation(operation);
    }
}
