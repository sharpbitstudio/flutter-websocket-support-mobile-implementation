import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:web_socket_support/web_socket_support.dart';
import 'package:web_socket_support_platform_interface/method_channel_web_socket_support.dart';
import 'package:web_socket_support_platform_interface/web_socket_support_platform_interface.dart';

import 'event_channel_mock.dart';
import 'method_channel_mock.dart';
import 'websocket_support_test.mocks.dart';

@GenerateMocks([WebSocketListener])
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('$WebSocketClient', () {
    test('listener can not be DummyWebSocketListener', () async {
      // verify no DummyWebSocketListener can be used
      expect(
          () => WebSocketClient(MockDummyWebSocketListener()),
          throwsA(predicate((e) =>
              e is PlatformException &&
              e.code == WebSocketClient.wrongListenerExceptionCode)));
    });

    test('Valid listener works', () async {
      // verify
      expect(WebSocketClient(MockWebSocketListener()), isA<WebSocketClient>());
    });

    test('connect', () async {
      final mockedWsListener = MockWebSocketListener();
      final webSocketClient = WebSocketClient(mockedWsListener);

      // Arrange
      final completer = Completer();
      final methodChannel = MethodChannelMock(
        channelName: MethodChannelWebSocketSupport.methodChannelName,
        methodMocks: [
          MethodMock(
              method: 'connect',
              action: () {
                _sendMessageFromPlatform(
                    MethodChannelWebSocketSupport.methodChannelName,
                    const MethodCall('onOpened'));
                completer.complete();
              }),
        ],
      );

      // Act
      await webSocketClient.connect('ws://example.com/',
          options: const WebSocketOptions(
            autoReconnect: true,
          ));

      // await completer
      await completer.future;

      // Assert
      // correct event sent to platform
      expect(
        methodChannel.log,
        <Matcher>[
          isMethodCall('connect', arguments: <String, Object>{
            'serverUrl': 'ws://example.com/',
            'options': {
              'autoReconnect': true,
              'pingInterval': 0,
              'headers': {},
            },
          }),
        ],
      );

      // platform response 'onOpened' created wsConnection
      verify(mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('disconnect', () async {
      final mockedWsListener = MockWebSocketListener();
      final webSocketClient = WebSocketClient(mockedWsListener);

      // Arrange
      final completer = Completer();
      final methodChannel = MethodChannelMock(
        channelName: MethodChannelWebSocketSupport.methodChannelName,
        methodMocks: [
          MethodMock(
              method: 'disconnect',
              action: () {
                _sendMessageFromPlatform(
                    MethodChannelWebSocketSupport.methodChannelName,
                    const MethodCall('onClosed', <String, Object>{
                      'code': 123,
                      'reason': 'test reason'
                    }));
                completer.complete();
              }),
        ],
      );

      // Act
      await webSocketClient.disconnect(code: 123, reason: 'test reason');

      // await completer
      await completer.future;

      // Assert
      // correct event sent to platform
      expect(
        methodChannel.log,
        <Matcher>[
          isMethodCall('disconnect', arguments: <String, Object>{
            'code': 123,
            'reason': 'test reason',
          }),
        ],
      );

      // platform response 'onOpened' created wsConnection
      verify(mockedWsListener.onWsClosed(123, 'test reason'));
      verifyNoMoreInteractions(mockedWsListener);
    });
  });

  group('$WebSocketClient callbacks', () {
    test('`onWsOpened` callback executed', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // verify
      verify(mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(mockedWsListener);
      expect(
          (WebSocketSupportPlatform.instance as MethodChannelWebSocketSupport)
              .listener,
          isNot(isA<DummyWebSocketListener>()));
    });

    test('`onWsClosing` callback executed', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onClosing',
              <String, Object>{'code': 234, 'reason': 'test reason 2'}));

      // verify
      verify(mockedWsListener.onWsClosing(234, 'test reason 2'));
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('`onWsClosed` callback executed', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onClosed',
              <String, Object>{'code': 345, 'reason': 'test reason 3'}));

      // verify
      verify(mockedWsListener.onWsClosed(345, 'test reason 3'));
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('`onStringMessage` callback executed', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // prepare
      // text message channel mock (before we is opened)
      final streamController = StreamController<String>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.textEventChannelName,
        stream: streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test event
      streamController.add('Text message 1');
      await streamController.close(); // ensure message delivered

      // verify
      verify(mockedWsListener.onWsOpened(any));
      verify(mockedWsListener.onStringMessage('Text message 1'));
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('`onByteArrayMessage` callback executed', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // prepare
      // byte array message channel mock (before ws is opened)
      final streamController = StreamController<Uint8List>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.byteEventChannelName,
        stream: streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test event
      streamController.add(Uint8List.fromList('Binary message 1'.codeUnits));
      await streamController.close(); // ensure message delivered

      // verify
      verify(mockedWsListener.onWsOpened(any));
      verify(mockedWsListener.onByteArrayMessage(
          Uint8List.fromList('Binary message 1'.codeUnits)));
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('`onError` callback executed by method channel', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onFailure', <String, Object>{
            'throwableType': 'TestType',
            'errorMessage': 'TestErrMsg',
            'causeMessage': 'TestErrCause'
          }));

      // verify
      final errorMatcher = isA<WebSocketException>()
          .having((e) => e.originType, 'throwableType', equals('TestType'))
          .having((e) => e.message, 'errorMessage', equals('TestErrMsg'))
          .having(
              (e) => e.causeMessage, 'causeMessage', equals('TestErrCause'));

      expect(
          verify(mockedWsListener
                  .onError(captureThat(isA<WebSocketException>())))
              .captured
              .single,
          errorMatcher);
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('`onError` callback executed by text event channel', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // prepare
      // text message channel mock (before we is opened)
      final streamController = StreamController<String>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.textEventChannelName,
        stream: streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test error event
      streamController.addError(
        PlatformException(
            code: 'ERROR_CODE_3', message: 'errMsg3', details: null),
      );
      await streamController.close();

      // verify
      final errorMatcher = isA<PlatformException>()
          .having((e) => e.code, 'An error code', equals('ERROR_CODE_3'))
          .having((e) => e.message, 'error message', equals('errMsg3'));

      expect(
          verify(mockedWsListener
                  .onError(captureThat(isA<PlatformException>())))
              .captured
              .single,
          errorMatcher);
      verify(mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(mockedWsListener);
    });

    test('`onError` callback executed by bytearray event channel', () async {
      final mockedWsListener = MockWebSocketListener();
      WebSocketClient(mockedWsListener);

      // prepare
      // text message channel mock (before we is opened)
      final streamController = StreamController<Uint8List>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.byteEventChannelName,
        stream: streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test error event
      streamController.addError(
        PlatformException(
            code: 'ERROR_CODE_4', message: 'errMsg4', details: null),
      );
      await streamController.close();

      // verify
      final errorMatcher = isA<PlatformException>()
          .having((e) => e.code, 'An error code', equals('ERROR_CODE_4'))
          .having((e) => e.message, 'error message', equals('errMsg4'));

      expect(
          verify(mockedWsListener
                  .onError(captureThat(isA<PlatformException>())))
              .captured
              .single,
          errorMatcher);
      verify(mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(mockedWsListener);
    });
  });

  group('$DefaultWebSocketListener callbacks', () {
    WidgetsFlutterBinding.ensureInitialized();

    WebSocketConnection? webSocketConnection;
    int? closedCode0;
    String? closedReason0;
    int? closingCode0;
    String? closingReason0;
    String? textMsg0;
    Uint8List? byteMsg0;
    Exception? exception0;

    // listener
    WebSocketListener listener;

    setUp(() {
      webSocketConnection = null;
      closedCode0 = null;
      closedReason0 = null;
      closingCode0 = null;
      closingReason0 = null;
      textMsg0 = null;
      byteMsg0 = null;
      exception0 = null;
    });

    test('DefaultWebSocketListener default constructor', () {
      // init listener
      listener = DefaultWebSocketListener(
        (wsc) => webSocketConnection = wsc,
        (code, reason) => {
          closedCode0 = code,
          closedReason0 = reason,
        },
        (msg) => textMsg0 = msg,
        (msg) => byteMsg0 = msg,
        (code, reason) => {
          closingCode0 = code,
          closingReason0 = reason,
        },
        (exc) => exception0 = exc,
      );

      // test on open
      var wsConnection = MockWebSockerConnection();
      listener.onWsOpened(wsConnection);

      // verify
      expect(webSocketConnection, wsConnection);

      // test on closing
      var closingCode = 123;
      var closingReason = 'closing reason 1';
      listener.onWsClosing(closingCode, closingReason);

      // verify
      expect(closingCode0, closingCode);
      expect(closingReason0, closingReason);

      // test on close
      var closedCode = 134;
      var closedReason = 'closed reason 1';
      listener.onWsClosed(closedCode, closedReason);

      // verify
      expect(closedCode0, closedCode);
      expect(closedReason0, closedReason);

      // test onTextMessage
      var textMsg = 'text message 1';
      listener.onStringMessage(textMsg);

      // verify
      expect(textMsg0, textMsg);

      // test onByteMessage
      var byteMsg = Uint8List.fromList(utf8.encode('text message 1'));
      listener.onByteArrayMessage(byteMsg);

      // verify
      expect(byteMsg0, byteMsg);

      // test onError
      var exception = Exception('exception 1');
      listener.onError(exception);

      // verify
      expect(exception0, exception);
    });

    test('DefaultWebSocketListener default positional parameters', () {
      // init listener
      listener = DefaultWebSocketListener(
        (wsc) => webSocketConnection = wsc,
        (code, reason) => {
          closedCode0 = code,
          closedReason0 = reason,
        },
      );

      // test on closing
      var closingCode = 123;
      var closingReason = 'closing reason 1';
      listener.onWsClosing(closingCode, closingReason);

      // test onTextMessage
      var textMsg = 'text message 1';
      listener.onStringMessage(textMsg);

      // test onByteMessage
      var byteMsg = Uint8List.fromList(utf8.encode('text message 1'));
      listener.onByteArrayMessage(byteMsg);

      // test onError
      var exception = Exception('exception 1');
      listener.onError(exception);
    });

    test('DefaultWebSocketListener forTextMessages constructor', () {
      // init listener
      listener = DefaultWebSocketListener.forTextMessages(
        (wsc) => webSocketConnection = wsc,
        (code, reason) => {
          closedCode0 = code,
          closedReason0 = reason,
        },
        (msg) => textMsg0 = msg,
      );

      // test on open
      var wsConnection = MockWebSockerConnection();
      listener.onWsOpened(wsConnection);

      // verify
      expect(webSocketConnection, wsConnection);

      // test on close
      var closedCode = 234;
      var closedReason = 'closed reason 2';
      listener.onWsClosed(closedCode, closedReason);

      // verify
      expect(closedCode0, closedCode);
      expect(closedReason0, closedReason);

      // test onTextMessage
      var textMsg = 'text message 2';
      listener.onStringMessage(textMsg);

      // verify
      expect(textMsg0, textMsg);

      // test onByteMessage
      expect(
          () => listener.onByteArrayMessage(
              Uint8List.fromList(utf8.encode('byte message 2'))),
          throwsA(isA<UnsupportedError>()));
    });

    test('DefaultWebSocketListener forByteMessages constructor', () {
      // init listener
      listener = DefaultWebSocketListener.forByteMessages(
        (wsc) => webSocketConnection = wsc,
        (code, reason) => {
          closedCode0 = code,
          closedReason0 = reason,
        },
        (msg) => byteMsg0 = msg,
      );

      // test on open
      var wsConnection = MockWebSockerConnection();
      listener.onWsOpened(wsConnection);

      // verify
      expect(webSocketConnection, wsConnection);

      // test on close
      var closedCode = 345;
      var closedReason = 'closed reason 3';
      listener.onWsClosed(closedCode, closedReason);

      // verify
      expect(closedCode0, closedCode);
      expect(closedReason0, closedReason);

      // test onByteMessage
      var byteMsg = Uint8List.fromList(utf8.encode('byte message 3'));
      listener.onByteArrayMessage(byteMsg);

      // verify
      expect(byteMsg0, byteMsg);

      // test onTextMessage
      expect(() => listener.onStringMessage('text message 3'),
          throwsA(isA<UnsupportedError>()));
    });
  });
}

Future<ByteData?> _sendMessageFromPlatform(
    String channelName, MethodCall methodCall,
    {Function(ByteData?)? callback}) {
  final envelope = const StandardMethodCodec().encodeMethodCall(methodCall);
  return TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .handlePlatformMessage(channelName, envelope, callback);
}

class MockMethodChannelWebSocketSupport extends Mock
    with MockPlatformInterfaceMixin
    implements MethodChannelWebSocketSupport {}

class MockMethodChannel extends Mock implements MethodChannel {}

class MockEventChannel extends Mock implements EventChannel {}

class MockWebSockerConnection extends Mock implements WebSocketConnection {}

class MockDummyWebSocketListener extends Mock
    implements DummyWebSocketListener {}

class MockTextStreamController extends Mock implements Stream<String> {}

class MockBteStreamController extends Mock implements Stream<Uint8List> {}
