import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:web_socket_support/web_socket_support.dart';
import 'package:web_socket_support_platform_interface/web_socket_connection.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final textController = TextEditingController();

  // locals
  WebSocketClient _wsClient;
  WebSocketConnection _webSocketConnection;
  String _lastMessage;

  @override
  void initState() {
    super.initState();
    _wsClient = WebSocketClient(DefaultWebSocketListener.forTextMessages(
        _onWsOpened, _onWsClosed, _onTextMessage));
    initPlatformState();
  }

  @override
  void dispose() {
    textController.dispose();
    super.dispose();
  }

  void _onWsOpened(WebSocketConnection webSocketConnection) {
    setState(() {
      _webSocketConnection = webSocketConnection;
    });
  }

  void _onWsClosed(int code, String reason) {
    setState(() {
      _webSocketConnection = null;
      _lastMessage = null;
    });
  }

  void _onTextMessage(String message) {
    setState(() {
      _lastMessage = message;
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      await _wsClient.connect("ws://echo.websocket.org");
    } on PlatformException catch (e) {
      print('Failed to connect to ws server. Error:$e');
    }
  }

  bool _ifConnected() {
    return _webSocketConnection != null;
  }

  void _sendMessage() {
    if (_webSocketConnection != null) {
      _webSocketConnection.sendTextMessage(textController.text);
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('WebSocketSupport example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Center(
              child: Text('Status: ' +
                  (_webSocketConnection != null
                      ? 'Connected'
                      : 'Disconnected')),
            ),
            if (_ifConnected()) WsControls(_sendMessage, textController),
            if (_lastMessage != null)
              Padding(
                padding: const EdgeInsets.only(top: 20),
                child: Text('Reply message from server: $_lastMessage'),
              ),
          ],
        ),
      ),
    );
  }
}

class WsControls extends StatelessWidget {
  final Function sendMessage;
  final TextEditingController textController;

  const WsControls(this.sendMessage, this.textController, {Key key})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10),
      child: Row(
        children: <Widget>[
          Expanded(
            child: TextField(
              textAlign: TextAlign.center,
              controller: textController,
              decoration: InputDecoration(
                focusedBorder: OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.greenAccent, width: 2.0),
                ),
                enabledBorder: OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.blue, width: 2.0),
                ),
                hintText: 'Enter message to send to server',
              ),
            ),
          ),
          IconButton(
            icon: Icon(Icons.send),
            onPressed: () => sendMessage(),
          ),
        ],
      ),
    );
  }
}
