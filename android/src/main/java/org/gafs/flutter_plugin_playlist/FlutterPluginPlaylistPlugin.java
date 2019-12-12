package org.gafs.flutter_plugin_playlist;

import android.app.Activity;
import android.app.Application;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.devbrackets.android.playlistcore.data.MediaProgress;

import org.gafs.flutter_plugin_playlist.data.AudioTrack;
import org.gafs.flutter_plugin_playlist.manager.PlaylistManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.util.PathUtils;

/**
 *
 * The core Cordova interface for the audio player
 * TODO: Move the proxied calls audioPlayerImpl.getPlaylistManager()
 * into the audio player class itself so the plugin doesn't know about
 * the playlist manager.
 *
 */
public class FlutterPluginPlaylistPlugin implements MethodCallHandler, RmxConstants, OnStatusReportListener {
  public static String TAG = "RmxAudioPlayer";

  private MethodChannel channel;

  private RmxAudioPlayer audioPlayerImpl;

  private boolean resetStreamOnPause = true;

  private FlutterPluginPlaylistPlugin(MethodChannel channel) {
      this.channel = channel;
  }

  /** Plugin registration. */
  public static void registerWith(final Registrar registrar) {
    PlaylistManager.init(registrar.activity().getApplication());

    MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_plugin_playlist");

    // Plugin instance
    FlutterPluginPlaylistPlugin plugin = new FlutterPluginPlaylistPlugin(channel);

    // register the plugin as the method call handler
    channel.setMethodCallHandler(plugin);

    // create and link the audioPlayer with the plugin
    plugin.audioPlayerImpl = new RmxAudioPlayer(plugin);

    PlaylistManager.getInstance().registerProgressListener(plugin.audioPlayerImpl);
    PlaylistManager.getInstance().registerPlaylistListener(plugin.audioPlayerImpl);

    AudioTrack.setAssetResolver(new AudioTrack.AssetResolver() {
      @Override
      public String getAsset(String asset) {
        return Uri.parse("/android_asset/" + registrar.lookupKeyForAsset(asset)).toString();
      }
    });
  }

