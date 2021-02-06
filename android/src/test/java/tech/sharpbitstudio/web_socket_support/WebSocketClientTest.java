package tech.sharpbitstudio.web_socket_support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_BYTE_MESSAGE;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_CODE;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_OPTIONS;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_REASON;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_TEXT_MESSAGE;
import static tech.sharpbitstudio.web_socket_support.Constants.ARGUMENT_URL;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_CONNECT;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_DISCONNECT;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_SEND_BYTE_MSG;
import static tech.sharpbitstudio.web_socket_support.Constants.IN_METHOD_NAME_SEND_TEXT_MSG;

import android.os.Handler;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.text.Charsets;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventType;

public class WebSocketClientTest {

  // mocks
  private final OkHttpClient mockedClient = Mockito.mock(OkHttpClient.class);
  private final Handler handler = Mockito.mock(Handler.class);
  private final ClientConfigurator configurator = Mockito.mock(ClientConfigurator.class);
  private final MethodChannel methodChannel = Mockito.mock(MethodChannel.class);
  private final EventChannel textMessageEventChannel = Mockito.mock(EventChannel.class);
  private final EventChannel binaryMessageEventChannel = Mockito.mock(EventChannel.class);
  private final EventSink textEventSink = Mockito.mock(EventSink.class);
  private final EventSink byteEventSink = Mockito.mock(EventSink.class);

  // locals
  private final AtomicReference<MethodCallHandler> callHandler = new AtomicReference<>();

  // tested class
  private WebSocketClient client;

  @BeforeEach
  void before() {
    Mockito.clearInvocations(mockedClient, handler, configurator, methodChannel,
        textMessageEventChannel, binaryMessageEventChannel, textEventSink);

    // common stubbing
    doAnswer(invocation -> {
      callHandler.set(invocation.getArgument(0));
      return null;
    }).when(methodChannel).setMethodCallHandler(any(MethodCallHandler.class));
    doAnswer(invocation -> {
      callHandler.get()
          .onMethodCall(new MethodCall(invocation.getArgument(0), invocation.getArgument(1)),
              invocation.getArgument(2));
      return null;
    }).when(methodChannel).invokeMethod(anyString(), anyMap(), any(Result.class));
    when(configurator.configure(any(OkHttpClient.class), anyMap()))
        .thenReturn(mockedClient);
    when(handler.post(any(Runnable.class))).then(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    });
    doAnswer(invocation -> {
      ((StreamHandler) invocation.getArgument(0)).onListen(null, textEventSink);
      return null;
    }).when(textMessageEventChannel).setStreamHandler(any(StreamHandler.class));
    doAnswer(invocation -> {
      ((StreamHandler) invocation.getArgument(0)).onListen(null, byteEventSink);
      return null;
    }).when(binaryMessageEventChannel).setStreamHandler(any(StreamHandler.class));

    client = new WebSocketClient(mockedClient, handler, configurator,
        methodChannel, textMessageEventChannel, binaryMessageEventChannel);
  }

  @Test
  void connectTest() {

    // prepare data
    Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_URL, "http://fakeUrl");
    arguments.put(ARGUMENT_OPTIONS, new HashMap<>());

    // stubbing
    when(mockedClient.newWebSocket(any(Request.class), any(WebSocketListener.class)))
        .then(invocation -> {
          client.onOpen(Mockito.mock(WebSocket.class), Mockito.mock(Response.class));
          return null;
        });

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_CONNECT, arguments, Mockito.mock(Result.class));

    // validate that WS_OPENED is called on method channel
    ArgumentCaptor<String> argumentMethodName = ArgumentCaptor.forClass(String.class);
    verify(methodChannel).invokeMethod(argumentMethodName.capture(), any());
    assertEquals(SystemEventType.WS_OPENED.getMethodName(), argumentMethodName.getValue());
  }

  @Test
  void clientDisconnectTest() {

    // data
    final Integer code = 1234;
    final String reason = "test reason 1";
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_CODE, code);
    arguments.put(ARGUMENT_REASON, reason);

    // stubbing
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.close(any(Integer.class), any(String.class)))
        .then(invocation -> {
          client.onClosed(Mockito.mock(WebSocket.class), invocation.getArgument(0, Integer.class),
              invocation.getArgument(1, String.class));
          return null;
        });

    // move state to connected
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_DISCONNECT, arguments, Mockito.mock(Result.class));

    // validate that disconnect call is propagated to WebSocket
    ArgumentCaptor<Integer> captor1 = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
    verify(mockedWebSocket).close(captor1.capture(), captor2.capture());
    assertEquals(code, captor1.getValue());
    assertEquals(reason, captor2.getValue());
  }

  @Test
  void serverDisconnectTest() {

    // data
    final int code = 1234;
    final String reason = "test reason 1";

    // test method
    client.onClosed(Mockito.mock(WebSocket.class), code, reason);

    // validate that WS_CLOSED is called on method channel
    ArgumentCaptor<String> methodNameCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> argumentsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(methodChannel).invokeMethod(methodNameCaptor.capture(), argumentsCaptor.capture());
    assertEquals(SystemEventType.WS_CLOSED.getMethodName(), methodNameCaptor.getValue());
    assertTrue(argumentsCaptor.getValue() instanceof Map);
    assertEquals(code, argumentsCaptor.getValue().get("code"));
    assertEquals(reason, argumentsCaptor.getValue().get("reason"));
  }

  @Test
  void sendTextMessageTest() {

    // data
    final String textMessage = "Test message 1";
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_TEXT_MESSAGE, textMessage);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_TEXT_MSG, arguments, Mockito.mock(Result.class));

    // verify correct message sent to web socket
    ArgumentCaptor<String> argumentMessage = ArgumentCaptor.forClass(String.class);
    verify(mockedWebSocket).send(argumentMessage.capture());
    assertEquals(textMessage, argumentMessage.getValue());
  }

  @Test
  void receiveTextMessageTest() {

    // data
    final String textMessage = "Test message 1";

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    client.onMessage(mockedWebSocket, textMessage);

    // verify that correct message is sent to EventSink
    ArgumentCaptor<String> argumentMessage = ArgumentCaptor.forClass(String.class);
    verify(textEventSink).success(argumentMessage.capture());
    assertEquals(textMessage, argumentMessage.getValue());
  }

  @Test
  void sendBinaryMessageTest() {

    // data
    final byte[] byteMessage = "Test message 1".getBytes(Charsets.UTF_8);
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_BYTE_MESSAGE, byteMessage);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_BYTE_MSG, arguments, Mockito.mock(Result.class));

    // verify correct message sent to web socket
    ArgumentCaptor<ByteString> argumentMessage = ArgumentCaptor.forClass(ByteString.class);
    verify(mockedWebSocket).send(argumentMessage.capture());
    assertEquals(ByteString.of(byteMessage), argumentMessage.getValue());
  }

  @Test
  void receiveBinaryMessageTest() {

    // data
    final byte[] bytes = "Test message 2".getBytes(Charsets.UTF_8);
    final ByteString byteMessage = ByteString.of(bytes);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    client.onMessage(mockedWebSocket, byteMessage);

    // verify that correct message is sent to EventSink
    ArgumentCaptor<byte[]> argumentMessage = ArgumentCaptor.forClass(byte[].class);
    verify(byteEventSink).success(argumentMessage.capture());
    assertArrayEquals(bytes, argumentMessage.getValue());
  }
}
