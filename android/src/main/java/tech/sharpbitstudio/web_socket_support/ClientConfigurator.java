package tech.sharpbitstudio.web_socket_support;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import okhttp3.OkHttpClient;

public class ClientConfigurator {

  public OkHttpClient configure(OkHttpClient okHttpClient, Map<String, Object> options) {
    // TODO: ws client needs to be customized based on options parameter
    return okHttpClient.newBuilder()
        .pingInterval(Duration.of(30, ChronoUnit.SECONDS)).build();
  }
}
