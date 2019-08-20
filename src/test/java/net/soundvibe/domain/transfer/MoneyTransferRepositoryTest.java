package net.soundvibe.domain.transfer;

import io.reactivex.disposables.Disposable;
import net.soundvibe.bus.*;
import net.soundvibe.domain.account.Account;
import net.soundvibe.domain.account.event.AccountDebited;
import net.soundvibe.domain.transfer.event.*;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MoneyTransferRepositoryTest {

    private final EventBus eventBus = new RxEventBus();
    private final MoneyTransferRepository sut = new MoneyTransferRepository();

    private Disposable disposable;

    @BeforeEach
    void setUp() {
        disposable = sut.subscribe(eventBus);
    }

    @AfterEach
    void tearDown() throws Exception {
        disposable.dispose();
        eventBus.close();
    }

    @Test
    void should_update_store_when_events_are_published() {
        var moneyTransferFailed1 = new MoneyTransferFailed("1", "cause1");
        var moneyTransferFailed2 = new MoneyTransferFailed("2", "cause2");
        var moneyTransferred = new MoneyTransferred("3", Money.of(100, "EUR"), "account1", "account2");
        eventBus.publish(moneyTransferFailed1);
        eventBus.publish(moneyTransferFailed2);
        eventBus.publish(moneyTransferred);
        eventBus.publish(moneyTransferred);
        eventBus.publish(new AccountDebited(Money.of(10, "EUR"),
                new Account("id", "foo", "bar", Money.of(0, "EUR"), null)));

        assertEquals(Optional.of(moneyTransferFailed1), sut.findById(moneyTransferFailed1.transferId));
        assertEquals(Optional.of(moneyTransferFailed2), sut.findById(moneyTransferFailed2.transferId));
        assertEquals(Optional.of(moneyTransferred), sut.findById(moneyTransferred.transferId));
        assertEquals(Optional.empty(), sut.findById("unknownId"));
    }
}