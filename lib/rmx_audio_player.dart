part of flutter_plugin_playlist;

final itemStatusChangeTypes = [
  RmxAudioStatusMessage.RMXSTATUS_PLAYING,
  RmxAudioStatusMessage.RMXSTATUS_PLAYBACK_POSITION,
  RmxAudioStatusMessage.RMXSTATUS_DURATION,
  RmxAudioStatusMessage.RMXSTATUS_BUFFERING,
  RmxAudioStatusMessage.RMXSTATUS_CANPLAY,
  RmxAudioStatusMessage.RMXSTATUS_LOADING,
  RmxAudioStatusMessage.RMXSTATUS_LOADED,
  RmxAudioStatusMessage.RMXSTATUS_PAUSE,
  RmxAudioStatusMessage.RMXSTATUS_COMPLETED,
  RmxAudioStatusMessage.RMXSTATUS_ERROR,
];

const message = 'CORDOVA RMXAUDIOPLAYER: Error storing message channel:';

class RmxAudioPlayer {
  static const MethodChannel _channel = const MethodChannel('flutter_plugin_playlist');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  AudioPlayerEventHandlers handlers = new AudioPlayerEventHandlers();
  AudioPlayerOptions options = new AudioPlayerOptions(
      verbose: false,
      resetStreamOnPause: false
  );

  bool _inititialized = false;
  Completer _initCompleter;

  /**
   * 'unknown' | 'ready' | 'error' | 'playing' | 'loading' | 'paused' | 'stopped'
   */
  String _currentState = 'unknown';
  bool _hasError = false;
  bool _hasLoaded = false;
  AudioTrack _currentItem = null;


  /**
   * The current summarized state of the player, as a string. It is preferred that you use the 'isX' accessors,
   * because they properly interpret the range of these values, but this field is exposed if you wish to observe
   * or interrogate it.
   */
  get currentState {
    return this._currentState;
  }

  /**
   * True if the plugin has been initialized. You'll likely never see this state; it is handled internally.
   */
  get isInitialized {
    return this._inititialized;
  }

  AudioTrack get currentTrack {
    return this._currentItem;
  }

  /**
   * If the playlist is stopped.
   */
  get isStopped {
    return this._currentState == 'stopped';
  }

  /**
   * If the playlist is currently playling a track.
   */
  get isPlaying {
    return this._currentState == 'playing';
  }

  /**
   * True if the playlist is currently paused
   */
  get isPaused {
    return this._currentState == 'paused' || this._currentState == 'stopped';
  }

  /**
   * True if the plugin is currently loading its *current* track.
   * On iOS, many tracks are loaded in parallel, so this only reports for the *current item*, e.g.
   * the item that will begin playback if you press pause.
   * If you need track-specific data, it is better to watch the onStatus stream and watch for RMXSTATUS_LOADING,
   * which will be raised independently & simultaneously for every track in the playlist.
   * On Android, tracks are only loaded as they begin playback, so this value and RMXSTATUS_LOADING should always
   * apply to the same track.
   */
  get isLoading {
    return this._currentState == 'loading';
  }

  /**
   * True if the plugin is seeking
   */
  get isSeeking {
    return this._currentState == 'seeking';
  }

  /**
   * True if the *currently playing track* has been loaded and can be played (this includes if it is *currently playing*).
   */
  get hasLoaded {
    return this._hasLoaded;
  }

  /**
   * True if the *current track* has reported an error. In almost all cases,
   * the playlist will automatically skip forward to the next track, in which case you will also receive
   * an RMXSTATUS_TRACK_CHANGED event.
   */
  get hasError {
    return this._hasError;
  }


  /**
   * Creates a new RmxAudioPlayer instance.
   */
  RmxAudioPlayer() {
    this.handlers = new AudioPlayerEventHandlers();
    _initCompleter = new Completer();
  }

  /**
   * Player interface
   */

