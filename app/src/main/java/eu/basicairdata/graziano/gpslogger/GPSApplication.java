/**
 * GPSApplication - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 20/5/2016
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package eu.basicairdata.graziano.gpslogger;

import android.Manifest;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GPSApplication extends Application implements GpsStatus.Listener, LocationListener {

    //private static final float M_TO_FT = 3.280839895f;
    private static final int NOT_AVAILABLE = -100000;

    //private static final int UM_METRIC_MS = 0;
    private static final int UM_METRIC_KMH = 1;
    //private static final int UM_IMPERIAL_FPS = 8;
    //private static final int UM_IMPERIAL_MPH = 9;

    private static final int STABILIZERVALUE = 3000;            // The application discards fixes for 3000 ms (minimum)
    private static final int DEFAULTHANDLERTIMER = 5000;        // The timer for turning off GPS on exit
    private static final int GPSUNAVAILABLEHANDLERTIMER = 7000; // The "GPS temporary unavailable" timer
    private int StabilizingSamples = 3;

    private static final int GPS_DISABLED = 0;
    private static final int GPS_OUTOFSERVICE = 1;
    private static final int GPS_TEMPORARYUNAVAILABLE = 2;
    private static final int GPS_SEARCHING = 3;
    private static final int GPS_STABILIZING = 4;
    private static final int GPS_OK = 5;

    // Preferences Variables
    // private boolean prefKeepScreenOn = true;                 // DONE in GPSActivity
    private boolean prefShowDecimalCoordinates  = false;
    private int     prefViewTracksWith          = 0;
    private int     prefUM                      = UM_METRIC_KMH;
    private float   prefGPSdistance             = 0f;
    private long    prefGPSupdatefrequency      = 1000L;
    private boolean prefEGM96AltitudeCorrection = false;
    private double  prefAltitudeCorrection      = 0d;
    private boolean prefExportKML               = true;
    private boolean prefExportGPX               = true;
    private int     prefGPXVersion              = 100;            // the version of the GPX schema
    private boolean prefExportTXT               = false;
    private int     prefKMLAltitudeMode         = 0;
    private int     prefShowTrackStatsType      = 0;
    private int     prefShowDirections          = 0;

    private boolean LocationPermissionChecked = false;            // If the flag is false the GPSActivity will check for Location Permission
    private boolean StoragePermissionChecked = false;             // If the flag is false Storage Permission must be asked
    private EventBusMSGNormal DoIfGrantStoragePermission = null;  // Store the message to send in case the user grant storage permission

    private LocationExtended PrevFix = null;
    private boolean isPrevFixRecorded = false;

    private LocationExtended PrevRecordedFix = null;

    private boolean MustUpdatePrefs = true;                     // True if preferences needs to be updated

    private boolean isCurrentTrackVisible = false;
    private boolean isContextMenuShareVisible = false;          // True if "Share with ..." menu is visible
    private boolean isContextMenuViewVisible = false;           // True if "View in *" menu is visible
    private String ViewInApp = "";                              // The string of default app name for "View"
                                                                // "" in case of selector

    // Singleton instance
    private static GPSApplication singleton;
    public static GPSApplication getInstance(){
        return singleton;
    }


    DatabaseHandler GPSDataBase;
    private String PlacemarkDescription = "";
    private boolean Recording = false;
    private boolean PlacemarkRequest = false;
    private long OpenInViewer = -1;                    // The index to be opened in viewer
    private long Share = -1;                                // The index to be Shared
    private boolean isGPSLocationUpdatesActive = false;
    private int GPSStatus = GPS_SEARCHING;

    private boolean NewTrackFlag = false;                   // The variable that handle the double-click on "Track Finished"
    final Handler newtrackhandler = new Handler();
    Runnable newtrackr = new Runnable() {
        @Override
        public void run() {
            NewTrackFlag = false;
        }
    };

    private boolean LocationSettingsFlag = false;           // The variable that handle the double-click on "Open Location Settings"
    final Handler locationsettingshandler = new Handler();
    Runnable locationsettingsr = new Runnable() {
        @Override
        public void run() {
            LocationSettingsFlag = false;
        }
    };

    private LocationManager mlocManager = null;             // GPS LocationManager
    private int _NumberOfSatellites = 0;
    private int _NumberOfSatellitesUsedInFix = 0;

    private int _Stabilizer = StabilizingSamples;
    private int HandlerTimer = DEFAULTHANDLERTIMER;

    private LocationExtended _currentLocationExtended = null;
    private LocationExtended _currentPlacemark = null;
    private Track _currentTrack = null;
    private List<Track> _ArrayListTracks = Collections.synchronizedList(new ArrayList<Track>());

    static SparseArray<Bitmap> thumbsArray = new SparseArray<>();       // The Array containing the Tracks Thumbnail

    Thumbnailer Th;
    Exporter Ex;
    private AsyncUpdateThreadClass asyncUpdateThread = new AsyncUpdateThreadClass();

    // The handler that switches off the location updates after a time delay:
    final Handler handler = new Handler();
    Runnable r = new Runnable() {

        @Override
        public void run() {
            setGPSLocationUpdates(false);
        }
    };

    final Handler gpsunavailablehandler = new Handler();
    Runnable unavailr = new Runnable() {

        @Override
        public void run() {
            if ((GPSStatus == GPS_OK) || (GPSStatus == GPS_STABILIZING)) {
                GPSStatus = GPS_TEMPORARYUNAVAILABLE;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            }
        }
    };


    // ------------------------------------------------------------------------------------ Service
    Intent GPSServiceIntent;
    GPSService GPSLoggerService;
    boolean isGPSServiceBound = false;

    private ServiceConnection GPSServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            GPSService.LocalBinder binder = (GPSService.LocalBinder) service;
            GPSLoggerService = binder.getServiceInstance();                     //Get instance of your service!
            Log.w("myApp", "[#] GPSApplication.java - GPSSERVICE CONNECTED - onServiceConnected event");
            isGPSServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w("myApp", "[#] GPSApplication.java - GPSSERVICE DISCONNECTED - onServiceDisconnected event");
            isGPSServiceBound = false;
        }
    };

    private void StartAndBindGPSService() {
        GPSServiceIntent = new Intent(GPSApplication.this, GPSService.class);
        //Start the service
        startService(GPSServiceIntent);
        //Bind to the service
        if (Build.VERSION.SDK_INT >= 14)
            bindService(GPSServiceIntent, GPSServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        else
            bindService(GPSServiceIntent, GPSServiceConnection, Context.BIND_AUTO_CREATE);
        Log.w("myApp", "[#] GPSApplication.java - StartAndBindGPSService");
    }


    /* private void UnbindGPSService() {                                                //UNUSED
        try {
            unbindService(GPSServiceConnection);                                        //Unbind to the service
            Log.w("myApp", "[#] GPSApplication.java - Service unbound");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to unbind the GPSService");
        }
    } */

    public void StopAndUnbindGPSService() {
        try {
            unbindService(GPSServiceConnection);                                        //Unbind to the service
            Log.w("myApp", "[#] GPSApplication.java - Service unbound");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to unbind the GPSService");
        }
        try {
            stopService(GPSServiceIntent);                                                  //Stop the service
            Log.w("myApp", "[#] GPSApplication.java - Service stopped");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to stop GPSService");
        }
    }


    // ------------------------------------------------------------------------ Getters and Setters
    public boolean getNewTrackFlag() {
        return NewTrackFlag;
    }

    public void setNewTrackFlag(boolean newTrackFlag) {
        if (newTrackFlag) {
            NewTrackFlag = true;
            newtrackhandler.removeCallbacks(newtrackr);         // Cancel the previous newtrackr handler
            newtrackhandler.postDelayed(newtrackr, 1500);       // starts the new handler
        } else {
            NewTrackFlag = false;
            newtrackhandler.removeCallbacks(newtrackr);         // Cancel the previous newtrackr handler
        }
    }

    public boolean getLocationSettingsFlag() {
        return LocationSettingsFlag;
    }

    public void setLocationSettingsFlag(boolean locationSettingsFlag) {
        if (locationSettingsFlag) {
            LocationSettingsFlag = true;
            locationsettingshandler.removeCallbacks(locationsettingsr);   // Cancel the previous locationsettingsr handler
            locationsettingshandler.postDelayed(locationsettingsr, 1000); // starts the new handler
        } else {
            LocationSettingsFlag = false;
            locationsettingshandler.removeCallbacks(locationsettingsr);   // Cancel the previous locationsettingsr handler
        }
    }

    public void setDoIfGrantStoragePermission(EventBusMSGNormal doIfGrantStoragePermission) {
        DoIfGrantStoragePermission = doIfGrantStoragePermission;
    }

    public EventBusMSGNormal getDoIfGrantStoragePermission() {
        return DoIfGrantStoragePermission;
    }

    public boolean isStoragePermissionChecked() {
        return StoragePermissionChecked;
    }

    public void setStoragePermissionChecked(boolean storagePermissionChecked) {
        StoragePermissionChecked = storagePermissionChecked;
    }

    public boolean isContextMenuShareVisible() {
        return isContextMenuShareVisible;
    }

    public boolean isContextMenuViewVisible() {
        return isContextMenuViewVisible;
    }

    public String getViewInApp() {
        return ViewInApp;
    }

    public boolean isLocationPermissionChecked() {
        return LocationPermissionChecked;
    }

    public void setLocationPermissionChecked(boolean locationPermissionChecked) {
        LocationPermissionChecked = locationPermissionChecked;
    }

    public void setHandlerTimer(int handlerTimer) {
        HandlerTimer = handlerTimer;
    }

    public int getHandlerTimer() {
        return HandlerTimer;
    }

    public int getGPSStatus() {
        return GPSStatus;
    }

    public int getPrefKMLAltitudeMode() {
        return prefKMLAltitudeMode;
    }

    public int getPrefGPXVersion() {
        return prefGPXVersion;
    }

    public void setOpenInViewer(long openInViewer) {
        OpenInViewer = openInViewer;
    }

    public long getOpenInViewer() {
        return OpenInViewer;
    }

    public long getShare() {
        return Share;
    }

    public void setShare(long share) {
        Share = share;
    }

    public double getPrefAltitudeCorrection() {
        return prefAltitudeCorrection;
    }

    public boolean getPrefEGM96AltitudeCorrection() {
        return prefEGM96AltitudeCorrection;
    }

    public boolean getPrefShowDecimalCoordinates() {
        return prefShowDecimalCoordinates;
    }

    public boolean getPrefExportKML() {
        return prefExportKML;
    }

    public boolean getPrefExportGPX() {
        return prefExportGPX;
    }

    public boolean getPrefExportTXT() {
        return prefExportTXT;
    }

    public int getPrefUM() {
        return prefUM;
    }

    public int getPrefShowTrackStatsType() {
        return prefShowTrackStatsType;
    }

    public int getPrefShowDirections() {
        return prefShowDirections;
    }

    public LocationExtended getCurrentLocationExtended() {
        return _currentLocationExtended == null ? null : _currentLocationExtended;
    }

    public void setPlacemarkDescription(String Description) {
        this.PlacemarkDescription = Description;
    }

    public Track getCurrentTrack() {
        return _currentTrack == null ? null : _currentTrack;
    }

    public int getNumberOfSatellites() {
        return _NumberOfSatellites;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return _NumberOfSatellitesUsedInFix;
    }

    public boolean getRecording() {
        return Recording;
    }

    public void setRecording(boolean recordingState) {
        PrevRecordedFix = null;
        Recording = recordingState;
    }

    public boolean getPlacemarkRequest() { return PlacemarkRequest; }

    public void setPlacemarkRequest(boolean placemarkRequest) { PlacemarkRequest = placemarkRequest; }

    public List<Track> getTrackList() {
        return _ArrayListTracks;
    }

    public boolean isCurrentTrackVisible() {
        return isCurrentTrackVisible;
    }

    public void setisCurrentTrackVisible(boolean currentTrackVisible) {
        isCurrentTrackVisible = currentTrackVisible;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        EventBus.getDefault().register(this);

        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);     // Location Manager

        File sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger");   // Create the Directories if not exist
        if (!sd.exists()) {
            sd.mkdir();
            Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
        }
        sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
        if (!sd.exists()) {
            sd.mkdir();
            Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
        }

        sd = new File(getApplicationContext().getFilesDir() + "/Thumbnails");
        if (!sd.exists()) {
            sd.mkdir();
            Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
        }

        EGM96 egm96 = EGM96.getInstance();                                              // Load EGM Grid
        if (egm96 != null) {
            if (!egm96.isEGMGridLoaded()) {
                egm96.LoadGridFromFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC", getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
            }
        }

        GPSDataBase = new DatabaseHandler(this);                                        // Initialize the Database

        // Prepare the current track
        if (GPSDataBase.getLastTrackID() == 0) GPSDataBase.addTrack(new Track());       // Creation of the first track if the DB is empty
        _currentTrack = GPSDataBase.getLastTrack();                                     // Get the last track

        LoadPreferences();                                                              // Load Settings

        // ----------------------------------------------------------------------------------------

        asyncUpdateThread.start();
        AsyncTODO ast = new AsyncTODO();
        ast.TaskType = "TASK_NEWTRACK";
        ast.location = null;
        AsyncTODOQueue.add(ast);

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        Log.w("myApp", "[#] GPSApplication.java - Max available VM memory = " + (int) (Runtime.getRuntime().maxMemory() / 1024) + " kbytes");

    }

    @Override
    public void onTerminate() {
        Log.w("myApp", "[#] GPSApplication.java - onTerminate");
        EventBus.getDefault().unregister(this);
        StopAndUnbindGPSService();
        super.onTerminate();
    }

    @Subscribe
    public void onEvent(EventBusMSGLong msg) {
        if (msg.MSGType == EventBusMSG.TRACK_SETPROGRESS) {
            long trackid = msg.id;
            long progress = msg.Value;
            if ((trackid > 0) && (progress >= 0)) {
                synchronized(_ArrayListTracks) {
                    for (Track T : _ArrayListTracks) {
                        if (T.getId() == trackid) T.setProgress((int) progress);
                    }
                }
            }
            return;
        }
    }

    @Subscribe
    public void onEvent(EventBusMSGNormal msg) {
        if (msg.MSGType == EventBusMSG.TRACK_EXPORTED) {
            long trackid = msg.id;
            if (trackid > 0) {
                synchronized(_ArrayListTracks) {
                    for (Track T : _ArrayListTracks) {
                        if (T.getId() == trackid) {
                            T.setProgress(0);
                            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
                            if (trackid == OpenInViewer) {
                                OpenInViewer = -1;
                                if (prefViewTracksWith == 0) {              // KML Viewer
                                    File file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", T.getName() + ".kml");
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.google-earth.kml+xml");
                                    startActivity(intent);
                                }
                                if (prefViewTracksWith == 1) {              // GPX Viewer
                                    File file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", T.getName() + ".gpx");
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setDataAndType(Uri.fromFile(file), "gpx+xml");
                                    startActivity(intent);
                                }
                            } else if (trackid == Share) {
                                Share = -1;
                                EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.INTENT_SEND, trackid));
                            } else {
                                EventBus.getDefault().post(EventBusMSG.TOAST_TRACK_EXPORTED);
                            }
                        }
                    }
                }
            }
            return;
        }
        if (msg.MSGType == EventBusMSG.EXPORT_TRACK) {
            long trackid = msg.id;
            Ex = new Exporter(trackid, prefExportKML, prefExportGPX, prefExportTXT, Environment.getExternalStorageDirectory() + "/GPSLogger");
            Ex.start();
            return;
        }
        if (msg.MSGType == EventBusMSG.SHARE_TRACK) {
            setShare(msg.id);
            Ex = new Exporter(Share, prefExportKML, prefExportGPX, prefExportTXT, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
            Ex.start();
            return;
        }
        if (msg.MSGType == EventBusMSG.VIEW_TRACK) {
            setOpenInViewer(msg.id);
            if (prefViewTracksWith == 0) {              // KML Viewer
                Ex = new Exporter(OpenInViewer, true, false, false, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
                Ex.start();
            }
            if (prefViewTracksWith == 1) {              // GPX Viewer
                Ex = new Exporter(OpenInViewer, false, true, false, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
                Ex.start();
            }
            return;
        }
        if (msg.MSGType == EventBusMSG.DELETE_TRACK) {
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_DELETE_TRACK " + msg.id;
            ast.location = null;
            AsyncTODOQueue.add(ast);
            return;
        }
        if (msg.MSGType == EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE) {
            long trackid = msg.id;
            if (trackid > 0) {
                synchronized(_ArrayListTracks) {
                    for (Track T : _ArrayListTracks) {
                        if (T.getId() == trackid) {
                            T.setProgress(0);
                            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
                            if (trackid == OpenInViewer) {
                                OpenInViewer = -1;
                            }
                            if (trackid == Share) {
                                Share = -1;
                            }
                        }
                    }
                }
            }
            return;
        }
    }

    @Subscribe
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.NEW_TRACK) {
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_NEWTRACK";
            ast.location = null;
            AsyncTODOQueue.add(ast);
            return;
        }
        if (msg == EventBusMSG.ADD_PLACEMARK) {
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_ADDPLACEMARK";
            ast.location = _currentPlacemark;
            _currentPlacemark.setDescription(PlacemarkDescription);
            AsyncTODOQueue.add(ast);
            return;
        }
        if (msg == EventBusMSG.APP_PAUSE) {
            handler.postDelayed(r, getHandlerTimer());  // Starts the switch-off handler (delayed by HandlerTimer)
            if ((_currentTrack.getNumberOfLocations() == 0) && (_currentTrack.getNumberOfPlacemarks() == 0)
                && (!Recording) && (!PlacemarkRequest)) StopAndUnbindGPSService();
            System.gc();                                // Clear mem from released objects with Garbage Collector
            return;
        }
        if (msg == EventBusMSG.APP_RESUME) {
            //Log.w("myApp", "[#] GPSApplication.java - Received EventBusMSG.APP_RESUME");
            AsyncPrepareTracklistContextMenu asyncPrepareTracklistContextMenu = new AsyncPrepareTracklistContextMenu();
            asyncPrepareTracklistContextMenu.start();
            handler.removeCallbacks(r);                 // Cancel the switch-off handler
            setHandlerTimer(DEFAULTHANDLERTIMER);
            setGPSLocationUpdates(true);
            if (MustUpdatePrefs) {
                MustUpdatePrefs = false;
                LoadPreferences();
            }
            StartAndBindGPSService();
            return;
        }
        if (msg == EventBusMSG.UPDATE_SETTINGS) {
            MustUpdatePrefs = true;
            return;
        }
    }

    public void setGPSLocationUpdates (boolean state) {
        // Request permissions = https://developer.android.com/training/permissions/requesting.html

        if (!state && !getRecording() && isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            GPSStatus = GPS_SEARCHING;
            mlocManager.removeGpsStatusListener(this);
            mlocManager.removeUpdates(this);
            isGPSLocationUpdatesActive = false;
        }
        if (state && !isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            mlocManager.addGpsStatusListener(this);
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this); // Requires Location update
            isGPSLocationUpdatesActive = true;
            StabilizingSamples = (int) Math.ceil(STABILIZERVALUE / prefGPSupdatefrequency);
        }
    }

    public void updateGPSLocationFrequency () {

        if (isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            mlocManager.removeGpsStatusListener(this);
            mlocManager.removeUpdates(this);
            StabilizingSamples = (int) Math.ceil(STABILIZERVALUE / prefGPSupdatefrequency);
            mlocManager.addGpsStatusListener(this);
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this);
        }
    }

    public void updateSats() {
        try {
            if ((mlocManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                GpsStatus gs = mlocManager.getGpsStatus(null);
                int sats_inview = 0;    // Satellites in view;
                int sats_used = 0;      // Satellites used in fix;

                if (gs != null) {
                    Iterable<GpsSatellite> sats = gs.getSatellites();
                    for (GpsSatellite sat : sats) {
                        sats_inview++;
                        if (sat.usedInFix()) sats_used++;
                        //Log.w("myApp", "[#] GPSApplication.java - updateSats: i=" + i);
                    }
                    _NumberOfSatellites = sats_inview;
                    _NumberOfSatellitesUsedInFix = sats_used;
                } else {
                    _NumberOfSatellites = NOT_AVAILABLE;
                    _NumberOfSatellitesUsedInFix = NOT_AVAILABLE;
                }
            } else {
                _NumberOfSatellites = NOT_AVAILABLE;
                _NumberOfSatellitesUsedInFix = NOT_AVAILABLE;
            }
        } catch (NullPointerException e) {
            _NumberOfSatellites = NOT_AVAILABLE;
            _NumberOfSatellitesUsedInFix = NOT_AVAILABLE;
            Log.w("myApp", "[#] GPSApplication.java - updateSats: Caught NullPointerException: " + e);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }


    private class AsyncPrepareTracklistContextMenu extends Thread {

        public AsyncPrepareTracklistContextMenu() {
        }

        public void run() {
            isContextMenuShareVisible = false;
            isContextMenuViewVisible = false;
            ViewInApp = "";

            final PackageManager pm = getPackageManager();

            // ----- menu share
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/xml");
            // Verify the intent will resolve to at least one activity
            if ((intent.resolveActivity(pm) != null)) isContextMenuShareVisible = true;

            // ----- menu view
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType("application/vnd.google-earth.kml+xml");

            if (prefViewTracksWith == 0) {              // KML Viewer
                intent.setType("application/vnd.google-earth.kml+xml");
            }
            if (prefViewTracksWith == 1) {              // GPX Viewer
                intent.setType("application/gpx+xml");
            }
            ResolveInfo ri = pm.resolveActivity(intent, 0); // Find default app
            if (ri != null) {
                //Log.w("myApp", "[#] FragmentTracklist.java - Open with: " + ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
                List<ResolveInfo> lri = pm.queryIntentActivities(intent, 0);
                //Log.w("myApp", "[#] FragmentTracklist.java - Found " + lri.size() + " viewers:");
                for (ResolveInfo tmpri : lri) {
                    //Log.w("myApp", "[#] " + ri.activityInfo.applicationInfo.packageName + " - " + tmpri.activityInfo.applicationInfo.packageName);
                    if (ri.activityInfo.applicationInfo.packageName.equals(tmpri.activityInfo.applicationInfo.packageName)) {
                        ViewInApp = ri.activityInfo.applicationInfo.loadLabel(pm).toString();
                        //Log.w("myApp", "[#]                              DEFAULT --> " + tmpri.activityInfo.applicationInfo.loadLabel(getPackageManager()));
                    }   //else Log.w("myApp", "[#]                                          " + tmpri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
                }
                isContextMenuViewVisible = true;
            }
            Log.w("myApp", "[#] GPSApplication.java - Tracklist ContextMenu prepared");
        }
    }

    // ------------------------------------------------------------------------- GpsStatus.Listener
    @Override
    public void onGpsStatusChanged(final int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                // TODO: get here the status of the GPS, and save into a GpsStatus to be used for satellites visualization;
                // Use GpsStatus getGpsStatus (GpsStatus status)
                // https://developer.android.com/reference/android/location/LocationManager.html#getGpsStatus(android.location.GpsStatus)
                updateSats();
                break;
        }
    }

    // --------------------------------------------------------------------------- LocationListener
    @Override
    public void onLocationChanged(Location loc) {
        //if ((loc != null) && (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
        if (loc != null) {      // Location data is valid
            //Log.w("myApp", "[#] GPSApplication.java - onLocationChanged: provider=" + loc.getProvider());
            if (loc.hasSpeed() && (loc.getSpeed() == 0)) loc.removeBearing();           // Removes bearing if the speed is zero
            LocationExtended eloc = new LocationExtended(loc);
            eloc.setNumberOfSatellites(getNumberOfSatellites());
            eloc.setNumberOfSatellitesUsedInFix(getNumberOfSatellitesUsedInFix());
            boolean ForceRecord = false;

            gpsunavailablehandler.removeCallbacks(unavailr);                            // Cancel the previous unavail countdown handler
            gpsunavailablehandler.postDelayed(unavailr, GPSUNAVAILABLEHANDLERTIMER);    // starts the unavailability timeout (in 7 sec.)

            if (GPSStatus != GPS_OK) {
                if (GPSStatus != GPS_STABILIZING) {
                    GPSStatus = GPS_STABILIZING;
                    _Stabilizer = StabilizingSamples;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                }
                else _Stabilizer--;
                if (_Stabilizer == 0) GPSStatus = GPS_OK;
                PrevFix = eloc;
                PrevRecordedFix = eloc;
                isPrevFixRecorded = true;
            }

            // Save fix in case this is a STOP or a START (the speed is "old>0 and new=0" or "old=0 and new>0")
            if ((PrevFix != null) && (PrevFix.getLocation().hasSpeed()) && (eloc.getLocation().hasSpeed()) && (GPSStatus == GPS_OK) && (Recording)
                    && (((eloc.getLocation().getSpeed() == 0) && (PrevFix.getLocation().getSpeed() != 0)) || ((eloc.getLocation().getSpeed() != 0) && (PrevFix.getLocation().getSpeed() == 0)))) {
                if (!isPrevFixRecorded) {                   // Record the old sample if not already recorded
                    AsyncTODO ast = new AsyncTODO();
                    ast.TaskType = "TASK_ADDLOCATION";
                    ast.location = PrevFix;
                    AsyncTODOQueue.add(ast);
                    PrevRecordedFix = PrevFix;
                    isPrevFixRecorded = true;
                }

                ForceRecord = true;                         // + Force to record the new
            }

            if (GPSStatus == GPS_OK) {
                AsyncTODO ast = new AsyncTODO();
                if ((Recording) && ((prefGPSdistance == 0) || (PrevRecordedFix == null) || (ForceRecord) || (loc.distanceTo(PrevRecordedFix.getLocation()) >= prefGPSdistance))) {
                    PrevRecordedFix = eloc;
                    ast.TaskType = "TASK_ADDLOCATION";
                    ast.location = eloc;
                    AsyncTODOQueue.add(ast);
                    isPrevFixRecorded = true;
                } else {
                    ast.TaskType = "TASK_UPDATEFIX";
                    ast.location = eloc;
                    AsyncTODOQueue.add(ast);
                    isPrevFixRecorded = false;
                }

                if (PlacemarkRequest) {
                    _currentPlacemark = new LocationExtended(loc);
                    _currentPlacemark.setNumberOfSatellites(getNumberOfSatellites());
                    _currentPlacemark.setNumberOfSatellitesUsedInFix(getNumberOfSatellitesUsedInFix());
                    PlacemarkRequest = false;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    EventBus.getDefault().post(EventBusMSG.REQUEST_ADD_PLACEMARK);
                }
                PrevFix = eloc;
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        GPSStatus = GPS_DISABLED;
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
    }

    @Override
    public void onProviderEnabled(String provider) {
        GPSStatus = GPS_SEARCHING;
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // This is called when the GPS status changes
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                //Log.w("myApp", "[#] GPSApplication.java - GPS Out of Service");
                gpsunavailablehandler.removeCallbacks(unavailr);            // Cancel the previous unavail countdown handler
                GPSStatus = GPS_OUTOFSERVICE;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                //Toast.makeText( getApplicationContext(), "GPS Out of Service", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                //Log.w("myApp", "[#] GPSApplication.java - GPS Temporarily Unavailable");
                gpsunavailablehandler.removeCallbacks(unavailr);            // Cancel the previous unavail countdown handler
                GPSStatus = GPS_TEMPORARYUNAVAILABLE;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                //Toast.makeText( getApplicationContext(), "GPS Temporarily Unavailable", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.AVAILABLE:
                gpsunavailablehandler.removeCallbacks(unavailr);            // Cancel the previous unavail countdown handler
                //Log.w("myApp", "[#] GPSApplication.java - GPS Available: " + _NumberOfSatellites + " satellites");
                break;
        }
    }


    public void UpdateTrackList() {
        long ID = GPSDataBase.getLastTrackID();
        if (ID > 0) {
            synchronized(_ArrayListTracks) {
                _ArrayListTracks.clear();
                _ArrayListTracks.addAll(GPSDataBase.getTracksList(0, ID - 1));
                if ((ID > 1) && (GPSDataBase.getTrack(ID - 1) != null)) {
                    String fname = (ID - 1) + ".png";
                    File file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                    if (!file.exists()) Th = new Thumbnailer(ID - 1);
                }
                if (_currentTrack.getNumberOfLocations() + _currentTrack.getNumberOfPlacemarks() > 0) {
                    Log.w("myApp", "[#] GPSApplication.java - Update Tracklist: current track (" + _currentTrack.getId() + ") visible into the tracklist");
                    _ArrayListTracks.add(0, _currentTrack);
                } else
                    Log.w("myApp", "[#] GPSApplication.java - Update Tracklist: current track not visible into the tracklist");
            }
            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
            //Log.w("myApp", "[#] GPSApplication.java - Update Tracklist: Added " + _ArrayListTracks.size() + " tracks");
        }
    }


// PREFERENCES LOADER ------------------------------------------------------------------------------

    private void LoadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // ---------Conversion from the previous versions of GPS Logger preferences
        if (preferences.contains("prefShowImperialUnits")) {       // The old boolean setting for imperial units in v.1.1.5
            Log.w("myApp", "[#] GPSApplication.java - Old setting prefShowImperialUnits present. Converting to new preference PrefUM.");
            boolean imperialUM = preferences.getBoolean("prefShowImperialUnits", false);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("prefUM", (imperialUM ? "8" : "0"));
            editor.remove("prefShowImperialUnits");
            editor.commit();
        }
        // -----------------------------------------------------------------------

        //prefKeepScreenOn = preferences.getBoolean("prefKeepScreenOn", true);
        prefShowDecimalCoordinates = preferences.getBoolean("prefShowDecimalCoordinates", false);
        prefViewTracksWith = Integer.valueOf(preferences.getString("prefViewTracksWith", "0"));
        prefUM = Integer.valueOf(preferences.getString("prefUM", "0")) + Integer.valueOf(preferences.getString("prefUMSpeed", "1"));
        prefGPSdistance = Float.valueOf(preferences.getString("prefGPSdistance", "0"));
        prefEGM96AltitudeCorrection = preferences.getBoolean("prefEGM96AltitudeCorrection", false);
        prefAltitudeCorrection = Double.valueOf(preferences.getString("prefAltitudeCorrection", "0"));
            Log.w("myApp", "[#] GPSApplication.java - Manual Correction set to " + prefAltitudeCorrection + " m");
        prefExportKML = preferences.getBoolean("prefExportKML", true);
        prefExportGPX = preferences.getBoolean("prefExportGPX", true);
        prefExportTXT = preferences.getBoolean("prefExportTXT", false);
        prefKMLAltitudeMode = Integer.valueOf(preferences.getString("prefKMLAltitudeMode", "1"));
        prefGPXVersion = Integer.valueOf(preferences.getString("prefGPXVersion", "100"));               // Default value = v.1.0
        prefShowTrackStatsType = Integer.valueOf(preferences.getString("prefShowTrackStatsType", "0"));
        prefShowDirections = Integer.valueOf(preferences.getString("prefShowDirections", "0"));
        StoragePermissionChecked = preferences.getBoolean("prefIsStoragePermissionChecked", false);

        long oldGPSupdatefrequency = prefGPSupdatefrequency;
        prefGPSupdatefrequency = Long.valueOf(preferences.getString("prefGPSupdatefrequency", "1000"));

        // ---------------------------------------------- Update the GPS Update Frequency if needed
        if (oldGPSupdatefrequency != prefGPSupdatefrequency) updateGPSLocationFrequency();

        // ---------------------------------------------------------------- Load EGM Grid if needed
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (!egm96.isEGMGridLoaded()) {
                egm96.LoadGridFromFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC", getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
            }
        }

        // ------------------------------------------------------------------- Request of UI Update
        EventBus.getDefault().post(EventBusMSG.APPLY_SETTINGS);
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
    }


