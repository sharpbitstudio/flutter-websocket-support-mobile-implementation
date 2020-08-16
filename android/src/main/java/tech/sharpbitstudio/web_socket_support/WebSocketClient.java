package tech.sharpbitstudio.web_socket_support;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventContext;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventType;

public class WebSocketClient extends WebSocketListener {

  private static final String TAG = "WebSocketClient";

  // The singleton HTTP client.
  public final OkHttpClient okHttpClient = new OkHttpClient();
  private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

  // locals
  private WebSocket webSocket;
  private boolean autoReconnect = false;
  private int delayedConnectAttempt;

  // consumers
  private BiConsumer<SystemEventType, SystemEventContext> systemConsumer;
  private Consumer<String> textMessageConsumer;
  private Consumer<ByteString> byteMessageConsumer;

  /**
   * Used to customize OkHttpClient and connect to WS Endpoint.
   * <p>
   * Creates a new web socket and immediately returns it. Creating a web socket initiates an
   * asynchronous process to connect the socket. Once that succeeds or fails, `listener` will be
   * notified. The caller must either close or cancel the returned web socket when it is no longer
   * in use.
   *
   * @param serverUrl           server URL.
   * @param options             key-value map data used to configure connection.
   * @param systemConsumer      systemConsumer
   * @param textMessageConsumer textMessageConsumer
   * @param byteMessageConsumer byteMessageConsumer
   */
  public void connect(String serverUrl, Map<String, Object> options,
      @NotNull BiConsumer<SystemEventType, SystemEventContext> systemConsumer,
      @NotNull Consumer<String> textMessageConsumer,
      @NotNull Consumer<ByteString> byteMessageConsumer) {

    if (webSocket != null) {
      Log.w(TAG, "WS Connection still active on new connect attempt. Disconnecting...");
      disconnect(); // call disconnect and wait for onClose
      // schedule next try and return for now...
      tryDelayedConnect(serverUrl, options, systemConsumer, textMessageConsumer,
          byteMessageConsumer);
      return;
    }

    // set locals
    this.autoReconnect = (boolean) options.computeIfAbsent("autoReconnect", (s) -> false);
    this.systemConsumer = systemConsumer;
    this.textMessageConsumer = textMessageConsumer;
    this.byteMessageConsumer = byteMessageConsumer;

    // prepare request
    Request request = new Request.Builder()
        .url(serverUrl)
        .build();

    // customize default ws client
    OkHttpClient client = okHttpClient.newBuilder()
        .pingInterval(Duration.of(30, ChronoUnit.SECONDS)).build();  // TODO

    // connect to server
    client.newWebSocket(request, this);

    // done
    Log.i(TAG, "Connection request sent to: " + serverUrl);
  }

  private void tryDelayedConnect(String serverUrl, Map<String, Object> options,
      BiConsumer<SystemEventType, SystemEventContext> systemConsumer,
      Consumer<String> textMessageConsumer, Consumer<ByteString> byteMessageConsumer) {
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
    mainThreadHandler.postDelayed(
        () -> connect(serverUrl, options, systemConsumer, textMessageConsumer, byteMessageConsumer),
        1000);
  }

  /**
   * Attempts to initiate a graceful shutdown of this web socket. Any already-enqueued messages will
   * be transmitted before the close message is sent but subsequent calls to send will return false
   * and their messages will not be enqueued.
   * <p>
   * Close code sent to server will be 1000.
   */
  public void disconnect() {
    autoReconnect = false;
    if (webSocket != null) {
      webSocket.close(1000, "Client done.");
    } else {
      Log.w(TAG, "WebSocket was null on disconnect.");
    }
  }

  /**
   * Sends String message to server via established WebSocket connection.
   * <p>
   * This method returns true if the message was enqueued. Messages that would overflow the outgoing
   * message buffer will be rejected and trigger a graceful shutdown of this web socket. This method
   * returns false in that case, and in any other case where this web socket is closing, closed, or
   * canceled.
   *
   * @param message String message to send to server
   * @return true if successful
   */
  public boolean sendTextMessage(String message) {
    if (webSocket != null) {
      return webSocket.send(message);
    } else {
      Log.w(TAG, "WebSocket is not connected yet. Unable to send text message...");
      return false;
    }
  }

