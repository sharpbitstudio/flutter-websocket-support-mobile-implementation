import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

import 'package:web_socket_support_example/main.dart';

class WebSocketSupportMock extends Mock
    with ChangeNotifier
    implements WebSocketSupport {}

void main() {
  Widget makeTargetWidgetTestable(
      Widget child, WsBackend backend, WebSocketSupport ws) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<WsBackend>.value(
          value: backend,
        ),
        ChangeNotifierProvider<WebSocketSupport>.value(
          value: ws,
        ),
      ],
      child: child,
    );
  }

  testWidgets('Verify connect and send operations',
      (WidgetTester tester) async {
    var app = WebSocketSupportExampleApp();
    var backend = WsBackend();
    var mockedWs = WebSocketSupportMock(); // mocked websocket support

    // stubbing
    var connected = false;
    when(mockedWs.isConnected()).thenAnswer((realInvocation) {
      return connected;
    });
    when(mockedWs.working).thenReturn(false);
    when(mockedWs.connect()).thenAnswer((realInvocation) {
      connected = true;
      mockedWs.notifyListeners();
      return Future<void>.value(null);
    });
    when(mockedWs.sendMessage()).thenAnswer((realInvocation) {
      backend.addMesage(
          ServerMessage(backend.textController.text, DateTime.now()));
      mockedWs.notifyListeners();
    });

    // Build our app and trigger a frame.
    await tester.pumpWidget(makeTargetWidgetTestable(app, backend, mockedWs));

    // expect main test widget vidible
    expect(
      find.text('WebSocketSupport example app'),
      findsOneWidget,
    );

    // Rebuild the widget after the state has changed.
    await tester.pump();

    // expect disconnected state
    expect(find.text('Disconnected'), findsOneWidget);
    expect(find.text('Connect'), findsOneWidget);
    expect(find.byKey(Key('textField')), findsNothing);

    // Tap the add button.
    await tester.tap(find.byKey(Key('connect')));

    // Rebuild the widget after the state has changed.
    await tester.pump();

    // expect connected state
    expect(find.text('Connected'), findsOneWidget);
    expect(find.text('Disconnect'), findsOneWidget);
    expect(find.byKey(Key('textField')), findsOneWidget);

    // enter 1 message and send
    var testMsg = 'test message 1';
    await tester.enterText(find.byKey(Key('textField')), testMsg);
    await tester.tap(find.byKey(Key('sendButton')));

    // Rebuild the widget after the state has changed.
    await tester.pump();

    // verify message response received and displayed
    expect(find.text('Connected'), findsOneWidget);
    expect(find.byKey(Key('textField')), findsOneWidget);
    expect(find.byKey(Key('replyHeader')), findsOneWidget);
    expect(find.byKey(Key(testMsg)), findsOneWidget);
  });
}
