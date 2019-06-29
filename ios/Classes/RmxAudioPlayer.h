//
// RmxAudioPlayer.h
// Music Controls Cordova Plugin
//
// Created by Juan Gonzalez on 12/16/16.
//

#ifndef RmxAudioPlayer_h
#define RmxAudioPlayer_h

#import <UIKit/UIKit.h>
#import <Flutter/Flutter.h>
#import <MediaPlayer/MediaPlayer.h>
#import <MediaPlayer/MPNowPlayingInfoCenter.h>
#import <MediaPlayer/MPMediaItem.h>
#import <AVFoundation/AVFoundation.h>

#import "Constants.h"
#import "AudioTrack.h"

typedef void (^RmxAudioPlayerEventListener)(NSString* result, id _Nullable args);

@interface RmxAudioPlayer: NSObject

// structural methods
- (void) pluginInitialize:(RmxAudioPlayerEventListener)statusListener;
- (void) setOptions:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) initialize:(FlutterMethodCall*)call result:(FlutterResult)result;

// public API

// Item management
- (void) setPlaylistItems:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) addItem:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) addAllItems:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) removeItem:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) removeItems:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) clearAllItems:(FlutterMethodCall*)call result:(FlutterResult)result;

// Playback management
- (void) play:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) playTrackByIndex:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) playTrackById:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) pause:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) skipForward:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) skipBack:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) seekTo:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) seekToQueuePosition:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) setPlaybackRate:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) setPlaybackVolume:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) setLoopAll:(FlutterMethodCall*)call result:(FlutterResult)result;

// Get accessors to manually update values. Note:
// these values are reported anyway via the onStatus event
// stream, you don't normally need to read these directly.
- (void) getPlaybackRate:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) getPlaybackVolume:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) getPlaybackPosition:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) getCurrentBuffer:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) getTotalDuration:(FlutterMethodCall*)call result:(FlutterResult)result;
- (void) getQueuePosition:(FlutterMethodCall*)call result:(FlutterResult)result;

// Cleanup
- (void) release:(FlutterMethodCall*)call result:(FlutterResult)result;

@end

#endif /* RmxAudioPlayer_h */
