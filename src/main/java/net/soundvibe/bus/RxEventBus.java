package net.soundvibe.bus;

import io.reactivex.Flowable;
import io.reactivex.processors.*;
import net.soundvibe.domain.base.*;


public class RxEventBus implements EventBus {

    private final FlowableProcessor<Event> eventSubject = PublishProcessor.<Event>create().toSerialized();
    private final FlowableProcessor<Command> commandSubject = PublishProcessor.<Command>create().toSerialized();

    @Override
    public <E extends Event> void publish(E event) {
        eventSubject.onNext(event);
    }

    @Override
    public <C extends Command> void publish(C command) {
        commandSubject.onNext(command);
    }

    @Override
    public Flowable<Event> observeEvents() {
        return eventSubject;
    }

    @Override
    public Flowable<Command> observeCommands() {
        return commandSubject;
    }

    @Override
    public void close(){
        eventSubject.onComplete();
        commandSubject.onComplete();
    }
}
