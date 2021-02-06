# Websocket support

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

## Contributing
See the Contributing guide for details on contributing to this project.
