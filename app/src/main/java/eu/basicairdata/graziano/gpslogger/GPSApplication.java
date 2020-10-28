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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.GnssStatus;
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
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GPSApplication extends Application implements LocationListener {

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

    public static final int APP_ORIGIN_NOT_SPECIFIED     = 0;
    public static final int APP_ORIGIN_GOOGLE_PLAY_STORE = 1;  // The app is installed via the Google Play Store

    public static final int JOB_TYPE_NONE       = 0;                // No operation
    public static final int JOB_TYPE_EXPORT     = 1;                // Bulk Exportation
    public static final int JOB_TYPE_VIEW       = 2;                // Bulk View
    public static final int JOB_TYPE_SHARE      = 3;                // Bulk Share
    public static final int JOB_TYPE_DELETE     = 4;                // Bulk Delete

    public static final String FLAG_RECORDING   = "flagRecording";  // The persistent Flag is set when the app is recording, in order to detect Background Crashes


    // Preferences Variables
    // private boolean prefKeepScreenOn = true;                 // DONE in GPSActivity
    private boolean prefShowDecimalCoordinates  = false;
    // private int     prefViewTracksWith          = 0;
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
    private boolean prefGPSWeekRolloverCorrected= false;
    private boolean prefShowLocalTime           = true;

    private boolean LocationPermissionChecked   = false;          // If the flag is false the GPSActivity will check for Location Permission
    private boolean isFirstRun                  = false;          // True if it is the first run of the app (the DB is empty)
    private boolean isJustStarted               = true;           // True if the application has just been started
    private boolean isMockProvider              = false;          // True if the location is from mock provider

    private LocationExtended PrevFix = null;
    private boolean isPrevFixRecorded = false;

    private MyGPSStatus myGPSStatusListener;

    private LocationExtended PrevRecordedFix = null;

    private boolean MustUpdatePrefs = true;                     // True if preferences needs to be updated

    private boolean isCurrentTrackVisible = false;
    private boolean isContextMenuShareVisible = false;          // True if "Share with ..." menu is visible
    private boolean isContextMenuViewVisible = false;           // True if "View in *" menu is visible
    private Drawable ViewInAppIcon = null;
    private String ViewInApp = "";                              // The string of default app name for "View"
                                                                // "" in case of selector

    private AppInfo TrackViewer = new AppInfo();

    // Singleton instance
    private static GPSApplication singleton;
    public static GPSApplication getInstance(){
        return singleton;
    }

    private Satellites satellites;

    DatabaseHandler GPSDataBase;
    private String PlacemarkDescription = "";
    private boolean Recording = false;
    private boolean PlacemarkRequest = false;
    private boolean isGPSLocationUpdatesActive = false;
    private int GPSStatus = GPS_SEARCHING;

    private int AppOrigin = APP_ORIGIN_NOT_SPECIFIED;       // Which package manager is used to install this app

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
    private int numberOfSatellitesTotal = 0;                // The total Number of Satellites
    private int numberOfSatellitesUsedInFix = 0;            // The Number of Satellites used in Fix

    private int GPSActivity_activeTab = 0;                  // The active tab on GPSActivity
    private int JobProgress = 0;
    private int JobsPending = 0;                            // The number of jobs to be done
    public int JobType = JOB_TYPE_NONE;                     // The type of job that is pending
    private boolean DeleteAlsoExportedFiles = false;        // When true, the deletion of some tracks will delete also the exported files of the tracks

    public int GPSActivityColorTheme;

    private int _Stabilizer = StabilizingSamples;
    private int HandlerTimer = DEFAULTHANDLERTIMER;

    private LocationExtended _currentLocationExtended = null;
    private LocationExtended _currentPlacemark = null;
    private Track _currentTrack = null;
    private List<Track> _ArrayListTracks = Collections.synchronizedList(new ArrayList<Track>());

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

    private final int MAX_ACTIVE_EXPORTER_THREADS = 3;      // The maximum number of Exporter threads to run simultaneously

    private List<ExportingTask> ExportingTaskList = new ArrayList<>();

    private AsyncPrepareTracklistContextMenu asyncPrepareTracklistContextMenu;
    private ExternalViewerChecker externalViewerChecker;
                                                            // The manager of the External Viewers

    BroadcastReceiver sReceiver = new ShutdownReceiver();   // The BroadcastReceiver for SHUTDOWN event


    // The handler that checks the progress of an exportation:
    private final int ExportingStatusCheckInterval = 16;          // The app updates the progress of exportation every 16 milliseconds
    final Handler ExportingStatusCheckHandler = new Handler();

    Runnable ExportingStatusChecker = new Runnable() {
        @Override
        public void run() {
            long Total = 0;
            long Progress = 0;
            int Exporters_Total = ExportingTaskList.size();     // The total amount of exportation into the current job
            int Exporters_Pending = 0;
            int Exporters_Running = 0;                          // The amount of exportation in progress
            int Exporters_Success = 0;                          // The amount of exportation finished with success
            int Exporters_Failed = 0;                           // The amount of exportation failed


            // Check Progress
            for (ExportingTask ET : ExportingTaskList) {
                Total += ET.getNumberOfPoints_Total();
                Progress += ET.getNumberOfPoints_Processed();
                if (ET.getStatus() == ExportingTask.STATUS_PENDING) Exporters_Pending++;
                if (ET.getStatus() == ExportingTask.STATUS_RUNNING) Exporters_Running++;
                if (ET.getStatus() == ExportingTask.STATUS_ENDED_SUCCESS) Exporters_Success++;
                if (ET.getStatus() == ExportingTask.STATUS_ENDED_FAILED) Exporters_Failed++;
            }

            // Update job progress
            if (Total != 0) {
                if (JobProgress != (int) Math.round(1000L * Progress / Total)) {        // The ProgressBar on FragmentJobProgress has android:max="1000"
                    JobProgress = (int) Math.round(1000L * Progress / Total);
                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                }
            } else {
                if (JobProgress != 0) {
                    JobProgress = 0;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                }
            }

            //Log.w("myApp", "[#] GPSApplication.java - ExportingStatusChecker running: " + 100*Progress/Total + "% - P "
            //        + Exporters_Pending + " - R " + Exporters_Running + " - S " + Exporters_Success + " - F " + Exporters_Failed);

            // Exportation Failed
            if (Exporters_Failed != 0) {
                EventBus.getDefault().post(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE);
                JobProgress = 0;
                JobsPending = 0;
                EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                return;
            }

            // Exportation Finished
            if (Exporters_Success == Exporters_Total) {
                if (JobType == JOB_TYPE_VIEW) {
                    if (!ExportingTaskList.isEmpty()) ViewTrack(ExportingTaskList.get(0));
                } else if (JobType == JOB_TYPE_SHARE) {
                    EventBus.getDefault().post(EventBusMSG.INTENT_SEND);
                } else {
                    EventBus.getDefault().post(EventBusMSG.TOAST_TRACK_EXPORTED);
                }
                JobProgress = 0;
                JobsPending = 0;
                EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                return;
            }

            // If needed, run another Exportation Thread
            if ((Exporters_Running < MAX_ACTIVE_EXPORTER_THREADS) && (Exporters_Pending > 0)) {
                for (ExportingTask ET : ExportingTaskList) {
                    if (ET.getStatus() == ExportingTask.STATUS_PENDING) {
                        //Log.w("myApp", "[#] GPSApplication.java - Run the export thread nr." + Exporters_Running + ": " + ET.getId());
                        ET.setStatus(ExportingTask.STATUS_RUNNING);
                        ExecuteExportingTask(ET);
                        break;
                    }
                }
            }

            ExportingStatusCheckHandler.postDelayed(ExportingStatusChecker, ExportingStatusCheckInterval);
        }
    };

    void startExportingStatusChecker() {
        ExportingStatusChecker.run();
    }

    void stopExportingStatusChecker() {
        ExportingStatusCheckHandler.removeCallbacks(ExportingStatusChecker);
    }


    // ------------------------------------------------------------------------------------ GPSStatus
    private class MyGPSStatus {
        private GpsStatus.Listener gpsStatusListener;
        private GnssStatus.Callback mGnssStatusListener;

        public MyGPSStatus() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mGnssStatusListener = new GnssStatus.Callback() {
                    @Override
                    public void onSatelliteStatusChanged(GnssStatus status) {
                        super.onSatelliteStatusChanged (status);
                        updateGNSSStatus(status);
                    }
                };
            } else {
                gpsStatusListener = new GpsStatus.Listener() {
                    @Override
                    public void onGpsStatusChanged(int event) {
                        switch (event) {
                            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                                updateGPSStatus();
                                break;
                        }
                    }
                };
            }
        }

        public void enable() {
            if (ContextCompat.checkSelfPermission(GPSApplication.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) mlocManager.registerGnssStatusCallback(mGnssStatusListener);
                else mlocManager.addGpsStatusListener(gpsStatusListener);
            }
        }

        public void disable() {
            if (ContextCompat.checkSelfPermission(GPSApplication.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) mlocManager.unregisterGnssStatusCallback(mGnssStatusListener);
                else mlocManager.removeGpsStatusListener(gpsStatusListener);
            }
        }
    }


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

    public boolean isContextMenuShareVisible() {
        return isContextMenuShareVisible;
    }

    public boolean isContextMenuViewVisible() {
        return isContextMenuViewVisible;
    }

    public String getViewInApp() {
        return ViewInApp;
    }

    public Drawable getViewInAppIcon() {
        return ViewInAppIcon;
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

    public boolean getPrefShowLocalTime() {
        return prefShowLocalTime;
    }

    public LocationExtended getCurrentLocationExtended() {
        return _currentLocationExtended;
    }

    public void setPlacemarkDescription(String Description) {
        this.PlacemarkDescription = Description;
    }

    public Track getCurrentTrack() {
        return _currentTrack;
    }

    public int getNumberOfSatellitesTotal() {
        return numberOfSatellitesTotal;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return numberOfSatellitesUsedInFix;
    }

    public boolean getRecording() {
        return Recording;
    }

    public void setRecording(boolean recordingState) {
        PrevRecordedFix = null;
        Recording = recordingState;
        if (Recording) FlagAdd(FLAG_RECORDING);
        else FlagRemove(FLAG_RECORDING);
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

    public int getAppOrigin() {
        return AppOrigin;
    }

    public int getJobProgress() {
        return JobProgress;
    }

    public int getJobsPending() {
        return JobsPending;
    }

    public void setJobsPending(int jobsPending) {
        JobsPending = jobsPending;
    }

    public int getGPSActivity_activeTab() {
        return GPSActivity_activeTab;
    }

    public void setGPSActivity_activeTab(int GPSActivity_activeTab) {
        this.GPSActivity_activeTab = GPSActivity_activeTab;
    }

    public List<ExportingTask> getExportingTaskList() {
        return ExportingTaskList;
    }

    public void setDeleteAlsoExportedFiles(boolean deleteAlsoExportedFiles) {
        DeleteAlsoExportedFiles = deleteAlsoExportedFiles;
    }

    public boolean isJustStarted() {
        return isJustStarted;
    }

    public void setJustStarted(boolean justStarted) {
        isJustStarted = justStarted;
    }

    public ExternalViewerChecker getExternalViewerChecker() {
        return externalViewerChecker;
    }

    public AppInfo getTrackViewer() {
        return TrackViewer;
    }

    public void setTrackViewer(AppInfo trackViewer) {
        TrackViewer = trackViewer;
    }

    // ------------------------------------------------------------------------ Utility

    private void DeleteFile(String filename) {
        File file = new File(filename);
        boolean deleted;
        if (file.exists ()) {
            deleted = file.delete();
            if (deleted) Log.w("myApp", "[#] GPSApplication.java - DeleteFile: " + filename + " deleted");
            else Log.w("myApp", "[#] GPSApplication.java - DeleteFile: " + filename + " unable to delete the File");
        }
        else Log.w("myApp", "[#] GPSApplication.java - DeleteFile: " + filename + " doesn't exists");
    }


    /* NOT USED, Commented out
    private boolean FileExists(String filename) {
        File file = new File(filename);
        return file.exists ();
    } */


    // Flags are Boolean SharedPreferences that are excluded by automatic Backups

    public void FlagAdd (String flag) {
        SharedPreferences preferences_nobackup = getSharedPreferences("prefs_nobackup",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences_nobackup.edit();
        editor.putBoolean(flag, true);
        editor.commit();
    }


    public void FlagRemove (String flag) {
        SharedPreferences preferences_nobackup = getSharedPreferences("prefs_nobackup", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences_nobackup.edit();
        editor.remove(flag);
        editor.commit();
    }


    public boolean FlagExists (String flag) {
        SharedPreferences preferences_nobackup = getSharedPreferences("prefs_nobackup",Context.MODE_PRIVATE);
        return preferences_nobackup.getBoolean(flag, false);
    }


    // --------------------------------------------------------------------------------------------

    @Override
    public void onCreate() {

        AppCompatDelegate.setDefaultNightMode(Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("prefColorTheme", "2")));

        super.onCreate();

        singleton = this;

        // work around the android.os.FileUriExposedException
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        final String CHANNEL_ID = "GPSLoggerServiceChannel";

        // Create notification channel for Android >= O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null,null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // -----------------------
        // TODO: Uncomment it to run the Week Rollover Tests (For Test Purpose)
        // SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        // editor.putBoolean("prefGPSWeekRolloverCorrected", false);
        // editor.commit();
        // -----------------------

        // -----------------------
        // TODO: Uncomment it to reload the EGM Grid File (For Test Purpose)
        //File file = new File(getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
        //if (file.exists ()) file.delete();
        // -----------------------

        //EventBus eventBus = EventBus.builder().addIndex(new EventBusIndex()).build();
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        EventBus.getDefault().register(this);

        satellites = new Satellites();                                                  // Satellites

        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);     // Location Manager

        myGPSStatusListener = new MyGPSStatus();                                        // GPS Satellites

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

        try {                                                                           // Determine the app installation source
            String installer;
            installer = getApplicationContext().getPackageManager().getInstallerPackageName(getApplicationContext().getPackageName());
            if (installer.equals("com.android.vending") || installer.equals("com.google.android.feedback"))
                AppOrigin = APP_ORIGIN_GOOGLE_PLAY_STORE;                               // App installed from Google Play Store
            else AppOrigin = APP_ORIGIN_NOT_SPECIFIED;                                  // Otherwise
        } catch (Throwable e) {
            Log.w("myApp", "[#] GPSApplication.java - Exception trying to determine the package installer");
            AppOrigin = APP_ORIGIN_NOT_SPECIFIED;
        }

        GPSDataBase = new DatabaseHandler(this);                                 // Initialize the Database

        // Prepare the current track
        if (GPSDataBase.getLastTrackID() == 0) {
            GPSDataBase.addTrack(new Track());                                          // Creation of the first track if the DB is empty
            isFirstRun = true;
        }
        _currentTrack = GPSDataBase.getLastTrack();                                     // Get the last track

        asyncPrepareTracklistContextMenu = new AsyncPrepareTracklistContextMenu();

        externalViewerChecker = new ExternalViewerChecker(getApplicationContext());

        LoadPreferences();                                                              // Load Settings

        // ----------------------------------------------------------------------------------------

        asyncUpdateThread.start();

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        //Log.w("myApp", "[#] GPSApplication.java - Max available VM memory = " + (int) (Runtime.getRuntime().maxMemory() / 1024) + " kbytes");

        IntentFilter filter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        registerReceiver(sReceiver, filter);
    }


    @Override
    public void onTerminate() {
        Log.w("myApp", "[#] GPSApplication.java - onTerminate");
        EventBus.getDefault().unregister(this);
        StopAndUnbindGPSService();
        unregisterReceiver(sReceiver);
        super.onTerminate();
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
            if (!asyncPrepareTracklistContextMenu.isAlive()) {
                asyncPrepareTracklistContextMenu = new AsyncPrepareTracklistContextMenu();
                asyncPrepareTracklistContextMenu.start();
            } else Log.w("myApp", "[#] GPSApplication.java - asyncPrepareTracklistContextMenu already alive");

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


    public void onShutdown() {
        if (AsyncTODOQueue != null) {
            GPSStatus = GPS_SEARCHING;

            Log.w("myApp", "[#] GPSApplication.java - onShutdown()");
            AsyncTODO ast = new AsyncTODO();
            ast.TaskType = "TASK_SHUTDOWN";
            ast.location = null;
            AsyncTODOQueue.add(ast);

            if (asyncUpdateThread.isAlive()) {
                try {
                    Log.w("myApp", "[#] GPSApplication.java - onShutdown(): asyncUpdateThread isAlive. join...");
                    asyncUpdateThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.w("myApp", "[#] GPSApplication.java - onShutdown() InterruptedException: " + e);
                }
            }
        }
    }


    public void setGPSLocationUpdates (boolean state) {
        // Request permissions = https://developer.android.com/training/permissions/requesting.html
        if (!state && !getRecording() && isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            GPSStatus = GPS_SEARCHING;
            myGPSStatusListener.disable();
            mlocManager.removeUpdates(this);
            isGPSLocationUpdatesActive = false;
            //Log.w("myApp", "[#] GPSApplication.java - setGPSLocationUpdates = false");
        }
        if (state && !isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            myGPSStatusListener.enable();
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this); // Requires Location update
            isGPSLocationUpdatesActive = true;
            //Log.w("myApp", "[#] GPSApplication.java - setGPSLocationUpdates = true");
            if (prefGPSupdatefrequency >= 1000) StabilizingSamples = (int) Math.ceil(STABILIZERVALUE / prefGPSupdatefrequency);
            else StabilizingSamples = (int) Math.ceil(STABILIZERVALUE / 1000);
        }
    }

    public void updateGPSLocationFrequency () {
        if (isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            //Log.w("myApp", "[#] GPSApplication.java - updateGPSLocationFrequency");
            myGPSStatusListener.disable();
            mlocManager.removeUpdates(this);
            if (prefGPSupdatefrequency >= 1000) StabilizingSamples = (int) Math.ceil(STABILIZERVALUE / prefGPSupdatefrequency);
            else StabilizingSamples = (int) Math.ceil(STABILIZERVALUE / 1000);
            myGPSStatusListener.enable();
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this);
        }
    }


    public void updateGPSStatus() {
        try {
            if ((mlocManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                satellites.updateStatus(mlocManager.getGpsStatus(null));
                numberOfSatellitesTotal = satellites.getNumSatsTotal();
                numberOfSatellitesUsedInFix = satellites.getNumSatsUsedInFix();
            } else {
                numberOfSatellitesTotal = NOT_AVAILABLE;
                numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            }
        } catch (NullPointerException e) {
            numberOfSatellitesTotal = NOT_AVAILABLE;
            numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Caught NullPointerException: " + e);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateGNSSStatus(android.location.GnssStatus status) {
        try {
            if ((mlocManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                satellites.updateStatus(status);
                numberOfSatellitesTotal = satellites.getNumSatsTotal();
                numberOfSatellitesUsedInFix = satellites.getNumSatsUsedInFix();
            } else {
                numberOfSatellitesTotal = NOT_AVAILABLE;
                numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            }
        } catch (NullPointerException e) {
            numberOfSatellitesTotal = NOT_AVAILABLE;
            numberOfSatellitesUsedInFix = NOT_AVAILABLE;
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Caught NullPointerException: " + e);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }


    private void ViewTrack(ExportingTask exportingTask) {
        File file;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(TrackViewer.PackageName);
        Log.w("myApp", "[#] GPSApplication.java - ViewTrack with " + TrackViewer.PackageName);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (TrackViewer.GPX) {
            // GPX Viewer
            file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", exportingTask.getName() + ".gpx");
            if (TrackViewer.requiresFileProvider) {
                Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                getApplicationContext().grantUriPermission(TrackViewer.PackageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uri, "application/gpx+xml");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/gpx+xml");
            }
        } else if (TrackViewer.KML) {
            // KML Viewer
            file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", exportingTask.getName() + ".kml");
            if (TrackViewer.requiresFileProvider) {
                Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                getApplicationContext().grantUriPermission(TrackViewer.PackageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uri, "application/vnd.google-earth.kml+xml");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.google-earth.kml+xml");
            }
        }
        if (TrackViewer.KML || TrackViewer.GPX) {
            try {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w("myApp", "[#] GPSApplication.java - ViewTrack: Unable to view the track: " + e);
                if (!asyncPrepareTracklistContextMenu.isAlive()) {
                    asyncPrepareTracklistContextMenu = new AsyncPrepareTracklistContextMenu();
                    asyncPrepareTracklistContextMenu.start();
                } else Log.w("myApp", "[#] GPSApplication.java - asyncPrepareTracklistContextMenu already alive");
            }
        }
    }


    public ArrayList<Track> getSelectedTracks() {
        ArrayList<Track> selTracks = new ArrayList<Track>();
        synchronized(_ArrayListTracks) {
            for (Track T : _ArrayListTracks) {
                if (T.isSelected()) {
                    selTracks.add(T);
                }
            }
        }
        return (selTracks);
    }


    public int getNumberOfSelectedTracks() {
        int nsel = 0;
        synchronized(_ArrayListTracks) {
            for (Track T : _ArrayListTracks) {
                if (T.isSelected()) nsel++;
            }
        }
        return nsel;
    }


    public void DeselectAllTracks() {
        synchronized(_ArrayListTracks) {
            for (Track T : _ArrayListTracks) {
                if (T.isSelected()) {
                    T.setSelected(false);
                    EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACKLIST_DESELECT, T.getId()));
                }
            }
        }
        EventBus.getDefault().post(EventBusMSG.REFRESH_TRACKLIST);
    }


    public void LoadJob (int jobType) {
        ExportingTaskList.clear();
        synchronized(_ArrayListTracks) {
            for (Track T : _ArrayListTracks) {
                if (T.isSelected()) {
                    ExportingTask ET = new ExportingTask();
                    ET.setId(T.getId());
                    ET.setName(T.getName());
                    ET.setNumberOfPoints_Total(T.getNumberOfLocations() + T.getNumberOfPlacemarks());
                    ET.setNumberOfPoints_Processed(0);
                    ExportingTaskList.add(ET);
                }
            }
        }
        JobsPending = ExportingTaskList.size();
        JobType = jobType;
    }


    public void ExecuteExportingTask (ExportingTask exportingTask) {
        switch (JobType) {
            case JOB_TYPE_NONE:
            case JOB_TYPE_DELETE:
                break;
            case JOB_TYPE_EXPORT:
                Ex = new Exporter(exportingTask, prefExportKML, prefExportGPX, prefExportTXT, Environment.getExternalStorageDirectory() + "/GPSLogger");
                Ex.start();
                break;
            case JOB_TYPE_VIEW:
                if (TrackViewer.GPX) Ex = new Exporter(exportingTask, false, true, false, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
                else if (TrackViewer.KML) Ex = new Exporter(exportingTask, true, false, false, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
                Ex.start();
                break;
            case JOB_TYPE_SHARE:
                Ex = new Exporter(exportingTask, prefExportKML, prefExportGPX, prefExportTXT, Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
                Ex.start();
                break;
            default:
                break;
        }
    }


    public void ExecuteJob () {
        if (!ExportingTaskList.isEmpty()) {
            switch (JobType) {
                case JOB_TYPE_NONE:
                    break;
                case JOB_TYPE_DELETE:
                    String S = "TASK_DELETE_TRACKS";
                    for (ExportingTask ET : ExportingTaskList) {
                        S = S + " " + ET.getId();
                    }
                    AsyncTODO ast = new AsyncTODO();
                    ast.TaskType = S;
                    ast.location = null;
                    AsyncTODOQueue.add(ast);
                    break;
                case JOB_TYPE_EXPORT:
                case JOB_TYPE_VIEW:
                case JOB_TYPE_SHARE:
                    startExportingStatusChecker();
                    break;
                default:
                    break;
            }
        } else {
            Log.w("myApp", "[#] GPSApplication.java - Empty Job, nothing processed");
            JobProgress = 0;
            JobsPending = 0;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public Bitmap getBitmap(Drawable d) {
        if (d instanceof BitmapDrawable) {
            Log.w("myApp", "[#] GPSApplication.java - getBitmap: instanceof BitmapDrawable");
            return ((BitmapDrawable) d).getBitmap();
        } else if ((Build.VERSION.SDK_INT >= 26) && (d instanceof AdaptiveIconDrawable)) {
            Log.w("myApp", "[#] GPSApplication.java - getBitmap: instanceof AdaptiveIconDrawable");
            AdaptiveIconDrawable icon = ((AdaptiveIconDrawable) d);
            int w = icon.getIntrinsicWidth();
            int h = icon.getIntrinsicHeight();
            Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            icon.setBounds(0, 0, w, h);
            icon.draw(canvas);
            return result;
        }
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        int defaultWidth = (int) (24 * density);
        int defaultHeight = (int) (24 * density);
        Log.w("myApp", "[#] GPSApplication.java - getBitmap: !(Build.VERSION.SDK_INT >= 26) && (d instanceof AdaptiveIconDrawable)");
        return Bitmap.createBitmap(defaultWidth, defaultHeight, Bitmap.Config.ARGB_8888);
    }


    private class AsyncPrepareTracklistContextMenu extends Thread {

        public AsyncPrepareTracklistContextMenu() {
        }

        public void run() {
            isContextMenuShareVisible = false;
            isContextMenuViewVisible = false;
            ViewInApp = "";
            ViewInAppIcon = null;

            final PackageManager pm = getPackageManager();

            // ----- menu share

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType("text/xml");
            if ((intent.resolveActivity(pm) != null)) isContextMenuShareVisible = true;     // Verify the intent will resolve to at least one activity

            // ----- menu view

            externalViewerChecker.makeAppInfoList();
            String pn = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("prefTracksViewer", "");
            if (!externalViewerChecker.isEmpty()) {
                isContextMenuViewVisible = true;
                for (AppInfo ai : externalViewerChecker.getAppInfoList()) {
                    if ((ai.PackageName.equals(pn)) || (externalViewerChecker.size() == 1)) {
                        ViewInApp = ai.Label + (ai.GPX ? " (GPX)" : " (KML)");

                        // Set View Icon
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= 26) {
                            bitmap = getBitmap(ai.Icon);
                        } else {
                            bitmap = ((BitmapDrawable) ai.Icon).getBitmap();
                        }
                        ViewInAppIcon = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                                (int) (24 * getResources().getDisplayMetrics().density),
                                (int) (24 * getResources().getDisplayMetrics().density), true));
                    }
                }
            }
            else isContextMenuViewVisible = false;

            Log.w("myApp", "[#] GPSApplication.java - Tracklist ContextMenu prepared");
            EventBus.getDefault().post(EventBusMSG.UPDATE_ACTIONBAR);
        }
    }


    // --------------------------------------------------------------------------- LocationListener
    @Override
    public void onLocationChanged(Location loc) {
        //if ((loc != null) && (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
        if (loc != null) {      // Location data is valid
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {          // For API >= 18
                if ((PrevFix == null) || (loc.isFromMockProvider()!=isMockProvider)) {  // Reset the number of satellites when the provider changes between GPS and MOCK
                    isMockProvider = loc.isFromMockProvider();
                    numberOfSatellitesTotal = NOT_AVAILABLE;
                    numberOfSatellitesUsedInFix = NOT_AVAILABLE;
                    if (isMockProvider) Log.w("myApp", "[#] GPSApplication.java - Provider Type = MOCK PROVIDER");
                    else Log.w("myApp", "[#] GPSApplication.java - Provider Type = GPS PROVIDER");
                }
            }

            //Log.w("myApp", "[#] GPSApplication.java - onLocationChanged: provider=" + loc.getProvider());
            if (loc.hasSpeed() && (loc.getSpeed() == 0)) loc.removeBearing();           // Removes bearing if the speed is zero
            // --------- Workaround for old GPS that are affected to Week Rollover
            //loc.setTime(loc.getTime() - 619315200000L);                               // Commented out, it simulate the old GPS hardware Timestamp
            if (loc.getTime() <= 1388534400000L)                                        // if the Location Time is <= 01/01/2014 00:00:00.000
                loc.setTime(loc.getTime() + 619315200000L);                             // Timestamp incremented by 1024×7×24×60×60×1000 = 619315200000 ms
                                                                                        // This value must be doubled every 1024 weeks !!!
            LocationExtended eloc = new LocationExtended(loc);
            eloc.setNumberOfSatellites(getNumberOfSatellitesTotal());
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
                    _currentPlacemark.setNumberOfSatellites(getNumberOfSatellitesTotal());
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
        List<Track> _OldArrayListTracks = new ArrayList<Track>();
        _OldArrayListTracks.addAll(_ArrayListTracks);

        if (ID > 0) {
            synchronized(_ArrayListTracks) {
                // Save Selections
                ArrayList <Long> SelectedT = new ArrayList<>();
                for (Track T : _ArrayListTracks) {
                    if (T.isSelected()) SelectedT.add(T.getId());
                }

                // Update the List
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

                // Restore the selection state
                for (Track T : _ArrayListTracks) {
                    for (Long SelT : SelectedT) {
                        if (SelT == T.getId()) {
                            T.setSelected(true);
                            break;
                        }
                    }
                }
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

        // ---------Remove the prefIsStoragePermissionChecked in preferences if present

        if (preferences.contains("prefIsStoragePermissionChecked")) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("prefIsStoragePermissionChecked");
            editor.commit();
        }

        // -----------------------------------------------------------------------

        //prefKeepScreenOn = preferences.getBoolean("prefKeepScreenOn", true);
        prefGPSWeekRolloverCorrected = preferences.getBoolean("prefGPSWeekRolloverCorrected", false);
        prefShowDecimalCoordinates = preferences.getBoolean("prefShowDecimalCoordinates", false);
        prefShowLocalTime = preferences.getBoolean("prefShowLocalTime", true);
        //prefViewTracksWith = Integer.valueOf(preferences.getString("prefViewTracksWith", "0"));
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

        long oldGPSupdatefrequency = prefGPSupdatefrequency;
        prefGPSupdatefrequency = Long.valueOf(preferences.getString("prefGPSupdatefrequency", "1000"));

        // ---------------------------------------------- Update the GPS Update Frequency if needed
        if (oldGPSupdatefrequency != prefGPSupdatefrequency) updateGPSLocationFrequency();

        // ---------------------------------------------------------------- If no Exportation formats are enabled, enable the GPX one
        if (!prefExportKML && !prefExportGPX && !prefExportTXT) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("prefExportGPX", true);
            editor.commit();
            prefExportGPX = true;
        }

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

            // ----------------------------------------------------------------------------------------
            // Apply the GPS Week Rollover Correction, for data already stored into the DB
            // ----------------------------------------------------------------------------------------

            if (!prefGPSWeekRolloverCorrected) {
                if (!isFirstRun) {
                    Log.w("myApp", "[#] GPSApplication.java - CORRECTING DATA FOR GPS WEEK ROLLOVER");
                    GPSDataBase.CorrectGPSWeekRollover();
                    Log.w("myApp", "[#] GPSApplication.java - DATA FOR GPS WEEK ROLLOVER CORRECTED");
                    UpdateTrackList();
                    Log.w("myApp", "[#] GPSApplication.java - TRACKLIST UPDATED WITH THE CORRECTED NAMES");
                }
                prefGPSWeekRolloverCorrected = true;
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.putBoolean("prefGPSWeekRolloverCorrected", true);
                editor.commit();
            }
            // ----------------------------------------------------------------------------------------

            boolean shutdown = false;

            while (!shutdown) {
                AsyncTODO asyncTODO;
                try {
                    asyncTODO = AsyncTODOQueue.take();
                } catch (InterruptedException e) {
                    Log.w("myApp", "[!] Buffer not available: " + e.getMessage());
                    break;
                }

                // Task: Safely Shutdown
                if (asyncTODO.TaskType.equals("TASK_SHUTDOWN")) {
                    shutdown = true;
                    Log.w("myApp", "[#] GPSApplication.java - AsyncUpdateThreadClass: SHUTDOWN EVENT.");
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
                    //Log.w("myApp", "[#] GPSApplication.java - TASK_ADDLOCATION: Added new Location in " + track.getId());
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

                // Task: Delete some tracks
                if (asyncTODO.TaskType.startsWith("TASK_DELETE_TRACKS")) {

                    String STokens = asyncTODO.TaskType.substring(19);
                    List<String> tokens = new ArrayList<>();
                    StringTokenizer tokenizer = new StringTokenizer(STokens, " ");
                    while (tokenizer.hasMoreElements()) {
                        tokens.add(tokenizer.nextToken());
                    }
                    if (!tokens.isEmpty()) {
                        JobProgress = 0;
                        int TracksToBeDeleted = tokens.size();
                        int TracksDeleted = 0;
                        for (String s : tokens) {
                            Track track = null;                 // The track found in the _ArrayListTracks
                            int i = Integer.valueOf(s);
                            if (i != _currentTrack.getId()) {   // Prevent the deletion of the current track
                                synchronized (_ArrayListTracks) {
                                    for (Track T : _ArrayListTracks) {
                                        if (T.getId() == i) {
                                            track = T;
                                            GPSDataBase.DeleteTrack(i);
                                            Log.w("myApp", "[#] GPSApplication.java - TASK_DELETE_TRACKS: Track " + i + " deleted.");
                                            _ArrayListTracks.remove(T);
                                            break;
                                        }
                                    }
                                }
                                if (track != null) {
                                    // Delete track files
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + track.getName() + ".txt");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + track.getName() + ".kml");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + track.getName() + ".gpx");
                                    DeleteFile(getApplicationContext().getFilesDir() + "/Thumbnails/" + track.getId() + ".png");
                                    if (DeleteAlsoExportedFiles) {
                                        // Delete exported files
                                        DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".txt");
                                        DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".kml");
                                        DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".gpx");
                                    }

                                    TracksDeleted++;
                                    JobProgress = (int) Math.round(1000L * TracksDeleted / TracksToBeDeleted);
                                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                                    if (JobsPending > 0) JobsPending--;
                                }
                            } else {
                                Log.w("myApp", "[#] GPSApplication.java - TASK_DELETE_TRACKS: Unable to delete the current track!");
                                TracksDeleted++;
                                JobProgress = (int) Math.round(1000L * TracksDeleted / TracksToBeDeleted);
                                EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                                if (JobsPending > 0) JobsPending--;
                            }
                        }
                    }
                    JobProgress = 0;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                    EventBus.getDefault().post(EventBusMSG.NOTIFY_TRACKS_DELETED);
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
                drawPaint.setColor(getResources().getColor(R.color.colorThumbnailLineColor));
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

                EndDotdrawPaint.setColor(getResources().getColor(R.color.colorThumbnailLineColor));
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
                    int GroupOfLocations = 200;
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

                        EventBus.getDefault().post(EventBusMSG.REFRESH_TRACKLIST);
                    }
                }
            }
        }
    }
}
