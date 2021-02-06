package tech.sharpbitstudio.web_socket_support;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import okhttp3.OkHttpClient;

/**
 * WebSocketSupportPlugin
 */
public class WebSocketSupportPlugin implements FlutterPlugin {

  private static final String TAG = "WebSocketSupportPlugin";

  public static final String PLUGIN_NAME = "tech.sharpbitstudio.web_socket_support";
  public static final String METHOD_CHANNEL_NAME = PLUGIN_NAME + "/methods";
  public static final String EVENT_CHANNEL_NAME_TEXT_MESSAGES = PLUGIN_NAME + "/text-messages";
  public static final String EVENT_CHANNEL_NAME_BINARY_MESSAGES = PLUGIN_NAME + "/binary-messages";

  // locals
  /// The MethodChannel and EventChannels used for communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel methodChannel;
  private EventChannel textMessageChannel;
  private EventChannel binaryMessageChannel;
  private WebSocketClient webSocketClient;

  // shared OkHttpClient
  private final OkHttpClient okHttpClient = new OkHttpClient();

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    // set plugin channels
    // method channel
    methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(),
        METHOD_CHANNEL_NAME);

    // text messages channel
    textMessageChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(),
        EVENT_CHANNEL_NAME_TEXT_MESSAGES);

    // binary messages channel
    binaryMessageChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(),
        EVENT_CHANNEL_NAME_BINARY_MESSAGES);

    // create WebSocketClient
    webSocketClient = new WebSocketClient(okHttpClient,
        new Handler(Looper.getMainLooper()), new ClientConfigurator(),
        methodChannel, textMessageChannel, binaryMessageChannel);

    // log success
    Log.i(TAG, "WebSocketSupportPlugin successfully initialized.");
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    // terminate WebSocketClient
    if (webSocketClient != null) {
      webSocketClient.terminate();
      webSocketClient = null;
    }

    // remove all handlers
    if (methodChannel != null) {
      methodChannel.setMethodCallHandler(null);
    }
    if (binaryMessageChannel != null) {
      binaryMessageChannel.setStreamHandler(null);
    }
    if (textMessageChannel != null) {
      textMessageChannel.setStreamHandler(null);
    }

    // remove channels
    methodChannel = null;
    binaryMessageChannel = null;
    textMessageChannel = null;

    // log clean-up success
    Log.i(TAG, "WebSocketSupportPlugin successfully cleaned up.");
  }
}
