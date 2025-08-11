package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import controller.QueryViewController;
import db.DBModel;
import db.ParameterHandler;
import gui.ProgressDialog;
import utils.Hashing;
import utils.Logger;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

public class RetroactiveParser implements PropertyChangeListener {

    private MontoyaApi api;
    private HttpRequestParser parser;
    private ParameterHandler parameterHandler;

    private QueryViewController queryViewController;
    private ParseTask task;
    private ProgressDialog progressDialog;
    private int historySize;

    public RetroactiveParser(MontoyaApi api, ParameterHandler parameterHandler, QueryViewController queryViewController) {
        this.api = api;
        this.parser = new HttpRequestParser(this.api);
        this.parameterHandler = parameterHandler;
        this.queryViewController = queryViewController;
    }

    public void init(int startValue, int endValue) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                historySize = endValue - startValue;
                progressDialog = new ProgressDialog("Parsing Parameters...");
                progressDialog.init();
            }
        });
        task = new ParseTask(startValue, endValue);
        task.addPropertyChangeListener(this);
        task.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            double historyProgress = (((double) progress * historySize / 100) - 1);
            progressDialog.updateProgressBarValue(progress);
            progressDialog.appendTaskOutput((String.format(
                    "Completed %d/%d of Burp history.\n", (int) historyProgress, historySize)));
        }
    }

    class ParseTask extends SwingWorker<Void, String> {

        private int startValue;
        private int endValue;

        public ParseTask(int startValue, int endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.execute();
        }

        @Override
        protected Void doInBackground() {
            try {
                List<ProxyHttpRequestResponse> proxyHttpRequestResponseList = api.proxy().history().subList(startValue - 1, endValue);
                List<HttpRequest> requestList = new ArrayList<>(proxyHttpRequestResponseList.stream().map(ProxyHttpRequestResponse::finalRequest).toList());

                int progress = 0;
                setProgress(0);
                int listSize = requestList.size();

                List<Object> parametersToSave = new ArrayList<>();

                for (int i = 0; i < requestList.size(); i++) {
                    HttpRequest request = requestList.get(i);

                    // Ignore requests out of scope
                    if (!api.scope().isInScope(request.url())) {
                        continue;
                    }

                    String messageHash = Hashing.sha1(request.toByteArray().getBytes());

                    progress = (int) ((i + 1) / (double) listSize * 100);
                    setProgress(progress);

                    var reqParsed = parser.parse(request);
                    if (reqParsed != null) {
                        parametersToSave.addAll(parameterHandler.addParametersThreaded(reqParsed.parameterHelpers, messageHash));
                    }
                }
                DBModel.saveBulk(parametersToSave);
                return null;
            } catch (Exception e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void done() {
            queryViewController.updateParameters();
            progressDialog.updateDialogDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            progressDialog.updateProgressBarValue(100);
            progressDialog.setTaskOutputText("Completed!\n");
        }
    }
}
