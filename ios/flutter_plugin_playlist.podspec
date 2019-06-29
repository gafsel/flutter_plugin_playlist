#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_plugin_playlist'
  s.version          = '0.0.1'
  s.summary          = 'A Flutter plugin for Android and iOS with native support for audio playlists, background support, and lock screen controls'
  s.description      = <<-DESC
A Flutter plugin for Android and iOS with native support for audio playlists, background support, and lock screen controls
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Gabriel Silva' => 'gabrielfsilva.ti@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.frameworks = 'AVFoundation', 'MediaPlayer'

  s.ios.deployment_target = '8.0'
end

