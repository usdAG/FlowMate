package db;

import burp.BurpExtender;
import burp.HttpResponse;
import burp.HttpResponseParser;
import burp.RegexMatcher;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import db.entities.InputParameter;
import db.entities.InputValue;
import db.entities.ParameterMatch;
import db.entities.Url;
import net.miginfocom.swing.MigLayout;
import utils.Hashing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeferMatching implements PropertyChangeListener {

    private MontoyaApi api;
    private HttpResponseParser parser;
    private ParameterHandler pHandler;
    private MatchHandler matchHandler;
    private JProgressBar progressBar;
    private JDialog progressDialog;
    private MatchTask task;
    private JTextArea taskOutput;
    private JButton closeButton;
    private int historySize;

    public DeferMatching(MontoyaApi api, HttpResponseParser parser, ParameterHandler pHandler, MatchHandler mHandler) {
        this.api = api;
        this.parser = parser;
        this.pHandler = pHandler;
        this.matchHandler = mHandler;
    }

    public void init() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                historySize = api.proxy().history().size() - BurpExtender.historyStart;
                progressDialog = new JDialog((Frame) null, "Matching Parameters...", true);
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
        });
        task = new MatchTask();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            double historyProgress = (((double) progress * historySize / 100) - 1);
            progressBar.setValue(progress);
            taskOutput.append(String.format(
                    "Completed %d/%d of Burp history.\n", (int) historyProgress, historySize));
        }
    }

    class MatchTask extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            try {
                int historySize = api.proxy().history().size();
                var proxyList = api.proxy().history().subList(BurpExtender.historyStart, historySize);
                List<InputParameter> allInputParameters = pHandler.parameterStorage.values().stream().toList();
                List<Object> allMatches = new ArrayList<>();
                Set<Integer> duplicateIdentifiers = new HashSet<>();
                Set<String> messageHashes = new HashSet<>();
                List<InputParameter> inputParametersMatchingToHistory = new ArrayList<>();
                Set<Integer> inputParamIdentifiers = new HashSet<>();

                int progress = 0;
                setProgress(0);
                int listSize = proxyList.size();

                for (int i = 0; i < listSize; i++) {

                    ProxyHttpRequestResponse proxyResponse = proxyList.get(i);
                    progress = (int) ((i + 1) / (double) listSize * 100);
                    setProgress(progress);

                    HttpResponse response = parser.parseResponse(proxyResponse.originalResponse(), proxyResponse.finalRequest());
                    String hash = Hashing.sha1(proxyResponse.finalRequest().toByteArray().getBytes());
                    messageHashes.add(hash);

                    for (InputParameter parameter : allInputParameters) {
                        InputParameter realParam = new InputParameter(parameter.getName(), parameter.getType(), parameter.getDomain());
                        if (inputParamIdentifiers.contains(parameter.getIdentifier())) {
                            realParam = inputParametersMatchingToHistory.stream().filter(e -> e.getIdentifier() == parameter.getIdentifier()).findFirst().get();
                        }
                        for (InputValue value : parameter.getOccurrenceEntities()) {
                            if (hash.equals(value.getMessageHash())) {
                                realParam.addOccurence(value);
                            }
                        }
                        if (realParam.getOccurrenceEntities().isEmpty()) {
                            continue;
                        }
                        if (!duplicateIdentifiers.contains(realParam.getIdentifier())) {
                            duplicateIdentifiers.add(realParam.getIdentifier());
                            inputParametersMatchingToHistory.add(realParam);
                            inputParamIdentifiers.add(realParam.getIdentifier());
                        }
                    }
                    List<InputParameter> noiseReducedParams = RegexMatcher.excludeParameters(inputParametersMatchingToHistory);
                    List<MatchHelperClass> respMatch = parser.getMatches(response, noiseReducedParams, hash);
                    List<Object> realMatches = matchHandler.addMatches(respMatch);
                    for (Object realMatch : realMatches) {
                        int identifier = -1;

                        if (realMatch instanceof ParameterMatch) {
                            identifier = ((ParameterMatch) realMatch).getIdentifier();
                        } else if (realMatch instanceof Url) {
                            identifier = ((Url) realMatch).getIdentifier();
                        }

                        if (identifier != -1 && !duplicateIdentifiers.contains(identifier)) {
                            duplicateIdentifiers.add(identifier);
                            allMatches.add(realMatch);
                        }
                    }
                }
                taskOutput.append("Saving Entities in Database...\n");
                DBModel.saveBulk(allMatches);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void done() {
            progressDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            progressBar.setValue(100);
            taskOutput.setText("Completed!\n");
        }
    }
}
