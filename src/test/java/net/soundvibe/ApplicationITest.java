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
import java.util.UUID;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodySubscribers.*;
import static java.net.http.HttpResponse.BodySubscribers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.soundvibe.web.HttpHeader.JSON_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApplicationITest {

    private static HttpServer httpServer;
    private static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
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

        private final URI accountPath = SERVICE_ROOT.resolve("/account");
        private final URI path = SERVICE_ROOT.resolve("/transfer");

        @Test
        void should_initiate_new_money_transfer_and_get_ok_status() throws IOException, InterruptedException {
            var accountFrom = new Account(UUID.randomUUID().toString(), "From", "Surname", Money.of(1000, "EUR"), null);
            var accountTo = new Account(UUID.randomUUID().toString(), "To", "Surname", Money.of(100, "EUR"), null);

            var requestFrom = HttpRequest.newBuilder(accountPath)
                    .POST(ofString(Json.toString(accountFrom)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseFrom = HTTP_CLIENT.send(requestFrom,
                    ri -> mapping(ofString(UTF_8), json -> Json.parse(json, Account.class)));
            assertEquals(CREATED.code(), responseFrom.statusCode());

            var requestTo = HttpRequest.newBuilder(accountPath)
                    .POST(ofString(Json.toString(accountTo)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseTo = HTTP_CLIENT.send(requestTo,
                    ri -> mapping(ofString(UTF_8), json -> Json.parse(json, Account.class)));
            assertEquals(CREATED.code(), responseTo.statusCode());

            var transferId = UUID.randomUUID().toString();
            var transferMoney = new TransferMoney(transferId, accountFrom.id, accountTo.id, Money.of(10.11, "EUR"));
            var requestTransfer = HttpRequest.newBuilder(path)
                    .POST(ofString(Json.toString(transferMoney)))
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseTransfer = HTTP_CLIENT.send(requestTransfer,
                    ri -> mapping(ofString(UTF_8), JsonObject::new));

            assertEquals(ACCEPTED.code(), responseTransfer.statusCode());
            assertEquals(transferId, responseTransfer.body().getString("transferId"));

            Thread.sleep(3000); //wait for transfer to be processed

            var requestStatus = HttpRequest.newBuilder(URI.create(path + "/" + transferId))
                    .GET()
                    .setHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                    .build();

            var responseStatus = HTTP_CLIENT.send(requestStatus,
                    ri -> mapping(ofString(UTF_8), json -> Json.parse(json, MoneyTransferred.class)));

            var expected = MoneyTransferred.from(transferMoney);
            assertEquals(OK.code(), responseStatus.statusCode());
            assertEquals(expected, responseStatus.body());
        }
    }
}