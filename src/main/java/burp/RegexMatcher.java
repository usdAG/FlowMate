package burp;

import db.entities.InputParameter;
import db.entities.InputValue;
import gui.container.RuleContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatcher {
    private static List<RuleContainer> ruleList;
    private PropertiesHandler propertiesHandler;

    public RegexMatcher(PropertiesHandler propertiesHandler) {
        this.propertiesHandler = propertiesHandler;
        this.propertiesHandler.setDefaultRulesOnFirstLoad();
        ruleList = this.propertiesHandler.loadNoiseReductionRules();
    }

    public void updateRuleList(List<RuleContainer> containers) {
        ruleList = new ArrayList<>(containers);
    }

    public static void excludeParameter(InputParameter parameter) {
        boolean excluded = false;
        String parameterType = parameter.getType();
        for (RuleContainer rule : ruleList) {
            if (rule.isActive()) {
                Pattern pattern = Pattern.compile(rule.getRegex(), rule.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
                if (rule.affectsParameterNames()) {
                    if ((rule.affectsQueryString() && parameterType.equals("GET")) ||
                            (rule.affectsBody() && parameterType.equals("POST")) ||
                            (rule.affectsCookie() && parameterType.equals("COOKIE"))) {
                        Matcher matcher = pattern.matcher(parameter.getName());
                        if (matcher.find()) {
                            excluded = true;
                            break;
                        }
                    }
                }
            }
        }
        parameter.setExcludedByNoiseReduction(excluded);
    }

    public static void excludeInputValues(InputValue inputValue) {
        boolean excluded = false;
        String inputValueType = inputValue.getType();
        for (RuleContainer rule : ruleList) {
            if (rule.isActive() && rule.affectsParameterValues()) {
                Pattern pattern = Pattern.compile(rule.getRegex(), rule.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
                if (rule.affectsQueryString() && inputValueType.equals("GET") ||
                        (rule.affectsBody() && inputValueType.equals("POST")) ||
                        (rule.affectsCookie() && inputValueType.equals("COOKIE"))) {
                    Matcher matcher = pattern.matcher(inputValue.getValue());
                    if (matcher.find()) {
                        excluded = true;
                        break;
                    }
                }
            }
        }
        inputValue.setExcludedByNoiseReduction(excluded);
    }

    public static void excludeParametersForSingleRule(List<InputParameter> parameters, RuleContainer rule) {
        for (InputParameter parameter : parameters) {
            for (InputValue inputValue : parameter.getOccurrenceEntities()) {
                    Pattern pattern = Pattern.compile(rule.getRegex(), rule.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
                    if (rule.affectsParameterNames()) {
                        // Apply the rule on parameter names
                        if (rule.affectsQueryString() && parameter.getType().equals("GET")) {
                            Matcher matcher = pattern.matcher(parameter.getName());
                            if (matcher.find()) {
                                parameter.setExcludedByNoiseReduction(rule.isActive());
                            }
                        } else if (rule.affectsBody() && parameter.getType().equals("POST")) {
                            Matcher matcher = pattern.matcher(parameter.getName());
                            if (matcher.find()) {
                                parameter.setExcludedByNoiseReduction(rule.isActive());
                            }
                        } else if (rule.affectsCookie() && parameter.getType().equals("COOKIE")) {
                            Matcher matcher = pattern.matcher(parameter.getName());
                            if (matcher.find()) {
                                parameter.setExcludedByNoiseReduction(rule.isActive());
                            }
                        }
                    }

                    if (rule.affectsParameterValues()) {
                        // Apply the rule on parameter values
                        if (rule.affectsQueryString() && inputValue.getType().equals("GET")) {
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(rule.isActive());
                            }
                        } else if (rule.affectsBody() && inputValue.getType().equals("POST")) {
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(rule.isActive());
                            }
                        } else if (rule.affectsCookie() && inputValue.getType().equals("COOKIE")) {
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(rule.isActive());
                            }
                        }
                    }
            }
        }
    }
}