// THE THREAD THAT DOES ASYNCHRONOUS OPERATIONS ---------------------------------------------------


    class AsyncTODO {
        String TaskType;
        LocationExtended location;
    }

    private BlockingQueue<AsyncTODO> AsyncTODOQueue = new LinkedBlockingQueue<>();

    private class AsyncUpdateThreadClass extends Thread {

        Track track;
        LocationExtended locationExtended;

        public AsyncUpdateThreadClass() {}

        public void run() {

            track = _currentTrack;
            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
            UpdateTrackList();

            while (true) {
                AsyncTODO asyncTODO;
                try {
                    asyncTODO = AsyncTODOQueue.take();
                } catch (InterruptedException e) {
                    Log.w("myApp", "[!] Buffer not available: " + e.getMessage());
                    break;
                }

                // Task: Create new track (if needed)
                if (asyncTODO.TaskType.equals("TASK_NEWTRACK")) {
                    if ((track.getNumberOfLocations() != 0) || (track.getNumberOfPlacemarks() != 0)) {
                        // ---- Delete 2 thumbs files forward - in case of user deleted DB in App manager (pngs could be already presents for the new IDS)
                        String fname = (track.getId() + 1) +".png";
                        File file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                        if (file.exists ()) file.delete ();
                        fname = (track.getId() + 2) +".png";
                        file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                        if (file.exists ()) file.delete ();
                        track = new Track();
                        // ----
                        track.setId(GPSDataBase.addTrack(track));
                        Log.w("myApp", "[#] GPSApplication.java - TASK_NEWTRACK: " + track.getId());
                        _currentTrack = track;
                        UpdateTrackList();
                    } else Log.w("myApp", "[#] GPSApplication.java - TASK_NEWTRACK: Track " + track.getId() + " already empty (New track not created)");
                    _currentTrack = track;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                }

                // Task: Add location to current track
                if (asyncTODO.TaskType.equals("TASK_ADDLOCATION")) {
                    locationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    locationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    locationExtended.setNumberOfSatellitesUsedInFix(asyncTODO.location.getNumberOfSatellitesUsedInFix());
                    _currentLocationExtended = locationExtended;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                    track.add(locationExtended);
                    GPSDataBase.addLocationToTrack(locationExtended, track);
                    _currentTrack = track;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    if (_currentTrack.getNumberOfLocations() + _currentTrack.getNumberOfPlacemarks() == 1) UpdateTrackList();
                }

                // Task: Add a placemark to current track
                if (asyncTODO.TaskType.equals("TASK_ADDPLACEMARK")) {
                    locationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    locationExtended.setDescription(asyncTODO.location.getDescription());
                    locationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    locationExtended.setNumberOfSatellitesUsedInFix(asyncTODO.location.getNumberOfSatellitesUsedInFix());
                    track.addPlacemark(locationExtended);
                    GPSDataBase.addPlacemarkToTrack(locationExtended, track);
                    _currentTrack = track;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    if (_currentTrack.getNumberOfLocations() + _currentTrack.getNumberOfPlacemarks() == 1) UpdateTrackList();
                }

                // Task: Update current Fix
                if (asyncTODO.TaskType.equals("TASK_UPDATEFIX")) {
                    _currentLocationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    _currentLocationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    _currentLocationExtended.setNumberOfSatellitesUsedInFix(asyncTODO.location.getNumberOfSatellitesUsedInFix());
                    EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                }

                if (asyncTODO.TaskType.contains("TASK_DELETE_TRACK")) {
                    Log.w("myApp", "[#] GPSApplication.java - Deleting Track ID = " + asyncTODO.TaskType.split(" ")[1]);
                    if (Integer.valueOf(asyncTODO.TaskType.split(" ")[1]) >= 0) {
                        long selectedtrackID = Integer.valueOf(asyncTODO.TaskType.split(" ")[1]);
                        synchronized(_ArrayListTracks) {
                            if (!_ArrayListTracks.isEmpty() && (selectedtrackID >= 0)) {
                                int i = 0;
                                boolean found = false;
                                do {
                                    if (_ArrayListTracks.get(i).getId() == selectedtrackID) {
                                        found = true;
                                        GPSDataBase.DeleteTrack(_ArrayListTracks.get(i).getId());
                                        Log.w("myApp", "[#] GPSApplication.java - Track " + _ArrayListTracks.get(i).getId() + " deleted.");
                                        _ArrayListTracks.remove(i);
                                    }
                                    i++;
                                } while ((i < _ArrayListTracks.size()) && !found);
                                //Log.w("myApp", "[#] GPSApplication.java - now DB Contains " + GPSDataBase.getLocationsTotalCount() + " locations");
                                //if (found) UpdateTrackList();
                            }
                        }
                    }
                }
            }
        }
    }





