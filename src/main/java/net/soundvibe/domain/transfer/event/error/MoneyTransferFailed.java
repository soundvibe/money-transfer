package net.soundvibe.domain.transfer.event.error;

import net.soundvibe.domain.base.Event;

import java.util.Objects;

public abstract class MoneyTransferFailed implements Event {

    public final String transferId;
    public final String cause;

    protected MoneyTransferFailed(String transferId, String cause) {
        this.transferId = transferId;
        this.cause = cause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoneyTransferFailed)) return false;
        final MoneyTransferFailed that = (MoneyTransferFailed) o;
        return transferId.equals(that.transferId) &&
                cause.equals(that.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId, cause);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "transferId='" + transferId + '\'' +
                ", cause='" + cause + '\'' +
                '}';
    }
}
