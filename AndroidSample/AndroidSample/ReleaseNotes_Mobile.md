### PortSIP VoIP SDK v19.6.1
**Release Date:** Dec 5, 2025

1. setSrtpPolicy API add enableGcmCryptoSuites to support SRTP AEAD (Authenticated Encryption with Associated Data) 128/256, per RFC 7714.
  When enable_gcm_crypto_suites is enabled, prefer the following order: kSrtpAeadAes256Gcm > kSrtpAeadAes128Gcm > kSrtpAes128CmSha1_80 > kSrtpAes128CmSha1_32.
2. When the video hasn't updated for 1 second, turn the video black.
3. Fixed play G711 wav file creash bug.
4. Avoid multiple instances of the SDK and duplicate logs.
5. **Android:** Utilize AAR files instead of JAR files for the Android SDK.
6. **iOS & macOS:** sendVideoStreamToRemote sendPcmStreamToRemote API data parameter NSData to  const unsigned char*
7. **Windows:** Video render added MAKEFOURCC('N', 'V', '1', '2') support.
   
---

### PortSIP VoIP SDK v19.6.0
**Release Date:** Sep 5, 2025

1. Reimplemented refreshRegistration, will disconnect the current transport and create a new transport to the server.
2. Fixed an issue where calls could not continue after switching networks during TCP transport.
3. Added support for noSDP Call with 183 early media.
4. **iOS:** Added new API `configureAudioSession`. Configures the audio session for proper audio management when using CallKit, to be called in `performAnswerCallAction` and `performStartCallAction` methods.
5. **Android:** Rebuilt to support 16KB page size.
6. **Android:** The minimum supported version of the Android SDK is API 21

---

### PortSIP VoIP SDK v19.5.1
**Release Date:** May 8, 2025

1. When the network uses IPv6 to register with the PBX, if the SDP contains an IPv4 address, map this IPv4 address to IPv6.
2. To maintain compatibility with only the `ssrc` line "a=ssrc:<ssrc-id>", ignore these `ssrc` lines.

---

### PortSIP VoIP SDK v19.5.0
**Release Date:** April 23, 2025

1. **IPv6 Compatibility:** In IPv6-only networks, SDP containing IPv4 addresses will now automatically map to IPv6 addresses.
2. **Conference Controls:** The `muteSession` method now supports muting the host's audio/video in conferences by setting `sessionId` to `CONFERENCE_SESSION_ID`.
3. **API Initialization:** In the `initialize` API, the `DNSServer` parameter now allows specifying a primary DNS server while preserving the system's existing DNS configurations.
4. **Bug Fix:** Resolved an issue with packet handling in AMR and AMR-WB bandwidth-efficient modes.
5. **Bug Fix:** Addressed a bug where video bitrate settings would not take effect.

---

### PortSIP VoIP SDK v19.4.7
**Release Date:** February 28, 2025

1. Support for SRTP with key lifetime (e.g., |2^31).
2. Re-implemented sending RTP keepalive packets.

---

### PortSIP VoIP SDK v19.4.5
**Release Date:** January 18, 2025

1. Fixed issue where audio could not be heard after transferring a call.

---

### PortSIP VoIP SDK v19.4.4
**Release Date:** December 12, 2024

1. Allows modification of RTP callback data.
2. Supports setting local video resolution (default is 720P).
3. Android: Sample build with gradle 8.7.3, Java 11, Android Studio 2024.2.1.
   
---

### PortSIP VoIP SDK v19.4.3
**Release Date:** November 22, 2024

1. When a call switches from video to audio, the direction of the video section is changed to INACTIVE in the SDP, and the video port is also changed to 0.
2. The maximum allowed length for the path of the certificate file is now 512 characters.

---

### PortSIP VoIP SDK v19.4.2
**Release Date:** August 23, 2024

1. When the direction in the SDP is INACTIVE (both sides hold), if the local call is unheld, the direction changes to sendrecv (previously, it was recvonly).
2. Fixed the issue of recording failure when only capturing received audio.

---

### PortSIP VoIP SDK v19.4.1
**Release Date:** July 3, 2024

1. SDP O line now uses the real IP.
2. Fixed the issue of video recording with audio and video being out of sync.
3. Modified the `muteSession` implementation to continue sending RTP data after muting the microphone.

---

### PortSIP VoIP SDK v19.4.0
**Release Date:** May 31, 2024

1. Added `enablePriorityIPv6Domain` API, allowing specification of the preferred protocol when a domain supports both IPv4 and IPv6 simultaneously, with the default priority being IPv4.
2. Enforced the inclusion of quotes around the DisplayName. The previous version of the SDK would add quotes when necessary.
3. Added `setUriUserEncoding` API, modifying the default URI user character that needs to be escaped.
4. Supported SIP messages with a maximum length of 16K; the previous version supported 8K.

---

