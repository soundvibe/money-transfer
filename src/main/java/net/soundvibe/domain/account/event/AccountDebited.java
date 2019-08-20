package net.soundvibe.domain.account.event;

import net.soundvibe.domain.account.Account;
import net.soundvibe.domain.base.Event;
import org.javamoney.moneta.Money;

import java.util.Objects;

public class AccountDebited implements Event {

    public final Money amountDebited;
    public final Account account;

    public AccountDebited(Money amountDebited, Account account) {
        this.amountDebited = amountDebited;
        this.account = account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountDebited)) return false;
        final AccountDebited that = (AccountDebited) o;
        return Objects.equals(amountDebited, that.amountDebited) &&
                Objects.equals(account, that.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountDebited, account);
    }

    @Override
    public String toString() {
        return "AccountDebited{" +
                "amountDebited=" + amountDebited +
                ", account=" + account +
                '}';
    }
}
