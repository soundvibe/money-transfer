package net.soundvibe.domain.account;

import io.micrometer.core.lang.Nullable;
import org.javamoney.moneta.Money;

public class Account {

    public final String id;
    public final String firstName;
    public final String lastName;
    public final Money balance;

    public Account(String id, String firstName, String lastName, Money balance) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.balance = balance;
    }

    public Account deposit(Money amount) {
        return new Account(id, firstName, lastName, balance.add(amount));
    }

    public Account withdraw(Money amount) {
        return new Account(id, firstName, lastName, balance.subtract(amount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        final Account account = (Account) o;
        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", balance=" + balance +
                '}';
    }
}
