package org.gafs.flutter_plugin_playlist.manager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.api.VideoViewApi;
import com.devbrackets.android.playlistcore.api.PlaylistItem;
import com.devbrackets.android.playlistcore.data.MediaProgress;
import com.devbrackets.android.playlistcore.manager.ListPlaylistManager;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.devbrackets.android.playlistcore.api.MediaPlayerApi;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.gafs.flutter_plugin_playlist.PlaylistItemOptions;
import org.gafs.flutter_plugin_playlist.TrackRemovalItem;
import org.gafs.flutter_plugin_playlist.data.AudioTrack;
import org.gafs.flutter_plugin_playlist.playlist.AudioApi;
import org.gafs.flutter_plugin_playlist.service.MediaService;

import okhttp3.OkHttpClient;

/**
 * A PlaylistManager that extends the {@link ListPlaylistManager} for use with the
 * {@link MediaService} which extends {@link com.devbrackets.android.playlistcore.service.BasePlaylistService}.
 */
public class PlaylistManager extends ListPlaylistManager<AudioTrack> implements OnErrorListener {

    private static PlaylistManager instance;

    private static final String TAG = "PlaylistManager";
    private List<AudioTrack> AudioTracks = new ArrayList<>();

    private boolean mediaServiceStarted = false;
    private float volumeLeft = 1.0f;
    private float volumeRight = 1.0f;
    private float playbackSpeed = 1.0f;
    private boolean loop = false;
    private boolean shouldStopPlaylist = false;
    private boolean previousInvoked = false;
    private boolean nextInvoked = false;
    private AudioTrack currentErrorTrack;

    // Really need a way to propagate the settings through the app
    private boolean resetStreamOnPause = true;

    private WeakReference<MediaControlsListener> mediaControlsListener = new WeakReference<>(null);
    private WeakReference<OnErrorListener> errorListener = new WeakReference<>(null);
    private WeakReference<MediaPlayerApi<AudioTrack>> currentMediaPlayer = new WeakReference<>(null);

    public static PlaylistManager getInstance() {
        return instance;
    }

