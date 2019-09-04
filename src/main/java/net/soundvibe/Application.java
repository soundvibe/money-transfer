package net.soundvibe;

import io.micrometer.core.instrument.Metrics;
import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.micrometer.*;
import io.vertx.micrometer.backends.*;
import net.soundvibe.bus.*;
import net.soundvibe.domain.account.*;
import net.soundvibe.domain.account.AccountRepository;
import net.soundvibe.domain.transfer.MoneyTransferRepository;
import net.soundvibe.web.*;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.*;

import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static io.vertx.micrometer.MicrometerMetricsOptions.DEFAULT_REGISTRY_NAME;
import static net.soundvibe.web.AccountHandler.*;
import static net.soundvibe.web.HttpHeader.JSON_CONTENT;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws InterruptedException {
        int port = args != null && args.length > 0 ? Integer.parseInt(args[0]) : 8181;
        startHttpServer(port);
    }

    public static HttpServer startHttpServer(int port) throws InterruptedException {
        var vertx = setupVertx();
        var httpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(port)
                .setSsl(false));

        var accountRepository = new AccountRepository();
        var moneyTransferRepository = new MoneyTransferRepository();
        var eventBus = new RxEventBus();
        var subscribers = List.<EventBusSubscriber>of(
                new AccountProcessor(accountRepository),
                moneyTransferRepository
        );
        var supervisor = new SubscribersSupervisor(subscribers);
        supervisor.subscribe(eventBus);

        var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/metrics").handler(PrometheusScrapingHandler.create());
        router.get("/health").handler(HealthCheckHandler.create(supervisor));
        router.get("/").handler(SwaggerHandler.create());

        var accountHandler = new AccountHandler(accountRepository);
        router.get("/account/:accountId").handler(accountHandler::get);
        router.delete("/account/:accountId").handler(accountHandler::close);
        router.post("/account")
                .consumes(JSON_CONTENT.value)
                .handler(accountHandler::open);

        var transferHandler = new TransferHandler(eventBus, moneyTransferRepository);
        router.post("/transfer")
                .consumes(JSON_CONTENT.value)
                .handler(transferHandler::transfer);
        router.get("/transfer/:transferId").handler(transferHandler::transferStatus);

        router.route("/*").handler(StaticHandler.create());
        router.route("/webjars/*").handler(StaticHandler.create("META-INF/resources/webjars"));

        var latch = new CountDownLatch(1);
        httpServer.requestHandler(router).listen(handler -> {
            if (handler.succeeded()) {
                log.info("Http Server started on port {}", handler.result().actualPort());
                latch.countDown();
            } else {
                log.error("Unable to start Http Server", handler.cause());
            }
        });

        if (!(latch.await(30, TimeUnit.SECONDS))) {
            System.exit(-1);
        }
        return httpServer;
    }

    private static Vertx setupVertx() {
        Metrics.addRegistry(prometheusBackend.getMeterRegistry());
        System.setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
        return Vertx.vertx(new VertxOptions()
                .setMetricsOptions(MICROMETER_METRICS_OPTIONS.setMicrometerRegistry(prometheusBackend.getMeterRegistry())));
    }

    private static final MicrometerMetricsOptions MICROMETER_METRICS_OPTIONS = new MicrometerMetricsOptions()
            .setEnabled(true)
            .setJvmMetricsEnabled(true)
            .setPrometheusOptions(new VertxPrometheusOptions()
                    .setEnabled(true))
            .setRegistryName(DEFAULT_REGISTRY_NAME);

    private static final BackendRegistry prometheusBackend = BackendRegistries.setupBackend(MICROMETER_METRICS_OPTIONS);

}
