package net.soundvibe.domain.transfer.command;

import net.soundvibe.domain.base.Command;
import org.javamoney.moneta.Money;

import java.util.UUID;

public class TransferMoney implements Command {

    public final String id;
    public final String accountIdFrom;
    public final String accountIdTo;
    public final Money amountToTransfer;

    public TransferMoney(String id, String accountIdFrom, String accountIdTo, Money amountToTransfer) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.accountIdFrom = accountIdFrom;
        this.accountIdTo = accountIdTo;
        this.amountToTransfer = amountToTransfer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransferMoney)) return false;
        final TransferMoney that = (TransferMoney) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "TransferMoney{" +
                "id='" + id + '\'' +
                ", accountIdFrom='" + accountIdFrom + '\'' +
                ", accountIdTo='" + accountIdTo + '\'' +
                ", amount=" + amountToTransfer +
                '}';
    }
}