    public static void init(final Application application) {
        if (instance != null) {
            instance.invokeStop();
            instance.clearItems();
        }

        instance = new PlaylistManager(application);

        // Registers the media sources to use the OkHttp client instead of the standard Apache one
        // Note: the OkHttpDataSourceFactory can be found in the ExoPlayer extension library `extension-okhttp`
        ExoMedia.setDataSourceFactoryProvider(new ExoMedia.DataSourceFactoryProvider() {
            @Override
            public DataSource.Factory provide(String userAgent, TransferListener listener) {
                // Updates the network data source to use the OKHttp implementation and allows it to follow redirects
                OkHttpClient httpClient = new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true).build();
                DataSource.Factory upstreamFactory = new OkHttpDataSourceFactory(httpClient, userAgent, listener);

                // Adds a cache around the upstreamFactory.
                // This sets a cache of 100MB, we might make this configurable.
                Cache cache = new SimpleCache(application.getCacheDir(), new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024));
                return new CacheDataSourceFactory(cache, upstreamFactory, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
            }
        });

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                instance.invokeStop();
                instance.clearItems();
            }
        });
    }

    private PlaylistManager(Application application) {
        super(application, MediaService.class);
        this.setParameters(AudioTracks, -1);
    }

    public void onMediaServiceInit(boolean hasInit) {
        mediaServiceStarted = hasInit; // this now implies that this.getPlaylistHandler() is not null.
    }

    public void onMediaPlayerChanged(MediaPlayerApi<AudioTrack> currentMediaPlayer) {
        if (this.currentMediaPlayer.get() != null) {
            this.currentMediaPlayer.clear();
            this.currentMediaPlayer = null;
        }
        this.currentMediaPlayer = new WeakReference<>(currentMediaPlayer);
        if (mediaServiceStarted) {
            setVolume(volumeLeft, volumeRight);
            setPlaybackSpeed(playbackSpeed); // dunno bout this one. probably should be independent.
        }
    }

    public void setOnErrorListener(OnErrorListener listener) {
        errorListener = new WeakReference<>(listener);
    }

    public void setMediaControlsListener(MediaControlsListener listener) {
        mediaControlsListener = new WeakReference<>(listener);
    }

    public boolean getResetStreamOnPause() {
        return resetStreamOnPause;
    }

    public void setResetStreamOnPause(boolean val) {
        resetStreamOnPause = val;
    }

    public AudioTrack getCurrentErrorTrack() {
        return currentErrorTrack;
    }

    public void setCurrentErrorTrack(PlaylistItem errorItem) {
        currentErrorTrack = (AudioTrack) errorItem;
    }

    public boolean isPlaying() {
        return getPlaylistHandler() != null && getPlaylistHandler().getCurrentMediaPlayer() != null && getPlaylistHandler().getCurrentMediaPlayer().isPlaying();
    }

    @Override
    public boolean onError(Exception e) {
        Log.i(TAG, "onError: " + e.toString());
        if (errorListener.get() != null) {
            errorListener.get().onError(e);
        }
        return true;
    }

    private boolean isShouldStopPlaylist() {
        return shouldStopPlaylist;
    }

    public void setShouldStopPlaylist(boolean shouldStopPlaylist) {
        this.shouldStopPlaylist = shouldStopPlaylist;
    }

    /*
     * isNextAvailable, getCurrentItem, and next() are overridden because there is
     * a glaring bug in playlist core where when an item completes, isNextAvailable and
     * getCurrentItem return wildly contradictory things, resulting in endless repeat
     * of the last item in the playlist.
     */

    @Override
    public boolean isNextAvailable() {
        boolean isAtEnd = getCurrentPosition() + 1 >= getItemCount();
        boolean isConstrained = getCurrentPosition() + 1 >= 0 && getCurrentPosition() + 1 < getItemCount();

        if (isAtEnd) {
            return loop;
        }
        return isConstrained;
    }

    @Override
    public AudioTrack getCurrentItem() {
        boolean isAtEnd = getCurrentPosition() + 1 == getItemCount();
        boolean isConstrained = getCurrentPosition() >= 0 && getCurrentPosition() < getItemCount();

        if (isAtEnd && isShouldStopPlaylist()) {
            return null;
        }
        if (isConstrained) {
            return getItem(getCurrentPosition());
        }
        return null;
    }

    @Override
    public AudioTrack previous() {
        setCurrentPosition(Math.max(0, getCurrentPosition() - 1));
        AudioTrack prevItem = getCurrentItem();

        if (!previousInvoked) { // this command came from the notification, not the user
            Log.i(TAG, "PlaylistManager.previous: invoked via service.");
            if (mediaControlsListener.get() != null) {
                mediaControlsListener.get().onPrevious(prevItem, getCurrentPosition());
            }
        }

        previousInvoked = false;
        return prevItem;
    }

    @Override
    public AudioTrack next() {
        if (isNextAvailable()) {
            setCurrentPosition(Math.min(getCurrentPosition() + 1, getItemCount()));
        } else {
            if (loop) {
                setCurrentPosition(BasePlaylistManager.INVALID_POSITION);
            } else {
                setShouldStopPlaylist(true);
                raiseAndCheckOnNext();
                return null;
            }
        }

        raiseAndCheckOnNext();
        return getCurrentItem();
    }

    private void raiseAndCheckOnNext() {
        AudioTrack nextItem = getCurrentItem();
        if (!nextInvoked) { // this command came from the notification, not the user
            Log.i(TAG, "PlaylistManager.next: invoked via service.");
            if (mediaControlsListener.get() != null) {
                mediaControlsListener.get().onNext(nextItem, getCurrentPosition());
            }
        }
        nextInvoked = false;
    }


    /*
     * List management
     */

    public void setAllItems(List<AudioTrack> items, PlaylistItemOptions options) {
        clearItems();
        addAllItems(items);
        setCurrentPosition(0);

        // If the options said to start from a specific position, do so.
        long seekStart = 0;
        if (options.getPlayFromPosition() > 0) {
            seekStart = options.getPlayFromPosition();
        } else if (options.getRetainPosition()) {
            MediaProgress progress = getCurrentProgress();
            if (progress != null) {
                seekStart = progress.getPosition();
            }
        }

        // If the options said to start from a specific id, do so.
        String idStart = null;
        if (options.getRetainPosition()) {
            if (options.getPlayFromId() != null) {
                idStart = options.getPlayFromId();
            }
        }
        if (idStart != null && !"".equals((idStart))) {
            int code = idStart.hashCode();
            setCurrentItem(code);
        }

        // We assume that if the playlist is fully loaded in one go,
        // that the next thing to happen will be to play. So let's start
        // paused, which will allow the player to pre-buffer until the
        // user says Go.
        beginPlayback(seekStart, options.getStartPaused());
    }

    public void addItem(AudioTrack item) {
        if (item == null) {
            return;
        }
        AudioTracks.add(item);
        setItems(AudioTracks);
    }

    public void insertItem(AudioTrack item, int index) {
        if (item == null) {
            return;
        }
        AudioTrack currentItem = getCurrentItem(); // may
        AudioTracks.add(index, item);
        setItems(AudioTracks);
        setCurrentPosition(AudioTracks.indexOf(currentItem));
    }

    public void addAllItems(List<AudioTrack> items) {
        AudioTrack currentItem = getCurrentItem(); // may be null
        AudioTracks.addAll(items);
        setItems(AudioTracks); // not *strictly* needed since they share the reference, but for good measure..
        setCurrentPosition(AudioTracks.indexOf(currentItem));
    }

    public void insertAllItems(List<AudioTrack> items, int index) {
        AudioTrack currentItem = getCurrentItem(); // may be null
        AudioTracks.addAll(index, items);
        setItems(AudioTracks); // not *strictly* needed since they share the reference, but for good measure..
        setCurrentPosition(AudioTracks.indexOf(currentItem));
    }

    public AudioTrack removeItem(int index, String itemId) {
        int currentPosition = getCurrentPosition();
        AudioTrack currentItem = getCurrentItem(); // may be null

        AudioTrack foundItem = null;
        int resolvedIndex = resolveItemPosition(index, itemId);
        if (resolvedIndex >= 0 && resolvedIndex < AudioTracks.size()) {
            foundItem = AudioTracks.get(resolvedIndex);
            AudioTracks.remove(resolvedIndex);

            setItems(AudioTracks);

            if (resolvedIndex <= currentPosition) {
                if (currentPosition == resolvedIndex) {
                    boolean wasPlaying = this.isPlaying();
                    if (this.getPlaylistHandler() != null) {
                        this.getPlaylistHandler().pause(true);
                    }

                    setCurrentPosition(AudioTracks.indexOf(currentItem));
                    this.beginPlayback(0, !wasPlaying);
                } else {
                    setCurrentPosition(AudioTracks.indexOf(currentItem));
                }
            }
        }

        return foundItem;
    }

    public ArrayList<AudioTrack> removeAllItems(ArrayList<TrackRemovalItem> items) {
        ArrayList<AudioTrack> removedTracks = new ArrayList<>();

        for (TrackRemovalItem item : items) {
            AudioTrack removed = removeItem(item.trackIndex, item.trackId);

            if (removed != null) {
                removedTracks.add(removed);
            }
        }

        return removedTracks;
    }

    public void clearItems() {
        if (this.getPlaylistHandler() != null) {
            this.getPlaylistHandler().stop();
        }
        AudioTracks.clear();
        setItems(AudioTracks);
        setCurrentPosition(BasePlaylistManager.INVALID_POSITION);
    }

    private int resolveItemPosition(int trackIndex, String trackId) {
        int resolvedPosition = -1;
        if (trackIndex >= 0 && trackIndex < AudioTracks.size()) {
            resolvedPosition = trackIndex;
        } else if (trackId != null && !"".equals(trackId)) {
            int itemPos = getPositionForItem(trackId.hashCode());
            if (itemPos != BasePlaylistManager.INVALID_POSITION) {
                resolvedPosition = itemPos;
            }
        }
        return resolvedPosition;
    }

    public boolean getLoop() {
        return loop;
    }

    public void setLoop(boolean newLoop) {
        loop = newLoop;
    }

    public float getVolumeLeft() {
        return volumeLeft;
    }

    public float getVolumeRight() {
        return volumeRight;
    }

    public void setVolume(float left, float right) {
        volumeLeft = left;
        volumeRight = right;

        if (currentMediaPlayer != null && currentMediaPlayer.get() != null) {
            Log.i("PlaylistManager", "setVolume completing with volume = " + left);
            currentMediaPlayer.get().setVolume(volumeLeft, volumeRight);
        }
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float speed) {
        playbackSpeed = speed;
        if (currentMediaPlayer != null && currentMediaPlayer.get() != null && currentMediaPlayer.get() instanceof AudioApi) {
            Log.i("PlaylistManager", "setPlaybackSpeed completing with speed = " + speed);
            ((AudioApi) currentMediaPlayer.get()).setPlaybackSpeed(playbackSpeed);
        }
    }

    public void beginPlayback(long seekPosition, boolean startPaused) {
        super.play(seekPosition, startPaused);
        try {
            setVolume(volumeLeft, volumeRight);
            setPlaybackSpeed(playbackSpeed);
        } catch (Exception e) {
            Log.w(TAG, "beginPlayback: Error setting volume or playback speed: " + e.getMessage());
        }
    }

    // If we wanted to implement a *native* player (like cordova-plugin-exoplayer),
    // we could do that here, following the example set by ExoMedia:
    // https://github.com/brianwernick/ExoMedia/blob/master/demo/src/main/java/com/devbrackets/android/exomediademo/manager/PlaylistManager.java
    // For this plugin's purposes (and in ExoMedia's audio demo) there is no need
    // to present audio controls, because that is done via the local notification and lock screen.

}
