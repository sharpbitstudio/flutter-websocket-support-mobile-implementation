package tech.sharpbitstudio.web_socket_support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static tech.sharpbitstudio.web_socket_support.ClientConfigurator.PING_INTERVAL_SEC;

import java.util.Collections;
import okhttp3.OkHttpClient;
import org.junit.Test;

public class ClientConfiguratorTest {

  // tested class
  private ClientConfigurator configurator;

  @Test
  public void pingIntervalSetupTest() {

    // prepare object
    configurator = new ClientConfigurator();

    // test method
    OkHttpClient result = configurator
        .configure(new OkHttpClient().newBuilder().build(), Collections.emptyMap());

    // validate returned OkHttpClient
    assertNotNull(result);
    assertEquals(PING_INTERVAL_SEC * 1000, result.pingIntervalMillis());
  }
}
