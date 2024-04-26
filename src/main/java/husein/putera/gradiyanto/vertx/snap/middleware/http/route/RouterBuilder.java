package husein.putera.gradiyanto.vertx.snap.middleware.http.route;

import io.vertx.ext.web.Router;

public class RouterBuilder {

  private final Router router;

  public RouterBuilder(Router router) {
    this.router = router;
  }

  public Router get() {
    Root.applyRoute(router);
    return router;
  }
}
