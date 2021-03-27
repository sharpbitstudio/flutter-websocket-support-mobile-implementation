package tech.sharpbitstudio.web_socket_support;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

public class ClientConfigurator {

  public static final int PING_INTERVAL_SEC = 30;

  public OkHttpClient configure(@NotNull OkHttpClient okHttpClient, Map<String, Object> options) {
    // TODO: ws client needs to be customized based on options parameter
    return okHttpClient.newBuilder()
        .pingInterval(Duration.of(PING_INTERVAL_SEC, ChronoUnit.SECONDS)).build();
  }
}
