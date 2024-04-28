package husein.putera.gradiyanto.vertx.snap.middleware.backend;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public record BackendEventBusMessage(
  String serviceName,
  String path,
  HttpMethod method,
  String body
) {

  public static final String FIELD_SERVICE_NAME = "serviceName";
  public static final String FIELD_BODY = "body";
  public static final String FIELD_PATH = "path";
  public static final String FIELD_METHOD = "method";

  public static Future<BackendEventBusMessage> from(RoutingContext ctx) {
    return Future.future(handler -> {
      HttpServerRequest request = ctx.request();
      request.body()
        .onSuccess(buffer -> {
          String body;
          try {
            body = new JsonObject(buffer.toString()).toString();
          } catch (Exception e) {
            body = buffer.toString();
          }
          handler.complete(new BackendEventBusMessage(
            ctx.pathParam(FIELD_SERVICE_NAME),
            request.path()
              + (request.query() != null ? ("?" + request.query()) : ""),
            request.method(),
            body
          ));
        })
        .onFailure(handler::fail);

    });
  }

  public static BackendEventBusMessage from(String jsonString) {
    JsonObject jsonObject = new JsonObject(jsonString);
    return new BackendEventBusMessage(
      jsonObject.getString(FIELD_SERVICE_NAME),
      jsonObject.getString(FIELD_PATH),
      HttpMethod.valueOf(jsonObject.getString(FIELD_METHOD)),
      jsonObject.getString(FIELD_BODY));
  }

  @Override
  public String toString() {
    return new JsonObject()
      .put(FIELD_SERVICE_NAME, serviceName)
      .put(FIELD_BODY, body)
      .put(FIELD_PATH, path)
      .put(FIELD_METHOD, method.name())
      .toString();
  }
}
