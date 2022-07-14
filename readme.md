# BasicAirData GPS Logger<br>[![Releases](http://img.shields.io/github/release/BasicAirData/GPSLogger.svg?label=%20release%20)](https://github.com/BasicAirData/GPSLogger/releases) [![GitHub license](https://img.shields.io/badge/license-GPL_3-blue.svg?label=%20license%20)](https://raw.githubusercontent.com/BasicAirData/GPSLogger/master/LICENSE) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/gpslogger/localized.svg)](https://crowdin.com/project/gpslogger) 
A GPS logger for Android mobile devices.<br>
Offered by [BasicAirData](https://www.basicairdata.eu) - Open and free DIY air data instrumentation and telemetry 

![alt tag](https://github.com/BasicAirData/GPSLogger/blob/master/screenshots/Image_01.png)

## Description

BasicAirData GPS Logger is a simple app to record your position and your path.<br>
It's a basic and lightweight GPS tracker focused on accuracy, with an eye to power saving.<br>
This app is very accurate in determining the orthometric height (the altitude above sea level), if you enable EGM96 altitude correction on settings.<br>
You can record all your trips, view them in your preferred viewer (it must be installed) directly from the in-app tracklist, and share them in KML, GPX, and TXT format in many ways.

The app is 100% free and open source.

For further information about this app you can read [this article](https://www.basicairdata.eu/projects/android/android-gps-logger/).<br>
[Here](https://www.basicairdata.eu/projects/android/android-gps-logger/getting-started-guide-for-gps-logger/) you can find a getting started guide.<br><br>
The application is freely downloadable from [Google Play(tm)](https://play.google.com/store/apps/details?id=eu.basicairdata.graziano.gpslogger) or directly here in this repository in /apk folder.<br>
You can install GPS Logger on your smartphone in one step, using the Google Store [QR-Code](https://github.com/BasicAirData/GPSLogger/blob/master/screenshots/qrcode%20-%20Google%20Store.png) or the latest APK [QR-Code](https://github.com/BasicAirData/GPSLogger/blob/master/screenshots/qrcode.png);

## Translations

The app is translated in many languages thanks to the precious collaboration of some willing users around the world.<br>
Do you want to add a new language to the app?<br>
Do you want to help us in translations?<br>
Join us on [Crowdin](https://crowdin.com/project/gpslogger) and help to translate and keep updated the app in your language!<br>
You can also subscribe our [Translation & Proofreading issue](https://github.com/BasicAirData/GPSLogger/issues/16) to keep informed on the topic.

## Reference documents

[Code of conduct](CODE_OF_CONDUCT.md)

[Contributing Information](CONTRIBUTING.md)

[Repository License](LICENSE)

## Frequently Asked Questions

<i>The following answers are related to the latest version of GPS Logger.</i>

<b>I've just installed the app, but it doesn't read the GPS signal.</b><br>
Please reboot your device, go in an open area and try to repeat your test. It seems not relevant, but a system reboot is really the solution in most of these cases.

<b>The location is active, but the app shows the message "GPS disabled".</b><br>
Please go on location section of your Android settings: the phone could be set to use the "Battery saving" locating method. This method uses Wi-Fi & mobile networks to estimate your location, without turn on the GPS. In case please switch to "Phone only" or "High accuracy" method to enable the GPS sensor.

<b>How can I view my recorded tracks?</b><br>
You can view a recorded track by going on tracklist tab and clicking on it. An actionbar appears, that should contain an eye icon, or the icon of a KML/GPX viewer. At least one KML/GPX viewer must be installed on your device; if not (in this case the icon will not be visible), please install it. If you installed more than one viewer, you can choose which one to use in GPS Logger settings. Good viewers for Android are GPX Viewer, Earth, or WRPElevationChart, but there are lots of good alternatives around.

<b>The "View" icon is not visible on actionbar.</b><br>
The "View" icon is visible, by selecting one single track of the tracklist, if you have at least one external viewer installed on your device. If you installed more than one viewer, into GPS Logger's settings you can choose which one to use. Good viewers for Android are GPX Viewer, Earth, or WRPElevationChart, but there are lots of good alternatives around.

<b>The "Share" icon is not visible on actionbar.</b><br>
The "Share" icon is visible, by selecting some tracks of the tracklist, if you have at least one application installed on your device with which to share the files. The formats you will share are set on "exportation" section of GPS Logger settings.

<b>Where the "Export" feature saves the files?</b><br>
The "Export" feature saves the files to a folder of your device. The exportation folder is selectable (on Android 5+): when you export a track for the first time, a dialog asks you to select the the local exportation folder. It is possible to select (and to change) the folder also in the app Settings, Interface section, by clicking the "Local Exportation Folder" preference.<br>
The selection of the folder is now mandatory because of a change on Google specifications, that restricted the permission to access the whole storage only in exceptional cases for privacy reasons. In fact, starting from v3.1.0, GPS Logger no longer needs the Storage permission because the permission to access the storage is limited to the folder selected.<br>
As a note, some special folders (like the root folders, or the Downloads) could be not suitable for the exportation, depending on the Android version. In this case please select another (sub)folder, or create a new one.<br>
On Android 4 the folder is not selectable, the files are saved to /GPSLogger.<br>

<b>How can I select a range of tracks on Tracklist?</b><br>
Select the first track of the range with a click, then long-click on the last one to select all the range. If with the first click you select a track, all the range will be selected; if with the first click you deselect a track, all the range will be deselected.

<b>My track is not shown (or partially shown) in Google Earth.</b><br>
GPS Logger might be set to show the track in 3D, and the track may be hidden under the terrain. Please go in the "Exportation" settings, switch the altitude mode to "Projected to ground" and try again.

<b>Why the app asks the location permission "only while using the app", and the "all the time" option is not present?</b><br>
Because the permission is related to the access, and not the use of the location by an app.
In GPS Logger the location is always accessed (started) when the app is in foreground, and then is kept active also in background. On Android 10+ the app needs the location permission "only while using the app", it doesn't need the "all the time" access.

<b>The app stops recording when running in background.</b><br>
The app may have been closed by the system during the background recording. To avoid it, as first step, go to the Android settings and turn off all battery monitoring and optimizations for GPS Logger. On Android 9+ check also that the application is NOT background restricted and verify that the background activity is allowed. Unfortunately any device brand implemented in a different way the settings to keep safe the background apps (yes, it's a big mess), so a small research must be done. On some brands you have to whitelist the background apps, whilst for some others you have to set the "high performances" power saving mode.<br>
To give a concrete example, on Android 11 Samsung will prevent apps work in background by default unless you exclude apps from battery optimizations. To keep your apps working properly go on Android Settings > Apps > GPS Logger > Battery > Battery optimization > All apps > GPS Logger > Don’t optimize.<br>
Moreover, other battery optimizers (like for example Duraspeed, or AccuBattery by Digibites) could restrict the background use, and also some anti-viruses are very aggressive with long running apps, and must be correctly set.<br>
As a last note, some people that had problems with background recording resolved with a re-installation of the app.<br>
<i>Android 10+ users should read also the previous answer about location permission.</i>

<b>Why GPS FIX time is different from the time of my Android device?</b><br>
Your android time could differ from GPS time depending on time zone and on daylight saving. You can go on app's settings, section "Interface", and verify that the GPS time is shown in local timezone instead of global GPS time (UTC based). If not, switch on the setting. Speaking of dates, it is important to point out that the app exports all the timestamps in UTC, as required by KML and GPX standards. The local time is used only for track names, for user convenience. As a note, the time of the GPS should be slightly different from Android time (some seconds of difference are common).
