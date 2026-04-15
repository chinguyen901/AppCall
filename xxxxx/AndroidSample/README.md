# Welcome to PortSIP VoIP SDK For Android

Create your SIP-based application for multiple platforms (iOS, Android, Windows, Mac OS and Linux) with our SDK.

The rewarding PortSIP VoIP SDK is a powerful and versatile set of tools that dramatically accelerate SIP application development. It includes a suite of stacks, SDKs, and some Sample projects, with each of them enables developers to combine all the necessary components to create an ideal development environment for every application's specific needs.

The PortSIP VoIP SDK complies with IETF and 3GPP standards, and is IMS-compliant (3GPP/3GPP2, TISPAN and PacketCable 2.0).
These high performance SDKs provide unified API layers for full user control and flexibility.


## Getting Started

You can download PortSIP VoIP SDK Sample projects at our [Website](https://www.portsip.com/download-portsip-voip-sdk/).
 Samples include demos for VC++, C#, VB.NET, Xcode (for iOS and Mac OS), Android Studio with the sample project source code provided (with SDK source code exclusive). The sample projects demonstrate how to create a powerful SIP application with our SDK easily and quickly.

## Contents

 The sample package for downloading contains almost all of materials for PortSIP VoIP SDK: documentation,
 Dynamic/Static libraries, sources, headers, datasheet, and everything else a SDK user might need!


## SDK User Manual

 To be started with, it is recommended to read the documentation of PortSIP VoIP SDK, [SDK User Manual page](https://www.portsip.com/voip-sdk-user-manual/), which gives a brief description of each API function.


## Website

Some general interest or often changing PortSIP SDK information will be posted on the [PortSIP website](https://www.portsip.com) in real time. The release contains links to the site, so while browsing you may see occasional broken links  if you are not connected to the Internet. To be sure everything needed for using the PortSIP VoIP SDK has been contained within the release.

## Support

Please send email to our <a href="mailto:support@portsip.com">Support team</a> if you need any help.

## Installation Prerequisites

To use PortSIP VoIP/IMS SDK for Android for development, SDK version with later than API-21 is required.

## Frequently Asked Questions
### 1. Where can I download the PortSIP VoIP SDK for test?
All sample projects of the %PortSIP VoIP SDK can be found and downloaded at:
<a href="https://www.portsip.com/download-portsip-voip-sdk/" target="_blank">https://www.portsip.com/download-portsip-voip-sdk/</a>
<a href="https://www.PortSIP.com/portsip-voip-sdk" target="_blank">https://www.PortSIP.com/portsip-voip-sdk</a>

### 2. How can I compile the sample project?

  1. Download the sample project from PortSIP website.
  2. Extract the .zip file.
  3. Open the project by your Android studio:
  4. Compile the sample project directly.  


### 3. How can I create a new project with PortSIP VoIP SDK?
  1. Download the sample project and evaluation SDK and extract it to a specified directory
  2. Run Android Studio and create a new Android Application Project
  3. Copy all files form libs directory under extracted directory to the libs directory of your new application.
  4. Import the dependent class form the SDK. For example:
			import com.portsip.OnPortSIPEvent;
			import com.portsip.PortSipSdk;
  5. Inherit the interface OnPortSIPEvent to process the callback events.
  6. Initialize SDK. For example:
			mPortSIPSDK = new PortSipSdk();
			mPortSIPSDK.setOnPortSIPEvent(instanceofOnPortSIPEvent);
			mPortSIPSDK.CreateCallManager(context);
			mPortSIPSDK.initialize(...);
  For more details please refer to the Sample project source code.

### 4. How can I test the P2P call (without SIP server)?
  1. Download and extract the SDK sample project ZIP file into local. Compile and run the "P2PSample" project.
  2. Run the P2Psample on two devices. For example, run it on device A and device B, and IP address for A is 192.168.1.10, IP address for B is 192.168.1.11.
  3. Enter a user name and password on A. For example, enter user name 111, and password aaa (you can enter anything for the password as the SDK will ignore it). Enter a user name and password on B. For example, enter user name 222, and password aaa.
  4. Click the "Initialize" button on A and B. If the default port 5060 is already in use, the P2PSample will prompt "Initialize failure". In case of this,
please click the "Uninitialize" button and change the local port, and click the "Initialize" button again.
  5. The log box will appear "Initialized" if the SDK is successfully initialized.
  6. To make call from A to B, enter "sip:222@192.168.1.11" and click "Dial" button; while to make call from B to A, enter "sip:111@192.168.1.10".

Note: If the local sip port is changed to other port, for example, A is using local port 5080, and B is using local port 6021, to make call from A to B, please enter
"sip:222@192.168.1.11:6021" and dial; while to make call from B to A, enter "sip:111@192.168.1.10:5080".

### 5. Is the SDK thread safe?
  Yes, the SDK is thread safe. You can call any of the API functions without the need to consider the multiple threads.  
Note: the SDK allows to call API functions in callback events directly - except for the "onAudioRawCallback", "onVideoRawCallback", "onRTPPacketCallback" callbacks.
