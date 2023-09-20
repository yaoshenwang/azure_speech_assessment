import 'dart:async';
import 'dart:io';

import 'package:just_audio/just_audio.dart' as ap;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter/services.dart';
import 'package:azure_speech_assessment/azure_speech_assessment.dart';
import 'dart:convert';

class AudioPlayer extends StatefulWidget {
  const AudioPlayer({
    required this.source,
    required this.onDelete,
    this.stringPath,
    Key? key,
  }) : super(key: key);

  /// Path from where to play recorded audio
  final ap.AudioSource source;

  /// Callback when audio file should be removed
  /// Setting this to null hides the delete button
  final VoidCallback onDelete;

  final String? stringPath;

  @override
  AudioPlayerState createState() => AudioPlayerState();
  @override
  void debugFillProperties(DiagnosticPropertiesBuilder properties) {
    super.debugFillProperties(properties);
    properties.add(DiagnosticsProperty<ap.AudioSource>('source', source));
    properties.add(ObjectFlagProperty<VoidCallback>.has('onDelete', onDelete));
  }
}

class AudioPlayerState extends State<AudioPlayer> {
  static const double _controlSize = 56;
  static const double _deleteBtnSize = 24;

  final _audioPlayer = ap.AudioPlayer();
  late StreamSubscription<void> _playerStateChangedSubscription;
  late StreamSubscription<Duration?> _durationChangedSubscription;
  late StreamSubscription<Duration> _positionChangedSubscription;
  Duration? _position;
  Duration? _duration;

  late AzureSpeechAssessment _speechAzure;
  String subKey = "1ae61ffbb49243e58cba2b322565d80f";
  String region = "eastus";
  String lang = "en-US";
  String timeout = "2000";

  String recognizeText = '请点击发送按钮以获取文字';
  String scoreText = '请点击获取分数按钮以获取分数';

  void activateSpeechRecognizer() {
    // MANDATORY INITIALIZATION
    AzureSpeechAssessment.initialize(subKey, region,
        lang: lang, timeout: timeout);

    _speechAzure.setFinalTranscription((text) {
      // do what you want with your final transcription
      recognizeText = text;
      setState(() {});
    });

    _speechAzure.setFinalAssessment((text) {
      // do what you want with your final transcription
      scoreText = getScore(text);
      setState(() {});
    });

    _speechAzure.setRecognitionStartedHandler(() {
      // called at the start of recognition (it could also not be used)
    });
  }

  String getScore(String text) {
    Map<String, dynamic> jsonMap = json.decode(text);
    var tempScore = jsonMap['NBest'][0]['PronunciationAssessment'];
    return tempScore.toString();
  }

  @override
  void initState() {
    _playerStateChangedSubscription =
        _audioPlayer.playerStateStream.listen((ap.PlayerState state) async {
      if (state.processingState == ap.ProcessingState.completed) {
        await stop();
      }
      setState(() {});
    });
    _positionChangedSubscription = _audioPlayer.positionStream
        .listen((Duration position) => setState(() {}));
    _durationChangedSubscription = _audioPlayer.durationStream
        .listen((Duration? duration) => setState(() {}));
    _init();

    _speechAzure = new AzureSpeechAssessment();
    activateSpeechRecognizer();

    super.initState();
  }

  Future<void> _init() async {
    await _audioPlayer.setAudioSource(widget.source);
    //await _audioPlayer.setAudioSource(ap.AudioSource.uri(Uri.parse(
    //'/var/mobile/Containers/Data/Application/B41D5370-A58F-4484-B7C9-EBD23CF1AD51/Documents/audio_1694772899246.m4a')));
  }