### PortSIP VoIP SDK v19.3.2
**Release Date:** February 29, 2024

1. The function `startPlayingFileToRemote` will stop automatically after the file has finished playing, eliminating the need to call `stopPlayingFileToRemote` within the `onPlayFileFinished` event.
2. Fixed the crash caused by multiple calls using different audio sampling rates simultaneously.
3. Android:Supports Android 14, adds Bluetooth permissions.

---

### PortSIP VoIP SDK v19.3.1
**Release Date:** January 18, 2024

1. If the server domain supports IPv6 and IPv4, IPv4 will be used first.
2. Android: Added onAudioFocusChange event, fired when the audio focus has been changed.

---

### PortSIP VoIP SDK v19.2.2
**Release Date:** November 30, 2023

1. Fixed IPv6 bug.
2. Supported SIP Notify message "duplicate-im-info" callback from `onRecvNotifyOfSubscription` event.
3. **iOS:** `startAudio` & `stopAudio` added `audioSession` parameter.

---

### PortSIP VoIP SDK v19.1.3
**Release Date:** October 18, 2023

1. Fixed the issue of holding/updating calls without SDP.

---

### PortSIP VoIP SDK v19.1
**Release Date:** September 25, 2023

1. Upgraded OpenSSL to 1.1.1v.
2. Upgraded c-ares to 1.19.1.
3. **iOS:** Supported Xcode 15.
4. **iOS:** Use XCFramework instead of Framework.
5. **iOS:** Added support for arm64 simulator.

---

### PortSIP VoIP SDK v19.0
**Release Date:** July 21, 2023

1. Media Engine upgrade: Better performance, improved sound quality, and better compatibility with network packet loss.
2. SIP Protocol stack upgrade.
3. Upgraded OpenSSL to 1.1.1s.
4. Audio recording supports MP3/AMR.
5. Video recording supports MP4.
6. Support for playing audio MP3/MP4 files or RTSP/RTMP streams to the other party. Replaced APIs `playVideoFileToRemote`, `stopPlayVideoFileToRemote`, `playAudioFileToRemote`, and `stopPlayAudioFileToRemote` with `startPlayingFileToRemote` and `stopPlayingFileToRemote`. Replaced events `onPlayAudioFileFinished` and `onPlayVideoFileFinished` with `onPlayFileFinished`.
7. Video conference now supports more layouts. The `createVideoConference` method has removed `displayLocalVideoInConference` and added `confLayout`, supporting layout types 0-7.
8. `unRegisterServer` now includes `waitMS` parameter; 0 means do not wait for unregister success.
9. Added `startPlayingFileLocally` and `stopPlayingFileLocally` APIs.
10. Allowed getting version without initializing SDK; returns a version string.
11. The incoming call cannot be accepted (e.g., due to audio codec mismatch, SRTP mismatch), triggering the `onInviteFailure` event with `sessionId` set to -1. Added parameters `callerDisplayName`, `caller`, `calleeDisplayName`, and `callee` to `onInviteFailure`.
12. Removed APIs `getAudioStatistics` and `getVideoStatistics`, replaced by `getStatistics` API and `onStatistics` event.
13. **iOS:** Event parameter `char*` changed to `NSString*`.
14. **iOS:** Removed API `refreshCall`. If the network changes and registration is successful, and there is a current call, it will automatically call REINVITE.
15. **Android:** Removed `CreateCallManager` and `DeleteCallManager` APIs; context parameters are passed in the SDK constructor.
16. **Android:** Added `unInitialize` API.

---

### PortSIP VoIP SDK v18.7
**Release Date:** March 21, 2023

1. Changed API `enableReliableProvisional` to `setReliableProvisional`, allowing setting to PRACK Supported or Required.
   - `enableReliableProvisional(true)` ==> `setReliableProvisional(2)`
   - `enableReliableProvisional(false)` ==> `setReliableProvisional(0)`
2. Supported remote unhold call to change SDP.
3. Supported iLBC mode=20.
4. Supported disabling SIP KeepAlive.

---

### PortSIP VoIP SDK v18.6
**Release Date:** December 12, 2022

1. Upgraded OpenSSL to 1.1.1s.
2. Upgraded c-ares to 1.8.1.
3. **iOS:** Supported Xcode 14 & iOS 16.
4. **iOS:** `IPHONEOS_DEPLOYMENT_TARGET` changed from 9 to 11.
5. **iOS:** Armv7 is no longer supported.

---

### PortSIP VoIP SDK v18.5
**Release Date:** June 10, 2022

1. Optimized the audio conference mixing sound quality.
2. When `updateCall` disables video, the video RTP port uses 0.
3. Supported park-info.
4. Added `refreshCall` API to update the address of the communication.
5. Fixed `updateCall` bug when using SRTP.

---

### PortSIP VoIP SDK v18.4
**Release Date:** April 26, 2022