  /**
   * Send ByteString to server via established WebSocket connection.
   * <p>
   * This method returns true if the message was enqueued. Messages that would overflow the outgoing
   * message buffer (16 MiB) will be rejected and trigger a graceful shutdown of this web socket.
   * This method returns false in that case, and in any other case where this web socket is closing,
   * closed, or canceled. This method returns immediately.
   *
   * @param message ByteString message to send to server
   * @return true if successful
   */
  public boolean sendByteMessage(ByteString message) {
    if (webSocket != null) {
      return webSocket.send(message);
    } else {
      Log.w(TAG, "WebSocket is not connected yet. Unable to send byte message...");
      return false;
    }
  }

  @Override
  public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
    Log.i(TAG, "WS connected. WebSocket:" + webSocket.toString());
    this.webSocket = webSocket;
    this.delayedConnectAttempt = 0;

    // notify consumers about onOpen event
    mainThreadHandler.post(() -> {
      if (systemConsumer != null) {
        systemConsumer
            .accept(SystemEventType.WS_OPENED, SystemEventContext.builder().build());
      } else {
        Log.wtf(TAG,
            "WebSocketClient initialized badly [systemConsumer is null]. Closing connection...");
        disconnect();
      }
    });
  }

  @Override
  public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
    Log.d(TAG, "Text message received. content:" + text);
    mainThreadHandler.post(() -> {
      if (textMessageConsumer != null) {
        textMessageConsumer.accept(text);
      } else {
        // maybe we are not interested in text messages?
        Log.w(TAG, "Text message received but no handler for text messages defined!");
      }
    });
  }

  @Override
  public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString byteString) {
    Log.d(TAG, "Byte message received. size:" + byteString.size());
    mainThreadHandler.post(() -> {
      if (byteMessageConsumer != null) {
        byteMessageConsumer.accept(byteString);
      } else {
        // maybe we are not interested in byte messages?
        Log.w(TAG, "Byte message received but no handler for byte messages defined!");
      }
    });
  }

  @Override
  public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    Log.i(TAG, "WS is about to close. Reason:" + reason);
    mainThreadHandler.post(() -> {
      if (systemConsumer != null) {
        // notify consumers about onClosing event
        systemConsumer.accept(SystemEventType.WS_CLOSING,
            SystemEventContext.builder().closeCode(code).closeReason(reason).build());
      } else {
        Log.wtf(TAG,
            "WebSocketClient initialized badly [systemConsumer is null]. Closing connection...");
        disconnect();
      }
    });
  }

  @Override
  public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    Log.i(TAG, "WS closed. Code: " + code + ", Reason:" + reason);
    mainThreadHandler.post(() -> {
      if (systemConsumer != null) {
        // notify consumers about onClosed event
        systemConsumer.accept(SystemEventType.WS_CLOSED,
            SystemEventContext.builder().closeCode(code).closeReason(reason).build());
      } else {
        Log.wtf(TAG,
            "WebSocketClient initialized badly [systemConsumer is null]. Closing connection...");
      }
      cleanUpOnClose();
    });
  }

  @Override
  public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t,
      @Nullable Response response) {
    Log.e(TAG, "Error occurred on ws channel. Error:" + t.getMessage());
    mainThreadHandler.post(() -> {
      if (systemConsumer != null) {
        // notify consumers about onClosed event
        systemConsumer.accept(SystemEventType.WS_FAILURE,
            SystemEventContext.builder().throwableType(t.getClass().getSimpleName())
                .errorMessage(t.getMessage())
                .causeMessage(t.getCause() != null ? t.getCause().toString() : null).build());
      } else {
        Log.wtf(TAG,
            "WebSocketClient initialized badly [systemConsumer is null]. Closing connection...");
      }
      cleanUpOnClose();
    });
  }

  private void cleanUpOnClose() {
    webSocket = null;
    // remove consumers
    systemConsumer = null;
    textMessageConsumer = null;
    byteMessageConsumer = null;
  }
}
