package net.soundvibe.domain.account;

import io.reactivex.disposables.Disposable;
import net.soundvibe.bus.*;
import net.soundvibe.domain.account.event.*;
import net.soundvibe.domain.transfer.command.TransferMoney;
import net.soundvibe.domain.transfer.event.MoneyTransferFailed;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AccountProcessorTest {

    private final AccountRepository accountRepository = new AccountRepository();
    private final AccountProcessor sut = new AccountProcessor(accountRepository);
    private final EventBus eventBus = new RxEventBus();

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
    void should_transfer_money_to_another_account() {
        var accountFrom = setupAccountWithBalance(Money.of(100, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "EUR"));

        var amountToTransfer = Money.of(100, "EUR");

        var creditedTestSubscriber = eventBus.observeEvents(AccountCredited.class).test();
        var debitedTestSubscriber = eventBus.observeEvents(AccountDebited.class).test();

        eventBus.publish(new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer));

        creditedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new AccountCredited(amountToTransfer, accountFrom))
                .dispose();

        debitedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new AccountDebited(amountToTransfer, accountTo))
                .dispose();

        assertAccountBalance(Money.of(0, "EUR"), accountFrom.id);
        assertAccountBalance(amountToTransfer, accountTo.id);
    }

    @Test
    void should_calculate_correct_balance_when_transactions_are_duplicated() {
        var accountFrom = setupAccountWithBalance(Money.of(100, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "EUR"));

        var amountToTransfer = Money.of(100, "EUR");

        var creditedTestSubscriber = eventBus.observeEvents(AccountCredited.class).test();
        var debitedTestSubscriber = eventBus.observeEvents(AccountDebited.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);

        eventBus.publish(transferMoney);
        eventBus.publish(transferMoney);

        var expectedCredited = new AccountCredited(amountToTransfer, accountFrom);
        var expectedDebited = new AccountDebited(amountToTransfer, accountTo);

        creditedTestSubscriber
                .awaitCount(2)
                .assertNoErrors()
                .assertValues(expectedCredited, expectedCredited)
                .dispose();

        debitedTestSubscriber
                .awaitCount(2)
                .assertNoErrors()
                .assertValues(expectedDebited, expectedDebited)
                .dispose();

        assertAccountBalance(Money.of(0, "EUR"), accountFrom.id);
        assertAccountBalance(amountToTransfer, accountTo.id);

    }

    @Test
    void should_fail_the_transfer_when_insufficient_balance() {
        var accountFrom = setupAccountWithBalance(Money.of(10, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "EUR"));

        var amountToTransfer = Money.of(10.01, "EUR");

        var transferFailedTestSubscriber = eventBus.observeEvents(MoneyTransferFailed.class).test();

        eventBus.publish(new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer));

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValueCount(1)
                .dispose();

        assertAccountBalance(Money.of(10, "EUR"), accountFrom.id);
        assertAccountBalance(Money.of(0, "EUR"), accountTo.id);
    }

    @Test
    void should_fail_when_destination_account_is_not_opened() {
        var accountFrom = setupAccountWithBalance(Money.of(10, "EUR"));
        var accountTo = new Account("id", "test", "test", Money.of(0, "EUR"), null);
        var amountToTransfer = Money.of(5.50, "EUR");
        var transferFailedTestSubscriber = eventBus.observeEvents(MoneyTransferFailed.class).test();

        eventBus.publish(new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer));

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValueCount(1)
                .dispose();
    }

    @Test
    void should_fail_when_source_account_is_not_opened() {
        var accountFrom = new Account("id", "test", "test", Money.of(0, "EUR"), null);
        var accountTo = setupAccountWithBalance(Money.of(10, "EUR"));
        var amountToTransfer = Money.of(5.50, "EUR");
        var transferFailedTestSubscriber = eventBus.observeEvents(MoneyTransferFailed.class).test();

        eventBus.publish(new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer));

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValueCount(1)
                .dispose();
    }

    private void assertAccountBalance(Money expected, String accountId) {
        assertEquals(Optional.of(expected), accountRepository.findById(accountId).map(account -> account.balance));
    }

    private Account setupAccountWithBalance(Money initialBalance) {
        var account = new Account(UUID.randomUUID().toString(), "Foo" + ++index, "Bar" + index, initialBalance, null);
        return accountRepository.open(account);
    }

    private int index = 0;
}