1. SIP Protocol stack upgrade.
2. Upgraded OpenSSL to 1.1.1n.
3. Supported changing audio devices during calls.
4. Added `refreshCal` function.
5. Fixed subject session ID <0 error.
6. Fixed dialog info version issue.
7. Fixed KeepAlive bug.
8. Fixed TCP/TLS disconnection from the network causing uninitialization crash bug.

---

### PortSIP VoIP SDK v18.3
**Release Date:** February 17, 2022

1. Allowed getting version without initializing SDK.
2. `SendVideoStream` no longer scales video.
3. P2P Call supports setting public IP (e.g., `localIp = "publicip:8.8.8.8"`).
4. Fixed `PlayVideoFileToRemote` mute audio bug.
5. Fixed video code empty check error.
6. Fixed an issue where enabling `rport` would remove `transport=tls` from the refresh registration contact header.
7. Android: Fixed bluetooth permission on android 12.
   
---

### PortSIP VoIP SDK v18.2
**Release Date:** November 11, 2021

1. Added parameters for callback events `onSendMessageSuccess`, `onSendMessageFailure`, `onSendOutOfDialogMessageSuccess`, and `onSendOutOfDialogMessageFailure` to include `sipMessage`.
2. Limited WAV file recording max size to 2GB.
3. Added `reg-id` to the SIP contact header.
4. Fixed SRTP tag bug.
5. Fixed the issue of not refreshing registration.

---

### PortSIP VoIP SDK v18.1
**Release Date:** May 21, 2021

1. Improved compatibility of H264 video decoder.
2. Fixed the delay problem after video HOLD/UNHOLD.
3. Added `setVideoOrientation` API.

---

### PortSIP VoIP SDK v18
**Release Date:** March 15, 2021

1. SIP Protocol stack upgrade.
2. Added screen sharing support (iOS, Android can only accept screen sharing).
3. `VIDEOSTREAM_CALLBACK_MODE` and `AUDIOSTREAM_CALLBACK_MODE` changed to `DIRECTION_MODE`.
4. API `setRtpCallback` renamed to `enableRtpCallback`, allowing callbacks to specify the session, direction (send/recv), and media type (audio/video/screen).
5. RTP Callback events `onSendingRTPPacket` and `onReceivedRTPPacket` renamed to `onRTPPacketCallback`.
6. `onInviteUpdated` changed:
   - Added screen parameters.
   - If the received SDP does not change, `onInviteUpdated` will not be triggered.
   - `UpdateCall` API success will trigger `onInviteUpdated`.
7. Changed `setRtpPortRange` parameters, removed `setRtcpPortRange` API.
8. New parameter `sipMessage` added for callback events `onInviteClosed`.

---

### PortSIP VoIP SDK v17.2
**Release Date:** December 5, 2020

1. New parameter `dnsServer` added to the `initialize` function to specify the DNS Server.
2. Removed `setLocalVideoWindow` API.
3. New parameter `localVideoWindow` added to `displayLocalVideo`, replacing `setLocalVideoWindow` API.
4. **iOS:** Fixed video conference bug.

---

### PortSIP VoIP SDK v17.1
**Release Date:** October 21, 2020

1. Upgraded WebRTC to 4214.
2. Added `dialog` to `AllowedEvent`.
3. Supported audio stereo recording.
4. Fixed NACK bug.
5. Fixed a bug where the audio file loop parameter was always true.
6. Fixed RTP KeepAlive bug.

---

### PortSIP VoIP SDK v17
**Release Date:** September 24, 2020

1. Audio & video engine upgrade.
2. SIP Protocol stack upgrade.
3. Upgraded OpenSSL to 1.1.1d.
4. Compatible with Xcode 12 & iOS 14.
5. Fixed various minor bugs.

---

### PortSIP VoIP SDK v16.6
**Release Date:** November 18, 2019

1. Fixed a memory leak defect.
2. **iOS:** SIPSample supports iOS 13 VoIP Service Call PUSH and APNs IM PUSH with PortPBX 12.
3. **iOS:** SIPSample supports iOS Dark UI.

---

### PortSIP VoIP SDK v16.5
**Release Date:** July 25, 2019

1. Added `setChannelInputVolumeScaling` API.
2. Fixed various minor bugs.

---

### PortSIP VoIP SDK v16.4
**Release Date:** April 1, 2019

1. Added `setVideoCropAndScale`. When the camera does not support the specified resolution, it allows the SDK to crop and scale to the specified resolution.
2. Local video render mirror.
3. `sendDTMF` supports `playDTMFTone`.
4. Supported H.264 High Profile.
5. `enableSessionTimer` added 2 modes.
6. `setPresenceStatus` supports "unpublished".

---

### PortSIP VoIP SDK v16.3
**Release Date:** September 26, 2018

1. Renamed `enableCallbackSendingSignaling` to `enableCallbackSignaling`, allowing enable/disable of sent or received SIP messages.
2. Fixed various minor bugs.
3. **iOS:** Fixed Bluetooth bug.

