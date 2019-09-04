package net.soundvibe.domain.transfer;

import io.reactivex.disposables.Disposable;
import net.soundvibe.bus.*;
import net.soundvibe.domain.base.Event;
import net.soundvibe.domain.transfer.event.*;
import net.soundvibe.domain.transfer.event.error.MoneyTransferFailed;

import java.util.Map;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

public class MoneyTransferRepository implements EventBusSubscriber {

    private final Map<String, Event> events = new ConcurrentHashMap<>();

    @Override
    public Disposable subscribe(EventBus eventBus) {
        return eventBus.observeEvents()
                .subscribe(this::updateStore);
    }

    public Optional<Event> findById(String transferId) {
        return Optional.ofNullable(events.get(transferId));
    }

    private void updateStore(Event event) {
        Match(event).option(
                Case($(instanceOf(MoneyTransferFailed.class)),
                        moneyTransferFailed -> events.putIfAbsent(moneyTransferFailed.transferId, moneyTransferFailed)),
                Case($(instanceOf(MoneyTransferred.class)),
                        moneyTransferred -> events.putIfAbsent(moneyTransferred.transferId, moneyTransferred))
        );
    }
}
