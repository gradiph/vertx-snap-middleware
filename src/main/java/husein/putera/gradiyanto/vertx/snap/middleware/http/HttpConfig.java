package husein.putera.gradiyanto.vertx.snap.middleware.http;

import io.vertx.core.json.JsonObject;

public record HttpConfig(int port, int timeout) {

  public static final String NAMESPACE = "http";
  public static final String FIELD_PORT = "port";
  public static final String FIELD_TIMEOUT = "timeout";

  public static HttpConfig from(JsonObject config) {
    JsonObject httpConfig = config.getJsonObject(NAMESPACE);
    return new HttpConfig(
      httpConfig.getInteger(FIELD_PORT),
      httpConfig.getInteger(FIELD_TIMEOUT) * 1000
    );
  }
}
