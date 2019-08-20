package net.soundvibe.domain.account;

import io.micrometer.core.instrument.*;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import net.soundvibe.bus.*;
import net.soundvibe.domain.account.event.*;
import net.soundvibe.domain.base.Event;
import net.soundvibe.domain.transfer.command.TransferMoney;
import net.soundvibe.domain.transfer.event.*;
import org.slf4j.*;

public class AccountProcessor implements EventBusSubscriber {

    private static final Logger log = LoggerFactory.getLogger(AccountProcessor.class);

    private final Counter errors = Metrics.counter("errors", Tags.of("processor", getClass().getSimpleName()));
    private final Counter counter = Metrics.counter("transferEvents", Tags.of("processor", getClass().getSimpleName()));
    private final AccountRepository accountRepository;

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
        var maybeFrom = accountRepository.findById(transferMoney.accountIdFrom);
        if (maybeFrom.isEmpty()) {
            return Flowable.just(new MoneyTransferFailed(transferMoney.id, String.format("Source account (%s) does not exist", transferMoney.accountIdFrom)));
        }
        var maybeTo = accountRepository.findById(transferMoney.accountIdTo);
        if (maybeTo.isEmpty()) {
            return Flowable.just(new MoneyTransferFailed(transferMoney.id, String.format("Destination account (%s) does not exist", transferMoney.accountIdTo)));
        }

        var accountFrom = maybeFrom.get();
        var accountTo = maybeTo.get();

        var creditedAccount = accountFrom.credit(transferMoney.amountToTransfer, transferMoney.id);
        if (creditedAccount.balance.isNegative()) {
            return Flowable.just(new MoneyTransferFailed(transferMoney.id, String.format("Account (%s) has insufficient balance to perform money transfer",
                    creditedAccount.id)));
        }
        var debitedAccount = accountTo.debit(transferMoney.amountToTransfer, transferMoney.id);
        return Flowable.just(
                new AccountCredited(transferMoney.amountToTransfer, accountRepository.save(creditedAccount)),
                new AccountDebited(transferMoney.amountToTransfer, accountRepository.save(debitedAccount)),
                MoneyTransferred.from(transferMoney)
        );
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
