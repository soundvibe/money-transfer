package net.soundvibe.bus;

import io.reactivex.Flowable;
import net.soundvibe.domain.base.*;
import org.slf4j.*;

public interface EventBus extends AutoCloseable {

    Logger log = LoggerFactory.getLogger(EventBus.class);

    <E extends Event> void publish(E event);
    <C extends Command> void publish(C command);

    Flowable<Event> observeEvents();
    Flowable<Command> observeCommands();

    default <E extends Event> Flowable<E> observeEvents(Class<E> eventClass) {
        return observeEvents().ofType(eventClass);
    }

    default <C extends Command> Flowable<C> observeCommands(Class<C> commandClass) {
        return observeCommands().ofType(commandClass);
    }
}
