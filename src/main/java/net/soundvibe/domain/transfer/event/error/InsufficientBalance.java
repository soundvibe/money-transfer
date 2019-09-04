package net.soundvibe.domain.transfer.event.error;

public class InsufficientBalance extends MoneyTransferFailed {

    public InsufficientBalance(String transferId, String accountId) {
        super(transferId, String.format("Account (%s) has insufficient balance to perform money transfer", accountId));
    }
}
