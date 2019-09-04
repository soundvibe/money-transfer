package net.soundvibe.domain.transfer.dto;

import org.javamoney.moneta.Money;

public class TransferMoneyDto {

    public final String accountIdFrom;
    public final String accountIdTo;
    public final Money amountToTransfer;

    public TransferMoneyDto(String accountIdFrom, String accountIdTo, Money amountToTransfer) {
        this.accountIdFrom = accountIdFrom;
        this.accountIdTo = accountIdTo;
        this.amountToTransfer = amountToTransfer;
    }
}
