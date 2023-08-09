package scheduling.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WorkerCountUpdatedEvent extends Event {
    private int newCount;
}
