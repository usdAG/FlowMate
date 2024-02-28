package db;

import audit.*;
import burp.api.montoya.MontoyaApi;
import db.entities.MatchValue;
import db.entities.ParameterMatch;
import db.entities.Session;
import db.entities.Url;
import gui.GettingStartedView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.SessionViewModel;
import utils.Logger;

import java.util.*;

// Handles the logic behind new-found matches
public class MatchHandler {

    private ParameterHandler parameterHandler;
    public Hashtable<Integer, MatchValue> matchValueStorage;
    public Hashtable<Integer, ParameterMatch> parameterMatchStorage;
    public ObservableList<ParameterMatch> observableParameterMatchList;
    public ObservableList<ParameterMatch> observableParameterMatchListSession;
    private boolean hasActiveSession = false;
    private String sessionName;
    private CrossSessionAudit crossSessionAudit;
    private CrossContentTypeAudit crossContentTypeAudit;
    private CrossScopeAudit crossScopeAudit;
    private HeaderMatchAudit headerMatchAudit;
    private LongDistanceMatchAudit longDistanceMatchAudit;
    private KeywordMatchAudit keywordMatchAudit;
    private MontoyaApi api;

    public MatchHandler(ParameterHandler parameterHandler, CrossSessionAudit crossSessionAudit, CrossContentTypeAudit crossContentTypeAudit,
                        CrossScopeAudit crossScopeAudit, HeaderMatchAudit headerMatchAudit, LongDistanceMatchAudit longDistanceMatchAudit,
                        KeywordMatchAudit keywordMatchAudit, MontoyaApi api) {
        this.matchValueStorage = new Hashtable<>();
        this.parameterMatchStorage = new Hashtable<>();
        this.parameterHandler = parameterHandler;
        this.observableParameterMatchList = FXCollections.observableArrayList();
        this.observableParameterMatchListSession = FXCollections.observableArrayList();
        this.crossSessionAudit = crossSessionAudit;
        this.crossContentTypeAudit = crossContentTypeAudit;
        this.crossScopeAudit = crossScopeAudit;
        this.headerMatchAudit = headerMatchAudit;
        this.longDistanceMatchAudit = longDistanceMatchAudit;
        this.keywordMatchAudit = keywordMatchAudit;
        this.api = api;
        loadEntities();
    }

    private void loadEntities() {
        List<ParameterMatch> parameterMatchEntityList = DBModel.loadAllMatchEntities().stream().toList();
        List<Integer> identifiersMatchEntities = parameterMatchEntityList.stream().map(ParameterMatch::getIdentifier).toList();
        parameterMatchStorage.putAll(combineListsIntoMatchEntityMap(identifiersMatchEntities, parameterMatchEntityList));
        this.observableParameterMatchList.addAll(parameterMatchEntityList);
        List<MatchValue> matchValueEntityList = DBModel.loadAllMatchEntryEntities().stream().toList();
        List<Integer> identifiersMatchEntryEntities = matchValueEntityList.stream().map(MatchValue::getIdentifier).toList();
        matchValueStorage.putAll(combineListsIntoMatchEntryEntityMap(identifiersMatchEntryEntities, matchValueEntityList));
    }

