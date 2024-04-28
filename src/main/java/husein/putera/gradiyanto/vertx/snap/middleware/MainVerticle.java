package husein.putera.gradiyanto.vertx.snap.middleware;

import husein.putera.gradiyanto.vertx.snap.middleware.backend.BackendConfig;
import husein.putera.gradiyanto.vertx.snap.middleware.backend.BackendVerticle;
import husein.putera.gradiyanto.vertx.snap.middleware.http.HttpConfig;
import husein.putera.gradiyanto.vertx.snap.middleware.http.HttpServerVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("java:S106")
public class MainVerticle extends AbstractVerticle {

  private final Logger logger;
  private final Map<String, String> deployments;

  public MainVerticle() {
    super();
    logger = LoggerFactory.getLogger(this.getClass());
    deployments = new HashMap<>();
  }

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigRetriever retriever = getConfigRetriever();
    retriever.getConfig()
      .onSuccess(config -> {
        logger.info("Set config: " + config.toString());

        vertx.deployVerticle(new HttpServerVerticle(HttpConfig.from(config)))
          .onSuccess(deploymentId -> setDeployed(deploymentId, HttpServerVerticle.class.getName()))
          .onFailure(startPromise::fail);

        vertx.deployVerticle(new BackendVerticle(BackendConfig.from(config)))
          .onSuccess(deploymentId -> setDeployed(deploymentId, BackendVerticle.class.getName()))
          .onFailure(startPromise::fail);
      })
      .onFailure(startPromise::fail);
  }

  @Override
  public void stop() {
    deployments.keySet()
      .forEach(this::undeploy);
  }

  protected ConfigRetriever getConfigRetriever() {
    ConfigStoreOptions store = new ConfigStoreOptions()
      .setType("file")
      .setFormat("hocon")
      .setConfig(new JsonObject()
        .put("hocon.env.override", true)
        .put("path", "app.conf")
      );
    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
      .addStore(store);
    return ConfigRetriever.create(vertx, options);
  }

  protected void setDeployed(String deploymentId, String deploymentName) {
    deployments.put(deploymentId, deploymentName);
    System.out.println("Deployed [" + deploymentName + "]");
  }

  protected void undeploy(String deploymentId) {
    vertx.undeploy(deploymentId)
      .onComplete(res -> {
        if (res.succeeded()) {
          System.out.println("Succeed to undeploy [" + deployments.get(deploymentId) + "]");
        } else {
          System.out.println("Failed to undeploy [" + deployments.get(deploymentId) + "]");
        }
      });
  }
}
