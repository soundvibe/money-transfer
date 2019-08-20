package net.soundvibe.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import static io.netty.handler.codec.http.HttpResponseStatus.PERMANENT_REDIRECT;

public class SwaggerHandler implements Handler<RoutingContext> {

    public static SwaggerHandler create() {
        return new SwaggerHandler();
    }

    @Override
    public void handle(RoutingContext ctx) {
        ctx.response()
                .putHeader("location", "/swagger/index.html")
                .setStatusCode(PERMANENT_REDIRECT.code())
                .end();
    }
}
