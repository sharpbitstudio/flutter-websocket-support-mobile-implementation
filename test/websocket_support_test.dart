import 'dart:async';
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
    WebSocketClient _webSocketSupport;
    MethodChannelWebSocketSupport _mockedWebSocket;

    setUp(() {
      // init variables
      _mockedWebSocket = MockMethodChannelWebSocketSupport();
      _webSocketSupport = WebSocketClient.private(_mockedWebSocket);
    });

    test('listener is NOT DummyWebSocketListener', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      // verify
      expect(
          (WebSocketSupportPlatform.instance as MethodChannelWebSocketSupport)
              .listener,
          isNot(isA<DummyWebSocketListener>()));
    });

    test('connect', () async {
      await _webSocketSupport.connect(
        'ws://example.com/',
        options: WebSocketOptions(),
      );
      // verify
      verify(_mockedWebSocket.connect('ws://example.com/',
          options: anyNamed('options')));
      verifyNoMoreInteractions(_mockedWebSocket);
    });

    test('disconnect', () async {
      await _webSocketSupport.disconnect();
      // verify
      verify(_mockedWebSocket.disconnect());
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
}

class MockMethodChannelWebSocketSupport extends Mock
    with MockPlatformInterfaceMixin
    implements MethodChannelWebSocketSupport {}

class MockWebSocketListener extends Mock implements WebSocketListener {}

class MockMethodChannel extends Mock implements MethodChannel {}

class MockEventChannel extends Mock implements EventChannel {}
