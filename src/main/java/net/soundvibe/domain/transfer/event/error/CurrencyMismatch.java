package net.soundvibe.domain.transfer.event.error;

import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;

public class CurrencyMismatch extends MoneyTransferFailed {
    public CurrencyMismatch(String transferId, CurrencyUnit from, CurrencyUnit to) {
        super(transferId, String.format("Currency mismatch: %s/%s", from, to));
    }
}
