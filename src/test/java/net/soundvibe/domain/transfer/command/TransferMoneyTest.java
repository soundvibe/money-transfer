package net.soundvibe.domain.transfer.command;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransferMoneyTest {

    @Test
    void should_be_equal() {
        var left = new TransferMoney("id1", "idFrom", "idTo", Money.of(100, "EUR"));
        var right = new TransferMoney("id1", "idFrom", "idTo", Money.of(100, "EUR"));

        assertEquals(left, right);
    }

    @Test
    void should_not_be_equal() {
        var left = new TransferMoney("id1", "idFrom", "idTo", Money.of(100, "EUR"));
        var right = new TransferMoney("id2", "idFrom", "idTo", Money.of(100, "EUR"));

        assertNotEquals(left, right);
    }
}