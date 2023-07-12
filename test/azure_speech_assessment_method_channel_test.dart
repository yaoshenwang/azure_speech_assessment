import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:azure_speech_assessment/azure_speech_assessment_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelAzureSpeechAssessment platform = MethodChannelAzureSpeechAssessment();
  const MethodChannel channel = MethodChannel('azure_speech_assessment');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
