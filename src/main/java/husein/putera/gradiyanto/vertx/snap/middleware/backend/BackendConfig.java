package husein.putera.gradiyanto.vertx.snap.middleware.backend;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public record BackendConfig(Map<String, Service> services) {

  public static final String NAMESPACE = "gateway";
  public static final String FIELD_SERVICES = "services";

  public static BackendConfig from(JsonObject config) {
    return new BackendConfig(
      Service.init(config.getJsonObject(NAMESPACE))
    );
  }
}
