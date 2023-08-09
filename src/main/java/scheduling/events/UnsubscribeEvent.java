package scheduling.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UnsubscribeEvent extends Event {

    private Object subscriber;

}
