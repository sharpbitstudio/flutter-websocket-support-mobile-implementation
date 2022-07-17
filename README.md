# Websocket support
[![pub package](https://img.shields.io/pub/v/web_socket_support.svg)](https://pub.dev/packages/web_socket_support) [![web_socket_support](https://github.com/sharpbitstudio/flutter-websocket-support-mobile-implementation/actions/workflows/master_build.yaml/badge.svg?branch=master)](https://github.com/sharpbitstudio/flutter-websocket-support-mobile-implementation/actions/workflows/master_build.yaml) [![codecov](https://codecov.io/gh/sharpbitstudio/flutter-websocket-support-mobile-implementation/branch/master/graph/badge.svg?token=UK2F6LLRRV)](https://codecov.io/gh/sharpbitstudio/flutter-websocket-support-mobile-implementation)

A Flutter plugin for websockets on Android (currently). This plugin is based on okHttp (for Android platform).

Plugin was created as an attempt to overcome shortcomings of Flutter standard WebSocket implementation (cookbook) like connection not staying open while screen is locked or the application is in background. This plugin solves these problems.

## Introduction

**Websocket support** uses Platform Channel to expose Dart APIs that Flutter application can use to communicate with platform specific websocket native libraries. For andorid, chosen java Websocket implementation is [OkHttp](https://square.github.io/okhttp/).

## Example

````dart
// WebSocketConnection will be obtained via _onWsOpen callback in WebSocketClient
WebSocketConnection _webSocketConnection;

// instantiate WebSocketClient with DefaultWebSocketListener and some callbacks
// Of course you can use you own WebSocketListener implementation
final WebSocketClient _wsClient = WebSocketClient(DefaultWebSocketListener.forTextMessages(
        (wsc) => _webSocketConnection = wsc,                       // _onWsOpen callback
        (code, msg) => print('Connection closed. Resaon: $msg'),  // _onWsClosed callback
        (msg) => print('Message received: $msg')));               // _onStringMessage callback
// ...
// connect to remote ws endpoint
await _wsClient.connect("ws://echo.websocket.org");

// ...
// After connection is established, use obtained WebSocketConnection instance to send messages
_webSocketConnection.sendTextMessage('Hello from Websocket support');
````

or see /example/lib/main.dart

## Build Dependencies
- Dart SDK version: >=2.12.0 <3.0.0
- Flutter SDK version: >=1.16.0
- Java version: 11
- AGP version: 7.2.x
- Gradle version: 7.4.2
- Android SDK min version: 21
- Android SDK target version: 32

## TODO
Unfortunately, iOS implementation is still missing. So, if you have know-how, and you're willing to implement it - you will be more than welcomed. Preffered WebSocket libs are [NWWebSocket](https://github.com/pusher/NWWebSocket) and [Starscream](https://github.com/daltoniam/Starscream), but we are opened for other options as well.

## Contributing
See the Contributing guide for details on contributing to this project.
