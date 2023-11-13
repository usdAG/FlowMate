package events;

import java.util.EventListener;

public interface RuleContainerListener extends EventListener {
    void onRuleChangeEvent(RuleContainerEvent event);
}