  /**
   * Returns a promise that resolves when the plugin is ready.
   */
  Future<dynamic> ready() {
    return this._initCompleter.future;
  }

  Future<dynamic> _onNativeStatus(MethodCall call) {
    // better or worse, we got an answer back from native, so we resolve.
    if (call.method == 'status') {
      return this._onStatus(call.arguments['trackId'], call.arguments['msgType'], call.arguments['value']);
    } else {
      print('Unknown audio player onStatus message:' + call.method);
      return Future.value();
    }
  }

  Future<dynamic> _exec(String method, [dynamic args]) {
    return _channel.invokeMethod(method, args);
  }

  Future<bool> initialize() async {
    _channel.setMethodCallHandler((call) => _onNativeStatus(call));

    try {
      await _exec('initialize');

      this._inititialized = true;
      this._initCompleter.complete(true);

      return true;
    } catch (ex) {
      this._initCompleter.completeError(ex);

      throw ex;
    }
  }

  /**
   * Sets the player options. This can be called at any time and is not required before playback can be initiated.
   */
  Future<dynamic> setOptions(AudioPlayerOptions options) {
    this.options = new AudioPlayerOptions(
      verbose: options.verbose??this.options.verbose,
    );
    return _exec('setOptions', options.toJson());
  }

  /**
   * Playlist item management
   */

  /**
   * Sets the entire list of tracks to be played by the playlist.
   * This will clear all previous items from the playlist.
   * If you pass options.retainPosition = true, the current playback position will be
   * recorded and used when playback restarts. This can be used, for example, to set the
   * playlist to a new set of tracks, but retain the currently-playing item to avoid skipping.
   */
  Future<dynamic> setPlaylistItems(List<AudioTrack> items, {PlaylistItemOptions options}) {
    return _exec('setPlaylistItems', {
      'items': this._validateTracks(items),
      'options': options?.toJson()
    });
  }

  /**
   * Add a single track to the end of the playlist
   */
  Future<dynamic> addItem(AudioTrack trackItem) async {
    var validTrackItem = this._validateTrack(trackItem);
    if (!validTrackItem) { throw new Exception('Provided track is null or not an audio track'); }
    return await _exec('addItem', validTrackItem);
  }

  /**
   * Adds the list of tracks to the end of the playlist.
   */
  Future<dynamic> addAllItems(List<AudioTrack> items) {
    return _exec('addAllItems', this._validateTracks(items));
  }

  /**
   * Removes a track from the playlist. If this is the currently playing item, the next item will automatically begin playback.
   */
  Future<dynamic> removeItem(AudioTrackRemoval removeItem) async {
    if (removeItem == null) { throw new Exception('Track removal spec is empty'); }
    if (removeItem.trackId == null && removeItem.trackIndex == null) { throw new Exception('Track removal spec is invalid'); }
    return await _exec('removeItem', removeItem.toJson());
  }

  /**
   * Removes all given tracks from the playlist; these can be specified either by trackId or trackIndex. If the removed items
   * include the currently playing item, the next available item will automatically begin playing.
   */
  Future<dynamic> removeItems(List<AudioTrackRemoval> items) {
    return _exec('removeItems', items.map((item) => item.toJson()).toList());
  }

  /**
   * Clear the entire playlist. This will result in the STOPPED event being raised.
   */
  Future<dynamic> clearAllItems() {
    return _exec('clearAllItems');
  }

  /**
   * Playback management
   */

  /**
   * Begin playback. If no tracks have been added, this has no effect.
   */
  Future<dynamic> play() async {
    dynamic status = await _exec('play');

    if (status != null) {
      await _onStatus(currentTrack?.trackId, RmxAudioStatusMessage.RMXSTATUS_PLAYING, status);
    }

    return status;
  }

  /**
   * Play the track at the given index. If the track does not exist, this has no effect.
   */
  Future<dynamic> playTrackByIndex(num index, {num position}) {
    return _exec('playTrackByIndex', {
      'index': index,
      'position': position
    });
  }

