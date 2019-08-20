package net.soundvibe.domain.transfer.event;

import net.soundvibe.domain.base.Event;

import java.util.Objects;

public class MoneyTransferFailed implements Event {

    public final String transferId;
    public final String cause;

    public MoneyTransferFailed(String transferId, String cause) {
        this.transferId = transferId;
        this.cause = cause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoneyTransferFailed)) return false;
        final MoneyTransferFailed that = (MoneyTransferFailed) o;
        return transferId.equals(that.transferId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId);
    }

    @Override
    public String toString() {
        return "MoneyTransferFailed{" +
                "transferId='" + transferId + '\'' +
                ", cause='" + cause + '\'' +
                '}';
    }
}
