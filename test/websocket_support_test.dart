import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:async/async.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:web_socket_support/web_socket_support.dart';
import 'package:web_socket_support_platform_interface/method_channel_web_socket_support.dart';
import 'package:web_socket_support_platform_interface/web_socket_listener.dart';
import 'package:web_socket_support_platform_interface/web_socket_options.dart';
import 'package:web_socket_support_platform_interface/web_socket_support_platform_interface.dart';

void main() {
  final WebSocketListener _mockListener = MockWebSocketListener();

  group('$WebSocketClient', () {
    WidgetsFlutterBinding.ensureInitialized();

    //
    // mock channels
    WebSocketClient _webSocketClient;
    MethodChannelWebSocketSupport _mockedWebSocket;

    setUp(() {
      // init variables
      _mockedWebSocket = MockMethodChannelWebSocketSupport();
      _webSocketClient = WebSocketClient.private(_mockedWebSocket);
    });

    test('listener is NOT DummyWebSocketListener', () async {
      await _webSocketClient.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      // verify
      expect(
          (WebSocketSupportPlatform.instance as MethodChannelWebSocketSupport)
              .listener,
          isNot(isA<DummyWebSocketListener>()));
    });

    test('Valid listener works', () async {
      // verify
      expect(WebSocketClient(MockWebSocketListener()), isA<WebSocketClient>());
    });

    test('DummyWebSocketListener throws exception', () async {
      // verify
      expect(() => WebSocketClient(MockDummyWebSocketListener()),
          throwsA(isA<PlatformException>()));
    });

    test('connect', () async {
      await _webSocketClient.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      // verify
      verify(_mockedWebSocket.connect('ws://example.com/',
          options: anyNamed('options')));
      verifyNoMoreInteractions(_mockedWebSocket);
    });

    test('invalid connect', () async {
      expect(
          () => _webSocketClient.connect(
                'ws://example.com/',
                options: null,
              ),
          throwsA(isA<AssertionError>()));
    });

    test('disconnect', () async {
      await _webSocketClient.disconnect(code: 123, reason: 'test reason');
      // verify
      verify(_mockedWebSocket.disconnect(code: 123, reason: 'test reason'));
      verifyNoMoreInteractions(_mockedWebSocket);
    });

    test('disconnect default values', () async {
      await _webSocketClient.disconnect();
      // verify
      verify(_mockedWebSocket.disconnect(code: 1000, reason: 'Client done.'));
      verifyNoMoreInteractions(_mockedWebSocket);
    });
  });

  group('$WebSocketClient callbacks', () {
    WidgetsFlutterBinding.ensureInitialized();

    //
    // mock channels
    MethodChannel _methodChannel;
    EventChannel _textMessages;
    EventChannel _byteMessages;
    WebSocketClient _webSocketSupport;
    Function _methodCallFunction;

    // EventChannel
    StreamController<String> _textMessagesController;
    StreamController<String> _onTextMessageController;
    StreamController<Uint8List> _byteMessagesController;
    StreamController<Uint8List> _onByteMessagesController;

    // async queues
    StreamQueue<String> _textQueue;
    StreamQueue<Uint8List> _byteQueue;

    setUp(() {
      // init variables
      _methodChannel = MockMethodChannel();
      _textMessages = MockEventChannel();
      _byteMessages = MockEventChannel();

      _webSocketSupport =
          WebSocketClient.private(MethodChannelWebSocketSupport.private(
        _mockListener,
        _methodChannel,
        _textMessages,
        _byteMessages,
      ));

      // MethodChannel
      when(_methodChannel.setMethodCallHandler(any))
          .thenAnswer((realInvocation) {
        _methodCallFunction = realInvocation.positionalArguments[0];
      });

      // text EventChannel
      _textMessagesController = StreamController<String>();
      when(_textMessages.receiveBroadcastStream())
          .thenAnswer((Invocation invoke) => _textMessagesController.stream);

      // Byte EventChannel
      _byteMessagesController = StreamController<Uint8List>();
      when(_byteMessages.receiveBroadcastStream())
          .thenAnswer((Invocation invoke) => _byteMessagesController.stream);

      // stream queues for async validation
      _onTextMessageController = StreamController<String>();
      _onByteMessagesController = StreamController<Uint8List>();
      _textQueue = StreamQueue(_onTextMessageController.stream);
      _byteQueue = StreamQueue(_onByteMessagesController.stream);
      when(_mockListener.onTextMessage(any)).thenAnswer((realInvocation) {
        _onTextMessageController.add(realInvocation.positionalArguments[0]);
      });
      when(_mockListener.onByteMessage(any)).thenAnswer((realInvocation) {
        _onByteMessagesController.add(realInvocation.positionalArguments[0]);
      });
    });

    tearDown(() {
      _textMessagesController.close();
      _onTextMessageController.close();
      _byteMessagesController.close();
      _onByteMessagesController.close();
    });

    test('listener is NOT DummyWebSocketListener', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      expect(
          (WebSocketSupportPlatform.instance as MethodChannelWebSocketSupport)
              .listener,
          isNot(isA<DummyWebSocketListener>()));
    });

    test('onConnect', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      _methodCallFunction.call(MethodCall('onOpened'));
      verify(_mockListener.onWsOpened(any));
      verifyNoMoreInteractions(_mockListener);
    });

    test('onClosing', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      _methodCallFunction.call(MethodCall(
        'onClosing',
        {'code': 1, 'reason': 'testReason1'},
      ));
      verify(_mockListener.onWsClosing(1, 'testReason1'));
      verifyNoMoreInteractions(_mockListener);
    });

    test('onClosed', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      _methodCallFunction.call(MethodCall(
        'onClosed',
        {'code': 2, 'reason': 'testReason2'},
      ));
      verify(_mockListener.onWsClosed(2, 'testReason2'));
      verifyNoMoreInteractions(_mockListener);
    });

    test('onTextMessage', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      _textMessagesController.add('test-text-message');

      // verify
      expect(await _textQueue.next.timeout(Duration(seconds: 1)),
          'test-text-message');
      verify(_mockListener.onTextMessage('test-text-message'));
      verifyNoMoreInteractions(_mockListener);
    });

    test('onByteMessage', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      _byteMessagesController
          .add(Uint8List.fromList('test-byte-message'.codeUnits));

      // verify
      expect(await _byteQueue.next.timeout(Duration(seconds: 1)),
          'test-byte-message'.codeUnits);
      verify(_mockListener.onByteMessage(any));
      verifyNoMoreInteractions(_mockListener);
    });
  });

  group('$DefaultWebSocketListener callbacks', () {
    WidgetsFlutterBinding.ensureInitialized();

    var _webSocketConnection;
    var _closedCode;
    var _closedReason;
    var _closingCode;
    var _closingReason;
    var _textMsg;
    var _byteMsg;
    var _exception;

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
      _listener.onTextMessage(textMsg);

      // verify
      expect(_textMsg, textMsg);

      // test onByteMessage
      var byteMsg = utf8.encode('text message 1');
      _listener.onByteMessage(byteMsg);

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
      _listener.onTextMessage(textMsg);

      // test onByteMessage
      var byteMsg = utf8.encode('text message 1');
      _listener.onByteMessage(byteMsg);

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
      _listener.onTextMessage(textMsg);

      // verify
      expect(_textMsg, textMsg);

      // test onByteMessage
      expect(() => _listener.onByteMessage(utf8.encode('byte message 2')),
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
      var byteMsg = utf8.encode('byte message 3');
      _listener.onByteMessage(byteMsg);

      // verify
      expect(_byteMsg, byteMsg);

      // test onTextMessage
      expect(() => _listener.onTextMessage('text message 3'),
          throwsA(isA<UnsupportedError>()));
    });
  });
}

class MockMethodChannelWebSocketSupport extends Mock
    with MockPlatformInterfaceMixin
    implements MethodChannelWebSocketSupport {}

class MockWebSocketListener extends Mock implements WebSocketListener {}

class MockMethodChannel extends Mock implements MethodChannel {}

class MockEventChannel extends Mock implements EventChannel {}

class MockWebSockerConnection extends Mock implements WebSocketConnection {}

class MockDummyWebSocketListener extends Mock
    implements DummyWebSocketListener {}

class MockTextStreamController extends Mock implements Stream<String> {}

class MockBteStreamController extends Mock implements Stream<Uint8List> {}
