package burp;

import audit.*;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import controller.SessionViewController;
import db.DBModel;
import db.MatchHandler;
import db.ParameterHandler;
import db.entities.InputParameter;
import db.entities.SessionParameter;
import utils.Hashing;

import java.util.ArrayList;
import java.util.List;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;

public class HttpListener implements HttpHandler {
    private HttpRequestParser reqParser;
    public HttpResponseParser respParser;
    private MontoyaApi api;
    public static boolean detectionIsActive = false;
    public static boolean hasActiveSession = false;
    public static boolean liveMatchingIsActive = true;
    public static List<SessionParameter> monitoredParameter;
    public ParameterHandler parameterHandler;
    public MatchHandler matchHandler;
    private String messageHash;
    private int messageId;


    public HttpListener(MontoyaApi api, CrossSessionAudit crossSessionAuditor, CrossContentTypeAudit crossContentTypeAuditor,
                        CrossScopeAudit crossScopeAuditor, HeaderMatchAudit headerMatchAuditor, LongDistanceMatchAudit longDistanceMatchAuditor,
                        KeywordMatchAudit keywordMatchAuditor) {
        this.api = api;
        this.reqParser = new HttpRequestParser(this.api);
        this.respParser = new HttpResponseParser(this.api);
        this.parameterHandler = new ParameterHandler();
        this.matchHandler = new MatchHandler(this.parameterHandler, crossSessionAuditor, crossContentTypeAuditor,
                crossScopeAuditor, headerMatchAuditor, longDistanceMatchAuditor, keywordMatchAuditor, api);
        this.messageId = 0;
        this.messageHash = "";
        monitoredParameter = new ArrayList<>();
    }


    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (detectionIsActive) {
            // Ignores the probe request as the originate from toolFlag 1024 == Burp Extender (Alternative could be by using the User-Agent header in the request)
            if (requestToBeSent.toolSource().isFromTool(ToolType.EXTENSIONS) || requestToBeSent.toolSource().isFromTool(ToolType.REPEATER))
                return continueWith(requestToBeSent);

            // Ignore requests out of scope
            if (!api.scope().isInScope(requestToBeSent.url()))
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
        if (detectionIsActive && liveMatchingIsActive) {
            // Ignores the probe request as the originate from toolFlag 1024 == Burp Extender (Alternative could be by using the User-Agent header in the request)
            if (responseReceived.toolSource().isFromTool(ToolType.EXTENSIONS) || responseReceived.toolSource().isFromTool(ToolType.REPEATER))
                return ResponseReceivedAction.continueWith(responseReceived);

            // Ignore requests out of scope
            if (!api.scope().isInScope(responseReceived.initiatingRequest().url()))
                return ResponseReceivedAction.continueWith(responseReceived);

            var reqParsed = this.parseRequest(responseReceived.initiatingRequest());
            var respParsed = this.parseResponse(responseReceived, responseReceived.initiatingRequest());

            var requestDomain = reqParsed.Url.getHost();
            // Searching for matching parameters
            List<InputParameter> noiseReeducatedParameters = this.parameterHandler.getRelevant(requestDomain);
            var newMatches = respParser.getMatches(respParsed, noiseReeducatedParameters, messageHash);
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

    public static void setLiveMatchingIsActive(boolean isActive) {
        liveMatchingIsActive = isActive;
    }

    public static void setHasActiveSession(boolean hasActiveSession) {
        HttpListener.hasActiveSession = hasActiveSession;
    }
}
