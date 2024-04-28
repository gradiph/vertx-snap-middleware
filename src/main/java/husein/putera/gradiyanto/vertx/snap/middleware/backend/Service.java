package husein.putera.gradiyanto.vertx.snap.middleware.backend;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Service(Type type, String host, Optional<Integer> port, long timeout) {

  public static Map<String, Service> init(JsonObject gatewayConfig) {
    Map<String, Service> services = new HashMap<>();
    gatewayConfig.getJsonObject(BackendConfig.FIELD_SERVICES)
      .forEach(entry -> {
        JsonObject value = (JsonObject) entry.getValue();
        Type type = Type.valueOf(value.getString("type").toUpperCase());
        services.put(entry.getKey(), new Service(
          type,
          value.getString("host"),
          Optional.ofNullable(value.getInteger("port", null)),
          value.getLong("timeout", 10L)
        ));
      });
    return services;
  }

  public enum Type {
    HTTP,
    HTTPS,
    TCP
  }
}
