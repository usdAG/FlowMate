package scheduling.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SubscribeEvent extends Event {

    private Object subscriber;

}
