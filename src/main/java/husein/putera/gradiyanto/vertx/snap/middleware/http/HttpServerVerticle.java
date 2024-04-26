package husein.putera.gradiyanto.vertx.snap.middleware.http;

import husein.putera.gradiyanto.vertx.snap.middleware.http.route.RouterBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

public class HttpServerVerticle extends AbstractVerticle {

  private final HttpConfig config;

  public HttpServerVerticle(HttpConfig config) {
    this.config = config;
  }

  @SuppressWarnings("java:S106")
  @Override
  public void start(Promise<Void> startPromise) {
    vertx.createHttpServer()
      .requestHandler(
        new RouterBuilder(Router.router(vertx))
          .get())
      .listen(config.port(), http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port " + config.port());
        } else {
          startPromise.fail(http.cause());
        }
      });
  }
}
