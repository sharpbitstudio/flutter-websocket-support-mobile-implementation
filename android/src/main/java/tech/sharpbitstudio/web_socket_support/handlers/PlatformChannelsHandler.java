package tech.sharpbitstudio.web_socket_support.handlers;

import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_BYTE_MESSAGE;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_OPTIONS;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_TEXT_MESSAGE;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_URL;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_CONNECT;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_DISCONNECT;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_SEND_BYTE_MSG;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_SEND_TEXT_MSG;
import static tech.sharpbitstudio.web_socket_support.Constants.METHOD_PLATFORM_VERSION;
import static tech.sharpbitstudio.web_socket_support.Constants.OUT_METHOD_NAME_ON_BYTE_MSG;
import static tech.sharpbitstudio.web_socket_support.Constants.OUT_METHOD_NAME_ON_TEXT_MSG;

import android.util.Log;
import androidx.annotation.NonNull;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import okio.ByteString;
import tech.sharpbitstudio.web_socket_support.WebSocketClient;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventContext;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventType;

public class PlatformChannelsHandler implements MethodCallHandler {

  private static final String TAG = "WebSocketMethodHandler";

  // locals
  private final WebSocketClient webSocketClient;
  private final MethodChannel methodChannel;

  @Getter
  private final StreamHandler binaryStreamHandler;
  @Getter
  private final StreamHandler textStreamHandler;

  private EventSink byteMessagesEventSink;
  private EventSink textMessagesEventSink;

  private BiConsumer<SystemEventType, SystemEventContext> systemMessagesConsumer;
  private Consumer<String> textMessagesConsumer;
  private Consumer<ByteString> byteMessagesConsumer;

  /**
   * WebSocketMethodCallHandler Constructor
   *
   * @param methodChannel method channel used to send and receive calls to and from flutter
   */
  public PlatformChannelsHandler(MethodChannel methodChannel) {

    // set method channel and subscribe as handler
    this.methodChannel = methodChannel;
    this.methodChannel.setMethodCallHandler(this);

    // init WebSocketListener
    webSocketClient = new WebSocketClient();

    // setup binaryStreamHandler
    binaryStreamHandler = new WebSocketStreamHandler((args, sink) -> {
      byteMessagesEventSink = sink;
      Log.i(TAG, "setBinaryMessageEventSink -> arguments:" + args + ", eventSink:" + sink
          .toString());
    }, (args) -> {
      byteMessagesEventSink = null;
      Log.i(TAG, "removeBinaryMessageEventSink -> arguments:" + args);
    });

    // setup textStreamHandler
    textStreamHandler = new WebSocketStreamHandler((args, sink) -> {
      textMessagesEventSink = sink;
      Log.i(TAG, "setTextMessageEventSink -> arguments:" + args + ", eventSink:" + sink
          .toString());
    }, (args) -> {
      textMessagesEventSink = null;
      Log.i(TAG, "removeTextMessageEventSink -> arguments:" + args);
    });

    // setup ws client consumers
    systemMessagesConsumer = this::onSystemEvent;
    textMessagesConsumer = this::onTextMessageEvent;
    byteMessagesConsumer = this::onByteMessageEvent;

    // all done
    Log.i(TAG, "WebSocketMethodCallHandler initialized.");
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    switch (call.method) {
      case METHOD_PLATFORM_VERSION: {
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        return;
      }

      // connect
      case IN_METHOD_NAME_CONNECT: {

        // get arguments from call
        String url = call.argument(ARGUMENT_URL);
        Map<String, Object> options = call.argument(ARGUMENT_OPTIONS);

        // connect to WS server
        webSocketClient
            .connect(Objects.requireNonNull(url), options, systemMessagesConsumer,
                textMessagesConsumer, byteMessagesConsumer);
        result.success(null);
        break;
      }

      // disconnect
      case IN_METHOD_NAME_DISCONNECT: {
        webSocketClient.disconnect();
        result.success(null);
        break;
      }

      // send text message
      case IN_METHOD_NAME_SEND_TEXT_MSG: {
        String message = call.argument(ARGUMENT_TEXT_MESSAGE);
        result.success(webSocketClient.sendTextMessage(message));
        break;
      }

      // send byte message
      case IN_METHOD_NAME_SEND_BYTE_MSG: {
        byte[] message = call.argument(ARGUMENT_BYTE_MESSAGE);
        result.success(webSocketClient
            .sendByteMessage(ByteString.of(message != null ? message : new byte[0])));
        break;
      }

      // if unexpected
      default:
        result.notImplemented();
    }
  }

  /// PRIVATE

  private void onSystemEvent(SystemEventType systemEventType,
      SystemEventContext systemEventContext) {
    methodChannel.invokeMethod(systemEventType.getMethodName(), systemEventContext.toMap());
  }

  private void onTextMessageEvent(String textMessage) {
    if (textMessagesEventSink != null) {
      try {
        textMessagesEventSink.success(textMessage);
      } catch (Exception e) {
        // sending system error should be critical
        Log.e(TAG,
            "Exception while trying to send data to text channel. data:" + textMessage);
        throw e;
      }
    } else {
      // fall back to method call
      Log.i(TAG, "TextMessagesEventSink was null! Falling back to method call.");
      methodChannel.invokeMethod(OUT_METHOD_NAME_ON_TEXT_MSG, textMessage);
    }
  }

  private void onByteMessageEvent(ByteString byteMessage) {
    if (byteMessagesEventSink != null) {
      try {
        byteMessagesEventSink.success(byteMessage.toByteArray());
      } catch (Exception e) {
        // sending system error should be critical
        Log.e(TAG,
            "Exception while trying to send data to byte channel. data.size():" + byteMessage
                .size());
        throw e;
      }
    } else {
      // fall back to method call
      Log.i(TAG, "ByteMessagesEventSink was null! Falling back to method call.");
      methodChannel.invokeMethod(OUT_METHOD_NAME_ON_BYTE_MSG, byteMessage.toByteArray());
    }
  }

  // clean any dangling resource
  public void cleanUp() {
    webSocketClient.disconnect();
    byteMessagesEventSink.endOfStream();
    byteMessagesEventSink = null;
    textMessagesEventSink.endOfStream();
    textMessagesEventSink = null;
  }
}
