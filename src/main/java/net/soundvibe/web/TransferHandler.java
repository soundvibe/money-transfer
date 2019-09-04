package net.soundvibe.web;

import io.vavr.control.Try;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.soundvibe.bus.EventBus;
import net.soundvibe.domain.transfer.MoneyTransferRepository;
import net.soundvibe.domain.transfer.command.TransferMoney;
import net.soundvibe.domain.transfer.dto.TransferMoneyDto;
import net.soundvibe.domain.transfer.event.*;
import net.soundvibe.domain.transfer.event.error.MoneyTransferFailed;
import net.soundvibe.json.Json;
import org.slf4j.*;

import java.util.Optional;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static net.soundvibe.web.HttpHeader.JSON_CONTENT;

public class TransferHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferHandler.class);

    private final EventBus eventBus;
    private final MoneyTransferRepository moneyTransferRepository;

    public TransferHandler(EventBus eventBus, MoneyTransferRepository moneyTransferRepository) {
        this.eventBus = eventBus;
        this.moneyTransferRepository = moneyTransferRepository;
    }

    public void transfer(RoutingContext ctx) {
        Try.of(() -> Json.parse(ctx.getBodyAsString(), TransferMoneyDto.class))
                .onFailure(e -> handleError(ctx, e, BAD_REQUEST.code()))
                .map(TransferMoney::from)
                .andThen(eventBus::publish)
                .onFailure(e -> handleError(ctx, e, BAD_GATEWAY.code()))
                .forEach(transferMoney -> ctx.response()
                        .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                        .setStatusCode(ACCEPTED.code())
                        .end(new JsonObject().put("transferId", transferMoney.id).encodePrettily()));

    }

    public void transferStatus(RoutingContext ctx) {
        Optional.ofNullable(ctx.request().getParam("transferId"))
                .flatMap(moneyTransferRepository::findById)
                .ifPresentOrElse(
                        event -> Match(event).of(
                                Case($(instanceOf(MoneyTransferFailed.class)),
                                        moneyTransferFailed -> run(() -> ctx.response()
                                                .setStatusCode(PRECONDITION_FAILED.code())
                                                .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                                                .end(Json.toString(moneyTransferFailed)))),
                                Case($(instanceOf(MoneyTransferred.class)),
                                        moneyTransferred -> run(() -> ctx.response()
                                                .setStatusCode(OK.code())
                                                .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                                                .end(Json.toString(moneyTransferred)))),
                                Case($(), () -> run(() -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR.code())
                                        .putHeader(JSON_CONTENT.name, JSON_CONTENT.value)
                                        .end(Json.toString(event))))
                        ),
                        () -> ctx.response()
                                .setStatusCode(NO_CONTENT.code())
                                .end());
    }

    private void handleError(RoutingContext ctx, Throwable e, int statusCode) {
        log.error("{} error", getClass().getSimpleName(), e);
        if (!ctx.response().ended()) {
            ctx.response()
                    .setStatusCode(statusCode)
                    .setStatusMessage(e.getMessage())
                    .end();
        }
    }
}
