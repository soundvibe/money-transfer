package net.soundvibe.domain.transfer.event.error;

public class SourceAccountNotFound extends MoneyTransferFailed {

    public SourceAccountNotFound(String transferId, String accountIdFrom) {
        super(transferId, String.format("Source account (%s) does not exist", accountIdFrom));
    }
}