  /**
   * Play the track matching the given trackId. If the track does not exist, this has no effect.
   */
  Future<dynamic> playTrackById(String trackId, {num position}) {
    return _exec('playTrackById', {
      'trackId':trackId,
      'position': position??0
    });
  }

  /**
   * Pause playback
   */
  Future<dynamic> pause() {
    return _exec('pause');
  }

  /**
   * Skip to the next track. If you are already at the end, and loop is false, this has no effect.
   * If you are at the end, and loop is true, playback will begin at the beginning of the playlist.
   */
  Future<dynamic> skipForward() {
    return _exec('skipForward');
  }

  /**
   * Skip to the previous track. If you are already at the beginning, this has no effect.
   */
  Future<dynamic> skipBack() {
    return _exec('skipBack');
  }

  /**
   * Seek to the given position in the currently playing track. If the value exceeds the track length,
   * the track will complete and playback of the next track will begin.
   */
  Future<dynamic> seekTo(num position) async {
    dynamic status = await _exec('seekTo', position);

    _onStatus(currentTrack?.trackId, RmxAudioStatusMessage.RMXSTATUS_SEEK, status);

    return status;
  }

  /**
   * (iOS only): Seek to the given position in the *entire queue of songs*.
   * Not implemented on Android since the Android player does not load track durations until the item
   * begins playback. On the TODO list to implement.
   */
  Future<dynamic> seekToQueuePosition(num position) {
    return _exec('seekToQueuePosition', position);
  }

  /**
   * Set the playback speed; a float value between [-1, 1] inclusive. If set to 0, this pauses playback.
   */
  Future<dynamic> setPlaybackRate(num rate) {
    return _exec('setPlaybackRate', rate);
  }

  /**
   * Set the playback volume. Float value between [0, 1] inclusive.
   * On both Android and iOS, this sets the volume of the media stream, which can be externally
   * controlled by setting the overall hardware volume.
   */
  Future<dynamic> setVolume(num volume) {
    return _exec('setPlaybackVolume', volume);
  }

  /**
   * Sets a flag indicating whether the playlist should loop back to the beginning once it reaches the end.
   */
  Future<dynamic> setLoop(bool loop) {
    return _exec('setLoopAll', loop);
  }

  /**
   * Get accessors
   */

  /**
   * Reports the current playback rate.
   */
  Future<dynamic> getPlaybackRate() {
    return _exec('getPlaybackRate');
  }

  /**
   * Reports the current playback volume
   */
  Future<dynamic> getVolume() {
    return _exec('getPlaybackVolume');
  }

  /**
   * Reports the playback position of the current item. You are recommended to handle the onStatus events
   * rather than this value, as this value will be stale by the time you receive it.
   */
  Future<dynamic> getPosition() {
    return _exec('getPlaybackPosition');
  }

  /**
   * Reports the buffer status of the current item. You are recommended to handle the onStatus events
   * rather than this value, as this value will be stale by the time you receive it.
   */
  Future<dynamic> getCurrentBuffer() {
    return _exec('getCurrentBuffer');
  }

  /**
   * (iOS only): Gets the overall playback position in the entire queue, in seconds (e.g. 1047 seconds).
   * Not implemented on Android since durations are not known ahead of time.
   */
  Future<dynamic> getQueuePosition() {
    return _exec('getQueuePosition');
  }