// THE THREAD THAT GENERATES A TRACK THUMBNAIL -----------------------------------------------------

    public class Thumbnailer {

        long Id;
        long NumberOfLocations;

        private Paint drawPaint = new Paint();
        private Paint BGPaint = new Paint();
        private Paint EndDotdrawPaint = new Paint();
        private Paint EndDotBGPaint = new Paint();
        private int Size = (int)(getResources().getDimension(R.dimen.thumbSize));

        private int Margin = (int) Math.ceil(getResources().getDimension(R.dimen.thumbLineWidth) * 3);
        private int Size_Minus_Margins = Size - 2 * Margin;

        private double MinLatitude;
        private double MinLongitude;

        double Distance_Proportion;
        double DrawScale;
        double Lat_Offset;
        double Lon_Offset;

        private AsyncThumbnailThreadClass asyncThumbnailThreadClass = new AsyncThumbnailThreadClass();

        public Thumbnailer(long ID) {

            Track track = GPSDataBase.getTrack(ID);
            //Log.w("myApp", "[#] GPSApplication.java - Bitmap Size = " + Size);

            if ((track.getNumberOfLocations() > 2) && (track.getDistance() >= 15) && (track.getValidMap() != 0)) {
                Id = track.getId();
                NumberOfLocations = track.getNumberOfLocations();

                // Setup Paints
                drawPaint.setColor(Color.parseColor("#c9c9c9"));
                drawPaint.setAntiAlias(true);
                drawPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth));
                //drawPaint.setStrokeWidth(2);
                drawPaint.setStyle(Paint.Style.STROKE);
                drawPaint.setStrokeJoin(Paint.Join.ROUND);
                drawPaint.setStrokeCap(Paint.Cap.ROUND);

                BGPaint.setColor(Color.BLACK);
                BGPaint.setAntiAlias(true);
                BGPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth) * 3);
                //BGPaint.setStrokeWidth(6);
                BGPaint.setStyle(Paint.Style.STROKE);
                BGPaint.setStrokeJoin(Paint.Join.ROUND);
                BGPaint.setStrokeCap(Paint.Cap.ROUND);

                EndDotdrawPaint.setColor(Color.parseColor("#c9c9c9"));
                EndDotdrawPaint.setAntiAlias(true);
                EndDotdrawPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth) * 2.5f);
                EndDotdrawPaint.setStyle(Paint.Style.STROKE);
                EndDotdrawPaint.setStrokeJoin(Paint.Join.ROUND);
                EndDotdrawPaint.setStrokeCap(Paint.Cap.ROUND);

                EndDotBGPaint.setColor(Color.BLACK);
                EndDotBGPaint.setAntiAlias(true);
                EndDotBGPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth) * 4.5f);
                EndDotBGPaint.setStyle(Paint.Style.STROKE);
                EndDotBGPaint.setStrokeJoin(Paint.Join.ROUND);
                EndDotBGPaint.setStrokeCap(Paint.Cap.ROUND);

                // Calculate the drawing scale
                double Mid_Latitude = (track.getMax_Latitude() + track.getMin_Latitude()) / 2;
                double Angle_From_Equator = Math.abs(Mid_Latitude);

                Distance_Proportion = Math.cos(Math.toRadians(Angle_From_Equator));
                //Log.w("myApp", "[#] GPSApplication.java - Distance_Proportion = " + Distance_Proportion);

                DrawScale = Math.max(track.getMax_Latitude() - track.getMin_Latitude(), Distance_Proportion * (track.getMax_Longitude() - track.getMin_Longitude()));
                Lat_Offset = Size_Minus_Margins * (1 - (track.getMax_Latitude() - track.getMin_Latitude()) / DrawScale) / 2;
                Lon_Offset = Size_Minus_Margins * (1 - (Distance_Proportion * (track.getMax_Longitude() - track.getMin_Longitude()) / DrawScale)) / 2;

                MinLatitude = track.getMin_Latitude();
                MinLongitude = track.getMin_Longitude();

                asyncThumbnailThreadClass.start();
            }
        }

        private class AsyncThumbnailThreadClass extends Thread {

            public AsyncThumbnailThreadClass() {}

            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                String fname = Id + ".png";
                File file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                if (file.exists()) file.delete();

                if (DrawScale > 0) {
                    int GroupOfLocations = 50;
                    Path path = new Path();
                    List<LatLng> latlngList = new ArrayList<>();

                    //Log.w("myApp", "[#] GPSApplication.java - Thumbnailer Thread started");
                    for (int i = 0; i < NumberOfLocations; i += GroupOfLocations) {
                        latlngList.addAll(GPSDataBase.getLatLngList(Id, i, i + GroupOfLocations - 1));
                    }
                    //Log.w("myApp", "[#] GPSApplication.java - Added " + latlngList.size() + " items to Path");
                    if (!latlngList.isEmpty()) {
                        Bitmap ThumbBitmap = Bitmap.createBitmap(Size, Size, Bitmap.Config.ARGB_8888);
                        Canvas ThumbCanvas = new Canvas(ThumbBitmap);

                        for (int i = 0; i < latlngList.size(); i++) {
                            if (i == 0)
                                path.moveTo((float) (Lon_Offset + Margin + Size_Minus_Margins * ((latlngList.get(i).Longitude - MinLongitude) * Distance_Proportion / DrawScale)),
                                        (float) (-Lat_Offset + Size - (Margin + Size_Minus_Margins * ((latlngList.get(i).Latitude - MinLatitude) / DrawScale))));
                            else
                                path.lineTo((float) (Lon_Offset + Margin + Size_Minus_Margins * ((latlngList.get(i).Longitude - MinLongitude) * Distance_Proportion / DrawScale)),
                                        (float) (-Lat_Offset + Size - (Margin + Size_Minus_Margins * ((latlngList.get(i).Latitude - MinLatitude) / DrawScale))));
                        }
                        ThumbCanvas.drawPath(path, BGPaint);
                        ThumbCanvas.drawPoint((float) (Lon_Offset + Margin + Size_Minus_Margins * ((latlngList.get(latlngList.size()-1).Longitude - MinLongitude) * Distance_Proportion / DrawScale)),
                                (float) (-Lat_Offset + Size - (Margin + Size_Minus_Margins * ((latlngList.get(latlngList.size()-1).Latitude - MinLatitude) / DrawScale))), EndDotBGPaint);
                        ThumbCanvas.drawPath(path, drawPaint);
                        ThumbCanvas.drawPoint((float) (Lon_Offset + Margin + Size_Minus_Margins * ((latlngList.get(latlngList.size()-1).Longitude - MinLongitude) * Distance_Proportion / DrawScale)),
                                (float) (-Lat_Offset + Size - (Margin + Size_Minus_Margins * ((latlngList.get(latlngList.size()-1).Latitude - MinLatitude) / DrawScale))), EndDotdrawPaint);

                        try {
                            FileOutputStream out = new FileOutputStream(file);
                            //Log.w("myApp", "[#] GPSApplication.java - FileOutputStream out = new FileOutputStream(file)");
                            //boolean res = ThumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
                            ThumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
                            //Log.w("myApp", "[#] GPSApplication.java - ThumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out): " + res);
                            out.flush();
                            //Log.w("myApp", "[#] GPSApplication.java - out.flush();");
                            out.close();
                            //Log.w("myApp", "[#] GPSApplication.java - out.close();");
                        } catch (Exception e) {
                            e.printStackTrace();
                            //Log.w("myApp", "[#] GPSApplication.java - Unable to save: " + Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + fname);
                        }

                        final String FilesDir = GPSApplication.getInstance().getApplicationContext().getFilesDir().toString() + "/Thumbnails/";
                        String Filename = FilesDir + Id + ".png";
                        file = new File(Filename);
                        if (file.exists ()) {
                            Bitmap bmp = BitmapFactory.decodeFile(Filename);
                            if (bmp != null) {
                                thumbsArray.put((int)Id, bmp);
                                Log.w("myApp", "[#] GPSApplication.java - Loaded track " + Id + " thumbnail");
                            }
                        }

                        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
                    }
                }
            }
        }
    }

}
