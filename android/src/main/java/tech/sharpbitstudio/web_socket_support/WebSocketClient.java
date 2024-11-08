package tech.sharpbitstudio.web_socket_support;

import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_CODE;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_OPTIONS;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_REASON;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_URL;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_CONNECT;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_DISCONNECT;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_SEND_BYTE_ARRAY_MSG;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_SEND_STRING_MSG;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.OUT_METHOD_NAME_ON_BYTE_ARRAY_MSG;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.OUT_METHOD_NAME_ON_STRING_MSG;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventContext;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventType;
import tech.sharpbitstudio.web_socket_support.handlers.WebSocketStreamHandler;

public class WebSocketClient extends WebSocketListener implements MethodCallHandler {

  private static final String TAG = "WebSocketClient";

  // The singleton HTTP client.
  public final OkHttpClient okHttpClient;
  private final Handler mainThreadHandler;
  private final ClientConfigurator clientConfigurator;
  private final MethodChannel methodChannel;

  // flutter event sinks
  private EventSink byteMessagesEventSink;
  private EventSink textMessagesEventSink;

  // locals
  private WebSocket webSocket;
  private boolean autoReconnect = false;
  private int delayedConnectAttempt;

  // constructor
  public WebSocketClient(
      @NonNull OkHttpClient okHttpClient,
      @NonNull Handler mainThreadHandler,
      @NonNull ClientConfigurator clientConfigurator,
      @NonNull MethodChannel methodChannel,
      @NonNull EventChannel textMessageEventChannel,
      @NonNull EventChannel binaryMessageEventChannel) {
    this.okHttpClient = okHttpClient;
    this.mainThreadHandler = mainThreadHandler;
    this.clientConfigurator = clientConfigurator;

    // subscribe as method channel handler
    this.methodChannel = methodChannel;
    this.methodChannel.setMethodCallHandler(this);

    // setup textStreamHandler and subscribe to textMessageEventChannel
    textMessageEventChannel.setStreamHandler(
        new WebSocketStreamHandler(
            (args, sink) -> {
              textMessagesEventSink = sink;
              Log.i(TAG, "TextMessage EventSink activated! [arguments:" + args + "]");
            },
            (args) -> {
              textMessagesEventSink = null;
              Log.i(TAG, "TextMessage EventSink removed! [arguments:" + args + "]");
            }));

    // setup binaryStreamHandler and subscribe to binaryMessageEventChannel
    binaryMessageEventChannel.setStreamHandler(
        new WebSocketStreamHandler(
            (args, sink) -> {
              byteMessagesEventSink = sink;
              Log.i(TAG, "BinaryMessage EventSink activated! [arguments:" + args + "]");
            },
            (args) -> {
              byteMessagesEventSink = null;
              Log.i(TAG, "BinaryMessage EventSink removed! [arguments:" + args + "]");
            }));

    Log.i(TAG, "WebSocketClient created.");
  }

  @Override
  public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
    Log.i(TAG, "WS connected. [instance hash:" + webSocket.hashCode() + "]");
    this.webSocket = webSocket;
    this.delayedConnectAttempt = 0;

