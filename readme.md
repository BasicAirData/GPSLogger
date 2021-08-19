# BasicAirData GPS Logger<br>[![Releases](http://img.shields.io/github/release/BasicAirData/GPSLogger.svg?label=%20release%20)](https://github.com/BasicAirData/GPSLogger/releases) [![GitHub license](https://img.shields.io/badge/license-GPL_3-blue.svg?label=%20license%20)](https://raw.githubusercontent.com/BasicAirData/GPSLogger/master/LICENSE) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/gpslogger/localized.svg)](https://crowdin.com/project/gpslogger) 
A GPS logger for Android mobile devices.<br>
Offered by [BasicAirData](http://www.basicairdata.eu) - Open and free DIY air data instrumentation and telemetry 

![alt tag](https://github.com/BasicAirData/GPSLogger/blob/master/screenshots/Image_01.png)

## Description

BasicAirData GPS Logger is a simple App to record your position and your path.<br>
It's a basic and lightweight GPS tracker focused on accuracy, with an eye to power saving.<br>
This app is very accurate in determining your altitude: enable EGM96 automatic altitude correction on settings!<br>
You can record all your trips, view them in your preferred Viewer (it must be installed) directly from the in-app tracklist, and share them in KML, GPX, and TXT format in many ways.

The app is 100% Free and Open Source.

For further information about this app you can read [this article](http://www.basicairdata.eu/projects/android/android-gps-logger/).<br>
[Here](http://www.basicairdata.eu/projects/android/android-gps-logger/getting-started-guide-for-gps-logger/) you can find a Getting Started Guide.<br><br>
The application is freely downloadable from [Google Play(tm)](https://play.google.com/store/apps/details?id=eu.basicairdata.graziano.gpslogger) or directly here in this repository in /apk folder.<br>
You can install GPS Logger on your smartphone in one step, using the Google Store [QR-Code](https://github.com/BasicAirData/GPSLogger/blob/master/screenshots/qrcode%20-%20Google%20Store.png) or the Latest APK [QR-Code](https://github.com/BasicAirData/GPSLogger/blob/master/screenshots/qrcode.png);

## Translations

The app is translated in many languages thanks to the precious collaboration of some willing users around the world.<br>
Do you want to add a new language to the app?<br>
Do you want to help us in translations?<br>
Join Us on [Crowdin](https://crowdin.com/project/gpslogger) and help to translate and keep updated the app in your Language!

## Reference documents

[Code of conduct](CODE_OF_CONDUCT.md)

[Contributing Information](CONTRIBUTING.md)

[Repository License](LICENSE)

## Frequently Asked Questions

<i>The following answers are related to the latest version of GPS Logger.</i>

<b>I've just installed the App, but it doesn't read the GPS Signal.</b><br>
Please reboot your Device, go in an open Area and try to repeat your test. It seems not relevant, but a system reboot is really the solution in most of these cases.

<b>The Location is active, but the App sees "GPS disabled".</b><br>
Please go on Location Section of your Android Settings: the Phone could be set to use the "Battery saving" Locating Method. This Method uses Wi-Fi & Mobile Networks to estimate your Location, without turn on the GPS. In case please switch to "Phone only" or "High accuracy" Method to enable the GPS Sensor.

<b>How can I view my recorded Tracks?</b><br>
You can view your Tracks by going on Tracklist Tab and clicking on it. An Actionbar will appear, that should contain an Eye Icon or the Icon of a KML/GPX Viewer. At least one KML/GPX viewer must be installed on your device; if not (in this case the Icon will not be visible), please install it. If You installed more than one Viewer, into GPS Logger's Settings you can choose which one to use. Good Viewers for Android are GPX Viewer, Earth, or WRPElevationChart, but there are lots of good Alternatives around.

<b>The "View" Icon is not visible on Actionbar.</b><br>
The "View" Icon is visible, by selecting one single Track of the Tracklist, if you have at least one external Viewer installed on your Device. If You installed more than one Viewer, into GPS Logger's Settings you can choose which one to use. Good Viewers for Android are GPX Viewer, Earth, or WRPElevationChart, but there are lots of good Alternatives around.

<b>The "Share" Icon is not visible on Actionbar.</b><br>
The "Share" Icon is visible, by selecting some Tracks of the Tracklist, if you have at least one Application installed on your Device with which to Share the Files. The Formats you will share are set on Exporting Section of GPS Logger's Settings.

<b>My track is not shown (or partially shown) in Google Earth.</b><br>
GPS Logger might be set to show the Track in 3D, and the Track may be hidden under the Terrain. Please go in the Exportation Settings, switch the Altitude Mode to "Projected to ground" and try again.

<b>Why the app asks the Location permission "only while using the app", and the "all the time" option is not present?</b><br>
Because the permission is related to the access, and not the use of the Location by an App.
In GPS Logger the Location is always accessed (started) when the app is in foreground, and then is kept active (i.e. is used) also in background. On Android 10+ the app needs the Location permission "only while using the app", it doesn't need the "all the time" access.

<b>The App stops recording when running in Background.</b><br>
The App may have been closed by the System during the Background Recording. To avoid it, as first Step, go to the Android Settings and turn off all Battery Monitoring and Optimizations for GPS Logger. On Android 9+ check also that the Application is NOT Background Restricted and verify that the Background Activity is allowed. Unfortunately any Device Brand implemented in a different Way the Settings to keep safe the Background Apps (yes, it's a big mess), so a small Research must be done. On some brands you have to whitelist the background apps, whilst for some others you have to set the "high performances" power saving mode.<br>
To give a concrete example, on Android 11 Samsung will prevent apps work in background by default unless you exclude apps from battery optimizations. To keep your apps working properly go on Android Settings > Apps > GPS Logger > Battery > Battery optimization > All apps > GPS Logger > Don’t optimize.<br>
Moreover, other battery optimizers (like for example AccuBattery by Digibites) could restrict the background use, and also some anti-Viruses are very aggressive with long running Apps, and must be correctly set.<br>
<i>Note: Android 10+ users should read also the previous answer about location permission.</i>

<b>Why GPS FIX Time is different from the Time of my Android Device?</b><br>
Your Android Time could differ from GPS Time depending on Time Zone and on Daylight Saving. You can go on App's Settings, section Interface, and verify that the GPS Time is Shown in Local Timezone instead of global GPS Time (UTC based). If not, switch on the Setting. Speaking of dates, it is important to point out that the app exports all the Timestamps in UTC, as required by KML and GPX standards. The local Time is used only for Track Names, for User Convenience. As a Note, the Time of the GPS should be slightly different from Android Time (some Seconds of Difference are common).