---

### PortSIP VoIP SDK v16.2
**Release Date:** May 23, 2018

1. PERS rewritten for the following purposes:
   - Provide the Session Border Controller feature.
   - Provide the VoIP Anti-Blocking feature for UAE.
   - Added support for TCP Tunnel.
2. Added the `enableRport` API to allow enabling/disabling `rport`, default is enabled.
3. Fixed various minor bugs.

---

### PortSIP VoIP SDK v16.1
**Release Date:** March 6, 2018

1. Added `enableEarlyMedia` function.
2. Removed `muteMicrophone` and `muteSpeaker` functions.
3. Renamed `setAudioQos` to `enableAudioQos`.
4. Renamed `setVideoQos` to `enableVideoQos`.

---

### PortSIP VoIP SDK v16
**Release Date:** February 10, 2018

1. Added video codec VP9 support.
2. OPUS supports 2 channels and InbandFEC (enabled by default).
3. Stability improvements for audio & video capturing and playback.
4. Removed Audio Statistics APIs:
   - `getDynamicVolumeLevel`
   - `getAudioRtcpStatistics`
   - `getAudioRtpStatistics`
   - `getNetworkStatistics`
   - Merged with the new API `getAudioStatistics`.
5. Removed Video Statistics APIs:
   - `enableVideoDecoderCallback`
   - `getVideoRtpStatistics`
   - Callback event: `onVideoDecoderCallback`.
   - Merged with the new API `getVideoStatistics`.
6. Removed `setVideoOrientation`; video orientation is auto-adjusted by SDK.
7. Parameter type `enableAudioStreamCallback` changed; removed types:
   - `AUDIOSTREAM_LOCAL_MIX`
   - `AUDIOSTREAM_REMOTE_MIX`
   - Added new type: `AUDIOSTREAM_BOTH`.
8. Added function `audioPlayLoopbackTest` for testing the availability of audio devices.
9. **iOS:** Added function `enableCallKit` for use by `callManager`.
10. **iOS:** Support for Hardware H.264 codec, providing 80% performance improvement; supports 720P on iPhone 5 and above.
11. **iOS:** Support for METAL rendering of video (only available for iOS 9 and above).
12. **iOS:** Removed H.263 and H.263+ video codecs.
13. **iOS:** Removed APIs `enableAEC`, `enableAGC`, and `enableANS` (implemented by the hardware of the iOS device).

---

### PortSIP VoIP SDK v15.2
**Release Date:** November 27, 2017

1. New parameter `sipMessage` added for callback events `onRecvOutOfDialogMessage`.
2. **Android:** SIPSample added Google FCM support.
3. **iOS:** SIPSample added VoIP PUSH support.

---

### PortSIP VoIP SDK v15.1
**Release Date:** August 1, 2017

1. New parameters `"TLSCertificatesRootPath"`, `"TLSCipherList"`, and `"verifyTLSCertificate"` added to the `initialize` function to specify TLS certificate verification.
2. Renamed `getExtensionHeaderValue` to `getSipMessageHeaderValue`.
3. Replaced `addExtensionHeader` with `addSipMessageHeader`.
4. Replaced `clearAddExtensionHeaders` with `clearAddedSipMessageHeaders`.
5. Replaced `modifyHeaderValue` with `modifySipMessageHeader`.
6. Replaced `clearModifyHeaders` with `clearModifiedSipMessageHeaders`.
7. Added `removeAddedSipMessageHeader` function.
8. Added `removeModifiedSipMessageHeader` function.
9. New parameter `"sipMessage"` added for callback events `onRegisterSuccess`, `onRegisterFailure`, and `onInviteFailure`, allowing the retrieval of the specified SIP header value from `"sipMessage"`.
10. New callback event `onRecvNotifyOfSubscription`. This event will be triggered when receiving a NOTIFY message of the subscription.
11. New callback event `onSubscriptionFailure`. This event will be triggered when sending SUBSCRIBE fails.
12. New callback event `onSubscriptionTerminated`. This event will be triggered when a subscription is terminated or expires.

---

### PortSIP VoIP SDK v15.0
**Release Date:** June 2, 2017

This is a major upgrade. We recommend updating.

