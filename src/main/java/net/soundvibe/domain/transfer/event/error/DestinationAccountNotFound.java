package net.soundvibe.domain.transfer.event.error;

public class DestinationAccountNotFound extends MoneyTransferFailed {

    public DestinationAccountNotFound(String transferId, String accountIdTo) {
        super(transferId, String.format("Destination account (%s) does not exist", accountIdTo));
    }
}
