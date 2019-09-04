package net.soundvibe.domain.account;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void should_deposit_given_amount() {
        var from = new Account("id", "", "", Money.of(100, "EUR"));
        var actual = from.deposit(Money.of(50, "EUR"));
        var expected = new Account("id", "", "", Money.of(150, "EUR"));

        assertEquals(expected, actual);
    }

    @Test
    void should_withdraw_given_amount() {
        var from = new Account("id", "", "", Money.of(100, "EUR"));
        var actual = from.withdraw(Money.of(50, "EUR"));
        var expected = new Account("id", "", "", Money.of(50, "EUR"));

        assertEquals(expected, actual);
    }
}