    Map<Integer, ParameterMatch> combineListsIntoMatchEntityMap (List<Integer> keys, List<ParameterMatch> values) {
        if (keys.size() != values.size()) {
            IllegalArgumentException exception = new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
            Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
            throw exception;
        }
        Map<Integer, ParameterMatch> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    Map<Integer, MatchValue> combineListsIntoMatchEntryEntityMap (List<Integer> keys, List<MatchValue> values) {
        if (keys.size() != values.size()) {
            IllegalArgumentException exception = new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
            Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
            throw exception;
        }
        Map<Integer, MatchValue> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    private List<Object> addMatchToDB(MatchHelperClass match) {
        List<Object> returnList = new ArrayList<>();

        String name = match.getName();
        String value = match.getValue();
        String type = match.getType();
        String responseContentType = match.getResponseContentType();
        String matchProof = match.getMatchProof();
        String messageHash = match.getMessageHash();
        String url = match.getUrl();

        // Check if a match was created for an out-of-scope URL, if yes, return since then there is no db entry for the URL
        if (!this.api.scope().isInScope(url))
            return returnList;

        ParameterMatch newParameterMatchEntity = new ParameterMatch(name, value, type, messageHash, url, match.getInputValueObj());
        MatchValue newMatchValueEntity = new MatchValue(name, value, responseContentType, matchProof, url, messageHash);

        Session session = null;

        if (hasActiveSession) {
            newParameterMatchEntity = new ParameterMatch(name, value, type, messageHash, url, this.sessionName, match.getInputValueObj());
            newMatchValueEntity = new MatchValue(name, value, responseContentType, matchProof, url, messageHash, this.sessionName);

            session = SessionViewModel.sessionTable.get(this.sessionName);
            this.crossSessionAudit.performAudit(newParameterMatchEntity, this.sessionName, match);
        }

        newParameterMatchEntity.addMatchEntryEntity(newMatchValueEntity);

        this.crossContentTypeAudit.performAudit(match);
        this.crossScopeAudit.performAudit(match);
        this.headerMatchAudit.performAudit(match);
        this.keywordMatchAudit.performAudit(match);
        this.longDistanceMatchAudit.performAudit(match, newMatchValueEntity, this);

        Url matchingUrlEntity = getMatchingUrlEntity(url);

        // matchingUrlEntity = null means there is no entry for it in the db and further continuing will lead to an error
        if (matchingUrlEntity == null)
            return returnList;

        if (!matchEntryExistsInDB(newMatchValueEntity)) {
            if (!matchEntityExistsInUrlEntity(matchingUrlEntity, name, value)) {
                matchingUrlEntity.addFound(newParameterMatchEntity);
                parameterMatchStorage.put(newParameterMatchEntity.getIdentifier(), newParameterMatchEntity);
                this.matchValueStorage.put(newMatchValueEntity.getIdentifier(), newMatchValueEntity);
                GettingStartedView.numberOfParameterMatches.setText(String.valueOf(parameterMatchStorage.size()));
                GettingStartedView.numberOfMatchValues.setText(String.valueOf(matchValueStorage.size()));
                // If a session is active add entities to the session and save the session and Url entity, otherwise save both ParameterMatch and Url entity
                if (session != null) {
                    session.addMatch(newParameterMatchEntity);
                    session.addMatchValue(newMatchValueEntity);
                    returnList.add(session);
                    returnList.add(matchingUrlEntity);
                    SessionViewModel.sessionTable.put(this.sessionName, session);
                } else {
                    returnList.add(newParameterMatchEntity);
                    returnList.add(matchingUrlEntity);
                }
            } else {
                ParameterMatch relatedParameterMatchEntity = getMatchEntityFromUrlEntity(name, value, url);
                relatedParameterMatchEntity.addMatchEntryEntity(newMatchValueEntity);
                this.matchValueStorage.put(newMatchValueEntity.getIdentifier(), newMatchValueEntity);
                GettingStartedView.numberOfMatchValues.setText(String.valueOf(matchValueStorage.size()));
                if (session != null) {
                    session.addMatch(relatedParameterMatchEntity);
                    session.addMatchValue(newMatchValueEntity);
                    returnList.add(session);
                    SessionViewModel.sessionTable.put(this.sessionName, session);
                } else {
                    returnList.add(relatedParameterMatchEntity);
                }
            }
            this.observableParameterMatchList.add(newParameterMatchEntity);
            if (newParameterMatchEntity.getSession().equals(sessionName)) {
                this.observableParameterMatchListSession.add(newParameterMatchEntity);
            }
        }
        return returnList;
    }

    public List<Object> addMatches(List<MatchHelperClass> matches) {
        List<Object> returnList = new ArrayList<>();
        for (MatchHelperClass match : matches) {
            returnList.addAll(addMatchToDB(match));
        }
        this.crossSessionAudit.renderFindings();
        return returnList;
    }

    private boolean matchEntryExistsInDB(MatchValue matchValueEntity) {
        int identifier = Objects.hash(matchValueEntity.getName(), matchValueEntity.getValue(), matchValueEntity.getMatchProof(),
                matchValueEntity.getUrl(), "not set");
        if (hasActiveSession) {
            identifier = Objects.hash(matchValueEntity.getName(), matchValueEntity.getValue(), matchValueEntity.getMatchProof(),
                    matchValueEntity.getUrl(), matchValueEntity.getSession());
        }
        return this.matchValueStorage.containsKey(identifier);
    }

    public boolean matchEntityExistsInUrlEntity(Url urlEntity, String name, String value) {
        int identifier = Objects.hash(name, value, urlEntity.getUrl());
        if (hasActiveSession) {
            identifier = Objects.hash(name, value, urlEntity.getUrl(), this.sessionName);
        }
        List<ParameterMatch> parameterMatchEntityList = urlEntity.getFound();
        return parameterMatchEntityList.stream().map(ParameterMatch::getIdentifier).toList().contains(identifier);
    }

    private ParameterMatch getMatchEntityFromUrlEntity(String name, String value, String url) {
        int identifier = Objects.hash(name, value, url);
        if (hasActiveSession) {
            identifier = Objects.hash(name, value, url, this.sessionName);
        }
        return parameterMatchStorage.get(identifier);
    }

    public Url getMatchingUrlEntity(String url) {
        Hashtable<Integer, Url> urlEntityStorage = parameterHandler.getUrlStorage();
        int identifier = Objects.hash(url);
        return urlEntityStorage.get(identifier);
    }

    public void setHasActiveSession(boolean isActive) {
        this.hasActiveSession = isActive;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void clearAllStorages() {
        matchValueStorage.clear();
        parameterMatchStorage.clear();
        observableParameterMatchList.clear();
        observableParameterMatchListSession.clear();
    }
}
