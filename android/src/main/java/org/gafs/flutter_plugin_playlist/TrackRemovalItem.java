package org.gafs.flutter_plugin_playlist;

public class TrackRemovalItem {
    public int trackIndex = -1;
    public String trackId = "";

    TrackRemovalItem(int index, String id) {
        trackIndex = index;
        trackId = id;
    }
}

