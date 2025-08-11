package burp;

import audit.*;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.proxy.http.*;
import controller.NoiseReductionController;
import controller.QueryViewController;
import controller.SessionViewController;
import db.DBModel;
import db.DeferMatching;
import db.InitDB;
import db.Neo4JDB;
import gui.*;
import model.NoiseReductionModel;
import model.QueryViewModel;
import model.SessionViewModel;
import utils.FileSystemUtil;
import utils.Logger;

import javax.swing.*;
import java.io.IOException;

public class BurpExtender implements BurpExtension  {

    private JTabbedPane tabbedPane;

    private GettingStartedView gettingStartedView;
    private AuditFindingView auditFindingView;
    private GuideView guideView;
    private QueryView queryView;
    private QueryViewModel queryViewModel;
    private QueryViewController queryViewController;
    private SessionView sessionView;
    private SessionViewModel sessionViewModel;
    private SessionViewController sessionViewController;
    private NoiseReductionView noiseReductionView;
    private NoiseReductionModel noiseReductionModel;
    private NoiseReductionController noiseReductionController;
    private CrossSessionAudit crossSessionAudit;
    private CrossContentTypeAudit crossContentTypeAudit;
    private CrossScopeAudit crossScopeAudit;
    private HeaderMatchAudit headerMatchAudit;
    private LongDistanceMatchAudit longDistanceMatchAudit;
    private KeywordMatchAudit keywordMatchAudit;
    private PropertiesHandler propertiesHandler;
    private RegexMatcher regexMatcher;
    private DeferMatching deferMatching;
    private HttpListener listener;
    private RetroactiveParser retroactiveParser;
    private MontoyaApi api;

    public Neo4JDB neo4j;
    public static DBModel dbModel;
    public static int historyStart;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("FlowMate");

        Logger.initialize(api.logging());
        Logger.getInstance().logToOutput("Logging initialized successfully");

