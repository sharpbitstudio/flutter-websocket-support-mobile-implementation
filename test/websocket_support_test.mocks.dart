// Mocks generated by Mockito 5.0.16 from annotations
// in web_socket_support/test/websocket_support_test.dart.
// Do not manually edit this file.

import 'dart:typed_data' as _i4;

import 'package:mockito/mockito.dart' as _i1;
import 'package:web_socket_support_platform_interface/web_socket_connection.dart'
    as _i3;
import 'package:web_socket_support_platform_interface/web_socket_listener.dart'
    as _i2;

// ignore_for_file: avoid_redundant_argument_values
// ignore_for_file: avoid_setters_without_getters
// ignore_for_file: comment_references
// ignore_for_file: implementation_imports
// ignore_for_file: invalid_use_of_visible_for_testing_member
// ignore_for_file: prefer_const_constructors
// ignore_for_file: unnecessary_parenthesis
// ignore_for_file: camel_case_types

/// A class which mocks [WebSocketListener].
///
/// See the documentation for Mockito's code generation for more information.
class MockWebSocketListener extends _i1.Mock implements _i2.WebSocketListener {
  MockWebSocketListener() {
    _i1.throwOnMissingStub(this);
  }

  @override
  void onWsOpened(_i3.WebSocketConnection? webSocketConnection) =>
      super.noSuchMethod(Invocation.method(#onWsOpened, [webSocketConnection]),
          returnValueForMissingStub: null);
  @override
  void onWsClosing(int? code, String? reason) =>
      super.noSuchMethod(Invocation.method(#onWsClosing, [code, reason]),
          returnValueForMissingStub: null);
  @override
  void onWsClosed(int? code, String? reason) =>
      super.noSuchMethod(Invocation.method(#onWsClosed, [code, reason]),
          returnValueForMissingStub: null);
  @override
  void onStringMessage(String? message) =>
      super.noSuchMethod(Invocation.method(#onStringMessage, [message]),
          returnValueForMissingStub: null);
  @override
  void onByteArrayMessage(_i4.Uint8List? message) =>
      super.noSuchMethod(Invocation.method(#onByteArrayMessage, [message]),
          returnValueForMissingStub: null);
  @override
  void onError(Exception? exception) =>
      super.noSuchMethod(Invocation.method(#onError, [exception]),
          returnValueForMissingStub: null);
  @override
  String toString() => super.toString();
}
