import 'package:flutter_test/flutter_test.dart';
import 'package:azure_speech_assessment/azure_speech_assessment.dart';
import 'package:azure_speech_assessment/azure_speech_assessment_platform_interface.dart';
import 'package:azure_speech_assessment/azure_speech_assessment_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAzureSpeechAssessmentPlatform
    with MockPlatformInterfaceMixin
    implements AzureSpeechAssessmentPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final AzureSpeechAssessmentPlatform initialPlatform = AzureSpeechAssessmentPlatform.instance;

  test('$MethodChannelAzureSpeechAssessment is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelAzureSpeechAssessment>());
  });

  test('getPlatformVersion', () async {
    AzureSpeechAssessment azureSpeechAssessmentPlugin = AzureSpeechAssessment();
    MockAzureSpeechAssessmentPlatform fakePlatform = MockAzureSpeechAssessmentPlatform();
    AzureSpeechAssessmentPlatform.instance = fakePlatform;

    expect(await azureSpeechAssessmentPlugin.getPlatformVersion(), '42');
  });
}
