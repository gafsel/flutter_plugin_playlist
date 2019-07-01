package org.gafs.flutter_plugin_playlist.data;

import com.devbrackets.android.playlistcore.annotation.SupportedMediaType;
import com.devbrackets.android.playlistcore.api.PlaylistItem;

import org.gafs.flutter_plugin_playlist.manager.PlaylistManager;

import java.util.HashMap;
import java.util.Map;

public class AudioTrack implements PlaylistItem {

    private final Map<?, ?> config;
    private float bufferPercentFloat = 0f;
    private int bufferPercent = 0;
    private long duration = 0;

    // In the iOS implementation, this returns nil if the data is bad.
    // We don't really have the option in Java; instead we will check the items afterwards
    // and just not add them to the list if they have bad data.
    public AudioTrack(Map<?, ?> config) {
        this.config = config;
    }

    public Map<?, ?> toDict() {
        Map<Object, Object> info = new HashMap<>();

        info.put("trackId", getTrackId());
        info.put("isStream", getIsStream());
        info.put("assetUrl", getMediaUrl());
        info.put("albumArt", getThumbnailUrl());
        info.put("artist", getArtist());
        info.put("album", getAlbum());
        info.put("title", getTitle());

        return info;
    }

    @Override
    public long getId() {
        // This is used by the underlying PlaylistManager to search for items by ID.
        // ListPlaylistManager.getPositionForItem uses item.id when PlaylistManager.setCurrentItem(id)
        // is called, basically finding the index of that ID.
        // Alternatively, simply use PlaylistManager.setCurrentPosition which uses index directly.
        // Probably easier in almost all cases.
        if (getTrackId() == null) { return 0; }
        return getTrackId().hashCode();
    }

    private <T> T getOption(String key, T defaultValue) {
        Object value = this.config.get(key);

        return value != null ? (T) value : defaultValue;
    }

    public boolean getIsStream() {
        return getOption("isStream", false);
    }

    public String getTrackId() {
        String trackId = getOption("trackId", "");
        if (trackId.equals("")) { return null; }
        return trackId;
    }

    @Override
    public boolean getDownloaded() {
        return false; // Would really like to set this to true once the cache has it...
    }

    @Override
    public String getDownloadedMediaUri() {
        return null; // ... at which point we can return a value here.
    }

    @Override
    @SupportedMediaType
    public int getMediaType() {
        return PlaylistManager.AUDIO;
    }

    @Override
    public String getMediaUrl() {
        return getOption("assetUrl", "");
    }

    @Override
    public String getThumbnailUrl() {
        String albumArt = getOption("albumArt", "");
        if (albumArt.equals("")) { return null; } // we should have a good default here.
        return albumArt;
    }

    @Override
    public String getArtworkUrl() {
        return getThumbnailUrl();
    }

    @Override
    public String getTitle() {
        return getOption("title", "");
    }

    @Override
    public String getAlbum() {
        return getOption("album", "");
    }

    @Override
    public String getArtist() {
        return getOption("artist", "");
    }

    // Since it seems ExoPlayer resets the buffering value when you seek,
    // we will set a max value we have seen for a particular item and not lower it.
    // We already know that the item is cached anyway, and testing proves it, there is no
    // playback delay the 2nd time around or when seeking back to positions you already played.

    public float getBufferPercentFloat() {
        return bufferPercentFloat;
    }

    public void setBufferPercentFloat(float buff) {
        // There is a bug in MediaProgress where if bufferPercent == 100 it sets bufferPercentFloat
        // to 100 instead of to 1.
        bufferPercentFloat = Math.min(Math.max(bufferPercentFloat, buff), 1f);
    }

    public int getBufferPercent() {
        return bufferPercent;
    }

    public void setBufferPercent(int buff) {
        bufferPercent = Math.max(bufferPercent, buff);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long dur) {
        duration = Math.max(duration, dur);
    }

}
