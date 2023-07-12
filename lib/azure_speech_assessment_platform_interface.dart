import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'azure_speech_assessment_method_channel.dart';

abstract class AzureSpeechAssessmentPlatform extends PlatformInterface {
  /// Constructs a AzureSpeechAssessmentPlatform.
  AzureSpeechAssessmentPlatform() : super(token: _token);

  static final Object _token = Object();

  static AzureSpeechAssessmentPlatform _instance = MethodChannelAzureSpeechAssessment();

  /// The default instance of [AzureSpeechAssessmentPlatform] to use.
  ///
  /// Defaults to [MethodChannelAzureSpeechAssessment].
  static AzureSpeechAssessmentPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AzureSpeechAssessmentPlatform] when
  /// they register themselves.
  static set instance(AzureSpeechAssessmentPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
