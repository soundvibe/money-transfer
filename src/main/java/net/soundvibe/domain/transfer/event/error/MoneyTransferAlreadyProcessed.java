package net.soundvibe.domain.transfer.event.error;

public class MoneyTransferAlreadyProcessed extends MoneyTransferFailed {
    public MoneyTransferAlreadyProcessed(String transferId) {
        super(transferId, String.format("Money transfer (%s) was already processed", transferId));
    }
}
