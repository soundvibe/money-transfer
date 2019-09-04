package net.soundvibe.domain.account;

import io.micrometer.core.instrument.*;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vavr.control.*;
import net.soundvibe.bus.*;
import net.soundvibe.domain.account.event.*;
import net.soundvibe.domain.base.*;
import net.soundvibe.domain.transfer.command.TransferMoney;
import net.soundvibe.domain.transfer.event.*;
import net.soundvibe.domain.transfer.event.error.*;
import org.slf4j.*;

import javax.money.CurrencyUnit;
import java.util.*;

import static io.vavr.control.Validation.*;

public class AccountProcessor implements EventBusSubscriber {

    private static final Logger log = LoggerFactory.getLogger(AccountProcessor.class);

    private final Counter errors = Metrics.counter("errors", Tags.of("processor", getClass().getSimpleName()));
    private final Counter counter = Metrics.counter("transferEvents", Tags.of("processor", getClass().getSimpleName()));
    private final AccountRepository accountRepository;
    private final Set<Command> processedCommands = new HashSet<>();

    public AccountProcessor(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Disposable subscribe(EventBus eventBus) {
        return eventBus.observeCommands(TransferMoney.class)
                .observeOn(Schedulers.single())
                .concatMap(this::transfer)
                .doOnError(this::handleError)
                .retry()
                .doOnNext(eventBus::publish)
                .subscribe(this::handleSuccess, this::handleError);
    }

    private Flowable<? extends Event> transfer(TransferMoney transferMoney) {
        var validation = validate(transferMoney);
        if (validation.isInvalid()) {
            return Flowable.just(validation.getError());
        }

        var accounts = validation.get();
        var debitedAccount = accounts.from.withdraw(transferMoney.amountToTransfer);
        var creditedAccount = accounts.to.deposit(transferMoney.amountToTransfer);
        return Flowable.just(
                new AccountCredited(transferMoney.amountToTransfer, accountRepository.save(creditedAccount)),
                new AccountDebited(transferMoney.amountToTransfer, accountRepository.save(debitedAccount)),
                MoneyTransferred.from(save(transferMoney))
        );
    }

    private Validation<MoneyTransferFailed, Accounts> validate(TransferMoney transferMoney) {
        if (processedCommands.contains(transferMoney)) {
            return invalid(new MoneyTransferAlreadyProcessed(transferMoney.id));
        }

        if (transferMoney.accountIdFrom.equals(transferMoney.accountIdTo)) {
            return invalid(new SameAccount(transferMoney.id, transferMoney.accountIdFrom));
        }

        if (transferMoney.amountToTransfer.isNegativeOrZero()) {
            return invalid(new NegativeOrZeroAmount(transferMoney.id));
        }

        var maybeFrom = accountRepository.findById(transferMoney.accountIdFrom);
        if (maybeFrom.isEmpty()) {
            return invalid(new SourceAccountNotFound(transferMoney.id, transferMoney.accountIdFrom));
        }
        var maybeTo = accountRepository.findById(transferMoney.accountIdTo);
        if (maybeTo.isEmpty()) {
            return invalid(new DestinationAccountNotFound(transferMoney.id, transferMoney.accountIdTo));
        }
        var accountFrom = maybeFrom.get();
        var accountTo = maybeTo.get();

        if (!transferMoney.amountToTransfer.getCurrency().equals(accountFrom.balance.getCurrency())) {
            return invalid(new CurrencyMismatch(transferMoney.id,
                    transferMoney.amountToTransfer.getCurrency(),
                    accountFrom.balance.getCurrency()));
        }

        if (!transferMoney.amountToTransfer.getCurrency().equals(accountTo.balance.getCurrency())) {
            return invalid(new CurrencyMismatch(transferMoney.id,
                    transferMoney.amountToTransfer.getCurrency(),
                    accountTo.balance.getCurrency()));
        }

        if (transferMoney.amountToTransfer.isGreaterThan(accountFrom.balance)) {
            return invalid(new InsufficientBalance(transferMoney.id, accountFrom.id));
        }

        return valid(new Accounts(accountFrom, accountTo));
    }

    private <T extends Command> T save(T command) {
        processedCommands.add(command);
        return command;
    }

    private static class Accounts {
        final Account from;
        final Account to;

        private Accounts(Account from, Account to) {
            this.from = from;
            this.to = to;
        }
    }

    private void handleSuccess(Event event) {
        log.info("Event was published successfully: {}", event);
        counter.increment();
    }

    private void handleError(Throwable e) {
        log.error("Got error during money transfer", e);
        errors.increment();
    }
}