        try {
            InitDB initDB = new InitDB();
            dbModel = initDB.start(FileSystemUtil.initDatabaseDirectory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.propertiesHandler = new PropertiesHandler(this.api);
        this.auditFindingView = new AuditFindingView(this.api, propertiesHandler);
        this.crossSessionAudit = new CrossSessionAudit(this.auditFindingView);
        this.crossContentTypeAudit = new CrossContentTypeAudit(this.auditFindingView);
        this.crossScopeAudit = new CrossScopeAudit(this.auditFindingView);
        this.headerMatchAudit = new HeaderMatchAudit(this.auditFindingView);
        this.longDistanceMatchAudit = new LongDistanceMatchAudit(this.auditFindingView, this.api);
        this.keywordMatchAudit = new KeywordMatchAudit(this.auditFindingView);
        this.regexMatcher = new RegexMatcher(this.propertiesHandler);
        this.listener = new HttpListener(api, crossSessionAudit, crossContentTypeAudit, crossScopeAudit, headerMatchAudit, longDistanceMatchAudit, keywordMatchAudit);
        this.deferMatching = new DeferMatching(this.api, this.listener.respParser, this.listener.parameterHandler, this.listener.matchHandler);
        api.http().registerHttpHandler(this.listener);

        //Setup UI components
        noiseReductionView = new NoiseReductionView();
        noiseReductionModel = new NoiseReductionModel(this.propertiesHandler, this.listener.matchHandler, this.listener.parameterHandler, this.deferMatching, this.regexMatcher);
        noiseReductionController = new NoiseReductionController(noiseReductionView, noiseReductionModel, this.auditFindingView);
        guideView = new GuideView();
        sessionView = new SessionView(this.api, this.listener.parameterHandler, this.listener.matchHandler);
        sessionViewModel = new SessionViewModel(this.api, this.listener.parameterHandler, this.listener.matchHandler);
        sessionViewController = new SessionViewController(this.api, this.sessionView, this.sessionViewModel, this.listener.matchHandler, this.listener.parameterHandler, this.crossSessionAudit);
        queryView = new QueryView(this.listener.parameterHandler, this.listener.matchHandler, this.api);
        queryViewModel = new QueryViewModel();
        queryViewController = new QueryViewController(api, queryView, queryViewModel, this.listener.parameterHandler, this.listener.matchHandler, sessionViewController);
        this.retroactiveParser = new RetroactiveParser(this.api, this.listener.parameterHandler, this.queryViewController);
        gettingStartedView = new GettingStartedView(this.api, this.propertiesHandler, this.deferMatching, this.listener.parameterHandler,
                this.listener.matchHandler, this.noiseReductionController, this.queryViewController, this.auditFindingView, this.sessionViewController, this.retroactiveParser);

        this.api.userInterface().registerContextMenuItemsProvider(new HistoryContextMenu(this.api, gettingStartedView));

        if (this.propertiesHandler.isFirstTimeLoading) {
            historyStart = this.api.proxy().history().size() + 1;
            this.propertiesHandler.saveHistoryStartValueInState(historyStart);
        } else {
            historyStart = this.propertiesHandler.loadHistoryStartValueInState();
        }

        noiseReductionController.addRuleContainerListener(queryViewController);
        deferMatching.addDeferMatchingFinishedListener(sessionViewController);
        deferMatching.addDeferMatchingFinishedListener(queryViewController);
        listener.addItemsAddedListener(queryViewController);

        api.extension().registerUnloadingHandler(new MyExtensionUnloadHandler());

        // ProxyRequestHandler to get the messageID for the burp history
        // Added them here because it somehow ensures that they are called in the right order
        api.proxy().registerRequestHandler(new ProxyRequestHandler() {
            @Override
            public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
                // messageID + 1 because it starts counting at 0 but burp history starts at 1
                int id = interceptedRequest.messageId() + 1;
                int historySize = api.proxy().history().size();
                // sometimes the id might is out of sync for some reason
                if (interceptedRequest.messageId() < historySize) {
                    id = historySize;
                }
                listener.setMessageId(id);
                if (HttpListener.hasActiveSession) {
                    sessionViewController.updateIdForLastSession(id);
                }
                return null;
            }

            @Override
            public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
                return null;
            }
        });

        // this Handler needs to stay here even though it's not really doing anything, but otherwise we receive wrong message IDs
        api.proxy().registerResponseHandler(new ProxyResponseHandler() {
            @Override
            public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
                // messageID + 1 because it starts counting at 0 but burp history starts at 1
                int id = interceptedResponse.messageId() + 1;
                int historySize = api.proxy().history().size();
//                // sometimes the id might is out of sync for some reason
                if (interceptedResponse.messageId() < historySize) {
                    id = historySize;
                }
                return null;
            }

            @Override
            public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
                return null;
            }
        });

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                tabbedPane = new JTabbedPane();
                tabbedPane.add("Getting Started", gettingStartedView);
                tabbedPane.addTab("Guides", guideView);
                tabbedPane.add("Noise Reduction", noiseReductionView);
                tabbedPane.add("Audit Findings", auditFindingView);
                tabbedPane.addTab("Query", queryView);
                tabbedPane.add("Sessions", sessionView);

                // add the custom tab to Burp's UI
                api.userInterface().registerSuiteTab("FlowMate", tabbedPane);
            }
        });

        Logger.getInstance().logToOutput("FlowMate started and ready!");
    }

    // shutdown neo4J database on unloading the extension
    private class MyExtensionUnloadHandler implements ExtensionUnloadingHandler {
        @Override
        public void extensionUnloaded() {
            if (dbModel != null)
                dbModel.closeSession();
            if(Neo4JDB.managementService != null)
                Neo4JDB.shutDown();
            
            Logger.getInstance().logToOutput("FlowMate shutdown completely.");
        }
    }
}
