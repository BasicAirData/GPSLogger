/*
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
import android.graphics.Bitmap;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GPSApplication extends Application implements GpsStatus.Listener, LocationListener {

    public static final float M_TO_FT = 3.280839895f;

    public static final int UM_METRIC_MS = 0;
    public static final int UM_METRIC_KMH = 1;
    public static final int UM_IMPERIAL_FPS = 8;
    public static final int UM_IMPERIAL_MPH = 9;

    public static final int STABILIZERVALUE = 3000;                 // The application discards fixes for 3000 ms (minimum)
    private static final int DEFAULTHANDLERTIMER = 5000;            // The timer for turning off GPS on exit
    private static final int GPSUNAVAILABLEHANDLERTIMER = 7000;     // The "GPS temporary unavailable" timer
    public int StabilizingSamples = 3;

    public static final int GPS_DISABLED = 0;
    public static final int GPS_OUTOFSERVICE = 1;
    public static final int GPS_TEMPORARYUNAVAILABLE = 2;
    public static final int GPS_SEARCHING = 3;
    public static final int GPS_STABILIZING = 4;
    public static final int GPS_OK = 5;

    // Preferences Variables
    // private boolean prefKeepScreenOn = true;                  // DONE in GPSActivity
    private boolean prefShowDecimalCoordinates = false;
    private int prefUM = UM_METRIC_KMH;
    private float prefGPSdistance = 0f;
    private long prefGPSupdatefrequency = 1000l;
    private boolean prefEGM96AltitudeCorrection = false;
    private double prefAltitudeCorrection = 0d;
    private boolean prefExportKML = true;
    private boolean prefExportGPX = true;
    private int prefKMLAltitudeMode = 0;
    private int prefShowTrackStatsType = 0;
    private int prefShowDirections = 0;

    private boolean PermissionsChecked = false;

    private LocationExtended PrevFix = null;
    private boolean isPrevFixRecorded = false;

    private LocationExtended PrevRecordedFix = null;

    private boolean MustUpdatePrefs = true;                     // True if preferences needs to be updated
    private long oldGPSupdatefrequency = 1000l;



    // Singleton instance
    private static GPSApplication singleton;
    public static GPSApplication getInstance(){
        return singleton;
    }

    DatabaseHandler GPSDataBase;
    private String PlacemarkDescription = "";
    private boolean Recording = false;
    private boolean PlacemarkRequest = false;
    private long OpenInGoogleEarth = -1;                    // The index to be opened with Google Earth
    private long Share = -1;                                // The index to be Shared
    private boolean isGPSLocationUpdatesActive = false;
    private boolean isGPSLoggerFolder = false;
    private int GPSStatus = GPS_SEARCHING;

    private boolean NewTrackFlag = false;                   // The variable that handle the double-click on "Track Finished"
    final Handler newtrackhandler = new Handler();
    Runnable newtrackr = new Runnable() {
        @Override
        public void run() {
            NewTrackFlag = false;
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
    private List<Track> _ArrayListTracks = new ArrayList<>();

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
            if (GPSStatus == GPS_OK) {
                GPSStatus = GPS_TEMPORARYUNAVAILABLE;
                EventBus.getDefault().post("UPDATE_FIX");
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
        startService(GPSServiceIntent);                                                    //Starting the service
        bindService(GPSServiceIntent, GPSServiceConnection, Context.BIND_AUTO_CREATE);     //Binding to the service!
        Log.w("myApp", "[#] GPSApplication.java - StartAndBindGPSService");
    }

    private void UnbindGPSService() {
        try {
            unbindService(GPSServiceConnection);                                        //Unbind to the service
            Log.w("myApp", "[#] GPSApplication.java - Service unbound");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to unbind the GPSService");
        }
    }

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

    public boolean isPermissionsChecked() {
        return PermissionsChecked;
    }

    public void setPermissionsChecked(boolean permissionsChecked) {
        PermissionsChecked = permissionsChecked;
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

    public void setOpenInGoogleEarth(long openInGoogleEarth) {
        OpenInGoogleEarth = openInGoogleEarth;
    }

    public long getOpenInGoogleEarth() {
        return OpenInGoogleEarth;
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
        List<Track> tl = new ArrayList<Track>();
        tl.addAll(_ArrayListTracks);
        return tl;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        StartAndBindGPSService();

        EventBus.getDefault().register(this);

        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);     // Location Manager

        File sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger");   // Create the Directories if not exist
        isGPSLoggerFolder = true;
        if (!sd.exists()) {
            isGPSLoggerFolder = sd.mkdir();
        }
        sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
        if (!sd.exists()) {
            isGPSLoggerFolder = sd.mkdir();
        }

        sd = new File(getApplicationContext().getFilesDir() + "/Thumbnails");
        if (!sd.exists()) {
            sd.mkdir();
        }

        EGM96 egm96 = EGM96.getInstance();                                              // Load EGM Grid
        if (egm96 != null) {
            if (!egm96.isEGMGridLoaded()) {
                egm96.LoadGridFromFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC");
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
    }


    @Override
    public void onTerminate() {
        Log.w("myApp", "[#] GPSApplication.java - onTerminate");
        EventBus.getDefault().unregister(this);
        StopAndUnbindGPSService();
        super.onTerminate();
    }

    @Subscribe
    public void onEvent(String msg) {
        if (msg.contains("TRACK_SETPROGRESS")) {
            long trackid = Long.valueOf(msg.split(" ")[1]);
            int progress = Integer.valueOf(msg.split(" ")[2]);
            if ((trackid > 0) && (progress >= 0)) {
                for (Track T : _ArrayListTracks) {
                    if (T.getId() == trackid) T.setProgress(progress);
                }
            }
        }
        if (msg.contains("TRACK_EXPORTED")) {
            long trackid = Long.valueOf(msg.split(" ")[1]);
            if (trackid > 0) {
                for (Track T : _ArrayListTracks) {
                    if (T.getId() == trackid) {
                        T.setProgress(0);
                        EventBus.getDefault().post("UPDATE_TRACKLIST");
                        if (trackid == OpenInGoogleEarth) {
                            OpenInGoogleEarth = -1;
                            File file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", T.getName() + ".kml");
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setDataAndType(Uri.fromFile(file), "application/vnd.google-earth.kml+xml");
                            startActivity(intent);
                        }
                        if (trackid == Share) {
                            Share = -1;
                            EventBus.getDefault().post("INTENT_SEND " + trackid);
                        }
                    }
                }
            }
        }
        if (msg.contains("EXPORT_TRACK")) {
            long trackid = Long.valueOf(msg.split(" ")[1]);
            Ex = new Exporter(trackid, prefExportKML, prefExportGPX, Environment.getExternalStorageDirectory() + "/GPSLogger");
            Ex.start();
        }
        if (msg.contains("SHARE_TRACK")) {
            setShare(Long.valueOf(msg.split(" ")[1]));
            Ex = new Exporter(Share, prefExportKML, prefExportGPX, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
            Ex.start();
        }
        if (msg.contains("VIEW_TRACK")) {
            setOpenInGoogleEarth(Long.valueOf(msg.split(" ")[1]));
            Ex = new Exporter(OpenInGoogleEarth, true, false, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
            Ex.start();
        }
        if (msg.equals("NEW_TRACK")) {
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_NEWTRACK";
            ast.location = null;
            AsyncTODOQueue.add(ast);
        }
        if (msg.contains("DELETE_TRACK")) {
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_" + msg;
            ast.location = null;
            AsyncTODOQueue.add(ast);
        }
        if (msg.equals("ADD_PLACEMARK")) {
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_ADDPLACEMARK";
            ast.location = _currentPlacemark;
            _currentPlacemark.setDescription(PlacemarkDescription);
            AsyncTODOQueue.add(ast);
        }
        if (msg.equals("APP_PAUSE")) {
            handler.postDelayed(r, getHandlerTimer());  // Starts the switch-off handler (delayed by HandlerTimer)
            //UnbindGPSService();
        }
        if (msg.equals("APP_RESUME")) {
            handler.removeCallbacks(r);                 // Cancel the switch-off handler
            setHandlerTimer(DEFAULTHANDLERTIMER);
            setGPSLocationUpdates(true);
            if (MustUpdatePrefs) {
                MustUpdatePrefs = false;
                LoadPreferences();
            }
            StartAndBindGPSService();
        }
        if (msg.equals("UPDATE_SETTINGS")) {
            MustUpdatePrefs = true;
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            final GpsStatus gs = this.mlocManager.getGpsStatus(null);
            int i = 0;          // In-view satellites;
            //int i_used = 0;   // TODO: Satellites used in fix; uncomment the following commented out lines to count them
            final Iterator<GpsSatellite> it = gs.getSatellites().iterator();

            while (it.hasNext()) {
                //GpsSatellite sat = it.next();
                //if (sat.usedInFix()) i_used += 1;
                it.next();
                i += 1;
            }
            _NumberOfSatellites = i;
            //_NumberOfSatellitesUsedInFix = i_used;
        } else {
            _NumberOfSatellites = 0;
            //_NumberOfSatellitesUsedInFix = 0;
        }
    }

    // ------------------------------------------------------------------------- GpsStatus.Listener
    @Override
    public void onGpsStatusChanged(final int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateSats();
                break;
        }
    }

    // --------------------------------------------------------------------------- LocationListener
    @Override
    public void onLocationChanged(Location loc) {
        if (loc != null) {      // Location data is valid
            LocationExtended eloc = new LocationExtended(loc);
            eloc.setNumberOfSatellites(getNumberOfSatellites());
            boolean ForceRecord = false;

            gpsunavailablehandler.removeCallbacks(unavailr);                            // Cancel the previous unavail countdown handler
            gpsunavailablehandler.postDelayed(unavailr, GPSUNAVAILABLEHANDLERTIMER);    // starts the unavailability timeout (in 7 sec.)

            if (GPSStatus != GPS_OK) {
                if (GPSStatus != GPS_STABILIZING) {
                    GPSStatus = GPS_STABILIZING;
                    _Stabilizer = StabilizingSamples;
                    EventBus.getDefault().post("UPDATE_FIX");
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
                    PlacemarkRequest = false;
                    EventBus.getDefault().post("UPDATE_TRACK");
                    EventBus.getDefault().post("REQUEST_ADD_PLACEMARK");
                }
                PrevFix = eloc;
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        GPSStatus = GPS_DISABLED;
        EventBus.getDefault().post("UPDATE_FIX");
    }

    @Override
    public void onProviderEnabled(String provider) {
        GPSStatus = GPS_SEARCHING;
        EventBus.getDefault().post("UPDATE_FIX");
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // This is called when the GPS status changes
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                //Log.w("myApp", "[#] GPSApplication.java - GPS Out of Service");
                gpsunavailablehandler.removeCallbacks(unavailr);            // Cancel the previous unavail countdown handler
                GPSStatus = GPS_OUTOFSERVICE;
                EventBus.getDefault().post("UPDATE_FIX");
                //Toast.makeText( getApplicationContext(), "GPS Out of Service", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                //Log.w("myApp", "[#] GPSApplication.java - GPS Temporarily Unavailable");
                gpsunavailablehandler.removeCallbacks(unavailr);            // Cancel the previous unavail countdown handler
                GPSStatus = GPS_TEMPORARYUNAVAILABLE;
                EventBus.getDefault().post("UPDATE_FIX");
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
            _ArrayListTracks.clear();
            _ArrayListTracks.addAll(GPSDataBase.getTracksList(0, ID - 1));
            if ((ID > 1) && (GPSDataBase.getTrack(ID - 1) != null)) {
                String fname = (ID - 1) +".png";
                File file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                if (!file.exists ()) Th = new Thumbnailer(ID - 1);
            }
            EventBus.getDefault().post("UPDATE_TRACKLIST");
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
        prefUM = Integer.valueOf(preferences.getString("prefUM", "0")) + Integer.valueOf(preferences.getString("prefUMSpeed", "1"));
        prefGPSdistance = Float.valueOf(preferences.getString("prefGPSdistance", "0"));
        prefEGM96AltitudeCorrection = preferences.getBoolean("prefEGM96AltitudeCorrection", false);
        prefAltitudeCorrection = Double.valueOf(preferences.getString("prefAltitudeCorrection", "0"));
            Log.w("myApp", "[#] GPSApplication.java - Manual Correction set to " + prefAltitudeCorrection + " m");
        prefExportKML = preferences.getBoolean("prefExportKML", true);
        prefExportGPX = preferences.getBoolean("prefExportGPX", true);
        prefKMLAltitudeMode = Integer.valueOf(preferences.getString("prefKMLAltitudeMode", "0"));
        prefShowTrackStatsType = Integer.valueOf(preferences.getString("prefShowTrackStatsType", "0"));
        prefShowDirections = Integer.valueOf(preferences.getString("prefShowDirections", "0"));

        oldGPSupdatefrequency = prefGPSupdatefrequency;
        prefGPSupdatefrequency = Long.valueOf(preferences.getString("prefGPSupdatefrequency", "1000"));

        // ---------------------------------------------- Update the GPS Update Frequency if needed
        if (oldGPSupdatefrequency != prefGPSupdatefrequency) updateGPSLocationFrequency();

        // ---------------------------------------------------------------- Load EGM Grid if needed
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (!egm96.isEGMGridLoaded()) {
                egm96.LoadGridFromFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC");
            }
        }

        // ------------------------------------------------------------------- Request of UI Update
        EventBus.getDefault().post("APPLY_SETTINGS");
        EventBus.getDefault().post("UPDATE_FIX");
        EventBus.getDefault().post("UPDATE_TRACK");
        EventBus.getDefault().post("UPDATE_TRACKLIST");
    }


// THE THREAD THAT DOES ASYNCHRONOUS OPERATIONS ---------------------------------------------------


    class AsyncTODO {
        String TaskType;
        LocationExtended location;
    }

    private BlockingQueue<AsyncTODO> AsyncTODOQueue = new LinkedBlockingQueue<AsyncTODO>();

    private class AsyncUpdateThreadClass extends Thread {

        Track track;
        LocationExtended locationExtended;

        public AsyncUpdateThreadClass() {}

        public void run() {

            track = _currentTrack;
            EventBus.getDefault().post("UPDATE_TRACK");
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
                    EventBus.getDefault().post("UPDATE_TRACK");
                }

                // Task: Add location to current track
                if (asyncTODO.TaskType.equals("TASK_ADDLOCATION")) {
                    locationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    locationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    _currentLocationExtended = locationExtended;
                    EventBus.getDefault().post("UPDATE_FIX");
                    track.add(locationExtended);
                    GPSDataBase.addLocationToTrack(locationExtended, track);
                    _currentTrack = track;
                    EventBus.getDefault().post("UPDATE_TRACK");
                }

                // Task: Add a placemark to current track
                if (asyncTODO.TaskType.equals("TASK_ADDPLACEMARK")) {
                    locationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    locationExtended.setDescription(asyncTODO.location.getDescription());
                    locationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    track.addPlacemark(locationExtended);
                    GPSDataBase.addPlacemarkToTrack(locationExtended, track);
                    _currentTrack = track;
                    EventBus.getDefault().post("UPDATE_TRACK");
                }

                // Task: Update current Fix
                if (asyncTODO.TaskType.equals("TASK_UPDATEFIX")) {
                    _currentLocationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    _currentLocationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    EventBus.getDefault().post("UPDATE_FIX");
                }

                if (asyncTODO.TaskType.contains("TASK_DELETE_TRACK")) {
                    Log.w("myApp", "[#] GPSApplication.java - Deleting Track ID = " + asyncTODO.TaskType.split(" ")[1]);
                    if (Integer.valueOf(asyncTODO.TaskType.split(" ")[1]) >= 0) {
                        long selectedtrackID = Integer.valueOf(asyncTODO.TaskType.split(" ")[1]);
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
                            if (found) UpdateTrackList();
                        }
                    }
                }
            }
        }
    }





// THE THREAD THAT GENERATES TRACKS THUMBNAIL -----------------------------------------------------

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
                    List<LatLng> latlngList = new ArrayList<LatLng>();

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
                            boolean res = ThumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
                            //Log.w("myApp", "[#] GPSApplication.java - ThumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out): " + res);
                            out.flush();
                            //Log.w("myApp", "[#] GPSApplication.java - out.flush();");
                            out.close();
                            //Log.w("myApp", "[#] GPSApplication.java - out.close();");
                        } catch (Exception e) {
                            e.printStackTrace();
                            //Log.w("myApp", "[#] GPSApplication.java - Unable to save: " + Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + fname);
                        }

                        EventBus.getDefault().post("UPDATE_TRACKLIST");
                    }
                }
            }
        }
    }
}
