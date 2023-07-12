import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'azure_speech_assessment_platform_interface.dart';

/// An implementation of [AzureSpeechAssessmentPlatform] that uses method channels.
class MethodChannelAzureSpeechAssessment extends AzureSpeechAssessmentPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('azure_speech_assessment');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