1. Support for CallKit on iOS.
2. Support for 3GPP Call-Waiting.
3. Support for 3GPP IMS Conferencing.
4. Support for Present Agent (PUBLISH).
5. Moved `LocalIP` and `localPort` parameters from `setUser` function to `initialize` function.
6. New parameter `"sessionId"` added for `setVideoBitrate` and `setVideoFrameRate` functions to specify the session.
7. Removed `setDisplayName` function. Use `setUser` function instead to set the display name.
8. Removed `detectMwi` function. Use `sendSubscription` function instead to check MWI.
9. Removed `presenceOnline` and `presenceOffline` functions. Use `setPresenceStatus` function instead.
10. Replaced `createConference` with `createAudioConference` and `createVideoConference`.
11. Renamed `enableCheckMwi` to `enableAutoCheckMwi`.
12. Renamed `presenceSubscribeContact` function to `presenceSubscribe`.
13. New parameter `"sipMessage"` added for callback events `onInviteIncoming`, `onInviteSessionProgress`, `onInviteRinging`, `onInviteAnswered`, and `onInviteUpdated` to allow obtaining the specified SIP header value from `"sipMessage"`.
14. New callback event `onDialogStateUpdated`. This event will be triggered when the monitored user is holding a call or ringing.
15. Added `roundTripTime` parameter to `getAudioRtcpStatistics` function.
16. Added `sendSubscription` and `terminateSubscription` functions.
17. Added `setPresenceStatus` function.
18. Added `outOfDialogRefer` function.
19. Added `attendedRefer2` function.
20. Added `removeUser` function.
21. Added `refreshRegistration` function. When the network changes between Wi-Fi and LTE, this API should be called to refresh the registration.
22. Added `setDefaultSubscriptionTime` function.
23. Added `setDefaultPublicationTime` function.
24. Added `setPresenceMode` function.
25. Added `presenceTerminateSubscribe` function.
26. Added `pickupBLFCall` function.
27. **iOS:** New functions `startAudio` and `stopAudio` added for use with CallKit.
28. **iOS:** CallKit support added for iOSSIPSample. Added new class `CallManager`.
29. **iOS:** Added `libc++.tbd` to "Link Binary With Libraries".
30. **Android:** Removed APIs `setSystemOutputMute`, `getSystemOutputMute`, `setSystemInputMute`, and `getSystemInputMute`.
31. Fixed some other minor bugs.

---

### PortSIP VoIP SDK v11.3
**Release Date:** December 26, 2016

1. Added new function `setInstanceId`.
2. Allowed changing `videoRawCallback` local video data.
3. Fixed some minor bugs.

---

### PortSIP VoIP SDK v11.2.4
**Release Date:** September 16, 2016

1. Supported IPv6 only for IPv4 server.
2. Support for recorded video only.
3. Added `setChannelOutputVolumeScaling` API.
4. Added mixed content support.
5. Fixed a small cracking sound bug when re-inviting.
6. Fixed some minor bugs.

---

### PortSIP VoIP SDK v11.2.3
**Release Date:** May 17, 2016

1. Added `enableVideoDecoderCallback` and `onVideoDecoderCallback` functions.
2. Added `setVideoMTU` function.
3. Changed `setVideoResolution` to create conference video size parameters.
4. Upgraded OpenSSL to v1.0.2g.
5. Fixed some minor bugs.

---

### PortSIP VoIP SDK v11.2.2
**Release Date:** March 20, 2015

1. **iOS:** Fixed bug that audio was routed to speaker automatically when SDK goes to background during the call.
2. **iOS:** Fixed bug where the SDK randomly crashes when it goes to background.
3. **iOS:** Fixed bug that failed to set the video resolution.
4. **iOS:** Fixed bug where no tap sound occurred in iOS after the call was established.
5. Upgraded OpenSSL to v1.0.2.
6. Changed AEC, AGC, and ANS functions.
7. Fixed some minor bugs.

---

### PortSIP VoIP SDK v11.2.1
**Release Date:** November 28, 2014

1. Added CABAC support for H.264.
2. Added parameters `sendFractionLost`, `sendCumulativeLost`, `recvFractionLost`, and `recvCumulativeLost` for the `getAudioRtcpStatistics` function.
3. Changed parameters `useVirtualAudioDevice` and `useVirtualVideoDevice` of the `initialize` function to `audioDeviceLayer` and `videoDeviceLayer`.
4. Fixed a bug with attended transfer if the UAS PRACK is enabled.
5. Fixed a crash bug when establishing the H.263 video call on iOS.
6. Fixed a bug where IPv6 did not work on Android.
7. Fixed a bug when recording video on iOS.
8. Updated the PERS transport encryption mechanism.
9. Fixed some minor bugs.

---

### PortSIP VoIP SDK v11.2 Update
**Release Date:** September 19, 2014

1. Released PortSIP VoIP SDK v11.2 for Mac OS X.

---

### PortSIP VoIP SDK v11.2
**Release Date:** July 18, 2014

1. Upgraded the SDK for iOS from 3.1 to 11.2.
2. Added a parameter for the `registerServer` function.
3. Fixed a bug with VP8.
4. Fixed some minor bugs.

---

### PortSIP VoIP SDK v11.1
**Release Date:** May 28, 2014

1. Fixed a memory leak bug when recording video.
2. Fixed the bug of iLBC codec for supporting 20ms and 30ms.
3. Fixed a bug on Android where received messages always had a length of 0.
4. Fixed a bug when parsing SDP offer/answer to get the dynamic codec RTP payload type.

