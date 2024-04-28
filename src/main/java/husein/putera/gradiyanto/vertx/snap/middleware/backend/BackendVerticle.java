package husein.putera.gradiyanto.vertx.snap.middleware.backend;

import husein.putera.gradiyanto.vertx.snap.middleware.http.HttpServerVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BackendVerticle extends AbstractVerticle {

  public static final String EB_ADDRESS = "backend";
  public static final byte SOCKET_END_BYTE = -0x01;
  private final BackendConfig config;
  protected Map<String, Service> services;
  private final Logger logger;

  public BackendVerticle(BackendConfig config) {
    logger = LoggerFactory.getLogger(this.getClass());
    this.config = config;
  }

  @Override
  public void start() {
    init();
    vertx.eventBus().consumer(EB_ADDRESS, message -> {
      BackendEventBusMessage gwMessage = BackendEventBusMessage.from(message.body().toString());
      String serviceName = gwMessage.serviceName();

      if (!services.containsKey(serviceName)) {
        logger.warn("Cannot send request [" + gwMessage.body() + "] to unregistered Service [" + serviceName + "]");
        message.fail(404, "Unregistered Service [" + serviceName + "]");
      }

      Service service = services.get(serviceName);
      switch (service.type()) {
        case HTTP, HTTPS -> sendHttpClientRequest(message, gwMessage, service);
        case TCP -> sendSocketRequest(message, gwMessage, service);
        default -> message.fail(500, "Cannot handle message [" + gwMessage + "]. Unknown service type: " + service.type());
      }
    });
  }

  protected RequestOptions constructRequestOptions(Message<Object> message, BackendEventBusMessage gwMessage, Service service) {
    String servicePath = gwMessage.path()
      .substring(HttpServerVerticle.PATH_PREFIX_GATEWAY.length() + gwMessage.serviceName().length());
    RequestOptions options = new RequestOptions()
      .setMethod(gwMessage.method())
      .setHost(service.host())
      .setURI(servicePath)
      .setHeaders(message.headers())
      .setSsl(service.type().equals(Service.Type.HTTPS));
    service.port().ifPresent(options::setPort);
    return options;
  }

  protected String getFullUrl(RequestOptions options) {
    return (Boolean.TRUE.equals(options.isSsl()) ? "https" : "http") + "://" + options.getHost()
      + (options.getPort() != null ? (":" + options.getPort()) : "")
      + options.getURI();
  }

  // TODO: HTTPS is still not supported
  protected void sendHttpClientRequest(Message<Object> message, BackendEventBusMessage gwMessage, Service service) {
    RequestOptions options = constructRequestOptions(message, gwMessage, service);
    logger.info("BACKEND REQUEST to [" + getFullUrl(options) + "] : " + gwMessage.body());
    vertx.createHttpClient(new HttpClientOptions()
        .setSsl(service.type().equals(Service.Type.HTTPS))
        .setTrustAll(true))
      .request(options)
      .timeout(service.timeout(), TimeUnit.SECONDS)
      .onSuccess(request -> request.send(gwMessage.body())
        .onSuccess(response -> response.body()
          .onSuccess(responseBuffer -> {
            logger.info("BACKEND RESPONSE from [" + getFullUrl(options) + "] (" + response.statusCode() + "): " + responseBuffer.toString());
            message.reply(
              responseBuffer.toString(),
              new DeliveryOptions()
                .setHeaders(response.headers()));
          }))
        .onFailure(cause -> {
          logger.error("Failed to send request [" + gwMessage.body() + "] to [" + getFullUrl(options) + "]", cause);
          message.fail(502, cause.getMessage());
        })
      )
      .onFailure(cause -> {
        logger.error("Failed to connect to Backend [" + getFullUrl(options) + "]", cause);
        message.fail(404, cause.getMessage());
      });
  }

  protected String getErrorMessage(BackendEventBusMessage gwMessage, Service service) {
    return "Failed to send message [" + gwMessage.body() + "] to Backend [" + service.host() + ":" + service.port() + "]";
  }

  protected void sendSocketRequest(Message<Object> message, BackendEventBusMessage gwMessage, Service service) {
    logger.info("BACKEND REQUEST to [" + service.host() + ":" + service.port() + "] : " + gwMessage.body());
    vertx.createNetClient()
      .connect(service.port().orElseThrow(() -> new NullPointerException("Backend port is missing")), service.host())
      .timeout(service.timeout(), TimeUnit.SECONDS)
      .onSuccess(conn -> {
        logger.debug("Connected to Backend [" + service.host() + ":" + service.port() + "]");
        Buffer readBuffer = Buffer.buffer();
        conn.write(gwMessage.body())
          .onSuccess(ignored -> conn.write(Buffer.buffer(new byte[]{ SOCKET_END_BYTE }))
            .onSuccess(ignored2 -> logger.debug("Finished sending message [" + gwMessage.body() + "] to Backend [" + service.host() + ":" + service.port() + "]"))
            .onFailure(cause -> {
              logger.error(getErrorMessage(gwMessage, service), cause);
              message.fail(502, cause.toString());
            }))
          .onFailure(cause -> {
            logger.error(getErrorMessage(gwMessage, service), cause);
            message.fail(502, cause.toString());
          });
        conn.handler(buffer -> {
          readBuffer.appendBuffer(buffer);
          if (buffer.getByte(buffer.length() - 1) == SOCKET_END_BYTE) {
            conn.end();
            String backendResponse = readBuffer.slice(0, readBuffer.length() - 1).toString();
            logger.info("BACKEND RESPONSE from [" + service.host() + ":" + service.port() + "] : " + backendResponse);
            message.reply(
              new JsonObject(backendResponse)
                .toString(),
              new DeliveryOptions()
                .addHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json"));
          }
        });
      })
      .onFailure(cause -> {
        logger.error("Failed to connect to Backend [" + service.host() + ":" + service.port() + "]", cause);
        message.fail(502, cause.toString());
      });
  }

  protected void init() {
    services = config.services();
  }
}
