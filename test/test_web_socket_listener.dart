import 'dart:async';
import 'dart:typed_data';

import 'package:async/async.dart';
import 'package:web_socket_support_platform_interface/web_socket_connection.dart';
import 'package:web_socket_support_platform_interface/web_socket_listener.dart';

class TestWebSocketListener extends WebSocketListener {
  final _textController = StreamController<String>();
  final _byteController = StreamController<Uint8List>();
  late StreamQueue<String> textQueue;
  late StreamQueue<Uint8List> byteQueue;
  WebSocketConnection? webSocketConnection;
  bool onClosingCalled = false;
  bool onClosedCalled = false;
  bool onErrorCalled = false;
  final errorCompleter = Completer();
  int? closingCode;
  String? closingReason;
  Exception? exception;

  TestWebSocketListener() {
    textQueue = StreamQueue(_textController.stream);
    byteQueue = StreamQueue(_byteController.stream);
  }

  @override
  void onWsOpened(WebSocketConnection webSocketConnection) {
    this.webSocketConnection = webSocketConnection;
  }

  @override
  void onStringMessage(String message) {
    _textController.add(message);
  }

  @override
  void onByteArrayMessage(Uint8List message) {
    _byteController.add(message);
  }

  @override
  void onWsClosing(int code, String reason) {
    onClosingCalled = true;
    closingCode = code;
    closingReason = reason;
    _textController.close();
    _byteController.close();
  }

  @override
  void onWsClosed(int code, String reason) {
    onClosedCalled = true;
    closingCode = code;
    closingReason = reason;
    _textController.close();
    _byteController.close();
  }

  @override
  void onError(Exception e) {
    onErrorCalled = true;
    exception = e;
    errorCompleter.complete();
  }

  Future<void> destroy() async {
    await textQueue.cancel();
    await byteQueue.cancel();
    await _textController.close();
    await _byteController.close();
  }
}
