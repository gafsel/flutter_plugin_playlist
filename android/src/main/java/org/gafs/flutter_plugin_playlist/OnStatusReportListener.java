package org.gafs.flutter_plugin_playlist;

public interface OnStatusReportListener {
  void onError(RmxAudioErrorType errorCode, String trackId, String message);
  void onStatus(RmxAudioStatusMessage what, String trackId, Object param);
}
