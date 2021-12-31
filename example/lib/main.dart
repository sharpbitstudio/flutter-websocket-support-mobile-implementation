// coverage:ignore-file
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:web_socket_support/web_socket_support.dart';

// ignore_for_file: avoid_print
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
      child: const WebSocketSupportExampleApp(),
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
    return _messages.isNotEmpty;
  }

  @override
  void dispose() {
    textController.dispose();
    super.dispose();
  }
}

// TestApp use this ChangeNotifier to listen for connection status changes
class WebSocketSupport with ChangeNotifier {
  static const String serverUrl = 'ws://ws.ifelse.io';

  final WsBackend _backend;

  // locals
  late WebSocketClient _wsClient;
  WebSocketConnection? _webSocketConnection;
  bool working = false;

  WebSocketSupport(this._backend) {
    _wsClient = WebSocketClient(DefaultWebSocketListener.forTextMessages(
      _onWsOpened,
      _onWsClosed,
      _onTextMessage,
      (_, __) => {},
      _onError,
    ));
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
      _webSocketConnection?.sendStringMessage(_backend.textController.text);
      _backend.textController.clear();
    }
  }

  Future<void> connect() async {
    working = true;
    _backend.textController.clear();
    _backend.clearMesages();
    try {
      await _wsClient.connect(serverUrl);
      notifyListeners();
    } on PlatformException catch (e) {
      final errorMsg = 'Failed to connect to ws server. Error:$e';
      print(errorMsg);
      _backend.addMesage(ServerMessage(errorMsg, DateTime.now()));
    }
  }

  Future<void> disconnect() async {
    working = true;
    await _wsClient.disconnect();
    notifyListeners();
  }
}

// ExampleApp uses WebSocketSupport to communicate with remote ws server
// App is able to send arbitrary text messages to remote echo server
// and will keep all remote servers replys in list as long as ws session is up.
class WebSocketSupportExampleApp extends StatelessWidget {
  const WebSocketSupportExampleApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('WebSocketSupport example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: const <Widget>[
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
  const WsControlPanel({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const SizedBox(height: 10),
        Center(
          child: Consumer<WebSocketSupport>(builder: (ctx, ws, _) {
            return Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Padding(
                  padding: const EdgeInsets.only(left: 10, right: 10),
                  child: Row(
                    children: [
                      // status title
                      const Text('WS status:'),
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
                      //status value
                      Text(
                        (ws.isConnected() ? 'Connected' : 'Disconnected'),
                        style: TextStyle(color: _connectionColor(ws)),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(left: 10, right: 10),
                  child: ElevatedButton(
                    key: const Key('connect'),
                    onPressed: ws.working
                        ? null
                        : () async {
                            ws.isConnected()
                                ? await ws.disconnect()
                                : await ws.connect();
                          },
                    child: ws.isConnected()
                        ? const Text('Disconnect')
                        : const Text('Connect'),
                  ),
                ),
              ],
            );
          }),
        ),
        const Divider(),
      ],
    );
  }

  MaterialColor _connectionColor(WebSocketSupport ws) =>
      ws.isConnected() ? Colors.green : Colors.red;
}

class WsTextInput extends StatelessWidget {
  const WsTextInput({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Consumer<WebSocketSupport>(builder: (ctx, ws, _) {
      return !ws.isConnected()
          ? const SizedBox.shrink()
          : Column(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: TextField(
                          key: const Key('textField'),
                          textAlign: TextAlign.center,
                          controller:
                              Provider.of<WsBackend>(context, listen: false)
                                  .textController,
                          decoration: const InputDecoration(
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
                        key: const Key('sendButton'),
                        icon: const Icon(Icons.send),
                        onPressed: () => ws.sendMessage(),
                      ),
                    ],
                  ),
                ),
                const Divider(),
              ],
            );
    });
  }
}

class WsMessages extends StatelessWidget {
  const WsMessages({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Consumer<WsBackend>(builder: (ctx, be, _) {
      return be.getMessages().isEmpty
          ? const SizedBox.shrink()
          : Expanded(
              child: Column(
                children: [
                  const Padding(
                    padding: EdgeInsets.only(top: 10, bottom: 10),
                    child: Text(
                      'Reply messages from: ${WebSocketSupport.serverUrl}',
                      key: Key('replyHeader'),
                    ),
                  ),
                  Expanded(
                    child: ListView.separated(
                      itemCount: be.getMessages().length,
                      separatorBuilder: (BuildContext context, int index) =>
                          const Divider(),
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
