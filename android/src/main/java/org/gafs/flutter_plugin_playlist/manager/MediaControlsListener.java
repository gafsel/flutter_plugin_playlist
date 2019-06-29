package org.gafs.flutter_plugin_playlist.manager;

import org.gafs.flutter_plugin_playlist.data.AudioTrack;

/*
* Interface to enable the PlaylistManager to send these events out.
* We could add more like play/pause/toggle/stop, but right now there
* are other ways to get all the other information.
*/
public interface MediaControlsListener {
  void onNext(AudioTrack currentItem, int currentIndex);
  void onPrevious(AudioTrack currentItem, int currentIndex);
}
