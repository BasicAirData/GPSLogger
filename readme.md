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

## Mini FAQ
<b>Q</b> - <i>I've just installed the app, but it doesn't read the GPS signal.</i><br>
<b>A</b> - Please try to reboot your device. Check also that the GPS signal is strong enough and, if not, go in an open area.

<b>Q</b> - <i>The Location is active, but the app sees "GPS disabled".</i><br>
<b>A</b> - Please go on Location section of your Android settings: the phone could be set to use the "Battery saving" Locating method. This method uses Wi-Fi & mobile networks to estimate your location, without turn on the GPS. In case please switch to "Phone only" or "High accuracy" method to enable the GPS sensor.

<b>Q</b> - <i>The "View", "Export", and "Share" menus are grayed out.</i><br>
<b>A</b> - Please check in Android System settings that GPS Logger has the Storage permission granted.

<b>Q</b> - <i>How can I see my recorded tracks?</i><br>
<b>A</b> - You can see your tracks clicking on it and using the "View in..." contextual menu. A KML viewer must be installed on your device; if not, please install it. One of the best KML viewers is Google Earth, but there are lots of good alternatives around.

<b>Q</b> - <i>The menu "View in" opens the Earth app, but doesn't show my track.</i><br>
<b>A</b> - It is a Earth problem. If you want to use Earth to view your tracks you have to open Earth before, and then go on GPS Logger and select "View in".

<b>Q</b> - <i>My track is not shown (or partially shown) in Google Earth.</i><br>
<b>A</b> - 1) Please check in Android System settings that Earth has the Storage permission granted; 2) GPS Logger might be set to show the track in 3D, and the track may be hidden under the terrain. Please go in the Exportation settings, switch the Altitude Mode to "Projected to ground" and try again.

<b>Q</b> - <i>The app sometimes stops recording when running in background.</i><br>
<b>A</b> - The app could be closed by the system during the background recording. To avoid it, you have to go on the Android Settings and turn off all battery monitoring and optimizations for GPS Logger. Sadly any device brand implemented in a different way the settings to keep safe the background apps, so a little research must be done. Furthermore, anti-viruses (like Avast) are very aggressive with long running apps, and must be correctly set.
