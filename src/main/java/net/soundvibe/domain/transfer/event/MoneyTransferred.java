package net.soundvibe.domain.transfer.event;

import net.soundvibe.domain.base.Event;
import net.soundvibe.domain.transfer.command.TransferMoney;
import org.javamoney.moneta.Money;

import java.util.Objects;

public class MoneyTransferred implements Event {

    public final String transferId;
    public final Money amount;
    public final String accountIdFrom;
    public final String accountIdTo;

    public MoneyTransferred(String transferId, Money amount, String accountIdFrom, String accountIdTo) {
        this.transferId = transferId;
        this.amount = amount;
        this.accountIdFrom = accountIdFrom;
        this.accountIdTo = accountIdTo;
    }

    public static MoneyTransferred from(TransferMoney transferMoney) {
        return new MoneyTransferred(transferMoney.id, transferMoney.amountToTransfer,
                transferMoney.accountIdFrom, transferMoney.accountIdTo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoneyTransferred)) return false;
        final MoneyTransferred that = (MoneyTransferred) o;
        return transferId.equals(that.transferId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId);
    }

    @Override
    public String toString() {
        return "MoneyTransferred{" +
                "transferId='" + transferId + '\'' +
                ", amount=" + amount +
                ", accountIdFrom='" + accountIdFrom + '\'' +
                ", accountIdTo='" + accountIdTo + '\'' +
                '}';
    }
}
