package model;

import burp.PropertiesHandler;
import burp.RegexMatcher;
import db.DBModel;
import db.DeferMatching;
import db.MatchHandler;
import db.ParameterHandler;
import gui.GettingStartedView;
import gui.container.RuleContainer;

import java.util.Map;
import java.util.Vector;

public class NoiseReductionModel {

    private PropertiesHandler propertiesHandler;
    private MatchHandler matchHandler;
    private ParameterHandler parameterHandler;
    private DeferMatching deferMatching;
    private RegexMatcher regexMatcher;
    private Vector<RuleContainer> ruleList;

    public NoiseReductionModel(PropertiesHandler propertiesHandler, MatchHandler matchHandler, ParameterHandler parameterHandler, DeferMatching deferMatching, RegexMatcher regexMatcher) {
        this.propertiesHandler = propertiesHandler;
        this.matchHandler = matchHandler;
        this.parameterHandler = parameterHandler;
        this.deferMatching = deferMatching;
        ruleList = new Vector<>();
        this.regexMatcher = regexMatcher;
    }
    public RuleContainer addRule(String name, String regex, boolean affectsName, boolean affectsValue,
                                         boolean affectsHeader, boolean affectsBody, boolean affectsCookie, boolean isActive, boolean caseInsensitive) {
        RuleContainer container = new RuleContainer(name, regex, affectsName, affectsValue, affectsHeader, affectsBody, affectsCookie, isActive, caseInsensitive);
        ruleList.add(container);
        updateRegexMatcherRuleList();
        return container;
    }

    public Vector<RuleContainer> getRuleList() {
        return ruleList;
    }

    public void setRuleList(Vector<RuleContainer> ruleList) {
        this.ruleList = ruleList;
    }

    public Vector<RuleContainer> loadRules() {
        Vector<RuleContainer> rules = new Vector<>(this.propertiesHandler.loadNoiseReductionRules());
        this.setRuleList(rules);
        updateRegexMatcherRuleList();
        return rules;
    }

    public void saveRuleInState(RuleContainer ruleContainer) {
        this.propertiesHandler.saveNoiseReductionRule(ruleContainer);
    }

    public void updateRuleInState(RuleContainer newRuleContainer, String oldRuleHash) {
        this.propertiesHandler.updateNoiseReductionRule(newRuleContainer, oldRuleHash);
        this.loadRules();
    }

    public void deleteRuleInState(RuleContainer ruleContainer) {
        this.propertiesHandler.deleteNoiseReductionRule(ruleContainer);
        this.loadRules();
    }

    private void updateRegexMatcherRuleList() {
        regexMatcher.updateRuleList(this.ruleList);
    }

    public void purgeDbAndStartRematch() {
        matchHandler.clearAllStorages();
        parameterHandler.clearAllMatchRelatedObjectsFromStorage();
        GettingStartedView.numberOfMatchValues.setText("0");
        GettingStartedView.numberOfParameterMatches.setText("0");
        String query1 = "Match (n:ParameterMatch) detach delete n";
        DBModel.executeCypher(query1, Map.of());
        String query2 = "Match (n:MatchValue) detach delete n";
        DBModel.executeCypher(query2, Map.of());
        SessionViewModel.deleteMatchesFromSession();
        this.deferMatching.init();
    }
}
