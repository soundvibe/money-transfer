package net.soundvibe.web;

import io.vavr.control.Try;
import io.vertx.ext.web.RoutingContext;
import net.soundvibe.domain.account.Account;
import net.soundvibe.domain.account.AccountRepository;
import net.soundvibe.json.Json;

import java.util.Optional;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static net.soundvibe.web.HttpHeader.JSON_CONTENT;

public class AccountHandler {

    private final AccountRepository accountRepository;

    public AccountHandler(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void open(RoutingContext ctx) {
        Try.of(() -> Json.parse(ctx.getBodyAsString(), Account.class))
                .map(accountRepository::open)
                .onFailure(e -> ctx.response()
                        .setStatusCode(BAD_REQUEST.code())
                        .setStatusMessage(e.getMessage())
                        .end())
                .forEach(account -> ctx.response()
                        .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                        .setStatusCode(CREATED.code())
                        .end(Json.toString(account)));
    }

    public void get(RoutingContext ctx) {
        Optional.ofNullable(ctx.request().getParam("accountId"))
                .flatMap(accountRepository::findById)
                .ifPresentOrElse(
                        account -> ctx.response()
                            .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                            .setStatusCode(OK.code())
                            .end(Json.toString(account)),
                        () -> ctx.response()
                                .setStatusCode(NO_CONTENT.code())
                                .end()
                );
    }

    public void close(RoutingContext ctx) {
        Optional.ofNullable(ctx.request().getParam("accountId"))
                .map(accountRepository::close)
                .ifPresentOrElse(
                        closedAccount -> ctx.response()
                                .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                                .setStatusCode(OK.code())
                                .end(Json.toString(closedAccount)),
                        () -> ctx.response()
                                .setStatusCode(NO_CONTENT.code())
                                .end());
    }
}
