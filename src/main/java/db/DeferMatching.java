package db;

import burp.BurpExtender;
import burp.HttpResponse;
import burp.HttpResponseParser;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import db.entities.*;
import events.DeferMatchingFinishedEvent;
import events.DeferMatchingFinishedListener;
import gui.ProgressDialog;
import model.SessionViewModel;
import utils.Hashing;
import utils.Logger;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

public class DeferMatching implements PropertyChangeListener {

    private MontoyaApi api;
    private HttpResponseParser parser;
    private ParameterHandler pHandler;
    private MatchHandler matchHandler;
    private MatchTask task;
    private int historySize;
    private ProgressDialog progressDialog;
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
                progressDialog = new ProgressDialog("Matching Parameters...");
                progressDialog.init();
            }
        });
        historySize = api.proxy().history().size() - BurpExtender.historyStart;
        task = new MatchTask();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            double historyProgress = (((double) progress * historySize / 100) - 1);
            progressDialog.updateProgressBarValue(progress);
            progressDialog.appendTaskOutput(String.format(
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

    class MatchTask extends SwingWorker<Void, String> {

        @Override
        protected Void doInBackground() {
            try {
                int historySize = api.proxy().history().size();
                var proxyList = api.proxy().history().subList(BurpExtender.historyStart, historySize);
                List<InputParameter> allInputParameters = pHandler.allInputParametersList;
                List<Object> allMatches = new ArrayList<>();
                Set<Integer> duplicateIdentifiers = new HashSet<>();
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

                HashMap<Integer, String> hashMap = new HashMap<>();
                String currentSessionName = "not set";
                if (matchHandler.isSessionActive()) {
                    List<Session> sessions = SessionViewModel.sessionTable.values().stream().toList();
                    currentSessionName = matchHandler.getSessionName();
                    hashMap = correctSessionName(sessions, listSize);
                }

                for (int i = 0; i < listSize; i++) {

                    if (matchHandler.isSessionActive()) {
                        if (!matchHandler.getSessionName().equals(hashMap.get(i)) && hashMap.size() == listSize) {
                            matchHandler.setSessionName(hashMap.get(i));
                        } else {
                            // correctSessionName method returns hashmap of size = 1 if there is only 1 session defined
                            matchHandler.setSessionName(hashMap.get(0));
                        }
                    }

                    ProxyHttpRequestResponse proxyResponse = proxyList.get(i);
                    progress = (int) ((i + 1) / (double) listSize * 100);
                    setProgress(progress);

                    burp.api.montoya.http.message.responses.HttpResponse originalResponse = proxyResponse.originalResponse();
                    if (originalResponse == null) {
                        continue;
                    }
                    HttpResponse response = parser.parseResponse(originalResponse, proxyResponse.finalRequest());
                    String hash = Hashing.sha1(proxyResponse.finalRequest().toByteArray().getBytes());

                    for (InputParameter parameter : allInputParameters) {
                        InputParameter realParam = new InputParameter(parameter.getName(), parameter.getType(), parameter.getDomain());
                        realParam.setExcludedByNoiseReduction(parameter.isExcludedByNoiseReduction());
                        if (inputParamIdentifiers.contains(parameter.getIdentifier())) {
                            realParam = inputParametersMatchingToHistory.stream().filter(e -> e.getIdentifier() == parameter.getIdentifier()).findFirst().get();
                        }
                        for (InputValue value : parameter.getOccurrenceEntities()) {
                            if (hash.equals(value.getMessageHash())) {
                                realParam.addOccurrence(value);
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
                    List<Object> realMatches = matchHandler.addMatchesThreaded(respMatch);
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
                publish("Saving Entities in Database...\n");
                DBModel.saveBulk(allMatches);
                if (matchHandler.isSessionActive())
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
        protected void process(List<String> chunks) {
            progressDialog.appendTaskOutput("Saving Entities in Database...\n");
        }

        @Override
        protected void done() {
            progressDialog.updateDialogDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            progressDialog.updateProgressBarValue(100);
            progressDialog.setTaskOutputText("Completed!\n");
            fireDeferMatchingFinishedEvent();
        }
    }
}