---

### PortSIP VoIP SDK v11.0
**Release Date:** May 8, 2014

This is a major upgrade; it's quite different from v8.2. In the future, the PortSIP VoIP SDK will have two branches, one from v8.2 and another from v11.0. The 8.x version is for some special customers.

We recommend normal customers upgrade to v11.0.

1. Provides a unified SDK with the same API functions and callback events on all platforms: Android, iOS, Windows, Mac, and Linux.
2. Many API/callback function names, parameters, and return values have changed or been removed. Please read the documentation carefully.
3. Allows calling SDK API functions in callbacks directly �� except for `onAudioRawCallback`, `onVideoRawCallback`, `onReceivedRtpPacket`, and `onSendingRtpPacket` callbacks.
4. Supports sending the YUV420 format video stream from a source instead of the camera.
5. Supports native 64-bit.

---

### PortSIP VoIP SDK v8.2
**Release Date:** February 18, 2014

1. Supports full PRACK.
2. Supports QoS preconditions.
3. Supports 3GPP tags.
4. Allows modification of the display name.
5. Added two parameters to `updateInvite` to allow removing existing audio/video media streams.
6. Supports setting the RTCP bandwidth parameter - RFC 3556.

7. Added four callbacks:
   - `onReceivedRefer`
   - `onReferAccepted`
   - `onReferRejected`
   - `onInviteConnected`
   - `onInviteSessionProgress`

8. Deleted four callbacks:
   - `onInviteUACConnected`
   - `onInviteUASConnected`
   - `onPASVTransferFailure`
   - `onPASVTransferSuccess`

9. Added eight functions:
   - `acceptRefer`
   - `rejectRefer`
   - `enable3GppTags`
   - `enableQosPreconditions`
   - `enableReliableProvisional`
   - `setDisplayName`
   - `setAudioRtcpBandwidth`
   - `setVideoRtcpBandwidth`

10. Deleted `setReliableProvisionalMode` function.
11. Removed the XMPP SDK.
12. Fixed some minor bugs.

---

### PortSIP VoIP SDK v8.1
**Release Date:** December 16, 2012

1. Supports H.264 high profile.
2. Added `enableCallbackSentSignaling` function to allow getting all sent SIP messages.
3. Added a function `setRtcpPortRange` to allow specifying the RTCP port range.
4. Allows non-contiguous RTP and RTCP port pairs.
5. Odd-numbered RTP port usage.
6. Fixed a bug for DTMF tone compatibility.
7. Added iSAC and ISAC audio codecs.
8. Added VP8 video codec.
9. Added a "RECORD_MODE" parameter for the `startVideoRecording` function.
10. Fixed some minor bugs.

---

### PortSIP VoIP SDK v8.0
**Release Date:** September 6, 2012

1. Supports UAC PRACK - `setReliableProvisionalMode` function.
2. Added two functions for user-settable RTP payload types:
   - `setAudioCodecPayloadType`
   - `setVideoCodecPayloadType`
3. Allows video calls without audio.
4. Added a parameter named "refreshMode" for the `enableSessionTimer` function.
5. Fixed some bugs.

---

### PortSIP VoIP SDK v7.2
**Release Date:** May 28, 2012

1. Added AMR-RED, maxptime, and ptime support for AMR and AMR-WB.
2. Virtual audio and video device support �� added two parameters for the `initialize` function.
3. Added `discardAudio` function.
4. Added `setRTPCallback` function to support accessing the sending and received RTP packets.
5. Added `getNetworkStatistics` function to obtain more network statistics.
6. Added `setAudioJitterBufferLevel` function to allow setting the audio jitter buffer.
7. Added `getNICNums` function to obtain the NIC (Network Interface Card) numbers.
8. Added a parameter for the `startAudioRecording` function to allow setting the record mode.
9. Fixed a few bugs.

---

### PortSIP VoIP SDK v7.1
**Release Date:** January 16, 2012

1. Added AMR, AMR-WB, and G.722.1 codecs.
2. Added two functions: `setAudioCodecParameter` and `setVideoCodecParameter`.
3. Removed the `setProfileLevelIdOfH264` function.
4. Supported RFC 4867.
5. Added `audioPlayLoopbackTest` function.
6. Fixed a few bugs.

---

### PortSIP VoIP SDK v7.0
**Release Date:** November 15, 2011

This is a major upgrade.

1. Optimized AEC, CNG, and NS features for crystal HD audio.
2. Optimized video codecs for crystal HD video.
3. Optimized the log system.
4. Supports tracking errors from the return values of API functions.
5. G.723.1, G.722.1, and AMR-WB are temporarily unavailable and will come with the next 7.1 version.
6. Changed the audio and video stream callback functions.
7. Integrated the "Device Manager APIs" into "PortSIP Core".
8. Deleted some functions and added new ones.
9. Supports recording video files in H.264 and H.263 format to reduce the file size.
10. Optimized the core algorithm to reduce CPU usage.