    // notify flutter about onOpen event
    mainThreadHandler.post(
        () ->
            methodChannel.invokeMethod(
                SystemEventType.WS_OPENED.getMethodName(),
                SystemEventContext.builder().build().toMap()));
  }

  @Override
  public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
    Log.d(TAG, "Text message received. content:" + text);
    mainThreadHandler.post(
        () -> {
          if (textMessagesEventSink != null) {
            try {
              textMessagesEventSink.success(text);
            } catch (Exception e) {
              // sending system error should be critical
              Log.e(TAG, "Exception while trying to send data to text channel.");
              throw e;
            }
          } else {
            // fall back to method call
            Log.i(TAG, "TextMessagesEventSink was null! Falling back to method call.");
            methodChannel.invokeMethod(OUT_METHOD_NAME_ON_STRING_MSG, text);
          }
        });
  }

  @Override
  public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString byteString) {
    Log.d(TAG, "Byte message received. size:" + byteString.size());
    mainThreadHandler.post(
        () -> {
          if (byteMessagesEventSink != null) {
            try {
              byteMessagesEventSink.success(byteString.toByteArray());
            } catch (Exception e) {
              // sending system error should be critical
              Log.e(TAG, "Exception while trying to send data to byte channel.");
              throw e;
            }
          } else {
            // fall back to method call
            Log.i(TAG, "ByteMessagesEventSink was null! Falling back to method call.");
            methodChannel.invokeMethod(OUT_METHOD_NAME_ON_BYTE_ARRAY_MSG, byteString.toByteArray());
          }
        });
  }

  @Override
  public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    Log.i(TAG, "WS is about to close. Code:" + code + ", Reason:" + reason);
    mainThreadHandler.post(
        () ->
            methodChannel.invokeMethod(
                SystemEventType.WS_CLOSING.getMethodName(),
                SystemEventContext.builder().closeCode(code).closeReason(reason).build().toMap()));
  }

  @Override
  public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    Log.i(TAG, "WS closed. Code:" + code + ", Reason:" + reason);
    mainThreadHandler.post(
        () -> {
          methodChannel.invokeMethod(
              SystemEventType.WS_CLOSED.getMethodName(),
              SystemEventContext.builder().closeCode(code).closeReason(reason).build().toMap());
          cleanUpOnClose();
        });
  }

  @Override
  public void onFailure(
      @NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
    Log.e(TAG, "Error occurred on ws channel. Error:" + t.getMessage() + ". Response:" + response);
    mainThreadHandler.post(
        () -> {
          final Map<String, Object> context =
              SystemEventContext.builder()
                  .throwableType(t.getClass().getSimpleName())
                  .errorMessage(t.getMessage())
                  .causeMessage(t.getCause() != null ? t.getCause().toString() : null)
                  .build()
                  .toMap();
          methodChannel.invokeMethod(SystemEventType.WS_FAILURE.getMethodName(), context);
          cleanUpOnClose();
        });
  }

  /**
   * Handles calls from Flutter (via Platform channel)
   *
   * <p>{@inheritDoc}
   *
   * @param call MethodCall
   * @param result Result
   */
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    switch (call.method) {

        // connect
      case IN_METHOD_NAME_CONNECT:
        {

          // get arguments from call
          final String url = call.argument(ARGUMENT_URL);
          final Map<String, Object> options = call.argument(ARGUMENT_OPTIONS);

          // connect to WS server
          connect(Objects.requireNonNull(url), options);
          result.success(true);
          break;
        }

        // disconnect
      case IN_METHOD_NAME_DISCONNECT:
        {

          // get arguments from call
          final Integer code = call.argument(ARGUMENT_CODE);
          final String reason = call.argument(ARGUMENT_REASON);

          disconnect(code, reason);
          result.success(true);
          break;
        }

        // send text message
      case IN_METHOD_NAME_SEND_STRING_MSG:
        {
          final String message = call.arguments();
          if (sendTextMessage(message)) {
            result.success(true);
          } else {
            // TODO: error code should be reconsidered
            Log.e(TAG, "Unable to send text message to Ws server!");
            result.error("01", "Unable to send text message!", null);
          }
          break;
        }

        // send byte message
      case IN_METHOD_NAME_SEND_BYTE_ARRAY_MSG:
        {
          final byte[] message = call.arguments();
          if (sendByteMessage(ByteString.of(message != null ? message : new byte[0]))) {
            result.success(true);
          } else {
            // TODO: error code should be reconsidered
            Log.e(TAG, "Unable to send binary message to Ws server!");
            result.error("02", "Unable to send binary message!", null);
          }
          break;
        }

        // if unexpected (all non specified methods)
      default:
        Log.w(TAG, "Unexpected MethodCall: " + call.method);
        result.notImplemented();
    }
  }

  public void terminate() {
    // TODO
    disconnect(1001, "Client terminated");
    this.methodChannel.setMethodCallHandler(null);
    Log.i(TAG, "WebSocketClient terminated.");
  }

  /// PRIVATE

  /**
   * Used to customize OkHttpClient and connect to WS Endpoint.
   *
   * <p>Creates a new web socket and immediately returns it. Creating a web socket initiates an
   * asynchronous process to connect the socket. Once that succeeds or fails, `listener` will be
   * notified. The caller must either close or cancel the returned web socket when it is no longer
   * in use.
   *
   * @param serverUrl server URL.
   * @param options key-value map data used to configure connection.
   */
  private void connect(String serverUrl, Map<String, Object> options) {

    if (webSocket != null) {
      Log.w(TAG, "WS Connection still active on new connect attempt. Disconnecting...");
      disconnect(1001, "Connection restart."); // call disconnect and wait for onClose
      // schedule next try and return for now...
      tryDelayedConnect(serverUrl, options);
      return;
    }

    // set locals
    this.autoReconnect = (boolean) options.computeIfAbsent("autoReconnect", (s) -> false);

    // prepare request
    final Request request = new Request.Builder().url(serverUrl).build();

    // customize default ws client
    final OkHttpClient client = clientConfigurator.configure(okHttpClient, options);

    // connect to server and register as listener
    client.newWebSocket(request, this);

    // done
    Log.i(TAG, "Connection request sent to: " + serverUrl);
  }

  private void tryDelayedConnect(String serverUrl, Map<String, Object> options) {
    // try connect again in 1 sec.
    delayedConnectAttempt++;
    Log.i(TAG, "Scheduling delayed connect #" + delayedConnectAttempt);
    if (delayedConnectAttempt > 3) {
      if (webSocket != null) {
        // kill current web-socket session
        Log.w(TAG, "Killing violently web socket connection...");
        webSocket.cancel();
      }
    }
    mainThreadHandler.postDelayed(() -> connect(serverUrl, options), 1000);
  }

  /**
   * Attempts to initiate a graceful shutdown of this web socket. Any already-enqueued messages will
   * be transmitted before the close message is sent but subsequent calls to send will return false
   * and their messages will not be enqueued.
   *
   * <p>Close code sent to server will be 1000.
   *
   * @param code disconnection code
   * @param reason Reason to disconnect
   */
  private void disconnect(Integer code, String reason) {
    autoReconnect = false;
    if (webSocket != null) {
      webSocket.close(code != null ? code : 1000, reason != null ? reason : "Client done.");
    } else {
      Log.w(TAG, "WebSocket was null on disconnect.");
    }
  }

  /**
   * Sends String message to server via established WebSocket connection.
   *
   * <p>This method returns true if the message was enqueued. Messages that would overflow the
   * outgoing message buffer will be rejected and trigger a graceful shutdown of this web socket.
   * This method returns false in that case, and in any other case where this web socket is closing,
   * closed, or canceled.
   *
   * @param message String message to send to server
   * @return true if successful
   */
  private boolean sendTextMessage(String message) {
    if (webSocket != null) {
      return webSocket.send(message);
    } else {
      Log.w(TAG, "WebSocket is not connected yet. Unable to send text message...");
      return false;
    }
  }

  /**
   * Send ByteString to server via established WebSocket connection.
   *
   * <p>This method returns true if the message was enqueued. Messages that would overflow the
   * outgoing message buffer (16 MiB) will be rejected and trigger a graceful shutdown of this web
   * socket. This method returns false in that case, and in any other case where this web socket is
   * closing, closed, or canceled. This method returns immediately.
   *
   * @param message ByteString message to send to server
   * @return true if successful
   */
  private boolean sendByteMessage(ByteString message) {
    if (webSocket != null) {
      return webSocket.send(message);
    } else {
      Log.w(TAG, "WebSocket is not connected yet. Unable to send byte message...");
      return false;
    }
  }

  private void cleanUpOnClose() {
    webSocket = null;
  }
}
