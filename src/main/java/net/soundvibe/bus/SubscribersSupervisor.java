package net.soundvibe.bus;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public final class SubscribersSupervisor {

    private static final Logger log = LoggerFactory.getLogger(SubscribersSupervisor.class);

    private final List<EventBusSubscriber> subscribers;
    private Map<EventBusSubscriber, Disposable> entries = Map.of();
    private Disposable subscription;

    public SubscribersSupervisor(List<EventBusSubscriber> subscribers) {
        this.subscribers = subscribers;
        Runtime.getRuntime().addShutdownHook(new Thread(this::unSubscribe));
    }

    public void subscribe(EventBus eventBus) {
        unSubscribe();

        entries = subscribers.stream().collect(toMap(identity(), e -> e.subscribe(eventBus)));

        subscription = Flowable.interval(1L, 1L, TimeUnit.MINUTES)
                .flatMapIterable(i -> entries.entrySet())
                .filter(entry -> entry.getValue().isDisposed())
                .doOnNext(entry -> log.warn("Detected disposed subscriber. Resubscribing to event bus..."))
                .subscribe(entry -> reSubscribe(entry, eventBus));
        log.info("Subscribed to event bus successfully");
    }

    public void unSubscribe() {
        Optional.ofNullable(subscription).ifPresent(Disposable::dispose);
        entries.values().forEach(Disposable::dispose);
        log.info("UnSubscribed from event bus successfully");
    }

    public boolean isHealthy() {
        return entries.values().stream().noneMatch(Disposable::isDisposed);
    }

    private synchronized void reSubscribe(Map.Entry<EventBusSubscriber, Disposable> entry, EventBus eventBus) {
        final Disposable disposable = entry.getKey().subscribe(eventBus);
        entries.put(entry.getKey(), disposable);
        log.info("Resubscribed to event bus successfully");
    }

}
