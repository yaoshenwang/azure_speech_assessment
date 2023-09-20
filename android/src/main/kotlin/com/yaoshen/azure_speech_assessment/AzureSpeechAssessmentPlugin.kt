package com.yaoshen.azure_speech_assessment;

//import androidx.core.app.ActivityCompat;

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** AzureSpeechAssessmentPlugin */
public class AzureSpeechAssessmentPlugin : FlutterPlugin, Activity(), MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var azureChannel: MethodChannel;
  private var microphoneStream: MicrophoneStream? = null;
  private var handler: Handler = Handler(Looper.getMainLooper());
  private var continuousListeningStarted: Boolean = false;
  private lateinit var reco: SpeechRecognizer;
  private var enableDictation: Boolean = false;
  private fun createMicrophoneStream(): MicrophoneStream {
    if (microphoneStream != null) {
      microphoneStream!!.close();
      microphoneStream = null;
    }

    microphoneStream = MicrophoneStream();
    return microphoneStream!!;
  }

  private var speakSynthesizer: SpeechSynthesizer? = null;

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    azureChannel = MethodChannel(
      flutterPluginBinding.getFlutterEngine().getDartExecutor(),
      "azure_speech_recognition"
    )
    azureChannel.setMethodCallHandler(this);

  }

  /*companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "azure_speech_recognition")
      channel.setMethodCallHandler(AzureSpeechRecognitionPlugin(registrar.activity(),channel))
    }
  }

  init{
    this.azureChannel = channel;
    this.azureChannel.setMethodCallHandler(this);

    handler = Handler(Looper.getMainLooper());
  }*/


  private fun getAudioConfig(): AudioConfig {
    return AudioConfig.fromDefaultMicrophoneInput();
  }


  @RequiresApi(Build.VERSION_CODES.KITKAT)
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
        "simpleVoice" -> {
          //_result = result;
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");
          val timeoutMs: String = "" + call.argument("timeout");

          simpleSpeechRecognition(speechSubscriptionKey, serviceRegion, lang, timeoutMs);
          result.success(true);

        }
        "micStream" -> {
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");


          micStreamRecognition(speechSubscriptionKey, serviceRegion, lang);
          result.success(true);

        }
        "continuousStream" -> {
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");


          micStreamContinuosly(speechSubscriptionKey, serviceRegion, lang);
          result.success(true);

        }
        "dictationMode" -> {
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");

          enableDictation = true;
          micStreamContinuosly(speechSubscriptionKey, serviceRegion, lang);
          result.success(true);

        }
        "intentRecognizer" -> {
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val appId: String = "" + call.argument("appId");
          val lang: String = "" + call.argument("language");


          recognizeIntent(speechSubscriptionKey, serviceRegion, appId, lang);
          result.success(true);

        }
        "keywordRecognizer" -> {
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");
          val kwsModel: String = "" + call.argument("kwsModel");

          keywordRecognizer(speechSubscriptionKey, serviceRegion, lang, kwsModel);
          result.success(true);

        }
        "simpleVoicePlus" -> {
          //_result = result;
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");
          val timeoutMs: String = "" + call.argument("timeout");

          simpleSpeechRecognitionPlus(speechSubscriptionKey, serviceRegion, lang, timeoutMs);
          result.success(true);

        }
        "soundRecord" -> {
          //_result = result;
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");
          val timeoutMs: String = "" + call.argument("timeout");
          val path: String = "" + call.argument("path");
          print("soundRecord called");

          soundRecord(speechSubscriptionKey, serviceRegion, lang, timeoutMs, path);
          result.success(true);

        }
        "soundRecordAssessment" -> {
          //_result = result;
          val permissionRequestId: Int = 5;
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");
          val timeoutMs: String = "" + call.argument("timeout");
          val path: String = "" + call.argument("path");
          print("soundRecord called");

          soundRecordAssessment(speechSubscriptionKey, serviceRegion, lang, timeoutMs, path);
          result.success(true);

        }
        "speakText" -> {
          val permissionRequestId: Int = 5;
          val text: String = "" + call.argument("text");
          val speechSubscriptionKey: String = "" + call.argument("subscriptionKey");
          val serviceRegion: String = "" + call.argument("region");
          val lang: String = "" + call.argument("language");
          val voiceName: String = "" + call.argument("voiceName");

          speakText(text, speechSubscriptionKey, serviceRegion, lang, voiceName);
          result.success(true);
        }
        "speakStop" -> {
          speakStop();
          result.success(true);
        }
        else -> {
          result.notImplemented()
        }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    azureChannel.setMethodCallHandler(null)
  }

  private fun simpleSpeechRecognition(
    speechSubscriptionKey: String,
    serviceRegion: String,
    lang: String,
    timeoutMs: String
  ) {
    val logTag: String = "simpleVoice";


    try {

      val audioInput: AudioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());

      val config: SpeechConfig =
        SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
      assert(config != null);

      config.speechRecognitionLanguage = lang;
      config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, timeoutMs);

      val reco: SpeechRecognizer = SpeechRecognizer(config, audioInput);

      assert(reco != null);

      val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync();

      assert(task != null);

      invokeMethod("speech.onRecognitionStarted", null);

      setOnTaskCompletedListener(task) { result ->
        val s = result.text
        Log.i(logTag, "Recognizer returned: $s")
        if (result.reason == ResultReason.RecognizedSpeech) {
          invokeMethod("speech.onFinalResponse", s);
        } else {
          invokeMethod("speech.onFinalResponse", "");
        }

        reco.close()

      }

    } catch (exec: Exception) {
      assert(false);
      invokeMethod("speech.onException", "Exception: " + exec.message);

    }
  }

  // SpeakStop
  private fun speakStop() {
    val logTag = "speakStop"
    try {
      speakSynthesizer?.StopSpeakingAsync()
    } catch (exec: Exception) {
      // Log.i(logTag, "speakStop ${exec.message}")
    }
  }

  // SpeakText
  private fun speakText(
    text: String,
    speechSubscriptionKey: String,
    serviceRegion: String,
    lang: String,
    voiceName: String
  ) {
    val logTag = "speakText";
    try {
      val audioConfig: AudioConfig = AudioConfig.fromDefaultSpeakerOutput();

      val config: SpeechConfig =
        SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);

      config.speechSynthesisLanguage = lang;
      config.speechSynthesisVoiceName = voiceName;

      speakSynthesizer = SpeechSynthesizer(config, audioConfig);

      assert(speakSynthesizer != null);

      invokeMethod("speech.onSpeakStarted", null);
      // Log.i(
      //   logTag,
      //   "speakText key: $speechSubscriptionKey, region: $serviceRegion, lang: $lang, $text"
      // );

      val speakTask = speakSynthesizer?.SpeakTextAsync(text);

      setOnTaskCompletedListener(speakTask as Future<SpeechSynthesisResult>) { result ->
        if (result.reason == ResultReason.SynthesizingAudioCompleted) {
          // Log.i(
          //   logTag,
          //   "speakText Completed audioLength:${result.audioLength} audioDuration: ${result.audioDuration}"
          // );
        } else if (result.reason == ResultReason.Canceled) {
          // val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result).toString()
          // Log.i(logTag, "speakText Canceled $cancellationDetails");
        }

        speakSynthesizer?.close()
        result.close()
        config.close()

        invokeMethod("speech.onSpeakStopped", null);
      }
    } catch (exec: Exception) {
      assert(false);
      // Log.i(logTag, "speakText unexpected ${exec.message}");
      invokeMethod("speech.onException", "Exception: " + exec.message);
    }
  }

  // Mic Streaming, it need the additional method implementend to get the data from the async task
  private fun micStreamRecognition(speechSubscriptionKey: String, serviceRegion: String, lang: String) {
    val logTag: String = "micStream";

    try {

      val audioInput: AudioConfig = AudioConfig.fromStreamInput(createMicrophoneStream());


      val config: SpeechConfig =
        SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
      assert(config != null);

      config.speechRecognitionLanguage = lang;

      val reco: SpeechRecognizer = SpeechRecognizer(config, audioInput);

      assert(reco != null);

      invokeMethod("speech.onRecognitionStarted", null);

      reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
        val s = speechRecognitionResultEventArgs.result.text
        //Log.i(logTag, "Intermediate result received: " + s)
        invokeMethod("speech.onSpeech", s);
      };


      val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync();


      setOnTaskCompletedListener(task) { result ->
        val s = result.text
        reco.close()
        //Log.i(logTag, "Recognizer returned: " + s)
        invokeMethod("speech.onFinalResponse", s);
      }

    } catch (exec: Exception) {
      assert(false);
      invokeMethod("speech.onException", "Exception: " + exec.message);
    }
  }

  // stream continuosly until you press the button to stop ! STILL NOT WORKING COMPLETELY

  private fun micStreamContinuosly(speechSubscriptionKey: String, serviceRegion: String, lang: String) {
    val logTag: String = "micStreamContinuos";


    lateinit var audioInput: AudioConfig;
    val content: ArrayList<String> = ArrayList<String>();


    Log.i(logTag, "StatoRiconoscimentoVocale: $continuousListeningStarted");

    if (continuousListeningStarted) {
      if (reco != null) {
        val task = reco.stopContinuousRecognitionAsync();

        setOnTaskCompletedListener(task) {
          Log.i(logTag, "Continuous recognition stopped.");
          continuousListeningStarted = false;
          invokeMethod("speech.onRecognitionStopped", null);
          reco.close();

        }
      } else {
        continuousListeningStarted = false;
      }
      return;
    }

    content.clear();

    try {

      //audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
      val config: SpeechConfig =
        SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
      assert(config != null);

      config.speechRecognitionLanguage = lang;

      if (enableDictation) {
        Log.i(logTag, "Enabled BF dictation");
        config.enableDictation();
        Log.i(logTag, "Enabled AF dictation");

      }

      reco = SpeechRecognizer(config, getAudioConfig());

      assert(reco != null);




      reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
        val s = speechRecognitionResultEventArgs.result.text
        content.add(s);
        Log.i(logTag, "Intermediate result received: $s")
        invokeMethod("speech.onSpeech", s);
        content.removeAt(content.size - 1);
      };

      reco.recognized.addEventListener { _, speechRecognitionResultEventArgs ->
        val s = speechRecognitionResultEventArgs.result.text
        content.add(s);
        Log.i(logTag, "Final result received: $s")
        invokeMethod("speech.onFinalResponse", s);
      };


      val _task2 = reco.startContinuousRecognitionAsync();

      setOnTaskCompletedListener(_task2) {
        continuousListeningStarted = true;
        invokeMethod("speech.onRecognitionStarted", null);

        //invokeMethod("speech.onStopAvailable",null);
      }


    } catch (exec: Exception) {
      assert(false);
      invokeMethod("speech.onException", "Exception: " + exec.message);

    }
  }


  /// Recognize Intent method from microsoft sdk

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  private fun recognizeIntent(
    speechSubscriptionKey: String,
    serviceRegion: String,
    appId: String,
    lang: String
  ) {
    val logTag: String = "intent";

    val content: ArrayList<String> = ArrayList<String>();

    content.add("");
    content.add("");

    try {

      val audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());


      val config: SpeechConfig =
        SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);

      assert(config != null);

      config.speechRecognitionLanguage = lang;

      val reco = IntentRecognizer(config, audioInput);

      val intentModel: LanguageUnderstandingModel = LanguageUnderstandingModel.fromAppId(appId);
      reco.addAllIntents(intentModel);

      reco.recognizing.addEventListener { o, intentRecognitionResultEventArgs ->
        val s = intentRecognitionResultEventArgs.result.text
        content[0] = s;
        Log.i(logTag, "Final result received: $s")
        invokeMethod("speech.onFinalResponse", TextUtils.join(System.lineSeparator(), content));
      };


      val task: Future<IntentRecognitionResult> = reco.recognizeOnceAsync();



      setOnTaskCompletedListener(task) { result ->
        Log.i(logTag, "Continuous recognition stopped.");

        var s = result.text;

        if (result.reason != ResultReason.RecognizedIntent) {
          val errorDetails =
            if (result.reason == ResultReason.Canceled) CancellationDetails.fromResult(result)
              .errorDetails else "";
          s =
            "Intent failed with " + result.reason + ". Did you enter your Language Understanding subscription?" + System.lineSeparator() + errorDetails;
        }

        val intentId = result.intentId;


        content[0] = s;
        content[1] = "[intent: $intentId ]";

        invokeMethod("speech.onSpeech", TextUtils.join(System.lineSeparator(), content));
        println("Stopped");
      }


    } catch (exec: Exception) {
      //Log.e("SpeechSDKDemo", "unexpected " + exec.message);
      assert(false);
      invokeMethod("speech.onException", "Exception: " + exec.message);
    }
  }

  private fun keywordRecognizer(
    speechSubscriptionKey: String,
    serviceRegion: String,
    lang: String,
    kwsModelFile: String
  ) {
    val logTag: String = "keyword";
    var continuousListeningStarted: Boolean = false;
    lateinit var reco: SpeechRecognizer;
    lateinit var audioInput: AudioConfig;
    val content: ArrayList<String> = ArrayList<String>();




    if (continuousListeningStarted) {
      if (reco != null) {
        val task: Future<Void> = reco.stopContinuousRecognitionAsync();

        setOnTaskCompletedListener(task) {
          Log.i(logTag, "Continuous recognition stopped.");
          continuousListeningStarted = false;
          azureChannel.invokeMethod("speech.onStartAvailable", null);
        }

      } else {
        continuousListeningStarted = false;
      }

      return;
    }

    content.clear();
    try {

      audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());

      val config: SpeechConfig =
        SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);

      assert(config != null);

      config.speechRecognitionLanguage = lang;

      reco = SpeechRecognizer(config, audioInput);

      reco.recognizing.addEventListener { o, speechRecognitionResultEventArgs ->
        val s = speechRecognitionResultEventArgs.result.text
        content.add(s);
        Log.i(logTag, "Intermediate result received: $s")
        invokeMethod("speech.onSpeech", TextUtils.join(" ", content));
        content.removeAt(content.size - 1);
      };

      reco.recognizing.addEventListener { _, speechRecognitionResultEventArgs ->
        val s: String;
        if (speechRecognitionResultEventArgs.result
            .reason == ResultReason.RecognizedKeyword
        ) {
          s = "Keyword: " + speechRecognitionResultEventArgs.result.text;
          Log.i(logTag, "Keyword recognized result received: $s");
        } else {
          s = "Recognized: " + speechRecognitionResultEventArgs.result.text;
          Log.i(logTag, "Final result received: $s");
        }
        content.add(s);
        invokeMethod("speech.onSpeech", s);
      };

      var kwsModel = KeywordRecognitionModel.fromFile(copyAssetToCacheAndGetFilePath(kwsModelFile));
      val task: Future<Void> = reco.startKeywordRecognitionAsync(kwsModel);


      setOnTaskCompletedListener(task) {
        continuousListeningStarted = true;

        invokeMethod("speech.onStopAvailable", null);
        println("Stopped");
      }


    } catch (exc: Exception) {

    }
  }


  /**
   * 执行一次语音识别操作
   *
   * @param speechSubscriptionKey Azure 语音服务订阅密钥
   * @param serviceRegion Azure 语音服务区域
   * @param lang 语音识别语言
   * @param timeoutMs 分段静默超时时间
   */
  private fun simpleSpeechRecognitionPlus(
    speechSubscriptionKey: String,
    serviceRegion: String,
    lang: String,
    timeoutMs: String
  ) {
    // 创建用于日志输出的标签
    val logTag: String = "simpleVoicePlus"

    try {
      // 从麦克风创建 AudioConfig 对象，用于配置语音识别的音频输入
      val audioInput: AudioConfig = AudioConfig.fromStreamInput(createMicrophoneStream())

      // 从 Azure 语音服务订阅密钥和服务区域创建 SpeechConfig 对象，用于配置语音识别的参数
      val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
      // 如果 SpeechConfig 对象创建失败，则会抛出异常
      assert(config != null)

      // 设置语音识别的语言和分段静默超时时间
      config.speechRecognitionLanguage = lang
      config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, timeoutMs)

      // 从 SpeechConfig 和 AudioConfig 对象创建 SpeechRecognizer 对象，用于执行语音识别操作
      val reco: SpeechRecognizer = SpeechRecognizer(config, audioInput)
      // 如果 SpeechRecognizer 对象创建失败，则会抛出异常
      assert(reco != null)

      val gradingSystem = PronunciationAssessmentGradingSystem.HundredMark
      val granularity = PronunciationAssessmentGranularity.Phoneme
      // 创建语音评估对象
      val pronunciationAssessmentConfig =
        PronunciationAssessmentConfig("", gradingSystem, granularity, true)
      pronunciationAssessmentConfig.setPhonemeAlphabet("IPA")
      // 设置语音评估对象
      pronunciationAssessmentConfig.applyTo(reco);

      // 调用 recognizeOnceAsync() 方法执行一次语音识别操作，并返回一个 Future<SpeechRecognitionResult> 对象，表示异步操作的结果
      val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync()
      // 如果异步操作失败，则会抛出异常
      assert(task != null)

      // 通知 Flutter 界面语音识别已经开始
      invokeMethod("speech.onRecognitionStarted", null)


      // 等待语音识别操作完成，并在语音识别完成后执行回调函数
      setOnTaskCompletedListener(task) { result ->
        // 获取语音识别结果
        val s = result.text
        Log.i(logTag, "Recognizer returned: $s")

        // 将语音识别结果传递给语音评估对象
        pronunciationAssessmentConfig.referenceText = s


        // 获取语音评估结果
        val pronunciationAssessmentResultJson =
          result.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
            .toString();
        //.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
        Log.i(logTag, "Pronunciation assessment result: $pronunciationAssessmentResultJson");


        // 如果语音识别成功，则将识别结果传递给 Flutter 界面
        if (result.reason == ResultReason.RecognizedSpeech) {
          invokeMethod("speech.onFinalResponse", s);
          invokeMethod("speech.onFinalAssessment", pronunciationAssessmentResultJson);
        } else {
          // 如果语音识别失败，则将空字符串传递给 Flutter 界面
          invokeMethod("speech.onFinalResponse", "");
        }

        // 关闭 SpeechRecognizer 对象
        reco.close()
        pronunciationAssessmentConfig.close();
        audioInput.close();
        config.close();

      }

    } catch (exec: Exception) {
      // 如果在语音识别过程中发生异常，则将异常信息传递给 Flutter 界面
      assert(false)
      invokeMethod("speech.onException", "Exception: " + exec.message)

    }
  }

  private fun soundRecord(
    speechSubscriptionKey: String,
    serviceRegion: String,
    lang: String,
    timeoutMs: String,
    path: String
  ) {
    // 创建用于日志输出的标签
    val logTag: String = "soundRecord"

    Log.i(logTag, "path:$path");

    try {
      // 从麦克风创建 AudioConfig 对象，用于配置语音识别的音频输入
      val audioInput: AudioConfig = AudioConfig.fromWavFileInput(path);

      // 从 Azure 语音服务订阅密钥和服务区域创建 SpeechConfig 对象，用于配置语音识别的参数
      val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
      // 如果 SpeechConfig 对象创建失败，则会抛出异常
      assert(config != null)

      // 设置语音识别的语言和分段静默超时时间
      config.speechRecognitionLanguage = lang
      config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, timeoutMs)

      // 从 SpeechConfig 和 AudioConfig 对象创建 SpeechRecognizer 对象，用于执行语音识别操作
      val reco: SpeechRecognizer = SpeechRecognizer(config, audioInput)
      // 如果 SpeechRecognizer 对象创建失败，则会抛出异常
      assert(reco != null)

      // 调用 recognizeOnceAsync() 方法执行一次语音识别操作，并返回一个 Future<SpeechRecognitionResult> 对象，表示异步操作的结果
      val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync()
      // 如果异步操作失败，则会抛出异常
      assert(task != null)

      // 通知 Flutter 界面语音识别已经开始
      invokeMethod("speech.onRecognitionStarted", null)


      // 等待语音识别操作完成，并在语音识别完成后执行回调函数
      setOnTaskCompletedListener(task) { result ->
        // 获取语音识别结果
        val s = result.text
        Log.i(logTag, "Recognizer returned: $s")


        // 如果语音识别成功，则将识别结果传递给 Flutter 界面
        if (result.reason == ResultReason.RecognizedSpeech) {
          invokeMethod("speech.onFinalResponse", s);
        } else {
          // 如果语音识别失败，则将空字符串传递给 Flutter 界面
          invokeMethod("speech.onFinalResponse", "failed");
        }

        // 关闭 SpeechRecognizer 对象
        reco.close()
        audioInput.close();
        config.close();

      }

    } catch (exec: Exception) {
      // 如果在语音识别过程中发生异常，则将异常信息传递给 Flutter 界面
      assert(false)
      invokeMethod("speech.onException", "Exception: " + exec.message)

    }
  }

  private fun soundRecordAssessment(
    speechSubscriptionKey: String,
    serviceRegion: String,
    lang: String,
    timeoutMs: String,
    path: String
) {
  // 创建用于日志输出的标签
  val logTag: String = "soundRecordAssessment"

  Log.i(logTag, "path:$path")

  try {
    // 从麦克风创建 AudioConfig 对象，用于配置语音识别的音频输入
    val audioInput: AudioConfig = AudioConfig.fromWavFileInput(path)

    // 从 Azure 语音服务订阅密钥和服务区域创建 SpeechConfig 对象，用于配置语音识别的参数
    val config: SpeechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)
    // 如果 SpeechConfig 对象创建失败，则会抛出异常
    assert(config != null)

    // 设置语音识别的语言和分段静默超时时间
    config.speechRecognitionLanguage = lang
    config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, timeoutMs)

    // 从 SpeechConfig 和 AudioConfig 对象创建 SpeechRecognizer 对象，用于执行语音识别操作
    val reco: SpeechRecognizer = SpeechRecognizer(config, audioInput)
    // 如果 SpeechRecognizer 对象创建失败，则会抛出异常
    assert(reco != null)

    val gradingSystem = PronunciationAssessmentGradingSystem.HundredMark
    val granularity = PronunciationAssessmentGranularity.Phoneme
    // 创建语音评估对象
    val pronunciationAssessmentConfig =
        PronunciationAssessmentConfig("", gradingSystem, granularity, true)
    pronunciationAssessmentConfig.setPhonemeAlphabet("IPA")
    // 设置语音评估对象
    pronunciationAssessmentConfig.applyTo(reco)

    // 调用 recognizeOnceAsync() 方法执行一次语音识别操作，并返回一个 Future<SpeechRecognitionResult> 对象，表示异步操作的结果
    val task: Future<SpeechRecognitionResult> = reco.recognizeOnceAsync()
    // 如果异步操作失败，则会抛出异常
    assert(task != null)

    // 通知 Flutter 界面语音识别已经开始
    invokeMethod("speech.onRecognitionStarted", null)

    // 等待语音识别操作完成，并在语音识别完成后执行回调函数
    setOnTaskCompletedListener(task) { result ->

      // 获取语音识别结果
      val s = result.text

      // 将语音识别结果传递给语音评估对象
      pronunciationAssessmentConfig.referenceText = s

      // 获取语音评估结果
      val pronunciationAssessmentResultJson =
          result.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult).toString()
      // .getProperty(PropertyId.SpeechServiceResponse_JsonResult)
      Log.i(logTag, "Pronunciation assessment result: $pronunciationAssessmentResultJson")

      // 如果语音识别成功，则将识别结果传递给 Flutter 界面
      if (result.reason == ResultReason.RecognizedSpeech) {
        invokeMethod("speech.onFinalAssessment", pronunciationAssessmentResultJson)
      } else {
        // 如果语音识别失败，则将空字符串传递给 Flutter 界面
        invokeMethod("speech.onFinalAssessment", "failed")
      }

      // 关闭 SpeechRecognizer 对象
      reco.close()
      pronunciationAssessmentConfig.close()
      audioInput.close()
      config.close()
    }
  } catch (exec: Exception) {
    // 如果在语音识别过程中发生异常，则将异常信息传递给 Flutter 界面
    assert(false)
    invokeMethod("speech.onException", "Exception: " + exec.message)
  }
}



  private val s_executorService: ExecutorService = Executors.newCachedThreadPool();


  private fun <T> setOnTaskCompletedListener(task: Future<T>, listener: (T) -> Unit) {
    s_executorService.submit {
      val result = task.get()
      listener(result)
    };
  }


  private interface OnTaskCompletedListener<T> {
    fun onCompleted(taskResult: T);
  }

  private fun setRecognizedText(s: String) {
    azureChannel.invokeMethod("speech.onSpeech", s);
  }

  private fun invokeMethod(method: String, arguments: Any?) {

    handler.post {
      azureChannel.invokeMethod(method, arguments);
    }
  }


  private fun copyAssetToCacheAndGetFilePath(filename: String): String {
    val cacheFile: File = File("$cacheDir/$filename");
    if (!cacheFile.exists()) {
      try {
        val iS: InputStream = assets.open(filename);
        val size: Int = iS.available();
        val buffer: ByteArray = ByteArray(size);
        iS.read(buffer);
        iS.close();
        val fos: FileOutputStream = FileOutputStream(cacheFile);
        fos.write(buffer);
        fos.close();
      } catch (e: Exception) {
        throw RuntimeException(e);
      }
    }
    return cacheFile.path;
  }
}