  @override
  void dispose() {
    _playerStateChangedSubscription.cancel();
    _positionChangedSubscription.cancel();
    _durationChangedSubscription.cancel();
    _audioPlayer.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisSize: MainAxisSize.max,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                //播放暂停按钮
                _buildControl(),

                //进度条
                _buildSlider(constraints.maxWidth / 2),

                //删除按钮
                IconButton(
                  icon: const Icon(Icons.delete,
                      color: Color(0xFF73748D), size: _deleteBtnSize),
                  onPressed: () {
                    _audioPlayer.stop().then((value) => widget.onDelete());
                  },
                ),
              ],
            ),
            const SizedBox(
              height: 20,
            ),
            Row(
              mainAxisSize: MainAxisSize.max,
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                OutlinedButton(
                  child: Text("获取文本"),
                  style: OutlinedButton.styleFrom(
                    primary: Colors.red,
                    side: BorderSide(
                      color: Colors.red,
                    ),
                  ),
                  onPressed: () {
                    _send();
                  },
                ),
                OutlinedButton(
                  child: Text("获取分数"),
                  style: OutlinedButton.styleFrom(
                    primary: Colors.red,
                    side: BorderSide(
                      color: Colors.red,
                    ),
                  ),
                  onPressed: () {
                    _getAssessmentScore(recognizeText);
                  },
                ),
              ],
            ),

            const SizedBox(
              height: 20,
            ),
            Text('${_duration ?? 0.0}'),
            const SizedBox(
              height: 20,
            ),
            //显示识别内容
            Text(recognizeText),
            const SizedBox(
              height: 20,
            ),

            //显示口语评分
            Text(scoreText)
          ],
        );
      },
    );
  }

  Widget _buildControl() {
    Icon icon;
    Color color;

    if (_audioPlayer.playerState.playing) {
      icon = const Icon(Icons.pause, color: Colors.red, size: 30);
      color = Colors.red.withOpacity(0.1);
    } else {
      final theme = Theme.of(context);
      icon = Icon(Icons.play_arrow, color: theme.primaryColor, size: 30);
      color = theme.primaryColor.withOpacity(0.1);
    }

    return ClipOval(
      child: Material(
        color: color,
        child: InkWell(
          child:
              SizedBox(width: _controlSize, height: _controlSize, child: icon),
          onTap: () {
            if (_audioPlayer.playerState.playing) {
              pause();
            } else {
              play();
            }
          },
        ),
      ),
    );
  }

  Widget _buildSlider(double widgetWidth) {
    bool canSetValue = false;
    final duration = _duration;
    final position = _position;

    if (duration != null && position != null) {
      canSetValue = position.inMilliseconds > 0;
      canSetValue &= position.inMilliseconds < duration.inMilliseconds;
    }

    double width = widgetWidth - _controlSize - _deleteBtnSize;
    width -= _deleteBtnSize;

    return SizedBox(
      width: width,
      child: Slider(
        activeColor: Theme.of(context).primaryColor,
        inactiveColor: Theme.of(context).colorScheme.secondary,
        onChanged: (v) {
          if (duration != null) {
            final position = v * duration.inMilliseconds;
            _audioPlayer.seek(Duration(milliseconds: position.round()));
          }
        },
        value: canSetValue && duration != null && position != null
            ? position.inMilliseconds / duration.inMilliseconds
            : 0.0,
      ),
    );
  }

  Future<void> play() {
    return _audioPlayer.play();
  }

  Future<void> pause() {
    return _audioPlayer.pause();
  }

  Future<void> stop() async {
    await _audioPlayer.stop();
    return _audioPlayer.seek(Duration.zero);
  }

  Future<void> _send() async {
    //防止用户未暂停，先提前暂停
    _audioPlayer.pause();

    if (Platform.isAndroid) {
      //发送录音
      AzureSpeechAssessment.soundRecord(widget.stringPath!);
    } else if (Platform.isIOS) {
      //发送录音
      AzureSpeechAssessment.soundRecord(widget.stringPath!.substring(7));
    }

    //_audioPlayer.stop().then((value) => widget.onDelete());
  }

  Future<void> _getAssessmentScore(String originalText) async {
    //防止用户未暂停，先提前暂停
    _audioPlayer.pause();

    if (Platform.isAndroid) {
      //发送录音
      AzureSpeechAssessment.soundRecordAssessment(widget.stringPath!);
    } else if (Platform.isIOS) {
      //发送录音
      AzureSpeechAssessment.soundRecordAssessment(
          widget.stringPath!.substring(7));
    }

    //_audioPlayer.stop().then((value) => widget.onDelete());
  }
}
