import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_plugin_playlist/flutter_plugin_playlist.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutter_plugin_playlist');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  // TODO implement tests
}
