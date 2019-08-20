package net.soundvibe.domain.account;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AccountRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public Account open(Account initialAccount) {
        return accounts.merge(initialAccount.id, initialAccount, (oldAccount, newAccount) -> {
            throw new IllegalStateException(String.format("Account (%s) already exists", oldAccount.id));
        });
    }

    public Account close(String accountId) {
        return accounts.remove(accountId);
    }

    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    Account save(Account account) {
        accounts.put(account.id, account);
        return account;
    }
}
