package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.proxy.http.*;
import controller.QueryViewController;
import model.QueryViewModel;
import controller.SessionViewController;
import model.SessionViewModel;
import db.DBModel;
import db.InitDB;
import db.Neo4JDB;
import gui.AuditFindingView;
import gui.GettingStartedView;
import gui.GuideView;
import gui.QueryView;
import gui.SessionView;
import utils.FileSystemUtil;
import utils.Logger;

import javax.swing.*;

import audit.AuditFinding;
import audit.CrossSessionAudit;
import audit.AuditFinding.FindingSeverity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Vector;

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
    private CrossSessionAudit crossSessionAudit;
    private HttpListener listener;
    private MontoyaApi api;

    public Neo4JDB neo4j;
    public static DBModel dbModel;
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

        auditFindingView = new AuditFindingView(this.api);
        this.crossSessionAudit = new CrossSessionAudit(this.auditFindingView);
        this.listener = new HttpListener(api, crossSessionAudit);
        api.http().registerHttpHandler(this.listener);

        //Setup UI components
        gettingStartedView = new GettingStartedView(this.api);
        
        guideView = new GuideView();
        sessionView = new SessionView(this.api, this.listener.parameterHandler, this.listener.matchHandler);
        sessionViewModel = new SessionViewModel(this.api, this.listener.parameterHandler, this.listener.matchHandler);
        sessionViewController = new SessionViewController(this.api, this.sessionView, this.sessionViewModel, this.listener.matchHandler, this.listener.parameterHandler, this.crossSessionAudit);
        queryView = new QueryView(this.listener.parameterHandler, this.listener.matchHandler, this.api);
        queryViewModel = new QueryViewModel();
        queryViewController = new QueryViewController(api, queryView, queryViewModel, this.listener.parameterHandler, this.listener.matchHandler, sessionViewController);
        


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
                tabbedPane.add("Audit Findings", auditFindingView);
                tabbedPane.addTab("Guides", guideView);
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