---

### PortSIP VoIP SDK v6.8
**Release Date:** June 10, 2011

1. Support for IPv6.
2. Added three functions for conference:
   - `createConferenceEx`
   - `joinToConference`
   - `removeFromConference`
3. Added two functions to obtain RTP statistics:
   - `getAudioRtpStatistics`
   - `getVideoRtpStatistics`
4. Added a new function `getLocalIP6` to obtain the local IP in IPv6 format.
5. Added a function `setKeepAliveTime` to enable/disable SIP keep alive.
6. Fixed some minor bugs.

---

### PortSIP VoIP SDK v6.6
**Release Date:** March 10, 2011

1. Added support for QVGA resolution for H.264 codec.
2. Added a new function to detect MWI status.
3. Fixed a bug for the G.722 codec.
4. Added a function to send out-of-dialog messages.
5. Added new functions `sendMessageEx` and `sendOutOfDialogMessageEx` to allow sending binary data via the MESSAGE method.
6. Added a function `discardAudio` to discard incoming and outgoing audio packets.
7. Added support for switching audio devices during calls.

---

### PortSIP VoIP SDK v6.5
**Release Date:** October 20, 2010

1. Added MP3 format support for conversation recording.
2. Supported playing wave files to a specific line (session).
3. Supported recording conversations on a specific line (session).
4. Optimized the RTP jitter buffer.

---

### PortSIP VoIP SDK v6.3
**Release Date:** August 20, 2010

1. Added a parameter named `appLogLevel` for the `initialize` function to generate SDK logs into files.
2. Changed `enableLog` function to `enableStackLog`.
3. Fixed a bug in PortXMPP to ensure compatibility with OpenFire.
4. Fixed a bug when calling PSTN that caused poor voice quality if the ptime was more than 20.

---

### PortSIP VoIP SDK v6.2
**Release Date:** July 26, 2010

1. Added a function to support sending messages via the MESSAGE method in the dialog (in-call): `sendMessage`.
2. Added an event called `receivedMessage` (for VC is SIP_RECV_MESSAGE) to receive messages via the MESSAGE method in the dialog (in-call).
3. Changed the `setPlayWaveFileToRemote` function parameters to support event reporting once playback is finished:
   - VC: added a callback function `fnPlayWaveFileToRemoteFinished`.
   - C#, VB, JavaScript, Delphi: added an event `playWaveFileToRemoteFinished`.
4. Changed the `setPlayWAVIFileToRemote` function parameters to support event reporting once playback is finished:
   - VC: added a callback function `fnPlayAviFileFinished`.
   - C#, VB, JavaScript, Delphi: added an event `playAviFileToRemoteFinished`.
5. Added `getDynamicVolumeLevel` function to obtain speaker and microphone dynamic volume levels.
6. Changed the video image capture mode to avoid the video image being upside down.
7. Fixed a bug in the Device Manager SDK: if set the "NULL" for "waveFile" parameter with `Device_playLocalWaveFile`, the Device Manager SDK would crash.
8. Supported calling PortSIP VoIP SDK directly using P/Invoke class wrapper without OCX.
9. Provided new SDK sample projects to demonstrate how to use P/Invoke class wrapper in WinForm and WPF applications.

---

### PortSIP VoIP SDK v6.1
**Release Date:** April 15, 2010

1. Added support for Firefox web phone.
2. Fixed a bug that caused no sound in audio conferencing.

---

### PortSIP VoIP SDK v6.0
**Release Date:** April 01, 2010

1. Added G.722.1, G.722, AMR-WB, SPEEX, and SPEEX-WB audio codecs.
2. Added 720P support for H.264.
3. Added three functions to support QoS that allow setting the DSCP value for audio and video RTP:
   - `setDSCPForQos`
   - `enableAudioQos`
   - `enableVideoQos`
4. Added a parameter for the `sendPcmStreamToRemote` function.
5. Changed the default payload type of iLBC to 97.
6. Added the function `setDisplayNameOfFrom` that allows changing the display name of the "From" SIP header.
7. Improved performance to reduce CPU usage.

---

### PortSIP VoIP SDK v5.5
**Release Date:** January 25, 2010

1. Added TCP transport for SIP signaling.
2. Added `forwardCall` function.
3. Added `setRtpKeepAlive` function.
4. Added the `appendTimestamp` parameter for `setAudioRecordPathName` and `setVideoRecordPathName`.
5. Added the `playLocalWaveFile` function in the Device Manager SDK.
6. Fixed a bug in the `setRtpPortRange` function.
7. Fixed a bug that prevented volume changes under Windows Vista/7.
8. Deleted the function named `setDefaultAudioInputDevice` from the Device Manager SDK.
9. Deleted the function named `setDefaultAudioOutputDevice` from the Device Manager SDK.