  @Override
  public void onMethodCall(final MethodCall call, final Result result) {
    String action = call.method;

    Log.i(TAG, "execute: " + action + ": ===> " + call.arguments);

    // capture callback
    if (INITIALIZE.equals(action)) {
      onStatus(RmxAudioStatusMessage.RMXSTATUS_REGISTER, "INIT", null);
      result.success(true);
    } else if (SET_OPTIONS.equals(action)) {
      Boolean resetStreamOnPause = call.argument("resetStreamOnPause");

      if (resetStreamOnPause == null) {
        resetStreamOnPause = this.resetStreamOnPause;
      }

      audioPlayerImpl.setResetStreamOnPause(resetStreamOnPause);
      // We don't do anything with these yet.
      result.success(call.arguments);
    } else if (RELEASE.equals(action)) {
      destroyResources();
      result.success(true);
    } else

    // Playlist management
    if (SET_PLAYLIST_ITEMS.equals(action)) {
      List<Map<?, ?>> items = (List<Map<?, ?>>) call.argument("items");
      Map<?, ?> optionsArgs = call.argument("options");
      PlaylistItemOptions options = new PlaylistItemOptions(optionsArgs);

      ArrayList<AudioTrack> trackItems = getTrackItems(items);
      audioPlayerImpl.getPlaylistManager().setAllItems(trackItems, options);

      for (AudioTrack playerItem : trackItems) {
        if (playerItem.getTrackId() != null) {
          onStatus(RmxAudioStatusMessage.RMXSTATUS_ITEM_ADDED, playerItem.getTrackId(), playerItem.toDict());
        }
      }

      result.success(true);
    } else if (ADD_PLAYLIST_ITEM.equals(action)) {
      Map<String, Object> args = (Map<String, Object>) call.arguments;
      Map<?, ?> item = (Map<?, ?>) args.get("item");
      AudioTrack playerItem = getTrackItem(item);

      Number index = (Number) args.get("index");

      if (playerItem.getTrackId() != null) {
        if (index != null && index.intValue() >= 0) {
          audioPlayerImpl.getPlaylistManager().insertItem(playerItem, index.intValue());
        } else {
          audioPlayerImpl.getPlaylistManager().addItem(playerItem); // makes its own check for null
        }

        onStatus(RmxAudioStatusMessage.RMXSTATUS_ITEM_ADDED, playerItem.getTrackId(), playerItem.toDict());
      }

      result.success(true);
    } else if (ADD_PLAYLIST_ITEMS.equals(action)) {
      Map<String, Object> args = (Map<String, Object>) call.arguments;
      List<Map<?, ?>> items = (List<Map<?, ?>>) args.get("items");
      ArrayList<AudioTrack> trackItems = getTrackItems(items);

      Number index = (Number) args.get("index");

      if (index != null && index.intValue() >= 0) {
        audioPlayerImpl.getPlaylistManager().insertAllItems(trackItems, index.intValue());
      } else {
        audioPlayerImpl.getPlaylistManager().addAllItems(trackItems);
      }

      for (AudioTrack playerItem : trackItems) {
        if (playerItem.getTrackId() != null) {
          onStatus(RmxAudioStatusMessage.RMXSTATUS_ITEM_ADDED, playerItem.getTrackId(), playerItem.toDict());
        }
      }

      result.success(true);
    } else if (REMOVE_PLAYLIST_ITEM.equals(action)) {
      Map<?, ?> removal = (Map<?, ?>) call.arguments;

      int trackIndex = option((Number) removal.get("trackIndex"), -1).intValue();
      String trackId = option((String) removal.get("trackId"), "");
      AudioTrack item = audioPlayerImpl.getPlaylistManager().removeItem(trackIndex, trackId);

      if (item != null) {
        onStatus(RmxAudioStatusMessage.RMXSTATUS_ITEM_REMOVED, item.getTrackId(), item.toDict());
        result.success(true);
      } else {
        result.error("RMXSTATUS_ITEM_REMOVED_FAIL", "RMXSTATUS_ITEM_REMOVED_FAIL", false);
      }
    } else if (REMOVE_PLAYLIST_ITEMS.equals(action)) {
      List<Map<?, ?>> items = (List<Map<?, ?>>) call.arguments;
      int removed = 0;

      if (items != null) {
        ArrayList<TrackRemovalItem> removals = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
          Map<?, ?> entry = items.get(index);
          if (entry == null) {
            continue;
          }
          int trackIndex = option((Number) entry.get("trackIndex"), -1).intValue();
          String trackId = option((String) entry.get("trackId"), "");
          removals.add(new TrackRemovalItem(trackIndex, trackId));

          ArrayList<AudioTrack> removedTracks = audioPlayerImpl.getPlaylistManager().removeAllItems(removals);

          if (removedTracks.size() > 0) {
            for (AudioTrack removedItem : removedTracks) {
              onStatus(RmxAudioStatusMessage.RMXSTATUS_ITEM_REMOVED, removedItem.getTrackId(), removedItem.toDict());
            }
            removed = removedTracks.size();
          }
        }
      }

      result.success(removed);
    } else if (CLEAR_PLAYLIST_ITEMS.equals(action)) {
      audioPlayerImpl.getPlaylistManager().clearItems();

      onStatus(RmxAudioStatusMessage.RMXSTATUS_PLAYLIST_CLEARED, "INVALID", null);
      result.success(true);
    } else

