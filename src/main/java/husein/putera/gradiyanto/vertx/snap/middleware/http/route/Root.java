package husein.putera.gradiyanto.vertx.snap.middleware.http.route;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class Root {

  private Root() {}

  public static void applyRoute(Router router) {
    Route route = router.route("/");
    route.handler(ctx -> {
      HttpServerResponse response = ctx.response();
      response.end("Your IP : " + ctx.request().remoteAddress());
    });
  }
}
