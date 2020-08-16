import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';
import 'package:web_socket_support_platform_interface/method_channel_web_socket_support.dart';
import 'package:web_socket_support_platform_interface/web_socket_connection.dart';
import 'package:web_socket_support_platform_interface/web_socket_listener.dart';
import 'package:web_socket_support_platform_interface/web_socket_options.dart';
import 'package:web_socket_support_platform_interface/web_socket_support_platform_interface.dart';

// Export necessary Classes from the platform_interface and this implementation
// so plugin users can use them directly.
export 'package:web_socket_support_platform_interface/web_socket_listener.dart'
    show WebSocketListener;
export 'package:web_socket_support_platform_interface/web_socket_connection.dart'
    show WebSocketConnection;
export 'package:web_socket_support_platform_interface/web_scoket_exception.dart'
    show WebSocketException;

class WebSocketClient extends WebSocketSupportPlatform {
  /// constructor
  WebSocketClient(WebSocketListener listener) {
    // this is necessary in order to use client supplied listener
    WebSocketSupportPlatform.instance = MethodChannelWebSocketSupport(listener);
  }

  /// This constructor is only used for testing and shouldn't be accessed by
  /// users of the plugin. It may break or change at any time.
  @visibleForTesting
  WebSocketClient.private(
    MethodChannelWebSocketSupport instance,
  ) {
    WebSocketSupportPlatform.instance = instance;
  }

  /// Initialize ws connection to specified server url.
  /// If connection was successful, [onWsOpened] will be invoked.
  @override
  Future<void> connect(String serverUrl,
      {WebSocketOptions options = const WebSocketOptions()}) async {
    assert(serverUrl != null);
    assert(options != null);
    if (WebSocketSupportPlatform.instance is DummyWebSocketListener) {
      throw PlatformException(
        code: 'WRONG_WS_LISTENER',
        message: 'Client must supply specific WebSocketListener implementation'
            ' which is not DummyWebSocketListener.',
      );
    }
    return await WebSocketSupportPlatform.instance.connect(
      serverUrl,
      options: options,
    );
  }

  /// Initialize ws connection close by client.
  /// When connection is successfully closed, [onWsClosed] will be invoked.
  @override
  Future<void> disconnect() async {
    return await WebSocketSupportPlatform.instance.disconnect();
  }
}

/// Default WebSocketListener implementation to ease listener use
class DefaultWebSocketListener implements WebSocketListener {
  final Function(WebSocketConnection) _onWsOpen;
  final Function(int, String) _onWsClosing;
  final Function(int, String) _onWsClosed;
  final Function(String) _onStringMessage;
  final Function(Uint8List) _onByteMessage;
  final Function(Exception) _onError;

  /// Default constructor
  DefaultWebSocketListener(
    this._onWsOpen,
    this._onWsClosed, [
    this._onStringMessage = _voidOnStringMessage,
    this._onByteMessage = _voidOnByteMessage,
    this._onWsClosing = _voidOnWsClosing,
    this._onError = _voidOnError,
  ]);

  /// Helper constructor for Text messages web-socket connection only
  DefaultWebSocketListener.forTextMessages(
    this._onWsOpen,
    this._onWsClosed,
    this._onStringMessage, [
    this._onWsClosing = _voidOnWsClosing,
    this._onError = _voidOnError,
  ]) : _onByteMessage = _invalidOnByteMessage;

  /// Helper constructor for Byte messages web-socket connection only
  DefaultWebSocketListener.forByteMessages(
    this._onWsOpen,
    this._onWsClosed,
    this._onByteMessage, [
    this._onWsClosing = _voidOnWsClosing,
    this._onError = _voidOnError,
  ]) : _onStringMessage = _invalidOnStringMessage;

  static void _voidOnStringMessage(String message) {}

  static void _invalidOnStringMessage(String message) {
    throw UnsupportedError('Text Messages are not supported!');
  }

  static void _voidOnByteMessage(Uint8List message) {}

  static void _invalidOnByteMessage(Uint8List message) {
    throw UnsupportedError('Byte Messages are not supported!');
  }

  static void _voidOnWsClosing(int code, String reason) {}

  static void _voidOnError(Exception exception) {}

  @override
  void onWsOpened(WebSocketConnection webSocketConnection) {
    _onWsOpen(webSocketConnection);
  }

  @override
  void onTextMessage(String message) {
    _onStringMessage(message);
  }

  @override
  void onByteMessage(Uint8List message) {
    _onByteMessage(message);
  }

  @override
  void onWsClosing(int code, String reason) {
    _onWsClosing(code, reason);
  }

  @override
  void onWsClosed(int code, String reason) {
    _onWsClosed(code, reason);
  }

  @override
  void onError(Exception exception) {
    _onError(exception);
  }
}
