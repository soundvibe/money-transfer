package net.soundvibe.domain.account.repository;

import net.soundvibe.domain.account.AccountRepository;
import net.soundvibe.domain.account.Account;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AccountRepositoryTest {

    private final AccountRepository sut = new AccountRepository();

    @Test
    void should_open_new_account() {
        var actual = new Account(UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59"));
        var expected = sut.open(actual);
        assertEquals(expected, actual);
    }

    @Test
    void should_close_opened_account() {
        var expected = sut.open(new Account(UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59")));
        var actual = sut.close(expected.id);
        assertEquals(expected, actual);
    }

    @Test
    void should_throw_when_account_already_opened() {
        var openedAccount = sut.open(new Account(
                UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59")));
        assertThrows(IllegalStateException.class, () -> sut.open(openedAccount));
    }

    @Test
    void should_find_opened_account() {
        var openedAccount = sut.open(new Account(
                UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59")));
        assertEquals(Optional.of(openedAccount), sut.findById(openedAccount.id));
    }

    @Test
    void should_not_find_account() {
        assertEquals(Optional.empty(), sut.findById("id"));
    }

    @Test
    void should_close_be_idempotant() {
        var expected = sut.open(new Account(UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59")));
        var actual = sut.close(expected.id);
        var actual2 = sut.close(expected.id);
        assertEquals(expected, actual);
        assertNull(actual2);
    }
}