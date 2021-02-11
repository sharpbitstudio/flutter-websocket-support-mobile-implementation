// coverage:ignore-file
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:web_socket_support/web_socket_support.dart';
import 'package:web_socket_support_platform_interface/web_socket_connection.dart';

void main() {
  final backend = WsBackend();
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider<WsBackend>.value(
          value: backend,
        ),
        ChangeNotifierProvider<WebSocketSupport>(
          create: (ctx) => WebSocketSupport(backend),
        ),
      ],
      child: WebSocketSupportExampleApp(),
    ),
  );
}

// TestApp use this ChangeNotifier to listen for changes regarding message list
class WsBackend with ChangeNotifier {
  final textController = TextEditingController();
  final List<ServerMessage> _messages = [];

  WsBackend() {
    print('WsBackend created.');
  }

  void addMesage(ServerMessage msg) {
    _messages.add(msg);
    notifyListeners();
  }

  void clearMesages() {
    _messages.clear();
    notifyListeners();
  }

  List<ServerMessage> getMessages() {
    return List.unmodifiable(_messages);
  }

  bool hasMessages() {
    return _messages != null && _messages.isNotEmpty;
  }

  @override
  void dispose() {
    textController.dispose();
    super.dispose();
  }
}

// TestApp use this ChangeNotifier to listen for connection status changes
class WebSocketSupport with ChangeNotifier {
  static final String SERVER_URI = 'ws://echo.websocket.org';

  final WsBackend _backend;

  // locals
  WebSocketClient _wsClient;
  WebSocketConnection _webSocketConnection;
  bool working = false;

  WebSocketSupport(this._backend) {
    _wsClient = WebSocketClient(DefaultWebSocketListener.forTextMessages(
      _onWsOpened,
      _onWsClosed,
      _onTextMessage,
      (_, __) => {},
      _onError,
    ));
    initConnection();
    print('WebSocketSupport created.');
  }

  void _onWsOpened(WebSocketConnection webSocketConnection) {
    _webSocketConnection = webSocketConnection;
    working = false;
    notifyListeners();
  }

  void _onWsClosed(int code, String reason) {
    _webSocketConnection = null;
    _backend.clearMesages();
    working = false;
    notifyListeners();
  }

  void _onTextMessage(String message) {
    _backend.addMesage(ServerMessage(message, DateTime.now()));
    notifyListeners();
  }

  void _onError(Exception ex) {
    print('_onError: Fatal error occured: $ex');
    _webSocketConnection = null;
    working = false;
    _backend.addMesage(
        ServerMessage('Error occured on WS connection!', DateTime.now()));
    notifyListeners();
  }

  bool isConnected() {
    return _webSocketConnection != null;
  }

  void sendMessage() {
    if (_webSocketConnection != null) {
      _webSocketConnection.sendTextMessage(_backend.textController.text);
      _backend.textController.clear();
    }
  }

  Future<void> connect() async {
    working = true;
    _backend.textController.clear();
    _backend.clearMesages();
    await _wsClient.connect(SERVER_URI);
    notifyListeners();
  }

  Future<void> disconnect() async {
    working = true;
    await _wsClient.disconnect();
    notifyListeners();
  }

  Future<void> initConnection() async {
    try {
      working = true;
      await _wsClient.connect(SERVER_URI);
    } on PlatformException catch (e) {
      print('Failed to connect to ws server. Error:$e');
    }
  }
}

// ExampleApp uses WebSocketSupport to communicate with remote ws server
// App is able to send arbitrary text messages to remote echo server
// and will keep all remote servers replys in list as long as ws session is up.
class WebSocketSupportExampleApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('WebSocketSupport example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            WsControlPanel(),
            WsTextInput(),
            WsMessages(),
          ],
        ),
      ),
    );
  }
}

class WsControlPanel extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SizedBox(height: 10),
        Center(
          child: Consumer<WebSocketSupport>(builder: (ctx, ws, _) {
            return Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Padding(
                  padding: const EdgeInsets.only(left: 10, right: 10),
                  child: Row(
                    children: [
                      Text('WS status:'),
                      Padding(
                        padding: const EdgeInsets.only(left: 5, right: 5),
                        child: Icon(
                          ws.isConnected()
                              ? Icons.check_circle_outlined
                              : Icons.highlight_off,
                          color: _connectionColor(ws),
                          size: 20,
                        ),
                      ),
                      Text(
                        (ws.isConnected() ? 'Connected' : 'Disconnected'),
                        style: TextStyle(color: _connectionColor(ws)),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 10, right: 10),
                  child: RaisedButton(
                    key: Key('connect'),
                    onPressed: ws.working
                        ? null
                        : () async {
                            ws.isConnected()
                                ? await ws.disconnect()
                                : await ws.connect();
                          },
                    child:
                        ws.isConnected() ? Text('Disconnect') : Text('Connect'),
                  ),
                ),
              ],
            );
          }),
        ),
        Divider(),
      ],
    );
  }

  MaterialColor _connectionColor(WebSocketSupport ws) =>
      ws.isConnected() ? Colors.green : Colors.red;
}

class WsTextInput extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<WebSocketSupport>(builder: (ctx, ws, _) {
      return !ws.isConnected()
          ? SizedBox.shrink()
          : Column(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: TextField(
                          key: Key('textField'),
                          textAlign: TextAlign.center,
                          controller:
                              Provider.of<WsBackend>(context, listen: false)
                                  .textController,
                          decoration: InputDecoration(
                            focusedBorder: OutlineInputBorder(
                              borderSide: BorderSide(
                                  color: Colors.greenAccent, width: 2.0),
                            ),
                            enabledBorder: OutlineInputBorder(
                              borderSide:
                                  BorderSide(color: Colors.blue, width: 2.0),
                            ),
                            hintText: 'Enter message to send to server',
                          ),
                        ),
                      ),
                      IconButton(
                        key: Key('sendButton'),
                        icon: Icon(Icons.send),
                        onPressed: () => ws.sendMessage(),
                      ),
                    ],
                  ),
                ),
                Divider(),
              ],
            );
    });
  }
}

class WsMessages extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<WsBackend>(builder: (ctx, be, _) {
      return be.getMessages().isEmpty
          ? SizedBox.shrink()
          : Expanded(
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 10, bottom: 10),
                    child: Text(
                      'Reply messages from: ${WebSocketSupport.SERVER_URI}',
                      key: Key('replyHeader'),
                    ),
                  ),
                  Expanded(
                    child: ListView.separated(
                      itemCount: be.getMessages().length,
                      separatorBuilder: (BuildContext context, int index) =>
                          Divider(),
                      itemBuilder: (BuildContext context, int index) {
                        var message = be.getMessages()[index];
                        return ListTile(
                          title: Text(
                            '${DateFormat.Hms().format(message.dateTime)}: ${message.message}',
                            key: Key(message.message),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            );
    });
  }
}

class ServerMessage {
  final String message;
  final DateTime dateTime;

  ServerMessage(this.message, this.dateTime);
}