  /**
   * @internal
   * Call this function to emit an onStatus event via the on('status') handler.
   * Internal use only, to raise events received from the native interface.
   */
  Future<void> _onStatus(String trackId, int type, dynamic value) async {
    var status = new OnStatusCallbackData(
        type: type,
        trackId: trackId,
        value: value
    );
    if (this.options.verbose) {
      print('RmxAudioPlayer.onStatus: ${RmxAudioStatusMessageDescriptions[type]}($type) [$trackId]: $value');
    }

    if (status.type == RmxAudioStatusMessage.RMXSTATUS_TRACK_CHANGED) {
      this._hasError = false;
      this._hasLoaded = false;
      this._currentState = 'loading';
      this._currentItem = AudioTrack.fromJson(status.value['currentItem']);
    }

    // The plugin's status changes only in response to specific events.
    if (itemStatusChangeTypes.indexOf(status.type) >= 0) {
      // Only change the plugin's *current status* if the event being raised is for the current active track.
      if (this._currentItem != null && this._currentItem.trackId == trackId) {

        if (status.value != null && status.value['status'] != null) {
          this._currentState = status.value['status'];
        }

        if (status.type == RmxAudioStatusMessage.RMXSTATUS_CANPLAY) {
          this._hasLoaded = true;
        }

        if (status.type == RmxAudioStatusMessage.RMXSTATUS_ERROR) {
          this._hasError = true;
        }
      }
    }

    this._emit('status', status);
  }

  /**
   * Subscribe to events raised by the plugin, e.g. on('status', (data) => { ... }),
   * For now, only 'status' is supported.
   *
   * @param eventName Name of event to subscribe to.
   * @param callback The callback function to receive the event data
   */
  on(String eventName, AudioPlayerEventHandler callback) {
    if (this.handlers[eventName] == null) {
      this.handlers[eventName] = [];
    }

    this.handlers[eventName].add(callback);
  }

  /**
   * Remove an event handler from the plugin
   * @param eventName The name of the event whose subscription is to be removed
   * @param handle The event handler to destroy. Ensure that this is the SAME INSTANCE as the handler
   * that was passed in to create the subscription!
   */
  off(String eventName, AudioPlayerEventHandler handler) {
    if (this.handlers[eventName] != null) {
      this.handlers[eventName].remove(handler);
    }
  }

  /**
   * @internal
   * Raises an event via the corresponding event handler. Internal use only.
   * @param args Event args to pass through to the handler.
   */
  _emit(String eventName, dynamic args) {

    var handler = this.handlers[eventName];
    if (handler != null) {
      for (var i = 0; i < handler.length; i++) {
        var callback = this.handlers[eventName][i];
        if (callback != null) {
          callback(eventName, args: args);
        }
      }
    }

    return true;
  }

  /**
   * Validates the list of AudioTrack items to ensure they are valid.
   * Used internally but you can call this if you need to :)
   *
   * @param items The AudioTrack items to validate
   */
  _validateTracks(List<AudioTrack> items) {
    if (items == null || items.isEmpty) { return []; }
    return items.map(this._validateTrack)
        .where((x) => x != null).toList(); // may produce an empty array!
  }

  /**
   * Validate a single track and ensure it is valid for playback.
   * Used internally but you can call this if you need to :)
   *
   * @param track The AudioTrack to validate
   */
  _validateTrack(AudioTrack track) {
    if (track == null) { return null; }
    // For now we will rely on TS to do the heavy lifting, but we can add a validation here
    // that all the required fields are valid. For now we just take care of the unique ID.
    track.trackId = track.trackId??this._generateUUID();
    return track.toJson();
  }


  /**
   * Generate a v4 UUID for use as a unique trackId. Used internally, but you can use this to generate track ID's if you want.
   */
  _generateUUID() { // Doesn't need to be perfect or secure, just good enough to give each item an ID.
    int d = new DateTime.now().millisecondsSinceEpoch;
    const template = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
    var random = new math.Random(d);
    // There are better ways to do this in ES6, we are intentionally avoiding the import
    // of an ES6 polyfill here.
    return template.replaceAllMapped(new RegExp("x|y"), (c) {
      var r = (d + random.nextInt(1000) * 16) % 16 | 0;
      d = (d / 16).floor();
      return (c.group(0) == 'x' ? r : (r & 0x3 | 0x8)).toRadixString(16);
    });
  }

}
