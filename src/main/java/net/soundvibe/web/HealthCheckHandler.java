package net.soundvibe.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.soundvibe.bus.SubscribersSupervisor;

import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

public class HealthCheckHandler implements Handler<RoutingContext> {

    private final SubscribersSupervisor subscribersSupervisor;

    private HealthCheckHandler(SubscribersSupervisor subscribersSupervisor) {
        this.subscribersSupervisor = subscribersSupervisor;
    }

    public static HealthCheckHandler create(SubscribersSupervisor subscribersSupervisor) {
        return new HealthCheckHandler(subscribersSupervisor);
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (subscribersSupervisor.isHealthy()) {
            ctx.response().end("Healthy");
        } else {
            ctx.response()
                    .setStatusCode(SERVICE_UNAVAILABLE.code())
                    .setStatusMessage("Service is down")
                    .end();
        }
    }
}
