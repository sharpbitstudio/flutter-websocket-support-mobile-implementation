package tech.sharpbitstudio.web_socket_support;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_BYTE_MESSAGE;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_OPTIONS;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_TEXT_MESSAGE;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.ARGUMENT_URL;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_CONNECT;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_DISCONNECT;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_SEND_BYTE_MSG;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.IN_METHOD_NAME_SEND_TEXT_MSG;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.OUT_METHOD_NAME_ON_BYTE_MSG;
import static tech.sharpbitstudio.web_socket_support.domain.Constants.OUT_METHOD_NAME_ON_TEXT_MSG;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tech.sharpbitstudio.web_socket_support.domain.SystemEventType;

@RunWith(JUnit4.class)
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

  @Before
  public void before() {
    Mockito.clearInvocations(mockedClient, handler, configurator, methodChannel,
        textMessageEventChannel, binaryMessageEventChannel, textEventSink);

    // common stubbing
    // methodChannel
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
    // configurator
    when(configurator.configure(any(OkHttpClient.class), anyMap()))
        .thenReturn(mockedClient);
    // handler
    when(handler.post(any(Runnable.class))).then(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    });
    when(handler.postDelayed(any(Runnable.class), anyLong())).then(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    });
    // event channels
    doAnswer(invocation -> {
      ((StreamHandler) invocation.getArgument(0)).onListen(null, textEventSink);
      return null;
    }).when(textMessageEventChannel).setStreamHandler(any(StreamHandler.class));
    doAnswer(invocation -> {
      ((StreamHandler) invocation.getArgument(0)).onListen(null, byteEventSink);
      return null;
    }).when(binaryMessageEventChannel).setStreamHandler(any(StreamHandler.class));

    // instantiate target class
    client = new WebSocketClient(mockedClient, handler, configurator,
        methodChannel, textMessageEventChannel, binaryMessageEventChannel);
  }

  @Test
  public void connectTest() {

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
  public void connectWhileConnectedTest() {

    // prepare data
    Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_URL, "http://fakeUrl");
    arguments.put(ARGUMENT_OPTIONS, new HashMap<>());

    // stubbing
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedClient.newWebSocket(any(Request.class), any(WebSocketListener.class)))
        .then(invocation -> {
          client.onOpen(mockedWebSocket, Mockito.mock(Response.class));
          return null;
        });
    doAnswer(invocation -> {
      client.onClosed(mockedWebSocket, 0, "test close reason");
      return null;
    }).when(mockedWebSocket).close(anyInt(), anyString());

    // 1st connect
    methodChannel.invokeMethod(IN_METHOD_NAME_CONNECT, arguments, Mockito.mock(Result.class));

    // validate that WS_OPENED is called on method channel
    ArgumentCaptor<String> argumentMethodName = ArgumentCaptor.forClass(String.class);
    verify(methodChannel).invokeMethod(argumentMethodName.capture(), any());
    assertEquals(SystemEventType.WS_OPENED.getMethodName(), argumentMethodName.getValue());

    // 2nd connect
    methodChannel.invokeMethod(IN_METHOD_NAME_CONNECT, arguments, Mockito.mock(Result.class));

    // verify new web socket called
    verify(configurator, times(2)).configure(any(OkHttpClient.class), anyMap());
    verify(mockedClient, times(2)).newWebSocket(any(), any());
  }

  @Test
  public void unableToDisconnectTest() {

    // prepare data
    Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_URL, "http://fakeUrl");
    arguments.put(ARGUMENT_OPTIONS, new HashMap<>());

    // stubbing
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    final boolean[] firstTime = {true};
    when(mockedClient.newWebSocket(any(Request.class), any(WebSocketListener.class)))
        .then(invocation -> {
          if (firstTime[0]) {
            client.onOpen(mockedWebSocket, Mockito.mock(Response.class));
            firstTime[0] = false;
          } else {
            client.onFailure(Mockito.mock(WebSocket.class), new RuntimeException(), null);
          }
          return null;
        });
    // when(mockedWebSocket).close(anyInt(), anyString()) is not mocked on purpose
    doAnswer(invocation -> {
      client.onClosed(mockedWebSocket, 0, "test close reason");
      return null;
    }).when(mockedWebSocket).cancel();

    // test connect
    methodChannel.invokeMethod(IN_METHOD_NAME_CONNECT, arguments, Mockito.mock(Result.class));

    // validate that WS_OPENED is called on method channel
    ArgumentCaptor<String> argumentMethodName = ArgumentCaptor.forClass(String.class);
    verify(methodChannel).invokeMethod(argumentMethodName.capture(), any());
    assertEquals(SystemEventType.WS_OPENED.getMethodName(), argumentMethodName.getValue());

    // 2nd connect
    methodChannel.invokeMethod(IN_METHOD_NAME_CONNECT, arguments, Mockito.mock(Result.class));

    // validate that WS_FAILURE was called 3 times (reconnect max retries)
    ArgumentCaptor<String> argumentMethodName2 = ArgumentCaptor.forClass(String.class);
    verify(methodChannel, times(3)).invokeMethod(argumentMethodName2.capture(), any());
    assertEquals(SystemEventType.WS_FAILURE.getMethodName(), argumentMethodName2.getValue());

    // verify new web socket called
    verify(configurator, times(2)).configure(any(OkHttpClient.class), anyMap());
    verify(mockedClient, times(2)).newWebSocket(any(), any());
  }

  @Test
  public void onCancelConsumerTest() {

    // 0 -> txt; 1 -> binary
    final StreamHandler[] streamHandlers = new StreamHandler[2];

    // stubbing
    // event channels
    doAnswer(invocation -> {
      streamHandlers[0] = invocation.getArgument(0);
      return null;
    }).when(textMessageEventChannel).setStreamHandler(any(StreamHandler.class));
    doAnswer(invocation -> {
      streamHandlers[1] = invocation.getArgument(0);
      return null;
    }).when(binaryMessageEventChannel).setStreamHandler(any(StreamHandler.class));

    // instantiate target class
    client = new WebSocketClient(mockedClient, handler, configurator,
        methodChannel, textMessageEventChannel, binaryMessageEventChannel);

    // init streams
    streamHandlers[0].onListen(null, textEventSink);
    streamHandlers[1].onListen(null, byteEventSink);

    // validate textMessagesEventSink & byteMessagesEventSink
    client.onMessage(Mockito.mock(WebSocket.class), "");
    verify(textEventSink).success(anyString());
    client.onMessage(Mockito.mock(WebSocket.class), ByteString.encodeUtf8(""));
    verify(byteEventSink).success(any(byte[].class));

    // kill text stream
    streamHandlers[0].onCancel(null);
    // validate that method call was used instead of EventSink
    client.onMessage(Mockito.mock(WebSocket.class), "");
    ArgumentCaptor<String> textMethodNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(methodChannel).invokeMethod(textMethodNameCaptor.capture(), any());
    assertEquals(OUT_METHOD_NAME_ON_TEXT_MSG, textMethodNameCaptor.getValue());

    // kill byte stream
    streamHandlers[1].onCancel(null);
    // validate that method call was used (for 2nd time) instead of EventSink
    client.onMessage(Mockito.mock(WebSocket.class), ByteString.encodeUtf8(""));
    ArgumentCaptor<String> byteMethodNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(methodChannel, times(2)).invokeMethod(byteMethodNameCaptor.capture(), any());
    assertEquals(OUT_METHOD_NAME_ON_BYTE_MSG, byteMethodNameCaptor.getValue());
  }

  @Test
  public void clientDisconnectTest() {

    // data
    final Map<String, Object> arguments = new HashMap<>();

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
    assertEquals(1000, (int) captor1.getValue()); // default close code
    assertEquals("Client done.", captor2.getValue()); // default reason
  }

  @Test
  public void clientDisconnectWhileDisconnectedTest() {

    // data
    final Map<String, Object> arguments = new HashMap<>();

    // stubbing
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.close(any(Integer.class), any(String.class)))
        .then(invocation -> {
          client.onClosed(Mockito.mock(WebSocket.class), invocation.getArgument(0, Integer.class),
              invocation.getArgument(1, String.class));
          return null;
        });

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_DISCONNECT, arguments, Mockito.mock(Result.class));

    // validate that disconnect call is propagated to WebSocket
    verify(mockedWebSocket, never()).close(anyInt(), anyString());
  }

  @Test
  public void serverAboutToDisconnectTest() {

    // data
    final int code = 4321;
    final String reason = "closing reason 1";

    // test method
    client.onClosing(Mockito.mock(WebSocket.class), code, reason);

    // validate that WS_CLOSED is called on method channel
    ArgumentCaptor<String> methodNameCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> argumentsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(methodChannel).invokeMethod(methodNameCaptor.capture(), argumentsCaptor.capture());
    assertEquals(SystemEventType.WS_CLOSING.getMethodName(), methodNameCaptor.getValue());
    assertNotNull(argumentsCaptor.getValue());
    assertEquals(code, argumentsCaptor.getValue().get("code"));
    assertEquals(reason, argumentsCaptor.getValue().get("reason"));
  }

  @Test
  public void serverDisconnectTest() {

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
    assertNotNull(argumentsCaptor.getValue());
    assertEquals(code, argumentsCaptor.getValue().get("code"));
    assertEquals(reason, argumentsCaptor.getValue().get("reason"));
  }

  @Test
  public void onWsFailureTest() {

    // data
    final String errorMessage = "onWsFailureTest exception!";
    final Throwable throwable = new RuntimeException(errorMessage,
        new RuntimeException("exception test cause!"));
    final Response response = Mockito.mock(Response.class);

    // test method
    client.onFailure(Mockito.mock(WebSocket.class), throwable, response);

    // validate that WS_CLOSED is called on method channel
    ArgumentCaptor<String> methodNameCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(methodChannel).invokeMethod(methodNameCaptor.capture(), mapCaptor.capture());
    assertEquals(SystemEventType.WS_FAILURE.getMethodName(), methodNameCaptor.getValue());
    assertNotNull(mapCaptor.getValue());
    assertEquals(throwable.getClass().getSimpleName(), mapCaptor.getValue().get("throwableType"));
    assertEquals(errorMessage, mapCaptor.getValue().get("errorMessage"));
    assertNotNull(mapCaptor.getValue().get("causeMessage"));
  }

  @Test
  public void onWsFailureNoCauseTest() {

    // data
    final String errorMessage = "onWsFailureTest exception!";
    final Throwable throwable = new RuntimeException(errorMessage);
    final Response response = Mockito.mock(Response.class);

    // test method
    client.onFailure(Mockito.mock(WebSocket.class), throwable, response);

    // validate that WS_CLOSED is called on method channel
    ArgumentCaptor<String> methodNameCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(methodChannel).invokeMethod(methodNameCaptor.capture(), mapCaptor.capture());
    assertEquals(SystemEventType.WS_FAILURE.getMethodName(), methodNameCaptor.getValue());
    assertNotNull(mapCaptor.getValue());
    assertEquals(throwable.getClass().getSimpleName(), mapCaptor.getValue().get("throwableType"));
    assertEquals(errorMessage, mapCaptor.getValue().get("errorMessage"));
    assertNull(mapCaptor.getValue().get("causeMessage"));
  }

  @Test
  public void sendTextMessageTest() {

    // data
    final String textMessage = "Test message 1";
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_TEXT_MESSAGE, textMessage);
    final Result result = Mockito.mock(Result.class);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.send(anyString())).thenReturn(true);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_TEXT_MSG, arguments, result);

    // verify correct message sent to web socket
    ArgumentCaptor<String> argumentMessage = ArgumentCaptor.forClass(String.class);
    verify(mockedWebSocket).send(argumentMessage.capture());
    assertEquals(textMessage, argumentMessage.getValue());
    verify(result).success(any());
  }

  @Test
  public void sendTextMessageErrorTest() {

    // data
    final String textMessage = "Test message 1 error";
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_TEXT_MESSAGE, textMessage);
    final Result result = Mockito.mock(Result.class);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.send(anyString())).thenReturn(false);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_TEXT_MSG, arguments, result);

    // verify correct result.error was called
    ArgumentCaptor<String> errorCodeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
    verify(result).error(errorCodeCaptor.capture(), errorMessageCaptor.capture(), any());
    assertEquals("01", errorCodeCaptor.getValue());
    assertEquals("Unable to send text message!", errorMessageCaptor.getValue());
  }

  @Test
  public void sendTextMessageWhileDisconnectedTest() {

    // data
    final String textMessage = "Test message 1";
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_TEXT_MESSAGE, textMessage);
    final Result result = Mockito.mock(Result.class);

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_TEXT_MSG, arguments, result);

    // verify error was called on result
    verify(result).error(anyString(), anyString(), any());
  }

  @Test
  public void receiveTextMessageTest() {

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

  @Test(expected = RuntimeException.class)
  public void receiveTextMessageEventSinkExceptionTest() {

    // data
    final String textMessage = "Test message 1";

    // stubbing
    doThrow(new RuntimeException("Text channel error!")).when(textEventSink).success(any());

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    client.onMessage(mockedWebSocket, textMessage);
  }

  @Test
  public void sendBinaryMessageTest() {

    // data
    final byte[] byteMessage = "Test message 1".getBytes(Charsets.UTF_8);
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_BYTE_MESSAGE, byteMessage);
    final Result result = Mockito.mock(Result.class);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.send(any(ByteString.class))).thenReturn(true);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_BYTE_MSG, arguments, result);

    // verify correct message sent to web socket
    ArgumentCaptor<ByteString> argumentMessage = ArgumentCaptor.forClass(ByteString.class);
    verify(mockedWebSocket).send(argumentMessage.capture());
    assertEquals(ByteString.of(byteMessage), argumentMessage.getValue());
    verify(result).success(any());
  }

  @Test
  public void sendNullBinaryMessageTest() {

    // data
    final Map<String, Object> arguments = new HashMap<>();
    final Result result = Mockito.mock(Result.class);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.send(any(ByteString.class))).thenReturn(true);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_BYTE_MSG, arguments, result);

    // verify correct message sent to web socket
    ArgumentCaptor<ByteString> argumentMessage = ArgumentCaptor.forClass(ByteString.class);
    verify(mockedWebSocket).send(argumentMessage.capture());
    assertEquals(ByteString.of(), argumentMessage.getValue());
    verify(result).success(any());
  }

  @Test
  public void sendBinaryMessageErrorTest() {

    // data
    final byte[] byteMessage = "Test message 1".getBytes(Charsets.UTF_8);
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_BYTE_MESSAGE, byteMessage);
    final Result result = Mockito.mock(Result.class);

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    when(mockedWebSocket.send(any(ByteString.class))).thenReturn(false);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_BYTE_MSG, arguments, result);

    // verify correct message sent to web socket
    ArgumentCaptor<String> errorCodeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
    verify(result).error(errorCodeCaptor.capture(), errorMessageCaptor.capture(), any());
    assertEquals("02", errorCodeCaptor.getValue());
    assertEquals("Unable to send binary message!", errorMessageCaptor.getValue());
  }

  @Test
  public void sendBinaryMessageWhileDisconnectedTest() {

    // data
    final byte[] byteMessage = "Test message 1".getBytes(Charsets.UTF_8);
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_BYTE_MESSAGE, byteMessage);
    final Result result = Mockito.mock(Result.class);

    // test method
    methodChannel.invokeMethod(IN_METHOD_NAME_SEND_BYTE_MSG, arguments, result);

    // verify error was called on result
    verify(result).error(anyString(), anyString(), any());
  }

  @Test
  public void receiveBinaryMessageTest() {

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

  @Test(expected = RuntimeException.class)
  public void receiveBinaryMessageEventSinkExceptionTest() {

    // data
    final byte[] bytes = "Test message 2".getBytes(Charsets.UTF_8);
    final ByteString byteMessage = ByteString.of(bytes);

    // stubbing
    doThrow(new RuntimeException("Text channel error!")).when(byteEventSink).success(any());

    // move state to connected
    final WebSocket mockedWebSocket = Mockito.mock(WebSocket.class);
    client.onOpen(mockedWebSocket, Mockito.mock(Response.class));

    // test method
    client.onMessage(mockedWebSocket, byteMessage);
  }

  @Test
  public void invokeInvalidMethodNameTest() {

    // data
    final byte[] byteMessage = "Test message 1".getBytes(Charsets.UTF_8);
    final Map<String, Object> arguments = new HashMap<>();
    arguments.put(ARGUMENT_BYTE_MESSAGE, byteMessage);
    final Result result = Mockito.mock(Result.class);

    // test method
    methodChannel.invokeMethod("invalidMethodName", arguments, result);

    // verify notImplemented called
    verify(result).notImplemented();
  }

  @Test
  public void terminateTest() {

    // test method
    client.terminate();

    // verify that MethodCallHandler is removed
    verify(methodChannel).setMethodCallHandler(null);
  }
}
