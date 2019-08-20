package net.soundvibe.domain.account.repository;

import net.soundvibe.domain.account.AccountRepository;
import net.soundvibe.domain.account.Account;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AccountRepositoryTest {

    private final AccountRepository sut = new AccountRepository();

    private static final String TRAN_ID = UUID.randomUUID().toString();

    @Test
    void should_open_new_account() {
        var actual = new Account(UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59"), TRAN_ID);
        var expected = sut.open(actual);
        assertEquals(expected, actual);
    }

    @Test
    void should_close_opened_account() {
        var expected = sut.open(new Account(UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59"), TRAN_ID));
        var actual = sut.close(expected.id);
        assertEquals(expected, actual);
    }

    @Test
    void should_throw_when_account_already_opened() {
        var openedAccount = sut.open(new Account(
                UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59"), TRAN_ID));
        assertThrows(IllegalStateException.class, () -> sut.open(openedAccount));
    }

    @Test
    void should_close_be_idempotant() {
        var expected = sut.open(new Account(UUID.randomUUID().toString(), "Linas", "Naginionis", Money.parse("EUR 10.59"), TRAN_ID));
        var actual = sut.close(expected.id);
        var actual2 = sut.close(expected.id);
        assertEquals(expected, actual);
        assertNull(actual2);
    }
}