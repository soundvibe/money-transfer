package net.soundvibe.json;

import net.soundvibe.domain.account.Account;
import net.soundvibe.domain.account.event.AccountCredited;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonTest {

    @Test
    void should_serialize_and_deserialize_event() {
        var expected = new AccountCredited(Money.of(55, "USD"),
                new Account("id", "foo", "bar", Money.of(0, "EUR"), null));
        var json = Json.toString(expected);
        System.out.println(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));

        var actual = Json.parse(json, AccountCredited.class);
        assertEquals(expected, actual);
    }
}