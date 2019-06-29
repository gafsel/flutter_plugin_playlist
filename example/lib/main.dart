import 'package:flutter/material.dart';
import 'package:flutter_plugin_playlist/flutter_plugin_playlist.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  double _seeking;
  double _position = 0;

  int _current = 0;
  int _total = 0;

  @override
  void initState() {
    super.initState();

    rmxAudioPlayer.initialize();

    rmxAudioPlayer.on('status', (eventName, {dynamic args}) {
      print(eventName + (args??"").toString());

      if ((args as OnStatusCallbackData).value != null) {
        setState(() {
          if ((args as OnStatusCallbackData).value['currentPosition'] != null) {
            _current = (args as OnStatusCallbackData).value['currentPosition'].toInt();
            _total =
                (((args as OnStatusCallbackData).value['duration']) ?? 0)
                    .toInt();

            if (_current > 0 && _total > 0) {
              _position = _current / _total;
            } else if (!rmxAudioPlayer.isLoading && !rmxAudioPlayer.isSeeking) {
              _position = 0;
            }

            if (_seeking != null && !rmxAudioPlayer.isSeeking && !rmxAudioPlayer.isLoading) {
              _seeking = null;
            }
          }
        });
      }
    });

  }

  _prepare() {
    rmxAudioPlayer.setPlaylistItems(
        [
          new AudioTrack(
              album: "Friends",
              artist: "Bon Jovi",
              assetUrl: "https://www.soundboard.com/mediafiles/22/223554-d1826dea-bfc3-477b-a316-20ded5e63e08.mp3",
              title: "I'll be there for you"
          ),
          new AudioTrack(
              album: "Friends",
              artist: "Ross",
              assetUrl: "https://www.soundboard.com/mediafiles/22/223554-fea5dfff-6c80-4e13-b0cf-9926198f50f3.mp3",
              title: "The Sound"
          ),
          new AudioTrack(
              album: "Friends",
              artist: "Friends",
              assetUrl: "https://www.soundboard.com/mediafiles/22/223554-3943c7cb-46e0-48b1-a954-057b71140e49.mp3",
              title: "F.R.I.E.N.D.S"
          ),
        ],
        options: new PlaylistItemOptions(
          startPaused: true,
        )
    );
  }

  _play() {
    setState(() {

    });

    rmxAudioPlayer.play().then((_){
      setState(() {

      });
    }).catchError(print);
  }

  _pause() {
    rmxAudioPlayer.pause()
        .then((_){
      print(_);
      setState(() {

      });
    }).catchError(print);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Player Example'),
        ),
        body: Center(
          child: _body(),
        ),
      ),
    );
  }

  Widget _body() {
    if (rmxAudioPlayer.currentTrack == null || rmxAudioPlayer.isStopped) {
      return _actionPrepare();
    }

    return _player();
  }

  Widget _actionPrepare() {
    return RaisedButton(
      child: const Text("Prepare Playlist"),
      onPressed: _prepare,
    );
  }

  Widget _player() {
    return Padding(
      padding: const EdgeInsets.all(10),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              new Text(_format(_current)),
              new Text(_format(_total))
            ],
          ),
          Slider(
            value: _seeking??_position,
            onChangeEnd: (val) async {
              if (_total > 0) {
                await rmxAudioPlayer.seekTo(val * _total);
              }
            },
            onChanged: (val) {
              if (_total > 0) {
                setState(() {
                  _seeking = val;
                });
              }
            },
          ),
          Material(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[

                IconButton(
                  onPressed: rmxAudioPlayer.skipBack,
                  icon: const Icon(Icons.skip_previous),
                ),

                IconButton(
                  onPressed: _onPressed(),
                  icon: _icon(),
                ),

                IconButton(
                  onPressed: rmxAudioPlayer.skipForward,
                  icon: const Icon(Icons.skip_next),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  String _format(int secs) {
    int sec = secs;

    int min = 0;
    if (secs > 60) {
      min = (sec / 60).floor();
      sec = sec % 60;
    }

    return (min >= 10 ? min.toString() : '0' + min.toString()) + ":"
        + (sec >= 10 ? sec.toString() : '0' + sec.toString());

  }

  _onPressed() {
    if (rmxAudioPlayer.isLoading || rmxAudioPlayer.isSeeking) return null;

    if (rmxAudioPlayer.isPlaying) return _pause;

    return _play;
  }

  Widget _icon() {
    if (rmxAudioPlayer.isLoading || rmxAudioPlayer.isSeeking) {
      return const CircularProgressIndicator();
    }

    if (rmxAudioPlayer.isPlaying) {
      return const Icon(Icons.pause_circle_outline);
    }

    return const Icon(Icons.play_circle_outline);
  }
}