    // Playback
    if (PLAY.equals(action)) {
      if (audioPlayerImpl.getPlaylistManager().getPlaylistHandler() != null) {
        boolean isPlaying = audioPlayerImpl.getPlaylistManager().getPlaylistHandler().getCurrentMediaPlayer() != null
                && audioPlayerImpl.getPlaylistManager().getPlaylistHandler().getCurrentMediaPlayer().isPlaying();
        // There's a bug in the threaded repeater that it stacks up the repeat calls instead of ignoring
        // additional ones or starting a new one. E.g. every time this is called, you'd get a new repeat cycle,
        // meaning you get N updates per second. Ew.
        if (!isPlaying) {
          audioPlayerImpl.getPlaylistManager().getPlaylistHandler().play();
          //audioPlayerImpl.getPlaylistManager().getPlaylistHandler().seek(position);

          if (audioPlayerImpl.getPlaylistManager().getCurrentItem() != null) {
            onStatus(RmxAudioStatusMessage.RMXSTATUS_PLAYING,
                    audioPlayerImpl.getPlaylistManager().getCurrentItem().getTrackId(),
                    audioPlayerImpl.getPlayerStatus(audioPlayerImpl.getPlaylistManager().getCurrentItem()));
          }
        }

        if (audioPlayerImpl.getPlaylistManager().getCurrentItem() != null) {
          result.success(audioPlayerImpl.getPlayerStatus(audioPlayerImpl.getPlaylistManager().getCurrentItem()));
        } else {
          result.success(null);
        }

      } else if (audioPlayerImpl.getPlaylistManager().getItemCount() > 0){
        audioPlayerImpl.getPlaylistManager().setCurrentPosition(0);
        audioPlayerImpl.getPlaylistManager().beginPlayback(0, false);
      } else {
        result.error(PLAY, PLAY, null);
      }
    } else if (PLAY_BY_INDEX.equals(action)) {
      Map<String, Object> args = (Map<String, Object>) call.arguments;
      int index = option((Number) args.get("index"), audioPlayerImpl.getPlaylistManager().getCurrentPosition()).intValue();
      long seekPosition = (long)(option((Number) args.get("position"), 0).longValue() * 1000.0);

      audioPlayerImpl.getPlaylistManager().setCurrentPosition(index);
      audioPlayerImpl.getPlaylistManager().beginPlayback(seekPosition, false);
      result.success(true);
    } else if (PLAY_BY_ID.equals(action)) {
      Map<String, Object> args = (Map<String, Object>) call.arguments;
      String trackId = (String) args.get("trackId");
      if (!"".equals((trackId))) {
        // alternatively we could search for the item and set the current index to that item.
        int code = trackId.hashCode();
        long seekPosition = (long)(option((Number) args.get("position"), 0).longValue() * 1000.0);
        audioPlayerImpl.getPlaylistManager().setCurrentItem(code);
        audioPlayerImpl.getPlaylistManager().beginPlayback(seekPosition, false);
      }
      result.success(true);
    } else if (PAUSE.equals(action)) {
      // Hmmm.
      // audioPlayerImpl.getPlaylistManager().invokePausePlay();
      if (audioPlayerImpl.getPlaylistManager().getPlaylistHandler() != null) {
        audioPlayerImpl.getPlaylistManager().getPlaylistHandler().pause(true);
      }
      result.success(true);
    } else if (SKIP_FORWARD.equals(action)) {
      audioPlayerImpl.getPlaylistManager().invokeNext();
      result.success(true);
    } else if (SKIP_BACK.equals(action)) {
      audioPlayerImpl.getPlaylistManager().invokePrevious();
      result.success(true);
    } else

    // On Android, the duration, playback position, etc are in milliseconds as whole numbers.
    // On iOS, it uses seconds as floats, e.g. 63.3 seconds. So we need to convert here.

