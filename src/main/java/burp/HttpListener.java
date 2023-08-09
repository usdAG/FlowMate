package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import controller.SessionViewController;
import db.DBModel;
import db.MatchHandler;
import db.ParameterHandler;
import session.SessionParameter;
import utils.Hashing;

import java.util.ArrayList;
import java.util.List;

import audit.CrossSessionAudit;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;

public class HttpListener implements HttpHandler {
    private HttpRequestParser reqParser;
    private HttpResponseParser respParser;
    private MontoyaApi api;
    public static boolean detectionIsActive = false;
    public static boolean hasActiveSession = false;
    private static List<SessionParameter> monitoredParameter;
    public ParameterHandler parameterHandler;
    public MatchHandler matchHandler;
    private String messageHash;
    private int messageId;


    public HttpListener(MontoyaApi api, CrossSessionAudit crossSessionAuditor) {
        this.api = api;
        this.reqParser = new HttpRequestParser(this.api);
        this.respParser = new HttpResponseParser(this.api);
        this.parameterHandler = new ParameterHandler();
        this.matchHandler = new MatchHandler(this.parameterHandler, crossSessionAuditor);
        this.messageId = 0;
        this.messageHash = "";
        monitoredParameter = new ArrayList<>();
    }


    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if(detectionIsActive) {
            // Ignores the probe request as the originate from toolFlag 1024 == Burp Extender (Alternative could be by using the User-Agent header in the request)
            if(requestToBeSent.toolSource().isFromTool(ToolType.EXTENSIONS) || requestToBeSent.toolSource().isFromTool(ToolType.REPEATER))
                return continueWith(requestToBeSent);

            // Ignore requests out of scope
            if(!api.scope().isInScope(requestToBeSent.url()))
                return continueWith(requestToBeSent);

            this.messageHash = Hashing.sha1(requestToBeSent.toByteArray().getBytes());

            var reqParsed = this.parseRequest(requestToBeSent);

            if (reqParsed != null) {
                parameterHandler.addParameters(reqParsed.parameterHelpers, messageHash);
            }
            if (hasActiveSession) {
                this.monitorSessionParameters(requestToBeSent);
            }
        }
        return continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        if (detectionIsActive) {
            // Ignores the probe request as the originate from toolFlag 1024 == Burp Extender (Alternative could be by using the User-Agent header in the request)
            if (responseReceived.toolSource().isFromTool(ToolType.EXTENSIONS) || responseReceived.toolSource().isFromTool(ToolType.REPEATER))
                return ResponseReceivedAction.continueWith(responseReceived);

            var reqParsed = this.parseRequest(responseReceived.initiatingRequest());
            var respParsed = this.parseResponse(responseReceived, responseReceived.initiatingRequest());

            // Ignore requests out of scope
            if(!api.scope().isInScope(responseReceived.initiatingRequest().url()))
                return ResponseReceivedAction.continueWith(responseReceived);

            var requestDomain = reqParsed.Url.getHost();
            // Searching for matching parameters
            var newMatches = respParser.getMatches(respParsed, this.parameterHandler.getRelevant(requestDomain), messageHash);
            List<Object> matchesList = matchHandler.addMatches(newMatches);
            DBModel.saveBulk(matchesList);

            SessionViewController.updateParameterListInActiveSession();
        }
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    private HttpRequest parseRequest(burp.api.montoya.http.message.requests.HttpRequest request){
        return reqParser.parse(request);
    }

    private HttpResponse parseResponse(HttpResponseReceived responseReceived, burp.api.montoya.http.message.requests.HttpRequest associatedRequest){
        return respParser.parseResponse(responseReceived, associatedRequest);
    }

    private void monitorSessionParameters(HttpRequestToBeSent request) {
        List<ParsedHttpParameter> params = request.parameters();
        List<ParsedHttpParameter> changedParams = new ArrayList<>();
        for (ParsedHttpParameter param : params) {
           for (SessionParameter sessionParam : monitoredParameter) {
               if (param.name().equals(sessionParam.getName()) && !param.value().equals(sessionParam.getValue())) {
                   changedParams.add(param);
               }
           }
        }
        if (!changedParams.isEmpty())
            // -1 because the method call comes from the requests-listener. Hence, the response id where the InputParameter changed is the response before this request
            SessionViewController.createSessionFromMonitor(changedParams, this.messageId);
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public void setMessageHash(String messageHash) {
        this.messageHash = messageHash;
    }

    public static void setMonitoredParameter(List<SessionParameter> params) {
        monitoredParameter = params;
    }
}
