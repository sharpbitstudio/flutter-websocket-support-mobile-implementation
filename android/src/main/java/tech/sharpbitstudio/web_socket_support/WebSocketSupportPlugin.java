package tech.sharpbitstudio.web_socket_support;

import android.util.Log;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import tech.sharpbitstudio.web_socket_support.handlers.PlatformChannelsHandler;

/**
 * WebSocketSupportPlugin
 */
public class WebSocketSupportPlugin implements FlutterPlugin {

  private static final String TAG = "WebSocketSupportPlugin";

  public static final String PLUGIN_NAME = "tech.sharpbitstudio.web_socket_support";
  public static final String METHOD_CHANNEL_NAME = PLUGIN_NAME + "/methods";
  public static final String EVENT_CHANNEL_NAME_TEXT_MESSAGES = PLUGIN_NAME + "/text-messages";
  public static final String EVENT_CHANNEL_NAME_BINARY_MESSAGES = PLUGIN_NAME + "/binary-messages";

  private PlatformChannelsHandler platformChannelsHandler;

  // locals
  /// The MethodChannel and EventChannels used for communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel methodChannel;
  private EventChannel textMessageChannel;
  private EventChannel binaryMessageChannel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    // set plugin channels
    initializeChannels(flutterPluginBinding);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    cleanUpChannels();
  }

  private void initializeChannels(FlutterPluginBinding flutterPluginBinding) {

    // method channel
    methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(),
        METHOD_CHANNEL_NAME);
    platformChannelsHandler = new PlatformChannelsHandler(methodChannel);

    // text messages channel
    textMessageChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(),
        EVENT_CHANNEL_NAME_TEXT_MESSAGES);
    textMessageChannel.setStreamHandler(platformChannelsHandler.getTextStreamHandler());

    // binary messages channel
    binaryMessageChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(),
        EVENT_CHANNEL_NAME_BINARY_MESSAGES);
    binaryMessageChannel.setStreamHandler(platformChannelsHandler.getBinaryStreamHandler());

    // log success
    Log.i(TAG, "WebSocketSupportPlugin successfully initialized.");
  }

  private void cleanUpChannels() {
    // remove handlers
    methodChannel.setMethodCallHandler(null);
    binaryMessageChannel.setStreamHandler(null);
    textMessageChannel.setStreamHandler(null);

    // remove platform channels handler
    platformChannelsHandler.cleanUp();
    platformChannelsHandler = null;

    // remove channels
    methodChannel = null;
    binaryMessageChannel = null;
    textMessageChannel = null;

    // log clean-up success
    Log.i(TAG, "Successfully cleaned up.");
  }
}
