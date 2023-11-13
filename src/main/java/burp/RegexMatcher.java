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

    public static List<InputParameter> excludeParameters(List<InputParameter> parameters) {
        List<InputParameter> parameterList = new ArrayList<>();
        for (InputParameter parameter : parameters) {
            for (InputValue inputValue : parameter.getOccurrenceEntities()) {
                for (RuleContainer rule : ruleList) {
                    if (rule.isActive()) {
                        Pattern pattern = Pattern.compile(rule.getRegex(), rule.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);

                        if (rule.affectsParameterNames()) {
                            // Apply the rule on parameter names
                            if (rule.affectsQueryString() && parameter.getType().equals("GET")) {
                                Matcher matcher = pattern.matcher(parameter.getName());
                                if (matcher.find()) {
                                    parameter.setExcludedByNoiseReduction(true);
                                    break;
                                }
                            } else if (rule.affectsBody() && parameter.getType().equals("POST")) {
                                Matcher matcher = pattern.matcher(parameter.getName());
                                if (matcher.find()) {
                                    parameter.setExcludedByNoiseReduction(true);
                                    break;
                                }
                            } else if (rule.affectsCookie() && parameter.getType().equals("COOKIE")) {
                                Matcher matcher = pattern.matcher(parameter.getName());
                                if (matcher.find()) {
                                    parameter.setExcludedByNoiseReduction(true);
                                    break;
                                }
                            }
                        }

                        if (rule.affectsParameterValues()) {
                            // Apply the rule on parameter values
                            if (rule.affectsQueryString() && inputValue.getType().equals("GET")) {
                                // Apply the rule on HTTP headers
                                Matcher matcher = pattern.matcher(inputValue.getValue());
                                if (matcher.find()) {
                                    inputValue.setExcludedByNoiseReduction(true);
                                    break;
                                }
                            } else if (rule.affectsBody() && inputValue.getType().equals("POST")) {
                                // Apply the rule on the response body
                                Matcher matcher = pattern.matcher(inputValue.getValue());
                                if (matcher.find()) {
                                    inputValue.setExcludedByNoiseReduction(true);
                                    break;
                                }
                            } else if (rule.affectsCookie() && inputValue.getType().equals("COOKIE")) {
                                // Apply the rule on cookies
                                Matcher matcher = pattern.matcher(inputValue.getValue());
                                if (matcher.find()) {
                                    inputValue.setExcludedByNoiseReduction(true);
                                    break;
                                }
                            }
                        }
                    } else {
                        parameter.setExcludedByNoiseReduction(false);
                        inputValue.setExcludedByNoiseReduction(false);
                    }
                }
            }
            parameterList.add(parameter);
        }
        return parameterList;
    }

    public static List<InputValue> excludeInputValues(List<InputValue> inputValues) {
        List<InputValue> returnList = new ArrayList<>();
        for (InputValue inputValue : inputValues) {
            for (RuleContainer rule : ruleList) {
                if (rule.isActive()) {
                    Pattern pattern = Pattern.compile(rule.getRegex(), rule.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
                    if (rule.affectsParameterValues()) {
                        // Apply the rule on parameter values
                        if (rule.affectsQueryString() && inputValue.getType().equals("GET")) {
                            // Apply the rule on HTTP headers
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(true);
                                break;
                            }
                        } else if (rule.affectsBody() && inputValue.getType().equals("POST")) {
                            // Apply the rule on the response body
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(true);
                                break;
                            }
                        } else if (rule.affectsCookie() && inputValue.getType().equals("COOKIE")) {
                            // Apply the rule on cookies
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(true);
                                break;
                            }
                        }
                    }
                } else {
                    inputValue.setExcludedByNoiseReduction(false);
                }
            }
            returnList.add(inputValue);
        }
        return returnList;
    }

    public static List<InputParameter> excludeParametersForSingleRule(List<InputParameter> parameters, RuleContainer rule) {
        List<InputParameter> parameterList = new ArrayList<>();
        for (InputParameter parameter : parameters) {
            for (InputValue inputValue : parameter.getOccurrenceEntities()) {
                if (rule.isActive()) {
                    Pattern pattern = Pattern.compile(rule.getRegex(), rule.isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
                    if (rule.affectsParameterNames()) {
                        // Apply the rule on parameter names
                        if (rule.affectsQueryString() && parameter.getType().equals("GET")) {
                            Matcher matcher = pattern.matcher(parameter.getName());
                            if (matcher.find()) {
                                parameter.setExcludedByNoiseReduction(true);
                                break;
                            }
                        } else if (rule.affectsBody() && parameter.getType().equals("POST")) {
                            Matcher matcher = pattern.matcher(parameter.getName());
                            if (matcher.find()) {
                                parameter.setExcludedByNoiseReduction(true);
                                break;
                            }
                        } else if (rule.affectsCookie() && parameter.getType().equals("COOKIE")) {
                            Matcher matcher = pattern.matcher(parameter.getName());
                            if (matcher.find()) {
                                parameter.setExcludedByNoiseReduction(true);
                                break;
                            }
                        }
                    }

                    if (rule.affectsParameterValues()) {
                        // Apply the rule on parameter values
                        if (rule.affectsQueryString() && inputValue.getType().equals("GET")) {
                            // Apply the rule on HTTP headers
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(true);
                                break;
                            }
                        } else if (rule.affectsBody() && inputValue.getType().equals("POST")) {
                            // Apply the rule on the response body
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(true);
                                break;
                            }
                        } else if (rule.affectsCookie() && inputValue.getType().equals("COOKIE")) {
                            // Apply the rule on cookies
                            Matcher matcher = pattern.matcher(inputValue.getValue());
                            if (matcher.find()) {
                                inputValue.setExcludedByNoiseReduction(true);
                                break;
                            }
                        }
                    }
                } else {
                    parameter.setExcludedByNoiseReduction(false);
                    inputValue.setExcludedByNoiseReduction(false);
                }
            }
            parameterList.add(parameter);
        }
        return parameterList;
    }
}
