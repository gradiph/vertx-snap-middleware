package husein.putera.gradiyanto.vertx.snap.middleware.http;

import husein.putera.gradiyanto.vertx.snap.middleware.backend.BackendEventBusMessage;
import husein.putera.gradiyanto.vertx.snap.middleware.backend.BackendVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

@SuppressWarnings("java:S1075")
public class HttpServerVerticle extends AbstractVerticle {

  public static final String PATH_PREFIX_GATEWAY = "/v1/gw/";
  private final HttpConfig config;
  private final Logger logger;

  public HttpServerVerticle(HttpConfig config) {
    logger = LoggerFactory.getLogger(this.getClass());
    this.config = config;
  }

  @SuppressWarnings("java:S106")
  @Override
  public void start(Promise<Void> startPromise) {
    vertx.createHttpServer()
      .requestHandler(getRouter())
      .listen(config.port(), http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port " + config.port());
        } else {
          startPromise.fail(http.cause());
        }
      });
  }

  protected Router getRouter() {
    Router router = Router.router(vertx);
    setRouteRoot(router);
    setRouteV1Gateway(router);
    return router;
  }

  protected void setRouteRoot(Router router) {
    String path = "/";
    Route route = router.route(path);
    route.handler(ctx -> {
      HttpServerResponse response = ctx.response();
      response.putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
        .end("Your IP : " + ctx.request().remoteAddress());
    });
  }

  protected void setRouteV1Gateway(Router router) {
    String path = PATH_PREFIX_GATEWAY + ":" + BackendEventBusMessage.FIELD_SERVICE_NAME + "/*";

    Route route = router.route(path);
    route.handler(ctx -> {
      HttpServerRequest request = ctx.request();
      HttpServerResponse response = ctx.response();
      JsonObject headers = new JsonObject();
      request.headers().forEach(headers::put);
      BackendEventBusMessage.from(ctx)
        .onSuccess(message -> vertx.eventBus()
          .request(
            BackendVerticle.EB_ADDRESS,
            message.toString(),
            new DeliveryOptions()
              .setHeaders(request.headers())
              .setSendTimeout(config.timeout()))
          .onSuccess(reply -> {
            response.headers()
              .setAll(reply.headers());
            response.end(reply.body().toString());
          })
          .onFailure(cause -> {
            logger.error("Failed to send message [" + message.body() + "] to event bus", cause);
            response.setStatusCode(404)
              .end(cause.toString());
          }))
        .onFailure(cause -> {
          logger.warn("Failed to get request body from [" + ctx.request().path() + "]", cause);
          response.setStatusCode(400)
            .end(cause.toString());
        });
    });
  }
}
