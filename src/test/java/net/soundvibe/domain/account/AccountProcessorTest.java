package net.soundvibe.domain.account;

import io.reactivex.disposables.Disposable;
import net.soundvibe.bus.*;
import net.soundvibe.domain.account.event.*;
import net.soundvibe.domain.transfer.command.TransferMoney;
import net.soundvibe.domain.transfer.event.*;
import net.soundvibe.domain.transfer.event.error.*;
import org.javamoney.moneta.*;
import org.junit.jupiter.api.*;

import javax.money.CurrencyUnit;
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
        var moneyTransferredSubscriber = eventBus.observeEvents(MoneyTransferred.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        creditedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new AccountCredited(amountToTransfer, accountTo))
                .dispose();

        debitedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new AccountDebited(amountToTransfer, accountFrom))
                .dispose();

        moneyTransferredSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(MoneyTransferred.from(transferMoney))
                .dispose();

        assertAccountBalance(Money.of(0, "EUR"), accountFrom.id);
        assertAccountBalance(amountToTransfer, accountTo.id);
    }

    @Test
    void should_disallow_transfer_money_to_the_same_account() {
        var accountFrom = setupAccountWithBalance(Money.of(100, "EUR"));

        var amountToTransfer = Money.of(100, "EUR");

        var moneyTransferFailedSubscriber = eventBus.observeEvents(SameAccount.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountFrom.id, amountToTransfer);
        eventBus.publish(transferMoney);

        moneyTransferFailedSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new SameAccount(transferMoney.id, accountFrom.id))
                .dispose();

        assertAccountBalance(Money.of(100, "EUR"), accountFrom.id);
    }

    @Test
    void should_disallow_transfer_money_of_negative_amount() {
        var accountFrom = setupAccountWithBalance(Money.of(100, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(10, "EUR"));

        var amountToTransfer = Money.of(-100, "EUR");

        var moneyTransferFailedSubscriber = eventBus.observeEvents(NegativeOrZeroAmount.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        moneyTransferFailedSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new NegativeOrZeroAmount(transferMoney.id))
                .dispose();

        assertAccountBalance(Money.of(100, "EUR"), accountFrom.id);
        assertAccountBalance(Money.of(10, "EUR"), accountTo.id);
    }

    @Test
    void should_disallow_transfer_money_of_zero_amount() {
        var accountFrom = setupAccountWithBalance(Money.of(100, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(10, "EUR"));

        var amountToTransfer = Money.of(0, "EUR");

        var moneyTransferFailedSubscriber = eventBus.observeEvents(NegativeOrZeroAmount.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        moneyTransferFailedSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new NegativeOrZeroAmount(transferMoney.id))
                .dispose();

        assertAccountBalance(Money.of(100, "EUR"), accountFrom.id);
        assertAccountBalance(Money.of(10, "EUR"), accountTo.id);
    }

    @Test
    void should_calculate_correct_balance_and_fail_second_transaction_when_transactions_are_duplicated() {
        var accountFrom = setupAccountWithBalance(Money.of(100, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "EUR"));

        var amountToTransfer = Money.of(100, "EUR");

        var creditedTestSubscriber = eventBus.observeEvents(AccountCredited.class).test();
        var debitedTestSubscriber = eventBus.observeEvents(AccountDebited.class).test();
        var alreadyProcessedSubscriber = eventBus.observeEvents(MoneyTransferAlreadyProcessed.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);

        eventBus.publish(transferMoney);
        eventBus.publish(transferMoney);

        var expectedCredited = new AccountCredited(amountToTransfer, accountTo);
        var expectedDebited = new AccountDebited(amountToTransfer, accountFrom);

        creditedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(expectedCredited)
                .dispose();

        debitedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(expectedDebited)
                .dispose();

        alreadyProcessedSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new MoneyTransferAlreadyProcessed(transferMoney.id))
                .dispose();

        assertAccountBalance(Money.of(0, "EUR"), accountFrom.id);
        assertAccountBalance(amountToTransfer, accountTo.id);
    }

    @Test
    void should_fail_the_transfer_when_insufficient_balance() {
        var accountFrom = setupAccountWithBalance(Money.of(10, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "EUR"));

        var amountToTransfer = Money.of(10.01, "EUR");

        var transferFailedTestSubscriber = eventBus.observeEvents(InsufficientBalance.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new InsufficientBalance(transferMoney.id, accountFrom.id))
                .dispose();

        assertAccountBalance(Money.of(10, "EUR"), accountFrom.id);
        assertAccountBalance(Money.of(0, "EUR"), accountTo.id);
    }

    @Test
    void should_fail_when_destination_account_is_not_opened() {
        var accountFrom = setupAccountWithBalance(Money.of(10, "EUR"));
        var accountTo = new Account("id", "test", "test", Money.of(0, "EUR"));
        var amountToTransfer = Money.of(5.50, "EUR");
        var transferFailedTestSubscriber = eventBus.observeEvents(DestinationAccountNotFound.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new DestinationAccountNotFound(transferMoney.id, accountTo.id))
                .dispose();
    }

    @Test
    void should_fail_when_transfer_and_from_currencies_dont_match() {
        var accountFrom = setupAccountWithBalance(Money.of(10, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "EUR"));
        var amountToTransfer = Money.of(5.50, "USD");
        var transferFailedTestSubscriber = eventBus.observeEvents(CurrencyMismatch.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new CurrencyMismatch(transferMoney.id, amountToTransfer.getCurrency(), accountFrom.balance.getCurrency()))
                .dispose();
    }

    @Test
    void should_fail_when_transfer_and_to_currencies_dont_match() {
        var accountFrom = setupAccountWithBalance(Money.of(10, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(0, "USD"));
        var amountToTransfer = Money.of(5.50, "EUR");
        var transferFailedTestSubscriber = eventBus.observeEvents(CurrencyMismatch.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new CurrencyMismatch(transferMoney.id, amountToTransfer.getCurrency(), accountTo.balance.getCurrency()))
                .dispose();
    }

    @Test
    void should_fail_when_source_account_is_not_opened() {
        var accountFrom = new Account("id", "test", "test", Money.of(0, "EUR"));
        var accountTo = setupAccountWithBalance(Money.of(10, "EUR"));
        var amountToTransfer = Money.of(5.50, "EUR");
        var transferFailedTestSubscriber = eventBus.observeEvents(SourceAccountNotFound.class).test();

        var transferMoney = new TransferMoney(UUID.randomUUID().toString(), accountFrom.id, accountTo.id, amountToTransfer);
        eventBus.publish(transferMoney);

        transferFailedTestSubscriber
                .awaitCount(1)
                .assertNoErrors()
                .assertValue(new SourceAccountNotFound(transferMoney.id, accountFrom.id))
                .dispose();
    }

    private void assertAccountBalance(Money expected, String accountId) {
        assertEquals(Optional.of(expected), accountRepository.findById(accountId).map(account -> account.balance));
    }

    private Account setupAccountWithBalance(Money initialBalance) {
        var account = new Account(UUID.randomUUID().toString(), "Foo" + ++index, "Bar" + index, initialBalance);
        return accountRepository.open(account);
    }

    private int index = 0;
}