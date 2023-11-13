package events;

import gui.container.RuleContainer;

import java.util.EventObject;

public class RuleContainerEvent extends EventObject {

    private RuleContainer ruleContainer;
    public RuleContainerEvent(Object source, RuleContainer ruleContainer) {
        super(source);
        this.ruleContainer = ruleContainer;
    }

    public RuleContainer getRuleContainer() {
        return ruleContainer;
    }
}
