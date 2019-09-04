package net.soundvibe.domain.transfer.event.error;

public class NegativeOrZeroAmount extends MoneyTransferFailed {

    public NegativeOrZeroAmount(String transferId) {
        super(transferId, "Transfer amount cannot be negative or zero");
    }

}
