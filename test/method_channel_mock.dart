import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

class MethodChannelMock {
  final Duration delay;
  final MethodChannel methodChannel;
  final List<MethodMock> methodMocks;
  final log = <MethodCall>[];

  MethodChannelMock({
    required String channelName,
    this.delay = Duration.zero,
    required this.methodMocks,
  }) : methodChannel = MethodChannel(channelName) {
    TestDefaultBinaryMessengerBinding.instance!.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, _handler);
  }

  Future _handler(MethodCall methodCall) async {
    log.add(methodCall);

    if (!methodMocks.map((e) => e.method).contains(methodCall.method)) {
      throw MissingPluginException('No implementation found for method '
          '${methodCall.method} on channel ${methodChannel.name}');
    }

    final methodMock =
        methodMocks.where((e) => e.method == methodCall.method).first;

    return Future.delayed(delay, () {
      // throw exception if defined as a result
      if (methodMock.result is Exception) {
        throw methodMock.result;
      }

      // execute action callback
      Future.delayed(Duration.zero, methodMock.action());

      // return result
      return Future.value(methodMock.result);
    });
  }

  void sendPlatformMessage(MethodCall methodCall) {
    final envelope = const StandardMethodCodec().encodeMethodCall(methodCall);
    TestDefaultBinaryMessengerBinding.instance!.defaultBinaryMessenger
        .handlePlatformMessage(methodChannel.name, envelope, (data) {});
  }
}

class MethodMock {
  final String method;
  final dynamic result;
  final Function() action;

  MethodMock({required this.method, this.result, this.action = _defaultAction});
  static void _defaultAction() {}
}
