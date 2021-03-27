package tech.sharpbitstudio.web_socket_support;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import org.junit.Test;
import org.mockito.Mockito;

public class WebSocketSupportPluginTest {

  // tested class
  private WebSocketSupportPlugin plugin = new WebSocketSupportPlugin();

  @Test
  public void onAttachedToEngineTest() {

    // prepare mocks
    final FlutterPluginBinding pluginBinding = Mockito.mock(FlutterPluginBinding.class);
    when(pluginBinding.getBinaryMessenger()).thenReturn(Mockito.mock(BinaryMessenger.class));

    // test method
    plugin.onAttachedToEngine(pluginBinding);

    // verify
    verify(pluginBinding, times(3)).getBinaryMessenger();
  }

  @Test
  public void onDetachedFromEngineTest() {

    // prepare mocks
    final FlutterPluginBinding pluginBinding = Mockito.mock(FlutterPluginBinding.class);
    when(pluginBinding.getBinaryMessenger()).thenReturn(Mockito.mock(BinaryMessenger.class));

    // attachToEngine
    plugin.onAttachedToEngine(pluginBinding);

    // verify
    verify(pluginBinding, times(3)).getBinaryMessenger();

    // test method
    plugin.onDetachedFromEngine(pluginBinding);

    // verify
    // TODO
  }

  @Test
  public void onDetachedFromEngineWithoutAttachTest() {

    // prepare mocks
    final FlutterPluginBinding pluginBinding = Mockito.mock(FlutterPluginBinding.class);

    // test method
    plugin.onDetachedFromEngine(pluginBinding);

    // verify
    // TODO
  }
}
