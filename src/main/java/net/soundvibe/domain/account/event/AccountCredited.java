package net.soundvibe.domain.account.event;

import net.soundvibe.domain.account.Account;
import net.soundvibe.domain.base.Event;
import org.javamoney.moneta.Money;

import java.util.Objects;

public class AccountCredited implements Event {

    public final Money amountCredited;
    public final Account account;

    public AccountCredited(Money amountCredited, Account account) {
        this.amountCredited = amountCredited;
        this.account = account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountCredited)) return false;
        final AccountCredited that = (AccountCredited) o;
        return Objects.equals(amountCredited, that.amountCredited) &&
                Objects.equals(account, that.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountCredited, account);
    }

    @Override
    public String toString() {
        return "AccountCredited{" +
                "amountCredited=" + amountCredited +
                ", account=" + account +
                '}';
    }
}