---

### PortSIP VoIP SDK v5.3
**Release Date:** October 15, 2009

1. Added `sendPcmStreamToRemote` function to support sending the audio stream directly in PCM format instead of audio device capture (Microphone).
2. Added `setSendAudioStream` function to allow enabling/disabling audio stream sending on a line.
3. Added a function named `addSupportedMimeType` that allows the SDK to receive SIP messages that include special MIME types.
4. Added a function named `sendInfo` that allows sending INFO messages in the dialog.
5. Added an event `receivedInfo`.
6. Added an event `receivedOptions`.

---

### PortSIP VoIP SDK v5.2
**Release Date:** September 16, 2009

1. Added `enableCheckMwi` function to allow enabling/disabling MWI checking.
2. Added `enableCabacOfH264` function to allow enabling/disabling CABAC in H.264.
3. Fixed some video compatibility issues.

---

### PortSIP VoIP SDK v5.1
**Release Date:** August 18, 2009

1. Added support for sending OPTIONS messages.
2. Added support for setting the audio capture sample (ptime).
3. Added a function to get the currently SDK version number.
4. Fixed a bug that allowed changing the video image size and position during video conversations.

---

### PortSIP VoIP SDK v5.0
**Release Date:** June 06, 2009

This is a major upgrade.

1. Added support for display names.
2. Added support for third-party video conferencing.
3. Changed the conference API functions.
4. Much better voice quality than the 4.x version.
5. Supported session timers.
6. Added the function `setVideoFrameRate` to set the video frame rate.
7. Changed the video stream callback parameter (for VB.NET, C#, Delphi, changed the `receivedVideoStream` event parameter).
8. Changed the audio stream callback parameter (for VB.NET, C#, Delphi, changed the `receivedAudioStream` event parameter).

---

### PortSIP VoIP SDK v4.8
**Release Date:** March 03, 2009

1. Added P2P call support without a SIP proxy server.
2. Supported TLS/SRTP.
3. Supported setting the RTP port range.
4. Fixed some bugs for video calls.

---

### PortSIP VoIP SDK v4.7
**Release Date:** November 18, 2008

1. Fixed a crash bug caused by calling uninitialize after refresh registration failed.
2. Added a parameter named `remoteVideoWindow` for the `setVideoImagePos` function.
3. Added support for call forwarding (redirect).
   - Added two functions:
     - `enableCallForwarding`
     - `disableCallForwarding`
4. Added an event: `inviteBeginForward`.

---

### PortSIP VoIP SDK v4.6
**Release Date:** October 08, 2008

1. Fixed a bug for answering INFO and MESSAGE requests.
2. Added support for adding custom SIP message headers.
3. Added support to modify SIP message headers.
4. Added function `reverseLocalViewVideo`.
5. Renamed `reverseRemoteVideo` to `reverseReceivedVideo`.
6. Renamed `reverseLocalVideo` to `reverseSendingVideo`.
7. Deleted `getLocalIP` function from PortSIP Core SDK.
8. Deleted `setLicenseKey` function from Device Manager SDK.
9. Added `addExtensionHeader` function.
10. Added `clearAddExtensionHeaders` function.
11. Added `modifyHeaderValue` function.
12. Added `clearModifyHeaders` function.

---

### PortSIP VoIP SDK v4.5
**Release Date:** August 19, 2008

1. Added support for the SIMPLE protocol for presence and IM.
2. Fixed an assertion failure bug when Auto Answer is enabled.
3. Fixed a bug to support multiple network cards.
4. Changed the `setPlayWaveFile` function name to `setPlayWaveFileToRemote`.
5. Changed the `playWaveFile` function name to `playLocalWaveFile`.
6. Added support for generating the SIP stack log.

---

### PortSIP VoIP SDK v4.5
**Release Date:** August 19, 2008

1. Added support for SIMPLE protocol for presence and IM.
2. Fixed an assertion failure bug when Auto Answer is enabled.
3. Fixed a bug to support multiple network cards.
4. Changed the `setPlayWaveFile` function name to `setPlayWaveFileToRemote`.
5. Changed the `playWaveFile` function name to `playLocalWaveFile`.
6. Added support for generating SIP stack logs.

---

### PortSIP VoIP SDK v4.3
**Release Date:** June 15, 2008

1. Optimized Automatic Gain Control.
2. Added video recording as AVI file.
3. Allowed direct access to the audio stream.
4. Added MJPG and YUV2 formats for video capture to support no-driver USB cameras.
5. Refreshed the video view when stopping video rendering.
6. Changed `startRecording` and `stopRecording` functions to `startAudioRecording` and `stopAudioRecording`.

---

### PortSIP VoIP SDK v4.2
**Release Date:** June 5, 2008

1. Added support for detecting incoming DTMF tones.

--- 
