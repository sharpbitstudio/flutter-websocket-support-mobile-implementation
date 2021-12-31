import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:web_socket_support/web_socket_support.dart';
import 'package:web_socket_support_platform_interface/method_channel_web_socket_support.dart';
import 'package:web_socket_support_platform_interface/web_socket_options.dart';
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
      final _mockedWsListener = MockWebSocketListener();
      final _webSocketClient = WebSocketClient(_mockedWsListener);

      // Arrange
      final _completer = Completer();
      final _methodChannel = MethodChannelMock(
        channelName: MethodChannelWebSocketSupport.methodChannelName,
        methodMocks: [
          MethodMock(
              method: 'connect',
              action: () {
                _sendMessageFromPlatform(
                    MethodChannelWebSocketSupport.methodChannelName,
                    const MethodCall('onOpened'));
                _completer.complete();
              }),
        ],
      );

      // Act
      await _webSocketClient.connect('ws://example.com/',
          options: const WebSocketOptions(
            autoReconnect: true,
          ));

      // await completer
      await _completer.future;

      // Assert
      // correct event sent to platform
      expect(
        _methodChannel.log,
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
      verify(_mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('disconnect', () async {
      final _mockedWsListener = MockWebSocketListener();
      final _webSocketClient = WebSocketClient(_mockedWsListener);

      // Arrange
      final _completer = Completer();
      final _methodChannel = MethodChannelMock(
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
                _completer.complete();
              }),
        ],
      );

      // Act
      await _webSocketClient.disconnect(code: 123, reason: 'test reason');

      // await completer
      await _completer.future;

      // Assert
      // correct event sent to platform
      expect(
        _methodChannel.log,
        <Matcher>[
          isMethodCall('disconnect', arguments: <String, Object>{
            'code': 123,
            'reason': 'test reason',
          }),
        ],
      );

      // platform response 'onOpened' created wsConnection
      verify(_mockedWsListener.onWsClosed(123, 'test reason'));
      verifyNoMoreInteractions(_mockedWsListener);
    });
  });

  group('$WebSocketClient callbacks', () {
    test('`onWsOpened` callback executed', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // verify
      verify(_mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(_mockedWsListener);
      expect(
          (WebSocketSupportPlatform.instance as MethodChannelWebSocketSupport)
              .listener,
          isNot(isA<DummyWebSocketListener>()));
    });

    test('`onWsClosing` callback executed', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onClosing',
              <String, Object>{'code': 234, 'reason': 'test reason 2'}));

      // verify
      verify(_mockedWsListener.onWsClosing(234, 'test reason 2'));
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('`onWsClosed` callback executed', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // action
      // execute methodCall from platform
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onClosed',
              <String, Object>{'code': 345, 'reason': 'test reason 3'}));

      // verify
      verify(_mockedWsListener.onWsClosed(345, 'test reason 3'));
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('`onStringMessage` callback executed', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // prepare
      // text message channel mock (before we is opened)
      final _streamController = StreamController<String>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.textEventChannelName,
        stream: _streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test event
      _streamController.add('Text message 1');
      await _streamController.close(); // ensure message delivered

      // verify
      verify(_mockedWsListener.onWsOpened(any));
      verify(_mockedWsListener.onStringMessage('Text message 1'));
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('`onByteArrayMessage` callback executed', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // prepare
      // byte array message channel mock (before ws is opened)
      final _streamController = StreamController<Uint8List>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.byteEventChannelName,
        stream: _streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test event
      _streamController.add(Uint8List.fromList('Binary message 1'.codeUnits));
      await _streamController.close(); // ensure message delivered

      // verify
      verify(_mockedWsListener.onWsOpened(any));
      verify(_mockedWsListener.onByteArrayMessage(
          Uint8List.fromList('Binary message 1'.codeUnits)));
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('`onError` callback executed by method channel', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

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
      final _errorMatcher = isA<WebSocketException>()
          .having((e) => e.originType, 'throwableType', equals('TestType'))
          .having((e) => e.message, 'errorMessage', equals('TestErrMsg'))
          .having(
              (e) => e.causeMessage, 'causeMessage', equals('TestErrCause'));

      expect(
          verify(_mockedWsListener
                  .onError(captureThat(isA<WebSocketException>())))
              .captured
              .single,
          _errorMatcher);
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('`onError` callback executed by text event channel', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // prepare
      // text message channel mock (before we is opened)
      final _streamController = StreamController<String>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.textEventChannelName,
        stream: _streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test error event
      _streamController.addError(
        PlatformException(
            code: 'ERROR_CODE_3', message: 'errMsg3', details: null),
      );
      await _streamController.close();

      // verify
      final _errorMatcher = isA<PlatformException>()
          .having((e) => e.code, 'An error code', equals('ERROR_CODE_3'))
          .having((e) => e.message, 'error message', equals('errMsg3'));

      expect(
          verify(_mockedWsListener
                  .onError(captureThat(isA<PlatformException>())))
              .captured
              .single,
          _errorMatcher);
      verify(_mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(_mockedWsListener);
    });

    test('`onError` callback executed by bytearray event channel', () async {
      final _mockedWsListener = MockWebSocketListener();
      WebSocketClient(_mockedWsListener);

      // prepare
      // text message channel mock (before we is opened)
      final _streamController = StreamController<Uint8List>.broadcast();
      EventChannelMock(
        channelName: MethodChannelWebSocketSupport.byteEventChannelName,
        stream: _streamController.stream,
      );

      // open ws
      await _sendMessageFromPlatform(
          MethodChannelWebSocketSupport.methodChannelName,
          const MethodCall('onOpened'));

      // action
      // emit test error event
      _streamController.addError(
        PlatformException(
            code: 'ERROR_CODE_4', message: 'errMsg4', details: null),
      );
      await _streamController.close();

      // verify
      final _errorMatcher = isA<PlatformException>()
          .having((e) => e.code, 'An error code', equals('ERROR_CODE_4'))
          .having((e) => e.message, 'error message', equals('errMsg4'));

      expect(
          verify(_mockedWsListener
                  .onError(captureThat(isA<PlatformException>())))
              .captured
              .single,
          _errorMatcher);
      verify(_mockedWsListener.onWsOpened(any));
      verifyNoMoreInteractions(_mockedWsListener);
    });
  });

  group('$DefaultWebSocketListener callbacks', () {
    WidgetsFlutterBinding.ensureInitialized();

    WebSocketConnection? _webSocketConnection;
    int? _closedCode;
    String? _closedReason;
    int? _closingCode;
    String? _closingReason;
    String? _textMsg;
    Uint8List? _byteMsg;
    Exception? _exception;

    // listener
    WebSocketListener _listener;

    setUp(() {
      _webSocketConnection = null;
      _closedCode = null;
      _closedReason = null;
      _closingCode = null;
      _closingReason = null;
      _textMsg = null;
      _byteMsg = null;
      _exception = null;
    });

    test('DefaultWebSocketListener default constructor', () {
      // init listener
      _listener = DefaultWebSocketListener(
        (wsc) => _webSocketConnection = wsc,
        (code, reason) => {
          _closedCode = code,
          _closedReason = reason,
        },
        (msg) => _textMsg = msg,
        (msg) => _byteMsg = msg,
        (code, reason) => {
          _closingCode = code,
          _closingReason = reason,
        },
        (exc) => _exception = exc,
      );

      // test on open
      var wsConnection = MockWebSockerConnection();
      _listener.onWsOpened(wsConnection);

      // verify
      expect(_webSocketConnection, wsConnection);

      // test on closing
      var closingCode = 123;
      var closingReason = 'closing reason 1';
      _listener.onWsClosing(closingCode, closingReason);

      // verify
      expect(_closingCode, closingCode);
      expect(_closingReason, closingReason);

      // test on close
      var closedCode = 134;
      var closedReason = 'closed reason 1';
      _listener.onWsClosed(closedCode, closedReason);

      // verify
      expect(_closedCode, closedCode);
      expect(_closedReason, closedReason);

      // test onTextMessage
      var textMsg = 'text message 1';
      _listener.onStringMessage(textMsg);

      // verify
      expect(_textMsg, textMsg);

      // test onByteMessage
      var byteMsg = Uint8List.fromList(utf8.encode('text message 1'));
      _listener.onByteArrayMessage(byteMsg);

      // verify
      expect(_byteMsg, byteMsg);

      // test onError
      var exception = Exception('exception 1');
      _listener.onError(exception);

      // verify
      expect(_exception, exception);
    });

    test('DefaultWebSocketListener default positional parameters', () {
      // init listener
      _listener = DefaultWebSocketListener(
        (wsc) => _webSocketConnection = wsc,
        (code, reason) => {
          _closedCode = code,
          _closedReason = reason,
        },
      );

      // test on closing
      var closingCode = 123;
      var closingReason = 'closing reason 1';
      _listener.onWsClosing(closingCode, closingReason);

      // test onTextMessage
      var textMsg = 'text message 1';
      _listener.onStringMessage(textMsg);

      // test onByteMessage
      var byteMsg = Uint8List.fromList(utf8.encode('text message 1'));
      _listener.onByteArrayMessage(byteMsg);

      // test onError
      var exception = Exception('exception 1');
      _listener.onError(exception);
    });

    test('DefaultWebSocketListener forTextMessages constructor', () {
      // init listener
      _listener = DefaultWebSocketListener.forTextMessages(
        (wsc) => _webSocketConnection = wsc,
        (code, reason) => {
          _closedCode = code,
          _closedReason = reason,
        },
        (msg) => _textMsg = msg,
      );

      // test on open
      var wsConnection = MockWebSockerConnection();
      _listener.onWsOpened(wsConnection);

      // verify
      expect(_webSocketConnection, wsConnection);

      // test on close
      var closedCode = 234;
      var closedReason = 'closed reason 2';
      _listener.onWsClosed(closedCode, closedReason);

      // verify
      expect(_closedCode, closedCode);
      expect(_closedReason, closedReason);

      // test onTextMessage
      var textMsg = 'text message 2';
      _listener.onStringMessage(textMsg);

      // verify
      expect(_textMsg, textMsg);

      // test onByteMessage
      expect(
          () => _listener.onByteArrayMessage(
              Uint8List.fromList(utf8.encode('byte message 2'))),
          throwsA(isA<UnsupportedError>()));
    });

    test('DefaultWebSocketListener forByteMessages constructor', () {
      // init listener
      _listener = DefaultWebSocketListener.forByteMessages(
        (wsc) => _webSocketConnection = wsc,
        (code, reason) => {
          _closedCode = code,
          _closedReason = reason,
        },
        (msg) => _byteMsg = msg,
      );

      // test on open
      var wsConnection = MockWebSockerConnection();
      _listener.onWsOpened(wsConnection);

      // verify
      expect(_webSocketConnection, wsConnection);

      // test on close
      var closedCode = 345;
      var closedReason = 'closed reason 3';
      _listener.onWsClosed(closedCode, closedReason);

      // verify
      expect(_closedCode, closedCode);
      expect(_closedReason, closedReason);

      // test onByteMessage
      var byteMsg = Uint8List.fromList(utf8.encode('byte message 3'));
      _listener.onByteArrayMessage(byteMsg);

      // verify
      expect(_byteMsg, byteMsg);

      // test onTextMessage
      expect(() => _listener.onStringMessage('text message 3'),
          throwsA(isA<UnsupportedError>()));
    });
  });
}

Future<ByteData?> _sendMessageFromPlatform(
    String channelName, MethodCall methodCall,
    {Function(ByteData?)? callback}) {
  final envelope = const StandardMethodCodec().encodeMethodCall(methodCall);
  return TestDefaultBinaryMessengerBinding.instance!.defaultBinaryMessenger
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
