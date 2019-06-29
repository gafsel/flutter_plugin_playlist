package org.gafs.flutter_plugin_playlist;

import java.util.HashMap;
import java.util.Map;

public class PlaylistItemOptions {
  private Map<?, ?> options;

  private boolean retainPosition = false;
  private long playFromPosition = -1L;
  private String playFromId = null;
  private boolean startPaused = true;

  PlaylistItemOptions(Map<?, ?> optionsObj) {
    this.options = optionsObj;
    if (this.options == null) {
      this.options = new HashMap<>();
    }

    this.retainPosition = getOption("retainPosition", false);
    this.startPaused = getOption("startPaused", true);
    this.playFromId = getOption("playFromId", null);

    try {
      playFromPosition = getOption("playFromPosition", 0L) * 1000L;
    } catch (Exception ex) {
      playFromPosition = -1L;
    }
  }

  PlaylistItemOptions(boolean retainPosition, long playFromPosition, boolean startPaused) {
    this.startPaused = startPaused;
    this.retainPosition = retainPosition;
    this.playFromPosition = playFromPosition;
  }

  private <T> T getOption(String key, T defaultValue) {
    T value = (T) options.get(key);
    return value != null ? value : defaultValue;
  }

  public boolean getStartPaused() {
    return startPaused;
  }

  public boolean getRetainPosition() {
    return retainPosition;
  }

  public long getPlayFromPosition() {
    return playFromPosition;
  }

  public String getPlayFromId() {
    return playFromId;
  }
}
