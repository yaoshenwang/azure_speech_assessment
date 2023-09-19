import Flutter
import UIKit
import MicrosoftCognitiveServicesSpeech

public class AzureSpeechAssessmentPlugin: NSObject, FlutterPlugin {
    var azureChannel: FlutterMethodChannel
    var continuousListeningStarted: Bool = false
    private var speechRecognizer: SPXSpeechRecognizer?
    private var speakSynthesizer: SPXSpeechSynthesizer?
    
    var text = ""
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "azure_speech_recognition", binaryMessenger: registrar.messenger())
        let instance: AzureSpeechAssessmentPlugin = AzureSpeechAssessmentPlugin(azureChannel: channel)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    init(azureChannel: FlutterMethodChannel) {
        self.azureChannel = azureChannel
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let args = call.arguments as? Dictionary<String, String>
        if (call.method == "simpleVoice") {
            let speechSubscriptionKey = args?["subscriptionKey"] ?? ""
            let serviceRegion = args?["region"] ?? ""
            let lang = args?["language"] ?? ""
            let timeoutMs = args?["timeout"] ?? ""
            print("Called simpleVoice \(speechSubscriptionKey) \(serviceRegion) \(lang) \(timeoutMs)")
            simpleSpeechRecognition(speechSubscriptionKey: speechSubscriptionKey, serviceRegion: serviceRegion, lang: lang, timeoutMs: timeoutMs)
        } else if(call.method == "micStream"){
            let speechSubscriptionKey = args?["subscriptionKey"] ?? ""
            let serviceRegion = args?["region"] ?? ""
            let lang = args?["language"] ?? ""
            let timeoutMs = args?["timeout"] ?? ""
            print("Called simpleVoice \(speechSubscriptionKey) \(serviceRegion) \(lang) \(timeoutMs)")
            DispatchQueue.global(qos: .userInteractive).async {
                self.micStreamSpeechRecognition(speechSubscriptionKey: speechSubscriptionKey, serviceRegion: serviceRegion, lang: lang, timeoutMs: timeoutMs)
            }
        } else if (call.method == "simpleVoicePlus") {
            let speechSubscriptionKey = args?["subscriptionKey"] ?? ""
            let serviceRegion = args?["region"] ?? ""
            let lang = args?["language"] ?? ""
            let timeoutMs = args?["timeout"] ?? ""
            print("Called simpleVoicePlus \(speechSubscriptionKey) \(serviceRegion) \(lang) \(timeoutMs)")
            simpleSpeechRecognitionPlus(speechSubscriptionKey: speechSubscriptionKey, serviceRegion: serviceRegion, lang: lang, timeoutMs: timeoutMs)
        } else if (call.method == "soundRecord") {
            let speechSubscriptionKey = args?["subscriptionKey"] ?? ""
            let serviceRegion = args?["region"] ?? ""
            let lang = args?["language"] ?? ""
            let timeoutMs = args?["timeout"] ?? ""
            let path = args?["path"] ?? ""
            print("Called soundRecord \(speechSubscriptionKey) \(serviceRegion) \(lang) \(timeoutMs) \(path)")
            soundRecord(speechSubscriptionKey: speechSubscriptionKey, serviceRegion: serviceRegion, lang: lang, timeoutMs: timeoutMs, path: path)
        }else if (call.method == "soundRecordAssessment") {
            let speechSubscriptionKey = args?["subscriptionKey"] ?? ""
            let serviceRegion = args?["region"] ?? ""
            let lang = args?["language"] ?? ""
            let timeoutMs = args?["timeout"] ?? ""
            let path = args?["path"] ?? ""
            let originalText = args?["originalText"] ?? ""
            print("Called soundRecord \(speechSubscriptionKey) \(serviceRegion) \(lang) \(timeoutMs) \(path) \(originalText)")
            soundRecordAssessment(speechSubscriptionKey: speechSubscriptionKey, serviceRegion: serviceRegion, lang: lang, timeoutMs: timeoutMs, path: path, originalText: originalText)
        } else if (call.method == "speakText") {
            let text = args?["text"] ?? ""
            let speechSubscriptionKey = args?["subscriptionKey"] ?? ""
            let serviceRegion = args?["region"] ?? ""
            let lang = args?["language"] ?? ""
            let voiceName = args?["voiceName"] ?? ""
            
            print("Called speakText \(speechSubscriptionKey) \(serviceRegion) \(lang)")
            
            speakText(text: text,speechSubscriptionKey: speechSubscriptionKey,serviceRegion: serviceRegion, lang: lang, voiceName: voiceName);
        } else if(call.method == "speakStop"){
            print("Called speakStop")
            speakStop();
        }
        else {
            result(FlutterMethodNotImplemented)
        }
    }
    
    public func simpleSpeechRecognition(speechSubscriptionKey : String, serviceRegion : String, lang: String, timeoutMs: String) {
        var speechConfig: SPXSpeechConfiguration?
        do {
            try speechConfig = SPXSpeechConfiguration(subscription: speechSubscriptionKey, region: serviceRegion)
            speechConfig!.enableDictation();
        } catch {
            print("error \(error) happened")
            speechConfig = nil
        }
        speechConfig?.speechRecognitionLanguage = lang
        speechConfig?.setPropertyTo(timeoutMs, by: SPXPropertyId.speechSegmentationSilenceTimeoutMs)
        
        let audioConfig = SPXAudioConfiguration()
        
        let reco = try! SPXSpeechRecognizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)
        
        //               reco.addRecognizingEventHandler() {reco, evt in
        
        //                   print("intermediate recognition result: \(evt.result.text ?? "(no result)")")
        
        //               }
        
        print("Listening...")
        
        let result = try! reco.recognizeOnce()
        print("recognition result: \(result.text ?? "(no result)"), reason: \(result.reason.rawValue)")
        
        if result.reason != SPXResultReason.recognizedSpeech {
            let cancellationDetails = try! SPXCancellationDetails(fromCanceledRecognitionResult: result)
            print("cancelled: \(result.reason), \(String(describing: cancellationDetails.errorDetails))")
            print("Did you set the speech resource key and region values?")
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: "")
        } else {
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: result.text)
        }
        
    }
    public func speakStop() {
        try! speakSynthesizer?.stopSpeaking()
    }
    
    public func speakText(text:String, speechSubscriptionKey : String, serviceRegion : String, lang: String, voiceName: String) {
        var speechConfig: SPXSpeechConfiguration?
        do {
            try speechConfig = SPXSpeechConfiguration(subscription: speechSubscriptionKey, region: serviceRegion)
            // speechConfig!.enableDictation()
            speechConfig!.speechSynthesisLanguage = lang
            speechConfig!.speechSynthesisVoiceName = voiceName
        } catch {
            print("error \(error) happened")
            speechConfig = nil
        }
        let audioConfig = SPXAudioConfiguration()
        speakSynthesizer = try! SPXSpeechSynthesizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)
        
        DispatchQueue.global().async{
            self.azureChannel.invokeMethod("speech.onSpeakStarted", arguments: "")
            let result = try! self.speakSynthesizer?.speakText(text)
            self.azureChannel.invokeMethod("speech.onSpeakStopped", arguments: "")
        }
        
    }
    
    public func micStreamSpeechRecognition(speechSubscriptionKey : String, serviceRegion : String, lang: String, timeoutMs: String) {
        if continuousListeningStarted == true {
            do {
                print("stopContinuousRecognition start \(continuousListeningStarted)")
                try speechRecognizer?.stopContinuousRecognition()
                print("stopContinuousRecognition end")
            } catch {
                
            }
            continuousListeningStarted = false
            return
        }
        var speechConfig: SPXSpeechConfiguration?
        do {
            try speechConfig = SPXSpeechConfiguration(subscription: speechSubscriptionKey, region: serviceRegion)
            speechConfig!.enableDictation()
        } catch {
            print("error \(error) happened")
            speechConfig = nil
        }
        //speechConfig?.speechRecognitionLanguage = lang
        //speechConfig?.setPropertyTo(timeoutMs, by: SPXPropertyId.speechSegmentationSilenceTimeoutMs)
        let audioConfig = SPXAudioConfiguration()
        speechRecognizer = try! SPXSpeechRecognizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)
        speechRecognizer?.addRecognizedEventHandler() { reco, evt in
            if self.text.isEmpty == false {
                self.text += " "
            }
            self.text += evt.result.text ?? ""
            DispatchQueue.global().async{
                self.azureChannel.invokeMethod("speech.onSpeech",arguments:evt.result.text ?? "")
                print("sentence recognition result: \(evt.result.text ?? "(no result)")")
            }
            //              self.azureChannel.invokeMethod("speech.onFinalResponse", arguments: self.text ?? "")
        }
        speechRecognizer?.addSessionStoppedEventHandler() {reco, evt in
            print("Received session stopped event. SessionId: \(evt.sessionId)")
            DispatchQueue.global().async{
                self.azureChannel.invokeMethod("speech.onFinalResponse", arguments: self.text)
                self.text = ""
                self.azureChannel.invokeMethod("speech.onRecognitionStopped",arguments:nil);
                self.speechRecognizer = nil
            }
            
        }
        DispatchQueue.global().async{
            self.azureChannel.invokeMethod("speech.onRecognitionStarted",arguments:nil)
        }
        
        print("Listening...")
        continuousListeningStarted = true
        do {
            try? speechRecognizer?.startContinuousRecognition()
        } catch {
            print("error \(error) happened")
        }
    }
    
    public func simpleSpeechRecognitionPlus(speechSubscriptionKey : String, serviceRegion : String, lang: String, timeoutMs: String) {
        var speechConfig: SPXSpeechConfiguration?
        do {
            try speechConfig = SPXSpeechConfiguration(subscription: speechSubscriptionKey, region: serviceRegion)
            speechConfig!.enableDictation();
        } catch {
            print("error \(error) happened")
            speechConfig = nil
        }
        speechConfig?.speechRecognitionLanguage = lang
        speechConfig?.setPropertyTo(timeoutMs, by: SPXPropertyId.speechSegmentationSilenceTimeoutMs)
        
        var referenceText: String = "";
        var pronunciationAssessmentConfig: SPXPronunciationAssessmentConfiguration?
        do {
            try pronunciationAssessmentConfig = SPXPronunciationAssessmentConfiguration.init(
                referenceText,
                gradingSystem: SPXPronunciationAssessmentGradingSystem.hundredMark,
                granularity: SPXPronunciationAssessmentGranularity.phoneme,
                enableMiscue: true)
        } catch {
            print("error \(error) happened")
            pronunciationAssessmentConfig = nil
            return
        }
        
        pronunciationAssessmentConfig?.phonemeAlphabet = "IPA"
        
        let audioConfig = SPXAudioConfiguration()
        
        let reco = try! SPXSpeechRecognizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)
        
        try! pronunciationAssessmentConfig?.apply(to: reco)
        
        //               reco.addRecognizingEventHandler() {reco, evt in
        
        //                   print("intermediate recognition result: \(evt.result.text ?? "(no result)")")
        
        //               }
        
        
        print("Listening...")
        
        let result = try! reco.recognizeOnce()
        print("recognition result: \(result.text ?? "(no result)"), reason: \(result.reason.rawValue)")
        
        pronunciationAssessmentConfig?.referenceText = result.text;
        
        let pronunciationAssessmentResultJson = result.properties?.getPropertyBy(SPXPropertyId.speechServiceResponseJsonResult)
        print("pronunciationAssessmentResultJson: \(pronunciationAssessmentResultJson ?? "(no result)")")
        
        if result.reason != SPXResultReason.recognizedSpeech {
            let cancellationDetails = try! SPXCancellationDetails(fromCanceledRecognitionResult: result)
            print("cancelled: \(result.reason), \(cancellationDetails.errorDetails)")
            print("Did you set the speech resource key and region values?")
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: "")
        } else {
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: result.text)
            azureChannel.invokeMethod("speech.onFinalAssessment", arguments: pronunciationAssessmentResultJson)
        }
        
        
        
    }
    
    
    public func soundRecord(speechSubscriptionKey : String, serviceRegion : String, lang: String, timeoutMs: String, path: String) {
        var speechConfig: SPXSpeechConfiguration?
        do {
            try speechConfig = SPXSpeechConfiguration(subscription: speechSubscriptionKey, region: serviceRegion)
        } catch {
            print("error \(error) happened")
            speechConfig = nil
        }
        speechConfig?.speechRecognitionLanguage = lang
        speechConfig?.setPropertyTo(timeoutMs, by: SPXPropertyId.speechSegmentationSilenceTimeoutMs)
        
        let audioConfig = SPXAudioConfiguration(wavFileInput:path)!
        
        let reco = try! SPXSpeechRecognizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)

        print("Listening...")
        
        let result = try! reco.recognizeOnce()
        print("recognition result: \(result.text ?? "(no result)"), reason: \(result.reason.rawValue)")
        if result.reason != SPXResultReason.recognizedSpeech {
            let cancellationDetails = try! SPXCancellationDetails(fromCanceledRecognitionResult: result)
            print("cancelled: \(result.reason), \(cancellationDetails.errorDetails)")
            print("Did you set the speech resource key and region values?")
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: "")
        } else {
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: result.text)
        }
        
        
    }

    public func soundRecordAssessment(speechSubscriptionKey : String, serviceRegion : String, lang: String, timeoutMs: String, path: String , originalText: String) {
        var speechConfig: SPXSpeechConfiguration?
        do {
            try speechConfig = SPXSpeechConfiguration(subscription: speechSubscriptionKey, region: serviceRegion)
        } catch {
            print("error \(error) happened")
            speechConfig = nil
        }
        speechConfig?.speechRecognitionLanguage = lang
        speechConfig?.setPropertyTo(timeoutMs, by: SPXPropertyId.speechSegmentationSilenceTimeoutMs)
        
        var referenceText: String = originalText;
        var pronunciationAssessmentConfig: SPXPronunciationAssessmentConfiguration?
        do {
            try pronunciationAssessmentConfig = SPXPronunciationAssessmentConfiguration.init(
                referenceText,
                gradingSystem: SPXPronunciationAssessmentGradingSystem.hundredMark,
                granularity: SPXPronunciationAssessmentGranularity.phoneme,
                enableMiscue: true)
        } catch {
            print("error \(error) happened")
            pronunciationAssessmentConfig = nil
            return
        }
        
        pronunciationAssessmentConfig?.phonemeAlphabet = "IPA"
        
        let audioConfig = SPXAudioConfiguration(wavFileInput:path)!
        
        let reco = try! SPXSpeechRecognizer(speechConfiguration: speechConfig!, audioConfiguration: audioConfig)
        
        try! pronunciationAssessmentConfig?.apply(to: reco)
        
        print("Giving Assessment...")
        
        let result = try! reco.recognizeOnce()
        
        let pronunciationAssessmentResultJson = result.properties?.getPropertyBy(SPXPropertyId.speechServiceResponseJsonResult)
        print("pronunciationAssessmentResultJson: \(pronunciationAssessmentResultJson ?? "(no result)")")
        
        if result.reason != SPXResultReason.recognizedSpeech {
            let cancellationDetails = try! SPXCancellationDetails(fromCanceledRecognitionResult: result)
            print("cancelled: \(result.reason), \(cancellationDetails.errorDetails)")
            print("Did you set the speech resource key and region values?")
            azureChannel.invokeMethod("speech.onFinalResponse", arguments: "")
        } else {
            azureChannel.invokeMethod("speech.onFinalAssessment", arguments: pronunciationAssessmentResultJson)
        }
        
        
    }    
}