    if (SEEK.equals(action)) {
      long position = 0;
      MediaProgress progress = audioPlayerImpl.getPlaylistManager().getCurrentProgress();
      if (progress != null) {
        position = progress.getPosition();
      }
      long positionVal = (long) (option((Number) call.arguments, position / 1000.0f).floatValue() * 1000.0);

      if (audioPlayerImpl.getPlaylistManager().getPlaylistHandler() != null) { // isPlaying &&
        boolean isPlaying = audioPlayerImpl.getPlaylistManager().getPlaylistHandler().getCurrentMediaPlayer().isPlaying();
        audioPlayerImpl.getPlaylistManager().getPlaylistHandler().seek(positionVal);
        if (!isPlaying) {
          audioPlayerImpl.getPlaylistManager().getPlaylistHandler().pause(false);
        }
      }

      if (audioPlayerImpl.getPlaylistManager().getCurrentItem() != null) {
        result.success(audioPlayerImpl.getPlayerStatus(audioPlayerImpl.getPlaylistManager().getCurrentItem()));
      } else {
        result.success(null);
      }
    } else if (SEEK_TO_QUEUE_POSITION.equals(action)) {
      // Not supported at the moment
      result.success(true);
    } else if (SET_PLAYBACK_RATE.equals(action)) {
      float speed = option(((Number) call.arguments), audioPlayerImpl.getPlaylistManager().getPlaybackSpeed()).floatValue();
      audioPlayerImpl.getPlaylistManager().setPlaybackSpeed(speed);
      result.success(true);
    } else if (SET_PLAYBACK_VOLUME.equals(action)) {
      float volume = option(((Number) call.arguments), audioPlayerImpl.getVolume()).floatValue();
      audioPlayerImpl.setVolume(volume);
      result.success(true);
    } else if (SET_LOOP_ALL.equals(action)) {
      Boolean loop = option(((Boolean) call.arguments), audioPlayerImpl.getPlaylistManager().getLoop());
      audioPlayerImpl.getPlaylistManager().setLoop(loop);
      result.success(true);
    } else

    // Getters
    if (GET_PLAYBACK_RATE.equals(action)) {
      float speed = audioPlayerImpl.getPlaylistManager().getPlaybackSpeed();
      result.success(speed);
    } else if (GET_PLAYBACK_VOLUME.equals(action)) {
      result.success(audioPlayerImpl.getVolume());
    } else if (GET_PLAYBACK_POSITION.equals(action)) {
      long position = 0;
      MediaProgress progress = audioPlayerImpl.getPlaylistManager().getCurrentProgress();
      if (progress != null) {
        position = progress.getPosition();
      }
      result.success(position / 1000.0f);
    } else if (GET_BUFFER_STATUS.equals(action)) {
      result.success(audioPlayerImpl.getPlayerStatus(null));
    } else if (GET_QUEUE_POSITION.equals(action)) {
      // Not yet implemented on android. I'm not sure how to, since the tracks haven't loaded yet.
      // On iOS, the AVQueuePlayer gets the metadata for all tracks immediately, that's why that works there.
      float queuePosition = 0f;
      result.success(queuePosition);
    } else {
      result.notImplemented();
    }
  }

  private AudioTrack getTrackItem(Map<?, ?> item) {
    if (item != null) {
      AudioTrack track = new AudioTrack(item);
      if (track.getTrackId() != null) {
        return track;
      }
      return null;
    }
    return null;
  }

  private ArrayList<AudioTrack> getTrackItems(List<Map<?, ?>> items) {
    ArrayList<AudioTrack> trackItems = new ArrayList<>();
    if (items != null && items.size() > 0) {
      for (int index = 0; index < items.size(); index++) {
        Map<?, ?> obj = items.get(index);
        AudioTrack track = getTrackItem(obj);
        if (track == null) {
          continue;
        }
        trackItems.add(track);
      }
    }
    return trackItems;
  }

  private <T> T option(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  private void destroyResources() {
    audioPlayerImpl.getPlaylistManager().clearItems();
  }

  @Override
  public void onError(RmxAudioErrorType errorCode, String trackId, String message) {
    Map<String, Object> error = new HashMap<>();
    try {
      error.put("code", errorCode.toString());
      error.put("message", message != null ? message : "");
    } catch (Exception e) {
      Log.e(TAG, "Exception while raising onStatus: ", e);
    }

    onStatus(RmxAudioStatusMessage.RMXSTATUS_ERROR, trackId, error);
  }

  @Override
  public void onStatus(RmxAudioStatusMessage what, String trackId, Object param) {
    Map<String, Object> status = new HashMap<>();

    try {
      status.put("msgType", what.getValue()); // not .ordinal()
      status.put("trackId", trackId);
      status.put("value", param);
    } catch (Exception e) {
      Log.e(TAG, "Exception while raising onStatus: ", e);
    }

    Log.v(TAG, "statusChanged:" + status.toString());

    channel.invokeMethod(RmxConstants.STATUS, status);
  }
}
