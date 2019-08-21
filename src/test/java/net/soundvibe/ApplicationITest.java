package net.soundvibe;

import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import net.soundvibe.domain.account.Account;
import net.soundvibe.domain.transfer.command.TransferMoney;
import net.soundvibe.domain.transfer.event.MoneyTransferred;
import net.soundvibe.json.Json;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodySubscribers.*;
import static java.net.http.HttpResponse.BodySubscribers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.soundvibe.web.HttpHeader.JSON_CONTENT;
import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApplicationITest {

    private static HttpServer httpServer;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final int PORT = 1234;
    private static final URI SERVICE_ROOT = URI.create(String.format("http://localhost:%d", PORT));

    @BeforeAll
    static void setUpAll() throws InterruptedException {
        httpServer = Application.startHttpServer(PORT);
    }

    @AfterAll
    static void tearDown() {
        httpServer.close();
    }


    @Nested
    @DisplayName("/account")
    class AccountTests {

        private final URI path = SERVICE_ROOT.resolve("/account");

        @Test
        void should_open_new_account() throws IOException, InterruptedException {
            var account = new Account(UUID.randomUUID().toString(), "Name", "Surname", Money.of(1000, "EUR"), null);

            var request = HttpRequest.newBuilder(path)
                    .POST(ofString(Json.toString(account)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var response = HTTP_CLIENT.send(request,
                    ri -> mapping(ofString(UTF_8), json -> Json.parse(json, Account.class)));

            assertEquals(CREATED.code(), response.statusCode());
            assertEquals(account, response.body());
        }

        @Test
        void should_fail_when_opening_mailformed_account() throws IOException, InterruptedException {
            var request = HttpRequest.newBuilder(path)
                    .POST(ofString("{}"))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());

            assertEquals(BAD_REQUEST.code(), response.statusCode());
        }

        @Test
        void should_close_opened_account() throws IOException, InterruptedException {
            var accountId = UUID.randomUUID().toString();
            var account = new Account(accountId, "Name", "Surname", Money.of(1000, "EUR"), null);

            var openRequest = HttpRequest.newBuilder(path)
                    .POST(ofString(Json.toString(account)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var openResponse = HTTP_CLIENT.send(openRequest,
                    ri -> mapping(ofString(UTF_8), json -> Json.parse(json, Account.class)));

            assertEquals(CREATED.code(), openResponse.statusCode());
            assertEquals(account, openResponse.body());

            var closeRequest = HttpRequest.newBuilder(URI.create(path + "/" + accountId))
                    .DELETE()
                    .build();

            var closeResponse = HTTP_CLIENT.send(closeRequest, HttpResponse.BodyHandlers.discarding());
            assertEquals(OK.code(), closeResponse.statusCode());
        }

        @Test
        void should_not_find_account_to_close() throws IOException, InterruptedException {
            var closeRequest = HttpRequest.newBuilder(URI.create(path + "/id"))
                    .DELETE()
                    .build();

            var closeResponse = HTTP_CLIENT.send(closeRequest, HttpResponse.BodyHandlers.discarding());
            assertEquals(NO_CONTENT.code(), closeResponse.statusCode());
        }
    }

    @Nested
    @DisplayName("/transfer")
    class TransferTests {

        private final URI accountUri = SERVICE_ROOT.resolve("/account");
        private final URI transferUri = SERVICE_ROOT.resolve("/transfer");

        @Test
        void should_initiate_new_money_transfer_and_get_ok_status() throws IOException, InterruptedException {
            var accountFrom = openAccount("From", "Surname", Money.of(1000, "EUR"));
            var accountTo = openAccount("To", "LastName", Money.of(100, "EUR"));

            var transferMoney = transferMoney(accountFrom.id, accountTo.id, Money.of(10.11, "EUR"));

            var requestStatus = HttpRequest.newBuilder(URI.create(transferUri + "/" + transferMoney.id))
                    .GET()
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var maybeResponse = pollForStatusCode(OK.code(), requestStatus);
            assertTrue(maybeResponse.isPresent());

            var expected = Optional.of(MoneyTransferred.from(transferMoney));
            assertEquals(expected, maybeResponse.map(HttpResponse::body));
        }

        @Test
        void should_not_find_random_transfer() throws IOException, InterruptedException {
            var requestStatus = HttpRequest.newBuilder(URI.create(transferUri + "/randomTransferId"))
                    .GET()
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseStatus = HTTP_CLIENT.send(requestStatus, ri -> discarding());
            assertEquals(NO_CONTENT.code(), responseStatus.statusCode());
        }

        @Test
        void should_transfer_fail_when_insufficient_funds() throws IOException, InterruptedException {
            var accountFrom = openAccount("From", "Surname", Money.of(10, "EUR"));
            var accountTo = openAccount("To", "LastName", Money.of(100, "EUR"));

            var transferMoney = transferMoney(accountFrom.id, accountTo.id, Money.of(10.11, "EUR"));

            var requestStatus = HttpRequest.newBuilder(URI.create(transferUri + "/" + transferMoney.id))
                    .GET()
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseStatus = HTTP_CLIENT.send(requestStatus, ri -> discarding());
            assertEquals(PRECONDITION_FAILED.code(), responseStatus.statusCode());
        }

        private Account openAccount(String firstName, String lastName, Money initialBalance) throws IOException, InterruptedException {
            var account = new Account(UUID.randomUUID().toString(), firstName, lastName, initialBalance, null);
            var requestFrom = HttpRequest.newBuilder(accountUri)
                    .POST(ofString(Json.toString(account)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();
            var response = HTTP_CLIENT.send(requestFrom,
                    ri -> mapping(ofString(UTF_8), json -> Json.parse(json, Account.class)));
            assertEquals(CREATED.code(), response.statusCode());
            return response.body();
        }

        private TransferMoney transferMoney(String accountIdFrom, String accountIdTo, Money amount) throws IOException, InterruptedException {
            var id = UUID.randomUUID().toString();
            var transferMoney = new TransferMoney(id, accountIdFrom, accountIdTo, amount);
            var requestTransfer = HttpRequest.newBuilder(transferUri)
                    .POST(ofString(Json.toString(transferMoney)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseTransfer = HTTP_CLIENT.send(requestTransfer, ri -> mapping(ofString(UTF_8), JsonObject::new));

            assertEquals(ACCEPTED.code(), responseTransfer.statusCode());
            var transferId = responseTransfer.body().getString("transferId");
            assertEquals(id, transferId);
            return transferMoney;
        }

        private static final int TIMES = 5;

        private Optional<HttpResponse<MoneyTransferred>> pollForStatusCode(int statusCode, HttpRequest httpRequest) throws InterruptedException {
            for (int i = 0; i < TIMES; i++) {
                try {
                    var responseStatus = HTTP_CLIENT.send(httpRequest,
                            ri -> mapping(ofString(UTF_8), json -> Json.parse(json, MoneyTransferred.class)));

                    if (responseStatus.statusCode() == statusCode) {
                        return Optional.of(responseStatus);
                    }
                } catch (Exception e) {
                    // response was not expected, ignoring...
                }
                Thread.sleep(1000);
            }
            return Optional.empty();
        }
    }
}