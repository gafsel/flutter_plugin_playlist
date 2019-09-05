# flutter_plugin_playlist

A Flutter plugin for Android and iOS with native support for audio playlists, background support, and lock screen controls.

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our 
[online documentation](https://flutter.dev/docs), which offers tutorials, 
samples, guidance on mobile development, and a full API reference.

# IMPORTANT

First of all, I'd like to give a special thank you to [codinronan](https://github.com/codinronan) for his exeptional work on the original plugin. It made my work in my apps a lot easier.

This plugin is an adaptation of [cordova-plugin-playlist](https://github.com/Rolamix/cordova-plugin-playlist)
to a Flutter plugin. The native source code and the documentation was based on it.

## 0. Index

1. [Notes](#1-notes)
2. [Installation](#2-installation)
3. [Usage](#3-usage)
4. [Todo](#4-todo)
5. [Credits](#5-credits)
6. [License](#6-license)

## 1. Notes

### On *Android*, utilizes a wrapper over ExoPlayer called [ExoMedia](https://github.com/brianwernick/ExoMedia). ExoPlayer is a powerful, high-quality player for Android provided by Google
### On iOS, utilizes a customized AVQueuePlayer in order to provide feedback about track changes, buffering, etc.; given that AVQueuePlayer can keep the audio session running between songs.

* This plugin intentionally does not display track cover art on the lock screen controls on iOS. Usage of the media image object on iOS is known to cause memory leaks. See the [Todo](#4-todo) section. The Swift version of that object does not (seem to) contain this memory leak, and rewriting this plugin to use Swift 4 is on the [Todo](#4-todo) list. This is fully supported on Android, however.

* This plugin is not designed to play mixable, rapid-fire, low-latency audio, as you would use in a game. A more appropriate cordova plugin for that use case is [cordova-plugin-nativeaudio](https://github.com/floatinghotpot/cordova-plugin-nativeaudio)

* Cannot mix audio; again the NativeAudio plugin is probably more appropriate. This is due to supporting the lock screen and command center controls: only an app in command of audio can do this, otherwise the controls have no meaning. I would like to add an option to do this, it should be fairly straightforward; at the cost of not supporting the OS-level controls for that invokation.

* If you are running this on iOS 9.3, this plugin requires a promise polyfill for the JS layer.

## 2. Installation

Add `flutter_plugin_playlist` as a dependency in pubspec.yml

For help on adding as a dependency, view the [documentation](https://flutter.io/using-packages/).

### Background Mode

* Android

    Android normally will give you ~2-3 minutes of background playback before killing your audio. Adding the WAKE_LOCK permission allows the plugin to utilize additional permissions to continue playing.

```
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

* iOS

    iOS will immediately stop playback when the app goes into the background if you do not include the `audio` `UIBackgroundMode`. iOS has an additional requirement that audio playback must never stop; when it does, the audio session will be terminated and playback cannot continue without user interaction.

## 3. Usage

Import the library with:

```dart
import 'package:flutter_plugin_playlist/flutter_plugin_playlist.dart';
```

Use the class `RmxAudioPlayer` for interacting directly with the plugin.

```dart
RmxAudioPlayer player = new RmxAudioPlayer();

player.initialize();

player.on('status', (eventName, arguments) {

   // TODO handle the status update

});

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

rmxAudioPlayer.play();	

```

These are the available resources:

* **initialize()**: Initializes the player (**MUST BE CALL FIRST OF ALL**)
* **currentState**: Returns the player current state in *unknown*, 
*ready*, *error*, *playing*, *loading*, *paused*, *stopped*
* **isInitialized**: If the player is initialized
* **currentTrack**: Returns the current track in the playlist
* **isStopped**: If the player is stopped
* **isPlaying**: If the player is playing
* **isPaused**: If the player is paused
* **isLoading**: If the player is loading
* **isSeeking**: If the player is seeking
* **hasLoaded**: True if the *currently playing track* has been loaded and can be played (this includes if it is *currently playing*).
* **hasError**: True if the *current track* has reported an error
* **ready()**: Returns a Future for when the player is initialized
* **setOptions(AudioPlayerOptions options)**: Sets the player options. This can be called at any time and is not required before playback can be initiated.
* **setPlaylistItems(List<AudioTrack> items, {PlaylistItemOptions options})**: Sets the entire list of tracks to be played by the playlist.
* **addItem(AudioTrack trackItem)**: Add a single track to the end of the playlist
* **addAllItems(List<AudioTrack> items)**: Adds the list of tracks to the end of the playlist.
* **removeItem(AudioTrackRemoval removeItem)**: Removes a track from the playlist. If this is the currently playing item, the next item will automatically begin playback.
* **removeItems(List<AudioTrackRemoval> items)**: Removes all given tracks from the playlist; these can be specified either by trackId or trackIndex.
* **clearAllItems()**: Clear the entire playlist. This will result in the STOPPED event being raised.
* **play()**: Begin playback. If no tracks have been added, this has no effect.
* **playTrackByIndex(num index, {num position})**: Play the track at the given index. If the track does not exist, this has no effect.
* **playTrackById(String trackId, {num position})**: Play the track matching the given trackId. If the track does not exist, this has no effect.
* **pause()**: Pause playback
* **skipForward()**: Skip to the next track. If you are already at the end, and loop is false, this has no effect.
* **skipBack()**: Skip to the previous track. If you are already at the beginning, this has no effect.
* **seekTo(num position)**: Seek to the given position in the currently playing track.
* **seekToQueuePosition(num position)**: (iOS only): Seek to the given position in the *entire queue of songs*.
* **setPlaybackRate(num rate)**: Set the playback speed
* **setVolume(num volume)**: Set the playback volume
* **setLoop(bool loop)**: Sets a flag indicating whether the playlist should loop back to the beginning once it reaches the end.
* **getPlaybackRate()**: Reports the current playback rate.
* **getVolume()**: Reports the current playback volume
* **getPosition()**: Reports the playback position of the current item.
* **getCurrentBuffer()**: Reports the buffer status of the current item.
* **getQueuePosition()**: (iOS only): Gets the overall playback position in the entire queue, in seconds (e.g. 1047 seconds).
* **on(String eventName, AudioPlayerEventHandler callback)**: Subscribe to events raised by the plugin, e.g. on('status', (data) => { ... }). For now, only 'status' is supported.
* **off(String eventName, AudioPlayerEventHandler handler)**: Remove an event handler from the plugin

Make sure to check the example and also the original cordova plugin for more details.

For Android, you can replace the notification icon by adding it at res/mipmap/icon.png.

## 4. Todo

A fast adaptation was the easier solution to my specific needs, so there is lots to improve in the dart code, like creating flutter widgets to encapsulate the service manipulation.

Any evolutions are welcome.

Plus, what was mapped by cordova-plugin-playlist:

There's so much more to do on this plugin. Some items I would like to see added if anyone wants to help:
* [iOS, Android] Add support for recording, similar to what is provided by `cordova-plugin-media`
* [iOS] Safely implement cover art for cover images displayed on the command/lock screen controls
* [iOS] Write this plugin in Swift instead of Objective-C. I didn't have time to learn Swift when I needed this.
* [iOS] Utilize [AudioPlayer](https://github.com/delannoyk/AudioPlayer) instead of directly implementing AVQueuePlayer. `AudioPlayer` includes some smart network recovery features
* Or, just add the smart network recovery features
* [iOS, Android] Add support for single-item repeat
* [iOS, Android] Add a full example

## 5. Credits

As already mentioned, this plugin is an adaptation of cordova-plugin-playlist:
* [cordova-plugin-playlist](https://github.com/Rolamix/cordova-plugin-playlist)

## 6. License

[The MIT License (MIT)](http://www.opensource.org/licenses/mit-license.html)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

