package net.soundvibe.domain.transfer.event.error;

public class SameAccount extends MoneyTransferFailed {

    public SameAccount(String transferId, String accountId) {
        super(transferId, "Source and destination accounts are the same: " + accountId);
    }
}
