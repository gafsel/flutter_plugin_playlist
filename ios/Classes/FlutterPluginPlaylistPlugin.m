#import "FlutterPluginPlaylistPlugin.h"

@interface FlutterPluginPlaylistPlugin() {
    
}

@property () RmxAudioPlayer* audioPlayer;

@end

@implementation FlutterPluginPlaylistPlugin

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"flutter_plugin_playlist"
            binaryMessenger:[registrar messenger]];
    FlutterPluginPlaylistPlugin* instance = [[FlutterPluginPlaylistPlugin alloc] init];
    
  [registrar addMethodCallDelegate:instance channel:channel];
    
  [instance initialize: channel];
    
    AssetResolver assetResolver = ^NSURL* (NSString* assetUrl){
        NSString* key = [registrar lookupKeyForAsset: assetUrl];
        return [[NSBundle mainBundle] URLForResource:key withExtension:nil];
    };
    
    [AudioTrack setAssetResolver:assetResolver];
}

- (void)initialize:(FlutterMethodChannel*) channel {
    self.audioPlayer = [[RmxAudioPlayer alloc] init];
    
    RmxAudioPlayerEventListener listener = ^(NSString* eventName, id _Nullable args){
        [channel invokeMethod:eventName arguments:args];
    };
    
    [self.audioPlayer pluginInitialize: listener];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"setOptions" isEqualToString:call.method]) {
      [self.audioPlayer setOptions:call result:result];
  } else if ([@"initialize" isEqualToString:call.method]) {
      [self.audioPlayer initialize:call result:result];
  } else if ([@"setPlaylistItems" isEqualToString:call.method]) {
      [self.audioPlayer setPlaylistItems:call result:result];
  } else if ([@"addItem" isEqualToString:call.method]) {
      [self.audioPlayer addItem:call result:result];
  } else if ([@"addAllItems" isEqualToString:call.method]) {
      [self.audioPlayer addAllItems:call result:result];
  } else if ([@"removeItem" isEqualToString:call.method]) {
      [self.audioPlayer removeItem:call result:result];
  } else if ([@"removeItems" isEqualToString:call.method]) {
      [self.audioPlayer removeItems:call result:result];
  } else if ([@"clearAllItems" isEqualToString:call.method]) {
      [self.audioPlayer clearAllItems:call result:result];
  } else if ([@"play" isEqualToString:call.method]) {
      [self.audioPlayer play:call result:result];
  } else if ([@"playTrackByIndex" isEqualToString:call.method]) {
      [self.audioPlayer playTrackByIndex:call result:result];
  } else if ([@"playTrackById" isEqualToString:call.method]) {
      [self.audioPlayer playTrackById:call result:result];
  } else if ([@"pause" isEqualToString:call.method]) {
      [self.audioPlayer pause:call result:result];
  } else if ([@"skipForward" isEqualToString:call.method]) {
      [self.audioPlayer skipForward:call result:result];
  } else if ([@"skipBack" isEqualToString:call.method]) {
      [self.audioPlayer skipBack:call result:result];
  } else if ([@"seekTo" isEqualToString:call.method]) {
      [self.audioPlayer seekTo:call result:result];
  } else if ([@"seekToQueuePosition" isEqualToString:call.method]) {
      [self.audioPlayer seekToQueuePosition:call result:result];
  } else if ([@"setPlaybackRate" isEqualToString:call.method]) {
      [self.audioPlayer setPlaybackRate:call result:result];
  } else if ([@"setPlaybackVolume" isEqualToString:call.method]) {
      [self.audioPlayer setPlaybackVolume:call result:result];
  } else if ([@"setLoopAll" isEqualToString:call.method]) {
      [self.audioPlayer setLoopAll:call result:result];
  } else if ([@"getPlaybackRate" isEqualToString:call.method]) {
      [self.audioPlayer getPlaybackRate:call result:result];
  } else if ([@"getPlaybackVolume" isEqualToString:call.method]) {
      [self.audioPlayer getPlaybackVolume:call result:result];
  } else if ([@"getPlaybackPosition" isEqualToString:call.method]) {
      [self.audioPlayer getPlaybackPosition:call result:result];
  } else if ([@"getCurrentBuffer" isEqualToString:call.method]) {
      [self.audioPlayer getCurrentBuffer:call result:result];
  } else if ([@"getTotalDuration" isEqualToString:call.method]) {
      [self.audioPlayer getTotalDuration:call result:result];
  } else if ([@"getQueuePosition" isEqualToString:call.method]) {
      [self.audioPlayer getQueuePosition:call result:result];
  } else if ([@"release" isEqualToString:call.method]) {
      [self.audioPlayer release:call result:result];
  } else {
    result(FlutterMethodNotImplemented);
  }
}

@end
