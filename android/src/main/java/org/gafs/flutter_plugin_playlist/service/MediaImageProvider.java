package org.gafs.flutter_plugin_playlist.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.devbrackets.android.playlistcore.components.image.ImageProvider;

// Because we are codegen'ing this to depend on the actual cordova app,
// we can use R directly. Otherwise, we'd have to use the cordova activity,
// but that would be a bit odd since this belongs to a service running
// outside that activity. I'm not sure if that would work.
// import __PACKAGE_NAME__.R;
import org.gafs.flutter_plugin_playlist.FakeR;
import org.gafs.flutter_plugin_playlist.data.AudioTrack;


public class MediaImageProvider implements ImageProvider<AudioTrack> {
    interface OnImageUpdatedListener {
        void onImageUpdated();
    }

    private RequestManager glide;
    private OnImageUpdatedListener listener;

    private FakeR fakeR;

    private NotificationImageTarget notificationImageTarget = new NotificationImageTarget();
    private RemoteViewImageTarget remoteViewImageTarget = new RemoteViewImageTarget();

    private Bitmap defaultNotificationImage;
    private Bitmap defaultArtworkImage;

    private Bitmap notificationImage;
    private Bitmap artworkImage;

    private int notificationIconId = 0;

    MediaImageProvider(Context context, OnImageUpdatedListener listener) {
        glide = Glide.with(context.getApplicationContext());
        fakeR = new FakeR(context.getApplicationContext());
        this.listener = listener;

        // R.drawable.img_playlist_notif_default
        // R.drawable.img_playlist_artwork_default
        defaultNotificationImage = BitmapFactory.decodeResource(context.getResources(), fakeR.getId("drawable", "img_playlist_notif_default"));
        defaultArtworkImage = BitmapFactory.decodeResource(context.getResources(), fakeR.getId("drawable", "img_playlist_artwork_default"));
    }

    @Override
    public int getNotificationIconRes() {
        return getMipmapIcon();
    }

    @Override
    public int getRemoteViewIconRes() {
        return getMipmapIcon();
    }

    @Override
    public Bitmap getLargeNotificationImage() {
        return notificationImage != null ? notificationImage : defaultNotificationImage;
    }

    @Override
    public Bitmap getRemoteViewArtwork() {
        return artworkImage != null ? artworkImage : defaultArtworkImage;
    }

    @Override
    public void updateImages(AudioTrack playlistItem) {
        glide.asBitmap().load(playlistItem.getThumbnailUrl()).into(notificationImageTarget);
        glide.asBitmap().load(playlistItem.getArtworkUrl()).into(remoteViewImageTarget);
    }

    private int getMipmapIcon() {
        // return R.mipmap.icon; // this comes from cordova itself.
        if (notificationIconId <= 0) {
            notificationIconId = fakeR.getId("mipmap", "icon");
            // API 28 moves the reference to this.
            if (notificationIconId <= 0) {
                notificationIconId = fakeR.getId("mipmap", "ic_launcher");
            }
        }
        return notificationIconId;
    }

    /**
     * A class used to listen to the loading of the large notification images and perform
     * the correct functionality to update the notification once it is loaded.
     * <p>
     * <b>NOTE:</b> This is a Glide Image loader class
     */
    private class NotificationImageTarget extends SimpleTarget<Bitmap> {
        @Override
        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
            notificationImage = resource;
            listener.onImageUpdated();
        }
    }

    /**
     * A class used to listen to the loading of the large lock screen images and perform
     * the correct functionality to update the artwork once it is loaded.
     * <p>
     * <b>NOTE:</b> This is a Glide Image loader class
     */
    private class RemoteViewImageTarget extends SimpleTarget<Bitmap> {
        @Override
        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
            artworkImage = resource;
            listener.onImageUpdated();
        }
    }
}
