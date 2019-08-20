package net.soundvibe.bus;

import io.reactivex.disposables.Disposable;

public interface EventBusSubscriber {

    Disposable subscribe(EventBus eventBus);

}
