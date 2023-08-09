package scheduling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for methods which listen to events dispatched by the EventLoop
// * @see de.usd.cooperator.scheduling.EventLoop
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {

    boolean longRunning() default false;

    boolean useUiThead() default false;
}