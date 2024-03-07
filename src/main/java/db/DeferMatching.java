package db;

import burp.BurpExtender;
import burp.HttpResponse;
import burp.HttpResponseParser;
import burp.RegexMatcher;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import db.entities.*;
import events.DeferMatchingFinishedEvent;
import events.DeferMatchingFinishedListener;
import model.SessionViewModel;
import net.miginfocom.swing.MigLayout;
import utils.Hashing;
import utils.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

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
    private List<DeferMatchingFinishedListener> listeners;

    public DeferMatching(MontoyaApi api, HttpResponseParser parser, ParameterHandler pHandler, MatchHandler mHandler) {
        this.api = api;
        this.parser = parser;
        this.pHandler = pHandler;
        this.matchHandler = mHandler;
        this.listeners = new ArrayList<>();
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

    public void removeDeferMatchingFinishedListener(DeferMatchingFinishedListener listener) {
        this.listeners.remove(listener);
    }

    public void addDeferMatchingFinishedListener(DeferMatchingFinishedListener listener) {
        this.listeners.add(listener);
    }

    private void fireDeferMatchingFinishedEvent() {
        DeferMatchingFinishedEvent event = new DeferMatchingFinishedEvent(this);
        for (DeferMatchingFinishedListener listener : this.listeners) {
            listener.onDeferMatchingFinishedEvent();
        }
    }

    class MatchTask extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() {
            try {
                int historySize = api.proxy().history().size();
                var proxyList = api.proxy().history().subList(BurpExtender.historyStart, historySize);
                List<InputParameter> allInputParameters = pHandler.observableInputParameterList;
                List<Object> allMatches = new ArrayList<>();
                Set<Integer> duplicateIdentifiers = new HashSet<>();
                Set<String> messageHashes = new HashSet<>();
                List<InputParameter> inputParametersMatchingToHistory = new ArrayList<>();
                Set<Integer> inputParamIdentifiers = new HashSet<>();

                List<ProxyHttpRequestResponse> tempProxyList = new ArrayList<>(proxyList);

                for (ProxyHttpRequestResponse requestResponse : tempProxyList) {
                    if (!api.scope().isInScope(requestResponse.finalRequest().url())) {
                        proxyList.remove(requestResponse);
                    }
                }

                int progress = 0;
                setProgress(0);
                int listSize = proxyList.size();

                List<Session> sessions = SessionViewModel.sessionTable.values().stream().toList();

                String currentSessionName = matchHandler.getSessionName();

                HashMap<Integer, String> hashMap = correctSessionName(sessions, listSize);

                for (int i = 0; i < listSize; i++) {
                    if (!matchHandler.getSessionName().equals(hashMap.get(i))) {
                        matchHandler.setSessionName(hashMap.get(i));
                    }

                    ProxyHttpRequestResponse proxyResponse = proxyList.get(i);
                    progress = (int) ((i + 1) / (double) listSize * 100);
                    setProgress(progress);

                    HttpResponse response = parser.parseResponse(proxyResponse.originalResponse(), proxyResponse.finalRequest());
                    String hash = Hashing.sha1(proxyResponse.finalRequest().toByteArray().getBytes());
                    messageHashes.add(hash);

                    for (InputParameter parameter : allInputParameters) {
                        InputParameter realParam = new InputParameter(parameter.getName(), parameter.getType(), parameter.getDomain());
                        realParam.setExcludedByNoiseReduction(parameter.isExcludedByNoiseReduction());
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
                    List<MatchHelperClass> respMatch = parser.getMatches(response, inputParametersMatchingToHistory, hash);
                    List<Object> realMatches = matchHandler.addMatches(respMatch);
                    for (Object realMatch : realMatches) {
                        int identifier = -1;

                        if (realMatch instanceof ParameterMatch) {
                            identifier = ((ParameterMatch) realMatch).getIdentifier();
                        } else if (realMatch instanceof Url) {
                            identifier = ((Url) realMatch).getIdentifier();
                        } else if (realMatch instanceof Session) {
                            identifier = ((Session) realMatch).getIdentifier();
                        }

                        if (identifier != -1 && !duplicateIdentifiers.contains(identifier)) {
                            duplicateIdentifiers.add(identifier);
                            allMatches.add(realMatch);
                        }
                    }
                }
                taskOutput.append("Saving Entities in Database...\n");
                DBModel.saveBulk(allMatches);
                matchHandler.setSessionName(currentSessionName);
                return null;
            } catch (Exception e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
                return null;
            }
        }

        private HashMap<Integer, String> correctSessionName(List<Session> sessions, int listSize) {
            HashMap<Integer, String> hashMap = new HashMap<>();
            for (int i = 0; i < listSize; i++) {
                int historyId = i + BurpExtender.historyStart - 1;
                for (Session session : sessions) {
                    if ((historyId >= session.getLowestHistoryId() - 2 && historyId < session.getHighestHistoryId())) {
                        hashMap.put(i, session.getName());
                        break;
                    }
                }
            }
            return hashMap;
        }

        @Override
        protected void done() {
            progressDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            progressBar.setValue(100);
            taskOutput.setText("Completed!\n");
            fireDeferMatchingFinishedEvent();
        }
    }
}
