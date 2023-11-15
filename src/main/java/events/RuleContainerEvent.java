package events;

import gui.container.RuleContainer;

import java.util.EventObject;

public class RuleContainerEvent extends EventObject {

    private RuleContainer ruleContainer;
    private boolean deleteAction;
    public RuleContainerEvent(Object source, RuleContainer ruleContainer, boolean deleteAction) {
        super(source);
        this.ruleContainer = ruleContainer;
        this.deleteAction = deleteAction;
    }

    public RuleContainer getRuleContainer() {
        return ruleContainer;
    }

    public boolean isDeleteAction() {
        return deleteAction;
    }
}
