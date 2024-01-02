/*
 * GPSApplication - Java Class for Android
 * Created by G.Capelli on 20/5/2016
 * This file is part of BasicAirData GPS Logger
 *
 * Copyright (C) 2011 BasicAirData
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
import android.app.ActivityManager;
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
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GPSApplication extends Application implements LocationListener {

    //private static final float M_TO_FT = 3.280839895f;
    public static final int NOT_AVAILABLE = -100000;

    private static final int STABILIZER_TIME = 3000;                // The application discards fixes for 3000 ms (minimum)
    private static final int DEFAULT_SWITCHOFF_HANDLER_TIME = 5000; // Default time for turning off GPS on exit
    private static final int GPS_UNAVAILABLE_HANDLER_TIME = 7000;   // The "GPS temporary unavailable" time

    private static final int MAX_ACTIVE_EXPORTER_THREADS = 3;       // The maximum number of Exporter threads to run simultaneously
    private static final int EXPORTING_STATUS_CHECK_INTERVAL = 16;  // The app updates the progress of exportation every 16 milliseconds

    public static final int GPS_DISABLED                = 0;
    public static final int GPS_OUTOFSERVICE            = 1;
    public static final int GPS_TEMPORARYUNAVAILABLE    = 2;
    public static final int GPS_SEARCHING               = 3;
    public static final int GPS_STABILIZING             = 4;
    public static final int GPS_OK                      = 5;

    public static final int JOB_TYPE_NONE       = 0;                // No operation
    public static final int JOB_TYPE_EXPORT     = 1;                // Bulk Exportation
    public static final int JOB_TYPE_VIEW       = 2;                // Bulk View
    public static final int JOB_TYPE_SHARE      = 3;                // Bulk Share
    public static final int JOB_TYPE_DELETE     = 4;                // Bulk Delete

    private static final String TASK_SHUTDOWN       = "TASK_SHUTDOWN";      // The AsyncTodo Type to Shut down the DB connection
    private static final String TASK_NEWTRACK       = "TASK_NEWTRACK";      // The AsyncTodo Type to create a new track into DB
    private static final String TASK_ADDLOCATION    = "TASK_ADDLOCATION";   // The AsyncTodo Type to create a new track into DB
    private static final String TASK_ADDPLACEMARK   = "TASK_ADDPLACEMARK";  // The AsyncTodo Type to create a new placemark into DB
    private static final String TASK_UPDATEFIX      = "TASK_UPDATEFIX";     // The AsyncTodo Type to update the current FIX
    private static final String TASK_DELETETRACKS   = "TASK_DELETETRACKS";  // The AsyncTodo Type to delete some tracks

    public static final String FLAG_RECORDING       = "flagRecording";      // The persistent Flag is set when the app is recording, in order to detect Background Crashes
    public static final String FILETYPE_KML         = ".kml";
    public static final String FILETYPE_GPX         = ".gpx";

    private static final float[] NEGATIVE = {
            -1.0f,      0,      0,     0,  248,         // red
                0,  -1.0f,      0,     0,  248,         // green
                0,      0,  -1.0f,     0,  248,         // blue
                0,      0,      0, 1.00f,    0          // alpha
    };

    public static final ColorMatrixColorFilter colorMatrixColorFilter
            = new ColorMatrixColorFilter(NEGATIVE);              // The color filter for Track thumbnails

    public static int TOAST_VERTICAL_OFFSET ;                    // The Y offset, in dp, for Toasts

    public static String DIRECTORY_TEMP;                         // The directory to store temporary tracks = getCacheDir() + "/Tracks"
    public static String DIRECTORY_FILESDIR_TRACKS;              // The directory that contains the empty gpx and kml file = getFilesDir() + "/URI"
    public static String FILE_EMPTY_GPX;                         // The full path of a empty GPX file
    public static String FILE_EMPTY_KML;                         // The full path of a empty KML file

    // Preferences Variables
    private boolean prefShowDecimalCoordinates;                  // If true the coordinates are shows in decimal notation
    private int     prefUM                      = PhysicalDataFormatter.UM_METRIC;     // The units of measurement to use for visualization
    private int     prefUMOfSpeed               = PhysicalDataFormatter.UM_SPEED_KMH;  // The units of measurement to use for visualization of the speeds
    private float   prefGPSdistance             = 0f;            // The distance filter value
    private float   prefGPSinterval             = 0f;            // The interval filter value
    private long    prefGPSupdatefrequency      = 1000L;         // The GPS Update frequency in milliseconds
    private boolean prefEGM96AltitudeCorrection;                 // True if the EGM96 altitude correction is active
    private double  prefAltitudeCorrection      = 0d;            // The manual offset for the altitude correction, in meters
    private boolean prefExportKML               = true;          // If true the KML file are exported on Share/Export
    private boolean prefExportGPX               = true;          // If true the GPX file are exported on Share/Export
    private int     prefGPXVersion              = 100;           // The version of the GPX schema
    private boolean prefExportTXT;                               // If true the TXT file are exported on Share/Export
    private int     prefKMLAltitudeMode         = 0;             // The altitude mode for KML files: 1="clampToGround"; 0="absolute"
    private int     prefShowTrackStatsType      = 0;             // What shown stats are based on: 0="Total time"; 1="Time in movement"
    private int     prefShowDirections          = 0;             // Visualization of headings: 0="NSWE"; 1="Degrees"
    private boolean prefGPSWeekRolloverCorrected;                // A flag for Week Rollover correction
    private boolean prefShowLocalTime           = true;          // I true the app shows GPS Time instead of local time
    private String  prefExportFolder            = "";            // The folder for tracks exportation

    private boolean mustUpdatePrefs             = true;          // True if preferences needs to be updated

    private boolean isLocationPermissionChecked;                 // If the flag is false the GPSActivity will check for Location Permission
    private boolean isFirstRun;                                  // True if it is the first run of the app (the DB is empty)
    private boolean isJustStarted               = true;          // True if the application has just been started
    private boolean isMockProvider;                              // True if the location is from mock provider
    private boolean isScreenOn                  = true;          // True if the screen of the device is ON
    private boolean isBackgroundActivityRestricted;              // True if the App is Background Restricted
    private boolean isBatteryOptimisedWarningVisible = true;     // True if the App shows the warning when the battery optimisation is active

    private LocationExtended prevFix            = null;          // The previous fix
    private LocationExtended prevRecordedFix    = null;          // The previous recorded fix
    private boolean isPrevFixRecorded;                           // true if the previous fix has been recorded
    private boolean isFirstFixFound;                             // True if at less one fix has been obtained
    private int isAccuracyDecimalCounter        = 0;             // 0 = The GPS has accuracy rounded to the meter (not precise antennas)

    private MyGPSStatus gpsStatusListener;                       // The listener for the GPS Status changes events

    private boolean isCurrentTrackVisible;                       // If true the current track is visible in Tracklist
    private boolean isContextMenuShareVisible;                   // True if "Share with ..." menu is visible
    private boolean isContextMenuViewVisible;                    // True if "View in *" menu is visible
    private Drawable viewInAppIcon = null;                       // The icon of the default app used as viewer
    private String viewInApp = "";                               // The string of default app name for "View"; "" in case of selector

    private boolean isSpaceForExtraTilesAvailable = true;        // True if there is space to show Time and Satellites in GPS Fix Tab;

    // Variables for multiple selection on Tracklist
    private long lastClickId = NOT_AVAILABLE;                    // The last item clicked on Tracklist
    private boolean lastClickState;                              // The state of the last item clicked on Tracklist

    private ExternalViewer trackViewer = new ExternalViewer();   // The class that makes and manage the list of external track viewers
    private final Satellites satellites = new Satellites();      // The class that contains all the information about satellites
    DatabaseHandler gpsDataBase;                                 // The handler for the GPSLogger Database of Tracks

    private String placemarkDescription = "";                    // The description of the Placemark (annotation) set by PlacemarkDialog
    private boolean isPlacemarkRequested;                        // True if the user requested to add a placemark (Annotation)
    private boolean isQuickPlacemarkRequest;                     // True if the user requested to add a placemark in a quick way (no annotation dialog)
    private boolean isRecording;                                 // True if the recording is active
    private boolean isBottomBarLocked;                           // True if the bottom bar is locked
    private boolean isGPSLocationUpdatesActive;                  // True if the Location Manager is active (is requesting FIXes)
    private int gpsStatus = GPS_SEARCHING;                       // The status of the GPS: GPS_DISABLED, GPS_OUTOFSERVICE,
                                                                 // GPS_TEMPORARYUNAVAILABLE, GPS_SEARCHING, GPS_STABILIZING;
    private LocationManager locationManager = null;              // GPS LocationManager
    private int numberOfSatellitesTotal = 0;                     // The total Number of Satellites
    private int numberOfSatellitesUsedInFix = 0;                 // The Number of Satellites used in Fix

    private int gpsActivityActiveTab = 1;                       // The active tab on GPSActivity
    private int jobProgress = 0;
    private int jobsPending = 0;                                 // The number of jobs to be done
    public int jobType = JOB_TYPE_NONE;                          // The type of job that is pending

    private int numberOfStabilizationSamples = 3;
    private int stabilizer = numberOfStabilizationSamples;       // The number of stabilization FIXes before the first valid Location
    private int handlerTime = DEFAULT_SWITCHOFF_HANDLER_TIME;              // The time for the GPS update requests deactivation

    private LocationExtended currentLocationExtended = null;     // The current Location
    private LocationExtended currentPlacemark = null;            // The location used to add the Placemark (Annotation)
    private Track currentTrack = null;                           // The current track. Used for adding Trackpoints and Annotations
    private Track trackToEdit = null;                            // The Track that the user selected to edit with the "Track Properties" Dialog
    private int selectedTrackTypeOnDialog = NOT_AVAILABLE;       // The Activity type selected into the Edit Details dialog.
                                                                 // It is a temporary variable, it is reset at every dialog opening

    private final List<Track> arrayListTracks
            = Collections.synchronizedList(new ArrayList<Track>());             // The list of Tracks

    private final List<ExportingTask> exportingTaskList
            = new ArrayList<>();                                 // The list of Exporting Tasks

    private AsyncPrepareActionmodeToolbar asyncPrepareActionmodeToolbar;  // Prepares the Action Mode menu asynchronously
    private ExternalViewerChecker externalViewerChecker;                        // The manager of the External Viewers
    BroadcastReceiver broadcastReceiver = new ActionsBroadcastReceiver();       // The BroadcastReceiver for SHUTDOWN and SCREEN_ON/OFF events

    Thumbnailer thumbnailer;                                     // It creates the Thumbnails of the Tracks asynchronously
    Exporter exporter;                                           // It exports the Tracks
    private final AsyncUpdateThreadClass asyncUpdateThread = new AsyncUpdateThreadClass();

    // ---------------------------------------------------------------------- Singleton instance

    private static GPSApplication singleton;
    public static GPSApplication getInstance(){
        return singleton;
    }

    // ---------------------------------------------------------------------- Handlers and Runnables

    // The Handler that prevents a double click of the Stop button of the bottom bar
    private boolean isStopButtonFlag;                         // True if the Stop button has been clicked
    private final Handler stopButtonHandler = new Handler();
    private final Runnable stopButtonRunnable = new Runnable() {
        @Override
        public void run() {
            isStopButtonFlag = false;
            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
        }
    };

    // The Handler that switches off the location updates after a time delay:
    private final Handler disableLocationUpdatesHandler = new Handler();
    private final Runnable disableLocationUpdatesRunnable = new Runnable() {
        @Override
        public void run() {
            setGPSLocationUpdates(false);
        }
    };

    // The Handler that try to enable location updates after a time delay.
    // It is used when the GPS provider is not available, to periodically check
    // if there is a new one available (for example when a Bluetooth GPS antenna is connected)
    private final Handler enableLocationUpdatesHandler = new Handler();
    private final Runnable enableLocationUpdatesRunnable = new Runnable() {
        @Override
        public void run() {
            setGPSLocationUpdates(false);
            setGPSLocationUpdates(true);
        }
    };

    // The Handler that sets the GPS Status to GPS_TEMPORARYUNAVAILABLE
    private final Handler gpsUnavailableHandler = new Handler();
    private final Runnable gpsUnavailableRunnable = new Runnable() {
        @Override
        public void run() {
            if ((gpsStatus == GPS_OK) || (gpsStatus == GPS_STABILIZING)) {
                gpsStatus = GPS_TEMPORARYUNAVAILABLE;
                stabilizer = numberOfStabilizationSamples;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            }
        }
    };

    // The Handler that checks the progress of an exportation
    private final Handler exportingStatusCheckHandler = new Handler();
    private final Runnable exportingStatusCheckRunnable = new Runnable() {
        @Override
        public void run() {
            long total = 0;
            long progress = 0;
            int exportersTotal = exportingTaskList.size();      // The total amount of exportation into the current job
            int exportersPending = 0;
            int exportersRunning = 0;                           // The amount of exportation in progress
            int exportersSuccess = 0;                           // The amount of exportation finished with success
            int exportersFailed = 0;                            // The amount of exportation failed

            // Check progress
            for (ExportingTask et : exportingTaskList) {
                total += et.getNumberOfPoints_Total();
                progress += et.getNumberOfPoints_Processed();
                if (et.getStatus() == ExportingTask.STATUS_PENDING) exportersPending++;
                if (et.getStatus() == ExportingTask.STATUS_RUNNING) exportersRunning++;
                if (et.getStatus() == ExportingTask.STATUS_ENDED_SUCCESS) exportersSuccess++;
                if (et.getStatus() == ExportingTask.STATUS_ENDED_FAILED) exportersFailed++;
            }

            // Update job progress
            if (total != 0) {
                if (jobProgress != (int) Math.round(1000L * progress / total)) {        // The ProgressBar on FragmentJobProgress has android:max="1000"
                    jobProgress = (int) Math.round(1000L * progress / total);
                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                }
            } else {
                if (jobProgress != 0) {
                    jobProgress = 0;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                }
            }

            //Log.w("myApp", "[#] GPSApplication.java - ExportingStatusChecker running: " + 100*progress/total + "% - P "
            //        + exportersPending + " - R " + exportersRunning + " - S " + exportersSuccess + " - F " + exportersFailed);

            // Exportation Failed
            if (exportersFailed != 0) {
                EventBus.getDefault().post(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE);
                if ((jobType == JOB_TYPE_EXPORT) && (getPrefExportFolder().startsWith("content://"))) {
                    Log.w("myApp", "[#] GPSApplication.java - Unable to export into " + getPrefExportFolder()
                                    + ". Preference reset");
                    GPSApplication.getInstance().setPrefExportFolder("");
                }
                jobProgress = 0;
                jobsPending = 0;
                EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                return;
            }

            // Exportation Finished
            if (exportersSuccess == exportersTotal) {
                if (jobType == JOB_TYPE_VIEW) {
                    if (!exportingTaskList.isEmpty()) viewTrack(exportingTaskList.get(0));
                } else if (jobType == JOB_TYPE_SHARE) {
                    EventBus.getDefault().post(EventBusMSG.INTENT_SEND);
                } else {
                    EventBus.getDefault().post(EventBusMSG.TOAST_TRACK_EXPORTED);
                }
                jobProgress = 0;
                jobsPending = 0;
                EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                return;
            }

            // If needed, run another Exportation Thread
            if ((exportersRunning < MAX_ACTIVE_EXPORTER_THREADS) && (exportersPending > 0)) {
                for (ExportingTask et : exportingTaskList) {
                    if (et.getStatus() == ExportingTask.STATUS_PENDING) {
                        //Log.w("myApp", "[#] GPSApplication.java - Run the export thread nr." + exportersRunning + ": " + et.getId());
                        et.setStatus(ExportingTask.STATUS_RUNNING);
                        executeExportingTask(et);
                        break;
                    }
                }
            }

            exportingStatusCheckHandler.postDelayed(exportingStatusCheckRunnable, EXPORTING_STATUS_CHECK_INTERVAL);
        }
    };

    // ----------------------------------------------------------------------  GPSStatus

    /**
     * The Class that manages the GPS Status, using the appropriate methods
     * depending on the Android Version.
     * - For VERSION_CODES > N it uses the new GnssStatus.Callback;
     * - For older Android it uses the legacy GpsStatus.Listener;
     */
    private class MyGPSStatus {
        private GpsStatus.Listener gpsStatusListener;
        private GnssStatus.Callback mGnssStatusListener;

        public MyGPSStatus() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mGnssStatusListener = new GnssStatus.Callback() {
                    @Override
                    public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
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

        /**
         * Enables the GPS Status listener
         */
        public void enable() {
            if (ContextCompat.checkSelfPermission(GPSApplication.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locationManager.registerGnssStatusCallback(mGnssStatusListener);
                else locationManager.addGpsStatusListener(gpsStatusListener);
            }
        }

        /**
         * Disables the GPS Status listener
         */
        public void disable() {
            if (ContextCompat.checkSelfPermission(GPSApplication.getInstance(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locationManager.unregisterGnssStatusCallback(mGnssStatusListener);
                else locationManager.removeGpsStatusListener(gpsStatusListener);
            }
        }
    }

    // ---------------------------------------------------------------------- Foreground Service

    Intent gpsServiceIntent;                            // The intent for GPSService
    GPSService gpsService;                              // The Foreground Service that keeps the app alive in Background
    boolean isGPSServiceBound = false;                  // True if the GPSService is bound

    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            GPSService.LocalBinder binder = (GPSService.LocalBinder) service;
            gpsService = binder.getServiceInstance();                     //Get instance of your service!
            Log.w("myApp", "[#] GPSApplication.java - GPSSERVICE CONNECTED - onServiceConnected event");
            isGPSServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w("myApp", "[#] GPSApplication.java - GPSSERVICE DISCONNECTED - onServiceDisconnected event");
            isGPSServiceBound = false;
        }
    };

    /**
     * Starts and Binds to the Foreground Service GPSService
     */
    private void startAndBindGPSService() {
        gpsServiceIntent = new Intent(GPSApplication.this, GPSService.class);
        //Start the service
        startService(gpsServiceIntent);
        //Bind to the service
        bindService(gpsServiceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        Log.w("myApp", "[#] GPSApplication.java - StartAndBindGPSService");
    }

    /**
     * Stops and Unbinds to the Foreground Service GPSService
     */
    public void stopAndUnbindGPSService() {
        try {
            unbindService(gpsServiceConnection);                                        //Unbind to the service
            Log.w("myApp", "[#] GPSApplication.java - Service unbound");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to unbind the GPSService");
        }
        try {
            stopService(gpsServiceIntent);                                                  //Stop the service
            Log.w("myApp", "[#] GPSApplication.java - Service stopped");
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Unable to stop GPSService");
        }
    }

    // ----------------------------------------------------------------------  Getters and Setters

    public boolean isStopButtonFlag() {
        return isStopButtonFlag;
    }

    public void setStopButtonFlag(boolean stopFlag, long millis) {
        if (stopFlag) {
            this.isStopButtonFlag = true;
            stopButtonHandler.removeCallbacks(stopButtonRunnable);           // Cancel the previous handler
            stopButtonHandler.postDelayed(stopButtonRunnable, millis);       // starts the new handler
        } else {
            this.isStopButtonFlag = false;
            stopButtonHandler.removeCallbacks(stopButtonRunnable);           // Cancel the previous handler
        }
    }

    public boolean isContextMenuShareVisible() {
        return isContextMenuShareVisible;
    }

    public boolean isContextMenuViewVisible() {
        return isContextMenuViewVisible;
    }

    public String getViewInApp() {
        return viewInApp;
    }

    public Drawable getViewInAppIcon() {
        return viewInAppIcon;
    }

    public boolean isLocationPermissionChecked() {
        return isLocationPermissionChecked;
    }

    public void setLocationPermissionChecked(boolean locationPermissionChecked) {
        isLocationPermissionChecked = locationPermissionChecked;
    }

    public long getLastClickId() {
        return lastClickId;
    }

    public void setLastClickId(long lastClickId) {
        this.lastClickId = lastClickId;
    }

    public boolean getLastClickState() {
        return lastClickState;
    }

    public void setLastClickState(boolean lastClickState) {
        this.lastClickState = lastClickState;
    }

    public void setHandlerTime(int handlerTime) {
        this.handlerTime = handlerTime;
    }

    public int getHandlerTime() {
        return handlerTime;
    }

    public int getGPSStatus() {
        return gpsStatus;
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

    public int getSelectedTrackTypeOnDialog() {
        return selectedTrackTypeOnDialog;
    }

    public void setSelectedTrackTypeOnDialog(int selectedTrackTypeOnDialog) {
        this.selectedTrackTypeOnDialog = selectedTrackTypeOnDialog;
    }

    public boolean isAccuracyDecimal() {
        return (isAccuracyDecimalCounter != 0);
    }

    public int getPrefUM() {
        return prefUM;
    }

    public int getPrefUMOfSpeed() {
        return prefUMOfSpeed;
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

    public String getPrefExportFolder() {
        return prefExportFolder;
    }

    public void setPrefExportFolder(String folder) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("prefExportFolder", folder);
        editor.commit();
        prefExportFolder = folder;
        Log.w("myApp", "[#] GPSApplication.java - prefExportFolder = " + folder);
    }

    public LocationExtended getCurrentLocationExtended() {
        return currentLocationExtended;
    }

    public void setPlacemarkDescription(String Description) {
        this.placemarkDescription = Description;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public int getNumberOfSatellitesTotal() {
        return numberOfSatellitesTotal;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return numberOfSatellitesUsedInFix;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recordingState) {
        prevRecordedFix = null;
        isRecording = recordingState;
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
        if (isRecording) addPreferenceFlag_NoBackup(FLAG_RECORDING);
        else clearPreferenceFlag_NoBackup(FLAG_RECORDING);
    }

    public boolean isPlacemarkRequested() { return isPlacemarkRequested; }

    public void setPlacemarkRequested(boolean placemarkRequested) {
        this.isPlacemarkRequested = placemarkRequested;
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
    }

    public void setQuickPlacemarkRequest(boolean quickPlacemarkRequest) {
        isQuickPlacemarkRequest = quickPlacemarkRequest;
    }

    public boolean isBottomBarLocked() {
        return isBottomBarLocked;
    }

    public void setBottomBarLocked(boolean locked) {
        isBottomBarLocked = locked;
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
    }

    public List<Track> getTrackList() {
        return arrayListTracks;
    }

    public boolean isCurrentTrackVisible() {
        return isCurrentTrackVisible;
    }

    public void setCurrentTrackVisible(boolean currentTrackVisible) {
        isCurrentTrackVisible = currentTrackVisible;
    }

    public boolean isBackgroundActivityRestricted() {
        return isBackgroundActivityRestricted;
    }

    public boolean isBatteryOptimisedWarningVisible() {
        return isBatteryOptimisedWarningVisible;
    }

    public void setBatteryOptimisedWarningVisible(boolean batteryOptimisedWarningVisible) {
        isBatteryOptimisedWarningVisible = batteryOptimisedWarningVisible;
    }

    public int getJobProgress() {
        return jobProgress;
    }

    public int getJobsPending() {
        return jobsPending;
    }

    public int getGPSActivityActiveTab() {
        return gpsActivityActiveTab;
    }

    public void setGPSActivityActiveTab(int gpsActivityActiveTab) {
        this.gpsActivityActiveTab = gpsActivityActiveTab;
    }

    public List<ExportingTask> getExportingTaskList() {
        return exportingTaskList;
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

    public void setTrackViewer(ExternalViewer trackViewer) {
        this.trackViewer = trackViewer;
    }

    public Track getTrackToEdit() {
        return trackToEdit;
    }

    public void setTrackToEdit(Track trackToEdit) {
        this.trackToEdit = trackToEdit;
    }

    public boolean isFirstFixFound() {
        return isFirstFixFound;
    }

    public boolean isSpaceForExtraTilesAvailable() {
        return isSpaceForExtraTilesAvailable;
    }

    public void setSpaceForExtraTilesAvailable(boolean spaceForExtraTilesAvailable) {
        isSpaceForExtraTilesAvailable = spaceForExtraTilesAvailable;
    }

    // ----------------------------------------------------------------------  Utilities

    /**
     * Creates the private application folders. No permission are needed to create them.
     * - DIRECTORY_TEMP = Where the app saves the tracks to be shared or viewed
     * - getApplicationContext().getFilesDir() + "/Thumbnails" = The private folder that contains the thumbnails of the tracks
     * - DIRECTORY_FILESDIR_TRACKS = The folder that contains the empty kml and gpx
     */
    public void createPrivateFolders() {
        File sd = new File(DIRECTORY_TEMP);
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());

        sd = new File(getApplicationContext().getFilesDir() + "/Thumbnails");
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());

        sd = new File(DIRECTORY_FILESDIR_TRACKS);
        if (!sd.exists()) {
            if (sd.mkdir()) Log.w("myApp", "[#] GPSApplication.java - Folder created: " + sd.getAbsolutePath());
            else Log.w("myApp", "[#] GPSApplication.java - Unable to create the folder: " + sd.getAbsolutePath());
        } else Log.w("myApp", "[#] GPSApplication.java - Folder exists: " + sd.getAbsolutePath());
    }

    /**
     * Deletes the file with the given filename.
     *
     * @param filename The name of the file, including the full path
     */
    private void fileDelete(String filename) {
        File file = new File(filename);
        boolean deleted;
        if (file.exists ()) {
            deleted = file.delete();
            if (deleted) Log.w("myApp", "[#] GPSApplication.java - DeleteFile: " + filename + " deleted");
            else Log.w("myApp", "[#] GPSApplication.java - DeleteFile: " + filename + " unable to delete the File");
        }
        else Log.w("myApp", "[#] GPSApplication.java - DeleteFile: " + filename + " doesn't exists");
    }

    /**
     * Finds all the files in a folder that have the name starting with a specified string.
     *
     * @param path The folder to search into
     * @param nameStart The starting characters
     */
    public File[] fileFind(String path, final String nameStart) {
        File _path = new File(path);
        try {
            return _path.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String name = file.getName(); //.toLowerCase();
                    return name.startsWith(nameStart);
                }
            });
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert the input string into a valid string for a file name
     *
     * @param str The desired file name, without path and extension
     * @return a valid string for a filename, basing on the input string
     */
    private String stringToDescFileName(String str) {
        if ((str == null) || str.isEmpty()) return "";
        // Remove the \ / :  * ?  " < > |
        // Remove heading and trailing spaces
        String sName = str.substring(0, Math.min(128, str.length()))
                .replace("\\","_")
                .replace("/","_")
                .replace(":","_")
                .replace(".","_")
                .replace("*","_")
                .replace("?","_")
                .replace("\"","_")
                .replace("<","_")
                .replace(">","_")
                .replace("|","_")
                .trim();
        if (sName.isEmpty()) return "";
        else return sName;
    }

    /**
     * Extracts the filename starting from a Track, basing on name and description.
     *
     * @param track The desired Track
     * @return a valid string for a filename (without .extension)
     */
    public String getFileName(Track track) {
        if (track.getDescription().isEmpty())
            return track.getName();
        else
            // Adds the separator and returns the filename = name - description (without .extension)
            return track.getName() + " - " + stringToDescFileName(track.getDescription());
    }

    /**
     * Extracts the folder name starting from the encoded uri.
     *
     * @param uriPath The encoded URI path
     * @return the path of the folder
     */
    public String extractFolderNameFromEncodedUri(String uriPath) {
        String spath = Uri.decode(uriPath);
        String pathSeparator = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? ":" : "/";
        if (spath.contains(pathSeparator)) {
            String[] spathParts = spath.split(pathSeparator);
            return spathParts[spathParts.length - 1];
        } else return spath;
    }

    /**
     * Deletes the old files from the app's Cache.
     * It keeps clean the DIRECTORY_TEMP, that contains the tracks
     * exported for the View and the Share feature.
     *
     * @param days The minimum age of the files that will be deleted
     */
    public void deleteOldFilesFromCache(int days) {
        class AsyncClearOldCache extends Thread {

            public AsyncClearOldCache() {
            }

            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
//                while (isJustStarted) {
//                    try {
//                        Log.w("myApp", "[#] GPSApplication.java - CACHE CLEANER - Lazy wait the GPSActivity");
//                        sleep(500);                               // Lazy wait the GPSActivity
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }

                try {
                    sleep(500);                               // Wait 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.w("myApp", "[#] GPSApplication.java -  - CACHE CLEANER - Start DeleteOldFilesFromCache");
                File cacheDir = new File(DIRECTORY_TEMP);
                if (cacheDir.isDirectory()) {
                    File[] files = cacheDir.listFiles();
                    if (files == null || files.length == 0) return;
                    for (File file : files) {
                        if (null != file) {
                            long lastModified = file.lastModified();
                            if (0 < lastModified) {
                                Date lastMDate = new Date(lastModified);
                                Date today = new Date(System.currentTimeMillis());
                                if (null != lastMDate && null != today) {
                                    long diff = today.getTime() - lastMDate.getTime();
                                    long diffDays = diff / (24 * 60 * 60 * 1000);
                                    if (days <= diffDays) {
                                        try {
                                            file.delete();
                                            Log.w("myApp", "[#] GPSApplication.java - CACHE CLEANER - Cached file " + file.getName() + " has " + diffDays + " days: DELETED");
                                        } catch (Exception e) {
                                            // it does nothing
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AsyncClearOldCache asyncClearOldCache = new AsyncClearOldCache();
        asyncClearOldCache.start();
    }

    // ---------------------------------------------------------------------- Preferences Excluded from Backup
    // These are Boolean SharedPreferences that are excluded by automatic Backups

    /**
     * Adds a boolean preference (excluded from backup).
     * This kind of Preference is used to store certain Flags, like the recording state.
     *
     * @param flag The name of the flag
     */
    public void addPreferenceFlag_NoBackup(String flag) {
        SharedPreferences preferences_nobackup = getSharedPreferences("prefs_nobackup",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences_nobackup.edit();
        editor.putBoolean(flag, true);
        editor.commit();
    }

    /**
     * Removes a boolean preference (excluded from backup).
     * This kind of Preference is used to store certain Flags, like the recording state.
     *
     * @param flag The name of the flag
     */
    public void clearPreferenceFlag_NoBackup(String flag) {
        SharedPreferences preferences_nobackup = getSharedPreferences("prefs_nobackup", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences_nobackup.edit();
        editor.remove(flag);
        editor.commit();
    }

    /**
     * Checks if a boolean preference (excluded from backup) exists.
     * This kind of Preference is used to store certain Flags, like the recording state.
     *
     * @param flag The name of the flag
     */
    public boolean preferenceFlagExists(String flag) {
        SharedPreferences preferences_nobackup = getSharedPreferences("prefs_nobackup",Context.MODE_PRIVATE);
        return preferences_nobackup.getBoolean(flag, false);
    }

    // ---------------------------------------------------------------------- Class @Overrides

    @Override
    public void onCreate() {
        // Sets the night mode, basing on App Preference
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("prefColorTheme", "2")));
        // Enables the Vector Drawable from Resource support
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        super.onCreate();

        singleton = this;

        // Workaround for the android.os.FileUriExposedException
        // Commented out because, starting from v3.1.0, the app doesn't expose any "file://" URI anymore.
        //StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        //StrictMode.setVmPolicy(builder.build());

        // Creates the notification channel for Android >= O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "GPSLoggerServiceChannel",
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
//        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
//        editor.putBoolean("prefGPSWeekRolloverCorrected", false);
//        editor.commit();
        // -----------------------

        // -----------------------
        // TODO: Uncomment it to reload the EGM Grid File (For Test Purpose)
//        File file = new File(getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
//        if (file.exists ()) file.delete();
        // -----------------------

        // Starts and registers EventBus
        //EventBus eventBus = EventBus.builder().addIndex(new EventBusIndex()).build();
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        EventBus.getDefault().register(this);

        TOAST_VERTICAL_OFFSET = (int)(75 * getResources().getDisplayMetrics().density);

        DIRECTORY_TEMP = getApplicationContext().getCacheDir() + "/Tracks";
        DIRECTORY_FILESDIR_TRACKS = getApplicationContext().getFilesDir() + "/URI";
        FILE_EMPTY_GPX = DIRECTORY_FILESDIR_TRACKS + "/empty.gpx";
        FILE_EMPTY_KML = DIRECTORY_FILESDIR_TRACKS + "/empty.kml";

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);     // Location Manager
        gpsStatusListener = new MyGPSStatus();                                              // GPS Satellites

        createPrivateFolders();

        // Creates the empty GPX
        File sd = new File(FILE_EMPTY_GPX);
        if (!sd.exists()) {
            try {
                sd.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.w("myApp", "[#] GPSApplication.java - Unable to create " + sd.getAbsolutePath());
            }
        }
        // Creates the empty KML
        sd = new File(FILE_EMPTY_KML);
        if (!sd.exists()) {
            try {
                sd.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.w("myApp", "[#] GPSApplication.java - Unable to create " + sd.getAbsolutePath());
            }
        }

        // Initialize the connection with the Database
        gpsDataBase = new DatabaseHandler(this);

        // Prepare the current track
        if (gpsDataBase.getLastTrackID() == 0) {
            gpsDataBase.addTrack(new Track());                                          // Creation of the first track if the DB is empty
            isFirstRun = true;
        }
        currentTrack = gpsDataBase.getLastTrack();                                      // Get the last track

        // Init Async operations
        asyncPrepareActionmodeToolbar = new AsyncPrepareActionmodeToolbar();
        externalViewerChecker = new ExternalViewerChecker(getApplicationContext());

        // Load Settings
        LoadPreferences();

        // Starts the Thread that manages the queue of the operation on the Database
        asyncUpdateThread.start();

        // Registers the Broadcast Receiver for ACTION_SHUTDOWN, ACTION_SCREEN_OFF, and ACTION_SCREEN_ON
        IntentFilter filter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onTerminate() {
        Log.w("myApp", "[#] GPSApplication.java - onTerminate");
        EventBus.getDefault().unregister(this);
        stopAndUnbindGPSService();
        unregisterReceiver(broadcastReceiver);
        super.onTerminate();
    }

    // --------------------------------------------------------------------------- LocationListener

    @Override
    public void onLocationChanged(@NonNull Location loc) {
        //if ((loc != null) && (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
        if (loc != null) {      // Location data is valid
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {          // For API >= 18
                if ((prevFix == null) || (loc.isFromMockProvider() != isMockProvider)) {  // Reset the number of satellites when the provider changes between GPS and MOCK
                    if (loc.isFromMockProvider() != isMockProvider) {
                        numberOfSatellitesTotal = NOT_AVAILABLE;
                        numberOfSatellitesUsedInFix = NOT_AVAILABLE;
                        isAccuracyDecimalCounter = 0;
                    }
                    isMockProvider = loc.isFromMockProvider();
                    if (isMockProvider) Log.w("myApp", "[#] GPSApplication.java - Provider Type = MOCK PROVIDER");
                    else Log.w("myApp", "[#] GPSApplication.java - Provider Type = GPS PROVIDER");
                }
            }

            if (Math.round(loc.getAccuracy()) != loc.getAccuracy())
                isAccuracyDecimalCounter = 10;                                          // Sets the visualization of the accuracy in decimal mode (>0)
            else
                isAccuracyDecimalCounter -= isAccuracyDecimalCounter > 0 ? 1 : 0;       // If the accuracy is integer for 10 samples, we start to show it rounded to the meter

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
            boolean forceRecord = false;

            gpsUnavailableHandler.removeCallbacks(gpsUnavailableRunnable);                            // Cancel the previous unavail countdown handler
            gpsUnavailableHandler.postDelayed(gpsUnavailableRunnable, GPS_UNAVAILABLE_HANDLER_TIME);  // starts the unavailability timeout (in 7 sec.)

            if (gpsStatus != GPS_OK) {
                if (gpsStatus != GPS_STABILIZING) {
                    gpsStatus = GPS_STABILIZING;
                    stabilizer = numberOfStabilizationSamples;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                }
                else stabilizer--;
                if (stabilizer <= 0) gpsStatus = GPS_OK;
                prevFix = eloc;
                prevRecordedFix = eloc;
                isPrevFixRecorded = true;
            }

            // Save fix in case this is a STOP or a START (the speed is "old>0 and new=0" or "old=0 and new>0")
            if ((prevFix != null) && (prevFix.getLocation().hasSpeed()) && (eloc.getLocation().hasSpeed()) && (gpsStatus == GPS_OK) && (isRecording)
                    && (((eloc.getLocation().getSpeed() == 0) && (prevFix.getLocation().getSpeed() != 0)) || ((eloc.getLocation().getSpeed() != 0) && (prevFix.getLocation().getSpeed() == 0)))) {
                if (!isPrevFixRecorded) {                   // Record the old sample if not already recorded
                    AsyncTODO ast = new AsyncTODO();
                    ast.taskType = TASK_ADDLOCATION;
                    ast.location = prevFix;
                    asyncTODOQueue.add(ast);
                    prevRecordedFix = prevFix;
                    isPrevFixRecorded = true;
                }
                forceRecord = true;                         // + Force to record the new
            }

            if ((isRecording) && (isPlacemarkRequested)) forceRecord = true;                                    //  Adding an annotation while recording also adds a trackpoint (issue #213)

            if (gpsStatus == GPS_OK) {
                AsyncTODO ast = new AsyncTODO();

                // Distance Filter and Interval Filter in AND
                // The Trackpoint is recorded when both filters are True.
//                if ((isRecording) && ((prevRecordedFix == null)
//                        || (forceRecord)
//                        || (((loc.getTime() - prevRecordedFix.getTime()) >= (prefGPSinterval * 1000.0f))
//                        && (loc.distanceTo(prevRecordedFix.getLocation()) >= prefGPSdistance)))) {

                // Distance Filter and Interval Filter in OR
                // The Trackpoint is recorded when at less one filter is True.
                if ((isRecording) && ((prevRecordedFix == null)
                        || (forceRecord)                                                                        // Forced to record the point
                        || ((prefGPSinterval == 0) && (prefGPSdistance == 0))                                   // No filters enabled --> it records all the points
                        || ((prefGPSinterval > 0)
                            && (prefGPSdistance > 0)                                                            // Both filters enabled, check conditions in OR
                            && (((loc.getTime() - prevRecordedFix.getTime()) >= (prefGPSinterval * 1000.0f))
                                || (loc.distanceTo(prevRecordedFix.getLocation()) >= prefGPSdistance)))
                        || ((prefGPSinterval > 0)
                            && (prefGPSdistance == 0)                                                           // Only interval filter enabled
                            && ((loc.getTime() - prevRecordedFix.getTime()) >= (prefGPSinterval * 1000.0f)))
                        || ((prefGPSinterval == 0)
                            && (prefGPSdistance > 0)                                                            // Only distance filter enabled
                            && ((loc.distanceTo(prevRecordedFix.getLocation()) >= prefGPSdistance)))
                        || (currentTrack.getNumberOfLocations() == 0))){                                        // It is the first point of a track

                    prevRecordedFix = eloc;
                    ast.taskType = TASK_ADDLOCATION;
                    ast.location = eloc;
                    asyncTODOQueue.add(ast);
                    isPrevFixRecorded = true;
                } else {
                    ast.taskType = TASK_UPDATEFIX;
                    ast.location = eloc;
                    asyncTODOQueue.add(ast);
                    isPrevFixRecorded = false;
                }
                if (isPlacemarkRequested) {
                    currentPlacemark = new LocationExtended(loc);
                    currentPlacemark.setNumberOfSatellites(getNumberOfSatellitesTotal());
                    currentPlacemark.setNumberOfSatellitesUsedInFix(getNumberOfSatellitesUsedInFix());
                    isPlacemarkRequested = false;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    if (!isQuickPlacemarkRequest) {
                        // Shows the dialog for placemark creation
                        EventBus.getDefault().post(EventBusMSG.REQUEST_ADD_PLACEMARK);
                    } else {
                        // Create a placemark, with an empty description, without showing the dialog
                        setPlacemarkDescription("");
                        EventBus.getDefault().post(EventBusMSG.ADD_PLACEMARK);
                    }
                }
                prevFix = eloc;
                isFirstFixFound = true;
            }
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        gpsStatus = GPS_DISABLED;
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        gpsStatus = GPS_SEARCHING;
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // This is called when the GPS status changes
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                //Log.w("myApp", "[#] GPSApplication.java - GPS Out of Service");
                gpsUnavailableHandler.removeCallbacks(gpsUnavailableRunnable);            // Cancel the previous unavail countdown handler
                gpsStatus = GPS_OUTOFSERVICE;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                //Toast.makeText( getApplicationContext(), "GPS Out of Service", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                //Log.w("myApp", "[#] GPSApplication.java - GPS Temporarily Unavailable");
                gpsUnavailableHandler.removeCallbacks(gpsUnavailableRunnable);            // Cancel the previous unavail countdown handler
                gpsStatus = GPS_TEMPORARYUNAVAILABLE;
                EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                //Toast.makeText( getApplicationContext(), "GPS Temporarily Unavailable", Toast.LENGTH_SHORT).show();
                break;
            case LocationProvider.AVAILABLE:
                gpsUnavailableHandler.removeCallbacks(gpsUnavailableRunnable);            // Cancel the previous unavail countdown handler
                //Log.w("myApp", "[#] GPSApplication.java - GPS Available: " + _NumberOfSatellites + " satellites");
                break;
        }
    }

    // ---------------------------------------------------------------------------

    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.NEW_TRACK) {
            AsyncTODO ast = new AsyncTODO();
            ast.taskType = TASK_NEWTRACK;
            ast.location = null;
            asyncTODOQueue.add(ast);
            return;
        }
        if (msg == EventBusMSG.ADD_PLACEMARK) {
            AsyncTODO ast = new AsyncTODO();
            ast.taskType = TASK_ADDPLACEMARK;
            ast.location = currentPlacemark;
            currentPlacemark.setDescription(placemarkDescription);
            asyncTODOQueue.add(ast);
            return;
        }
        if (msg == EventBusMSG.APP_PAUSE) {
            disableLocationUpdatesHandler.postDelayed(disableLocationUpdatesRunnable, getHandlerTime());  // Starts the switch-off handler (delayed by HandlerTimer)
            if ((currentTrack.getNumberOfLocations() == 0) && (currentTrack.getNumberOfPlacemarks() == 0)
                && (!isRecording) && (!isPlacemarkRequested)) stopAndUnbindGPSService();
            System.gc();                                // Clear mem from released objects with Garbage Collector
            return;
        }
        if (msg == EventBusMSG.APP_RESUME) {
            isScreenOn = true;
            //Log.w("myApp", "[#] GPSApplication.java - Received EventBusMSG.APP_RESUME");
            if (!asyncPrepareActionmodeToolbar.isAlive()) {
                asyncPrepareActionmodeToolbar = new AsyncPrepareActionmodeToolbar();
                asyncPrepareActionmodeToolbar.start();
            } else Log.w("myApp", "[#] GPSApplication.java - asyncPrepareActionmodeToolbar already alive");

            disableLocationUpdatesHandler.removeCallbacks(disableLocationUpdatesRunnable);                 // Cancel the switch-off handler
            setHandlerTime(DEFAULT_SWITCHOFF_HANDLER_TIME);
            setGPSLocationUpdates(true);
            if (mustUpdatePrefs) {
                mustUpdatePrefs = false;
                LoadPreferences();
            }
            startAndBindGPSService();

            // Check if the App is Background Restricted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                if ((activityManager != null) && (activityManager.isBackgroundRestricted())) {
                    isBackgroundActivityRestricted = true;
                    Log.w("myApp", "[#] GPSApplication.java - THE APP IS BACKGROUND RESTRICTED!");
                } else {
                    isBackgroundActivityRestricted = false;
                }
            } else {
                isBackgroundActivityRestricted = false;
            }
            return;
        }
        if (msg == EventBusMSG.UPDATE_SETTINGS) {
            mustUpdatePrefs = true;
            return;
        }
    }

    /**
     * Gently shuts off the thread that manages the Database, waiting the end
     * of the transaction queue.
     * This method is called by the ActionBroadcastReceiver when it
     * receives a Intent.ACTION_SHUTDOWN.
     */
    public void onShutdown() {
        gpsStatus = GPS_SEARCHING;
        Log.w("myApp", "[#] GPSApplication.java - onShutdown()");
        AsyncTODO ast = new AsyncTODO();
        ast.taskType = TASK_SHUTDOWN;
        ast.location = null;
        asyncTODOQueue.add(ast);
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

    /**
     * This method is called by the ActionBroadcastReceiver when it
     * receives a Intent.ACTION_SCREEN_OFF.
     * <p>
     * By setting isScreenOn = false, it disables the EventBus messages at every FIX,
     * in order to save battery power.
     */
    public void onScreenOff() {
        isScreenOn = false;
        Log.w("myApp", "[#] GPSApplication.java - SCREEN_OFF");
    }

    /**
     * This method is called by the ActionBroadcastReceiver when it
     * receives a Intent.ACTION_SCREEN_ON.
     * By setting isScreenOn = true, it enables the normal flow of EventBus messages.
     */
    public void onScreenOn() {
        Log.w("myApp", "[#] GPSApplication.java - SCREEN_ON");
        isScreenOn = true;
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
    }

    /**
     * Enables / Disables the GPS Location Updates
     *
     * @param state Tne state of GPS Location Updates: true = enabled; false = disabled.
     */
    public void setGPSLocationUpdates (boolean state) {
        enableLocationUpdatesHandler.removeCallbacks(enableLocationUpdatesRunnable);

        if (!state && !isRecording() && isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            gpsStatus = GPS_SEARCHING;
            gpsStatusListener.disable();
            locationManager.removeUpdates(this);
            isGPSLocationUpdatesActive = false;
            //Log.w("myApp", "[#] GPSApplication.java - setGPSLocationUpdates = false");
        }
        if (state && !isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            boolean enabled = false;
            try {
                //throw new IllegalArgumentException();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this); // Requires Location update
                enabled = true;
            } catch (IllegalArgumentException e) {
                gpsStatus = GPS_OUTOFSERVICE;
                enableLocationUpdatesHandler.postDelayed(enableLocationUpdatesRunnable, 1000);  // Starts the switch-off handler (delayed by HandlerTimer)
                Log.w("myApp", "[#] GPSApplication.java - unable to set GPSLocationUpdates: GPS_PROVIDER not available");
            }
            if (enabled) {
                // The location updates are active!
                gpsStatusListener.enable();
                isGPSLocationUpdatesActive = true;
                Log.w("myApp", "[#] GPSApplication.java - setGPSLocationUpdates = true");
                if (prefGPSupdatefrequency >= 1000)
                    numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / prefGPSupdatefrequency);
                else numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / 1000);
            }
        }
    }

    /**
     * Updates the GPS Location update frequency, basing on the value of prefGPSupdatefrequency.
     * Set prefGPSupdatefrequency to a new value before calling this in order to change
     * frequency.
     */
    public void updateGPSLocationFrequency () {
        if (isGPSLocationUpdatesActive
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            //Log.w("myApp", "[#] GPSApplication.java - updateGPSLocationFrequency");
            gpsStatusListener.disable();
            locationManager.removeUpdates(this);
            if (prefGPSupdatefrequency >= 1000) numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / prefGPSupdatefrequency);
            else numberOfStabilizationSamples = (int) Math.ceil(STABILIZER_TIME / 1000);
            gpsStatusListener.enable();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefGPSupdatefrequency, 0, this);
        }
    }

    /**
     * Updates the GPS Status for legacy Androids.
     */
    public void updateGPSStatus() {
        try {
            if ((locationManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                satellites.updateStatus(locationManager.getGpsStatus(null));
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
        if (gpsStatus != GPS_OK) {
            if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Used=" + numberOfSatellitesUsedInFix + " Total=" + numberOfSatellitesTotal);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }

    /**
     * Updates the GPS Status for new Androids (Build.VERSION_CODES >= N).
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateGNSSStatus(android.location.GnssStatus status) {
        try {
            if ((locationManager != null) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
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
        if (gpsStatus != GPS_OK) {
            if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
            //Log.w("myApp", "[#] GPSApplication.java - updateSats: Used=" + numberOfSatellitesUsedInFix + " Total=" + numberOfSatellitesTotal);
        }
        //Log.w("myApp", "[#] GPSApplication.java - updateSats: Total=" + _NumberOfSatellites + " Used=" + _NumberOfSatellitesUsedInFix);
    }

    /**
     * View a Track exported by a specific ExportingTask, using the set trackViewer.
     * The trackViewer should be set prior to call this.
     *
     * @param exportingTask the ExportingTask that exported the Track
     */
    private void viewTrack(ExportingTask exportingTask) {
        File file;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(trackViewer.packageName);
        Log.w("myApp", "[#] GPSApplication.java - ViewTrack with " + trackViewer.packageName);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (!trackViewer.fileType.isEmpty()) {
            file = new File(DIRECTORY_TEMP + "/", exportingTask.getName() + trackViewer.fileType);
            Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
            getApplicationContext().grantUriPermission(trackViewer.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, trackViewer.mimeType);
            try {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w("myApp", "[#] GPSApplication.java - ViewTrack: Unable to view the track: " + e);
                if (!asyncPrepareActionmodeToolbar.isAlive()) {
                    asyncPrepareActionmodeToolbar = new AsyncPrepareActionmodeToolbar();
                    asyncPrepareActionmodeToolbar.start();
                } else Log.w("myApp", "[#] GPSApplication.java - asyncPrepareActionmodeToolbar already alive");
            }
        }
    }

    /**
     * Updates the Tracklist (re-)reading it from the Database.
     */
    public void UpdateTrackList() {
        long ID = gpsDataBase.getLastTrackID();

        if (ID > 0) {
            synchronized(arrayListTracks) {
                // Save Selections
                ArrayList <Long> SelectedT = new ArrayList<>();
                for (Track T : arrayListTracks) {
                    if (T.isSelected()) SelectedT.add(T.getId());
                }

                // Update the List
                arrayListTracks.clear();
                arrayListTracks.addAll(gpsDataBase.getTracksList(0, ID - 1));
                if ((ID > 1) && (gpsDataBase.getTrack(ID - 1) != null)) {
                    String fname = (ID - 1) + ".png";
                    File file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                    if (!file.exists()) thumbnailer = new Thumbnailer(ID - 1);
                }
                if (currentTrack.getNumberOfLocations() + currentTrack.getNumberOfPlacemarks() > 0) {
                    Log.w("myApp", "[#] GPSApplication.java - Update Tracklist: current track (" + currentTrack.getId() + ") visible into the tracklist");
                    arrayListTracks.add(0, currentTrack);
                } else
                    Log.w("myApp", "[#] GPSApplication.java - Update Tracklist: current track not visible into the tracklist");

                // Restore the selection state
                for (Track T : arrayListTracks) {
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

    /**
     * Extracts and returns the list of the selected tracks on the Tracklist.
     *
     * @return the ArrayList of the selected tracks.
     */
    public ArrayList<Track> getSelectedTracks() {
        ArrayList<Track> selTracks = new ArrayList<>();
        synchronized(arrayListTracks) {
            for (Track T : arrayListTracks) {
                if (T.isSelected()) {
                    selTracks.add(T);
                }
            }
        }
        return (selTracks);
    }

    /**
     * Returns the number of selected tracks on the Tracklist.
     *
     * @return the number of selected tracks.
     */
    public int getNumberOfSelectedTracks() {
        int nsel = 0;
        synchronized(arrayListTracks) {
            for (Track T : arrayListTracks) {
                if (T.isSelected()) nsel++;
            }
        }
        return nsel;
    }

    /**
     * Deselects all the tracks on the Tracklist.
     */
    public void deselectAllTracks() {
        synchronized(arrayListTracks) {
            for (Track T : arrayListTracks) {
                if (T.isSelected()) {
                    T.setSelected(false);
                    EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACKLIST_DESELECT, T.getId()));
                }
            }
        }
        EventBus.getDefault().post(EventBusMSG.REFRESH_TRACKLIST);
    }

    /**
     * Starts the ExportingStatusChecker.
     * The ExportingStatusChecker regularly updates the status of the exportation,
     * updating the progressbar at the bottom of the tracklist.
     * The ExportingStatusChecker will end as soon as the last ExportingTask of
     * the ExportingTaskList is done.
     */
    void startExportingStatusChecker() {
        exportingStatusCheckRunnable.run();
    }

    /**
     * Executes the specified ExportingTask.
     *
     * @param exportingTask The ExportingTask to execute
     */
    public void executeExportingTask(ExportingTask exportingTask) {
        switch (jobType) {
            case JOB_TYPE_EXPORT:
                exporter = new Exporter(exportingTask, prefExportKML, prefExportGPX, prefExportTXT, prefExportFolder);
                exporter.start();
                break;
            case JOB_TYPE_VIEW:
                if (trackViewer.fileType.equals(FILETYPE_GPX)) exporter = new Exporter(exportingTask, false, true, false, DIRECTORY_TEMP);
                if (trackViewer.fileType.equals(FILETYPE_KML)) exporter = new Exporter(exportingTask, true, false, false, DIRECTORY_TEMP);
                exporter.start();
                break;
            case JOB_TYPE_SHARE:
                exporter = new Exporter(exportingTask, prefExportKML, prefExportGPX, prefExportTXT, DIRECTORY_TEMP);
                exporter.start();
                break;
            case JOB_TYPE_NONE:
            case JOB_TYPE_DELETE:
            default:
                break;
        }
    }

    /**
     * Loads a Job.
     * A Job is an operation to do with a set of Tracks.
     * It is described by a type (jobType) and by a list of ExportingTask.
     * Loading a Job means populate the ExportingTaskList and set a jobType.
     * <p>
     * For example, if you want to export a set of Tracks, you should set as selected
     * some tracks of the arrayListTracks (the tracks on the TrackList),
     * call this method with jobType = JOB_TYPE_EXPORT,
     * and then call executeJob.
     *
     * @param jobType The Job Type (JOB_TYPE_DELETE, JOB_TYPE_EXPORT, JOB_TYPE_VIEW...)
     */
    public void loadJob(int jobType) {
        exportingTaskList.clear();
        synchronized(arrayListTracks) {
            for (Track t : arrayListTracks) {
                if (t.isSelected()) {
                    ExportingTask et = new ExportingTask();
                    et.setId(t.getId());
                    et.setName(getFileName(t));
                    et.setNumberOfPoints_Total(t.getNumberOfLocations() + t.getNumberOfPlacemarks());
                    et.setNumberOfPoints_Processed(0);
                    exportingTaskList.add(et);
                }
            }
        }
        jobsPending = exportingTaskList.size();
        this.jobType = jobType;
    }

    /**
     * Executes a Job.
     * A Job is an operation to do with a set of Tracks.
     * It is described by a type (jobType) and by a list of ExportingTask.
     * <p>
     * For example, if you want to export a set of Tracks, you should set as selected
     * some tracks of the arrayListTracks (the tracks on the TrackList),
     * call this method with jobType = JOB_TYPE_EXPORT,
     * and then call executeJob.
     */
    public void executeJob() {
        if (!exportingTaskList.isEmpty()) {
            switch (jobType) {
                case JOB_TYPE_NONE:
                    break;
                case JOB_TYPE_DELETE:
                    String s = TASK_DELETETRACKS;
                    for (ExportingTask et : exportingTaskList) {
                        s = s + " " + et.getId();
                    }
                    AsyncTODO ast = new AsyncTODO();
                    ast.taskType = s;
                    ast.location = null;
                    asyncTODOQueue.add(ast);
                    break;
                case JOB_TYPE_EXPORT:
                case JOB_TYPE_VIEW:
                case JOB_TYPE_SHARE:
                    createPrivateFolders();
                    startExportingStatusChecker();
                    break;
                default:
                    break;
            }
        } else {
            Log.w("myApp", "[#] GPSApplication.java - Empty Job, nothing processed");
            jobProgress = 0;
            jobsPending = 0;
        }
    }

    /**
     * Gets a Bitmap starting from a Drawable.
     * It is user to extract the icon of the viewers, in order to use them into Action Mode
     * Toolbar of the Tracklist instead of the generic eye icon when a default viewer is set.
     * Starting from Android VERSION_CODES.O, the apps could have an Adaptive Icon: this method
     * obtains a bitmap usable on the toolbar.
     *
     * @param drawable The icon Drawable
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Bitmap getBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            Log.w("myApp", "[#] GPSApplication.java - getBitmap: instanceof BitmapDrawable");
            return ((BitmapDrawable) drawable).getBitmap();
        } else if ((Build.VERSION.SDK_INT >= 26) && (drawable instanceof AdaptiveIconDrawable)) {
            Log.w("myApp", "[#] GPSApplication.java - getBitmap: instanceof AdaptiveIconDrawable");
            AdaptiveIconDrawable icon = ((AdaptiveIconDrawable) drawable);
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
        Log.w("myApp", "[#] GPSApplication.java - getBitmap: !(Build.VERSION.SDK_INT >= 26) && (drawable instanceof AdaptiveIconDrawable)");
        return Bitmap.createBitmap(defaultWidth, defaultHeight, Bitmap.Config.ARGB_8888);
    }


    public boolean isExportFolderWritable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri uri = Uri.parse(prefExportFolder);
            Log.w("myApp", "[#] GPSApplication.java - isExportFolderWritable: " + prefExportFolder);

            final List<UriPermission> list = getApplicationContext().getContentResolver().getPersistedUriPermissions();
            for (final UriPermission item : list) {
                Log.w("myApp", "[#] GPSApplication.java - isExportFolderWritable check: " + item.getUri());
                if (item.getUri().equals(uri)) {
                    try {
                        DocumentFile pickedDir;
                        if (prefExportFolder.startsWith("content")) {
                            pickedDir = DocumentFile.fromTreeUri(getInstance(), uri);
                        } else {
                            pickedDir = DocumentFile.fromFile(new File(prefExportFolder));
                        }
                        if ((pickedDir == null) || (!pickedDir.exists())) {
                            Log.w("myApp", "[#] GPSApplication.java - THE EXPORT FOLDER DOESN'T EXIST");
                            return false;
                        }
                        if ((!pickedDir.canRead()) || !pickedDir.canWrite()) {
                            Log.w("myApp", "[#] GPSApplication.java - CANNOT READ/WRITE INTO THE EXPORT FOLDER");
                            return false;
                        }
                        return true;
                    } catch (IllegalArgumentException e) {
                        Log.w("myApp", "[#] GPSApplication.java - IllegalArgumentException - isExportFolderWritable = FALSE: " + item.getUri());
                    }
                }
                // Releases the unused persistable permission
                getApplicationContext().getContentResolver().releasePersistableUriPermission(item.getUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            Log.w("myApp", "[#] GPSApplication.java - isExportFolderWritable = FALSE");
            return false;
        } else {
            // Old Android 4, check that the app has the storage permission and the folder /GPSLogger exists.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                File sd = new File(prefExportFolder);
                if (!sd.exists()) {
                    return sd.mkdir();
                } else return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------------- Preferences

    /**
     * (re-)Loads the Preferences and Launch signals in order to updates the UI.
     */
    private void LoadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();

        // -----------------------
        // TODO: Uncomment it to test the conversion of prefUMSpeed into prefUMOfSpeed (For Test Purpose)
        //editor.putString("prefUMSpeed", "0").commit();
        // -----------------------

        prefUM = Integer.parseInt(preferences.getString("prefUM", "0"));

        // Conversion from the previous versions of the unit of measurement of the speeds
        if (preferences.contains("prefUMSpeed")) {       // The old setting
            Log.w("myApp", "[#] GPSApplication.java - Old setting prefUMSpeed present (" + preferences.getString("prefUMSpeed", "0") + "). Converting to new preference prefUMOfSpeed.");
            String UMspd = preferences.getString("prefUMSpeed", "0");
            switch (prefUM) {
                case PhysicalDataFormatter.UM_METRIC:
                    editor.putString("prefUMOfSpeed", (UMspd.equals("0") ? String.valueOf(PhysicalDataFormatter.UM_SPEED_MS) : String.valueOf(PhysicalDataFormatter.UM_SPEED_KMH)));
                    break;
                case PhysicalDataFormatter.UM_IMPERIAL:
                    editor.putString("prefUMOfSpeed", (UMspd.equals("0") ? String.valueOf(PhysicalDataFormatter.UM_SPEED_FPS) : String.valueOf(PhysicalDataFormatter.UM_SPEED_MPH)));
                    break;
                case PhysicalDataFormatter.UM_NAUTICAL:
                    editor.putString("prefUMOfSpeed", (UMspd.equals("0") ? String.valueOf(PhysicalDataFormatter.UM_SPEED_KN) : String.valueOf(PhysicalDataFormatter.UM_SPEED_MPH)));
                    break;
            }
            editor.remove("prefUMSpeed");
            editor.commit();
        } else prefUMOfSpeed = Integer.parseInt(preferences.getString("prefUMOfSpeed", "1"));

        // Remove the prefIsStoragePermissionChecked in preferences if present
        if (preferences.contains("prefIsStoragePermissionChecked")) {
            editor.remove("prefIsStoragePermissionChecked");
            editor.commit();
        }

        //prefKeepScreenOn = preferences.getBoolean("prefKeepScreenOn", true);
        prefGPSWeekRolloverCorrected = preferences.getBoolean("prefGPSWeekRolloverCorrected", false);
        prefShowDecimalCoordinates = preferences.getBoolean("prefShowDecimalCoordinates", false);
        prefShowLocalTime = preferences.getBoolean("prefShowLocalTime", true);

        try {
            prefGPSdistance = Float.parseFloat(preferences.getString("prefGPSdistance", "0"));
        }
        catch(NumberFormatException nfe) {
            prefGPSdistance = 0;
        }
        try {
            prefGPSinterval = Float.parseFloat(preferences.getString("prefGPSinterval", "0"));
            }
        catch(NumberFormatException nfe) {
            prefGPSinterval = 0;
        }

        Log.w("myApp", "[#] GPSApplication.java - prefGPSdistance = " + prefGPSdistance + " m");

        prefEGM96AltitudeCorrection = preferences.getBoolean("prefEGM96AltitudeCorrection", false);
        prefAltitudeCorrection = Double.parseDouble(preferences.getString("prefAltitudeCorrection", "0"));
        Log.w("myApp", "[#] GPSApplication.java - Manual Correction set to " + prefAltitudeCorrection + " m");
        prefExportKML = preferences.getBoolean("prefExportKML", true);
        prefExportGPX = preferences.getBoolean("prefExportGPX", true);
        prefExportTXT = preferences.getBoolean("prefExportTXT", false);
        prefKMLAltitudeMode = Integer.parseInt(preferences.getString("prefKMLAltitudeMode", "1"));
        prefGPXVersion = Integer.parseInt(preferences.getString("prefGPXVersion", "100"));               // Default value = v.1.0
        prefShowTrackStatsType = Integer.parseInt(preferences.getString("prefShowTrackStatsType", "0"));
        prefShowDirections = Integer.parseInt(preferences.getString("prefShowDirections", "0"));

        double altcorm = Double.parseDouble(preferences.getString("prefAltitudeCorrection", "0"));
        double altcor = preferences.getString("prefUM", "0").equals("0") ? altcorm : altcorm * PhysicalDataFormatter.M_TO_FT;
        double distfilterm = Double.parseDouble(preferences.getString("prefGPSdistance", "0"));
        double distfilter = preferences.getString("prefUM", "0").equals("0") ? distfilterm : distfilterm * PhysicalDataFormatter.M_TO_FT;
        editor.putString("prefAltitudeCorrectionRaw", String.valueOf(altcor));
        editor.putString("prefGPSdistanceRaw", String.valueOf(distfilter));
        //editor.remove("prefGPSDistanceRaw");
        editor.commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) prefExportFolder = preferences.getString("prefExportFolder", "");
        else setPrefExportFolder(Environment.getExternalStorageDirectory() + "/GPSLogger");

        long oldGPSupdatefrequency = prefGPSupdatefrequency;
        prefGPSupdatefrequency = Long.parseLong(preferences.getString("prefGPSupdatefrequency", "1000"));

        // Update the GPS Update Frequency if needed
        if (oldGPSupdatefrequency != prefGPSupdatefrequency) updateGPSLocationFrequency();

        // If no Exportation formats are enabled, enable the GPX one
        if (!prefExportKML && !prefExportGPX && !prefExportTXT) {
            editor.putBoolean("prefExportGPX", true);
            editor.commit();
            prefExportGPX = true;
        }

        // Load EGM Grid if needed
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            egm96.loadGrid(prefExportFolder, getApplicationContext().getFilesDir().toString());
        }

        // Request of UI Update
        EventBus.getDefault().post(EventBusMSG.APPLY_SETTINGS);
        EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
        EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
    }

    // ---------------------------------------------------------------------- Threads

    /**
     * The Thread that prepares the Action Mode Toolbar of the Tracklist asynchronously.
     * It evaluates the visibility of the Share button, the visibility
     * and the icon of the View button, basing on installed apps on the System.
     */
    private class AsyncPrepareActionmodeToolbar extends Thread {

        public AsyncPrepareActionmodeToolbar() {
        }

        public void run() {
            isContextMenuShareVisible = false;
            isContextMenuViewVisible = false;
            viewInApp = "";
            viewInAppIcon = null;

            final PackageManager pm = getPackageManager();

            // ----- menu share

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType("text/xml");
            if ((intent.resolveActivity(pm) != null)) isContextMenuShareVisible = true;     // Verify the intent will resolve to at least one activity

            // ----- menu view

            externalViewerChecker.makeExternalViewersList();
            String pn = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("prefTracksViewer", "");
            if (!externalViewerChecker.isEmpty()) {
                isContextMenuViewVisible = true;
                for (ExternalViewer ev : externalViewerChecker.getExternalViewersList()) {
                    if ((ev.packageName.equals(pn)) || (externalViewerChecker.size() == 1)) {
                        viewInApp = ev.label + (ev.fileType.equals(FILETYPE_GPX) ? " (GPX)" : " (KML)");

                        // Set View Icon
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= 26) {
                            bitmap = getBitmap(ev.icon);
                        } else {
                            bitmap = ((BitmapDrawable) ev.icon).getBitmap();
                        }
                        viewInAppIcon = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
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

    /**
     * The Class defines a Database transaction to be enqueued
     */
    private static class AsyncTODO {
        String taskType;
        LocationExtended location;
    }

    private final BlockingQueue<AsyncTODO> asyncTODOQueue
            = new LinkedBlockingQueue<>();                      // The FIFO for asynchronous DB operations

    /**
     * The Thread that manages and executes the Database operations asynchronously.
     * It takes one by one the elements of the asyncTODOQueue and executes them
     * in FIFO order.
     * When the asyncTODOQueue list is empty, the thread blocks waiting the next item.
     */
    private class AsyncUpdateThreadClass extends Thread {

        Track track;
        LocationExtended locationExtended;

        public AsyncUpdateThreadClass() {}

        public void run() {

            track = currentTrack;
            EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
            UpdateTrackList();

            // ----------------------------------------------------------------------------------------
            // If needed, applies the GPS Week Rollover Correction for data already stored into the DB
            if (!prefGPSWeekRolloverCorrected) {
                if (!isFirstRun) {
                    // Applies the GPS Week Rollover Correction, for data already stored into the DB
                    Log.w("myApp", "[#] GPSApplication.java - CORRECTING DATA FOR GPS WEEK ROLLOVER");
                    gpsDataBase.CorrectGPSWeekRollover();
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
                    asyncTODO = asyncTODOQueue.take();
                } catch (InterruptedException e) {
                    Log.w("myApp", "[!] Buffer not available: " + e.getMessage());
                    break;
                }

                // Task: Safely Shutdown
                if (asyncTODO.taskType.equals(TASK_SHUTDOWN)) {
                    shutdown = true;
                    Log.w("myApp", "[#] GPSApplication.java - AsyncUpdateThreadClass: SHUTDOWN EVENT.");
                }

                // Task: Create new track (if needed)
                if (asyncTODO.taskType.equals(TASK_NEWTRACK)) {
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
                        track.setId(gpsDataBase.addTrack(track));
                        Log.w("myApp", "[#] GPSApplication.java - TASK_NEWTRACK: " + track.getId());
                        currentTrack = track;
                        UpdateTrackList();
                    } else Log.w("myApp", "[#] GPSApplication.java - TASK_NEWTRACK: Track " + track.getId() + " already empty (New track not created)");
                    currentTrack = track;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                }

                // Task: Add location to current track
                if (asyncTODO.taskType.equals(TASK_ADDLOCATION)) {
                    locationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    locationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    locationExtended.setNumberOfSatellitesUsedInFix(asyncTODO.location.getNumberOfSatellitesUsedInFix());
                    currentLocationExtended = locationExtended;
                    if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                    track.add(locationExtended);
                    gpsDataBase.addLocationToTrack(locationExtended, track);
                    //Log.w("myApp", "[#] GPSApplication.java - TASK_ADDLOCATION: Added new Location in " + track.getId());
                    currentTrack = track;
                    if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    if (currentTrack.getNumberOfLocations() + currentTrack.getNumberOfPlacemarks() == 1) UpdateTrackList();
                }

                // Task: Add a placemark to current track
                if (asyncTODO.taskType.equals(TASK_ADDPLACEMARK)) {
                    locationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    locationExtended.setDescription(asyncTODO.location.getDescription());
                    locationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    locationExtended.setNumberOfSatellitesUsedInFix(asyncTODO.location.getNumberOfSatellitesUsedInFix());
                    track.addPlacemark(locationExtended);
                    gpsDataBase.addPlacemarkToTrack(locationExtended, track);
                    currentTrack = track;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                    if (currentTrack.getNumberOfLocations() + currentTrack.getNumberOfPlacemarks() == 1) UpdateTrackList();
                }

                // Task: Update current Fix
                if (asyncTODO.taskType.equals(TASK_UPDATEFIX)) {
                    currentLocationExtended = new LocationExtended(asyncTODO.location.getLocation());
                    currentLocationExtended.setNumberOfSatellites(asyncTODO.location.getNumberOfSatellites());
                    currentLocationExtended.setNumberOfSatellitesUsedInFix(asyncTODO.location.getNumberOfSatellitesUsedInFix());
                    if (isScreenOn) EventBus.getDefault().post(EventBusMSG.UPDATE_FIX);
                }

                // Task: Delete some tracks
                if (asyncTODO.taskType.startsWith(TASK_DELETETRACKS)) {

                    String sTokens = asyncTODO.taskType.substring(asyncTODO.taskType.indexOf(" ") + 1);
                    Log.w("myApp", "[#] GPSApplication.java - DELETING (" + sTokens + ")");
                    List<String> tokens = new ArrayList<>();
                    StringTokenizer tokenizer = new StringTokenizer(sTokens, " ");
                    while (tokenizer.hasMoreElements()) {
                        tokens.add(tokenizer.nextToken());
                    }
                    if (!tokens.isEmpty()) {
                        jobProgress = 0;
                        int tracksToBeDeleted = tokens.size();
                        int tracksDeleted = 0;
                        for (String s : tokens) {
                            Track track = null;                 // The track found in the _ArrayListTracks
                            int i = Integer.valueOf(s);
                            if (i != currentTrack.getId()) {   // Prevent the deletion of the current track
                                synchronized (arrayListTracks) {
                                    for (Track t : arrayListTracks) {
                                        if (t.getId() == i) {
                                            track = t;
                                            gpsDataBase.DeleteTrack(i);
                                            Log.w("myApp", "[#] GPSApplication.java - TASK_DELETE_TRACKS: Track " + i + " deleted.");
                                            arrayListTracks.remove(t);
                                            break;
                                        }
                                    }
                                }
                                if (track != null) {
                                    // Delete track files
                                    if (fileFind(DIRECTORY_TEMP, track.getName()) != null) {
                                        for (File f : fileFind(DIRECTORY_TEMP, track.getName())) {
                                            Log.w("myApp", "[#] GPSApplication.java - Deleting: " + f.getAbsolutePath());
                                            fileDelete(f.getAbsolutePath());
                                        }
                                    }
                                    // Delete thumbnail
                                    fileDelete(getApplicationContext().getFilesDir() + "/Thumbnails/" + track.getId() + ".png");

                                    tracksDeleted++;
                                    jobProgress = (int) Math.round(1000L * tracksDeleted / tracksToBeDeleted);
                                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                                    if (jobsPending > 0) jobsPending--;
                                }
                            } else {
                                Log.w("myApp", "[#] GPSApplication.java - TASK_DELETE_TRACKS: Unable to delete the current track!");
                                tracksDeleted++;
                                jobProgress = (int) Math.round(1000L * tracksDeleted / tracksToBeDeleted);
                                EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                                if (jobsPending > 0) jobsPending--;
                            }
                        }
                    }
                    jobProgress = 0;
                    EventBus.getDefault().post(EventBusMSG.UPDATE_JOB_PROGRESS);
                    EventBus.getDefault().post(EventBusMSG.NOTIFY_TRACKS_DELETED);
                }
            }
        }
    }

    /**
     * The Thread that generates the Thumbnail of the Track with the given id.
     */
    public class Thumbnailer {

        long id;
        long numberOfLocations;

        private final Paint drawPaint = new Paint();
        private final Paint bgPaint = new Paint();
        private final Paint endDotdrawPaint = new Paint();
        private final Paint endDotBGPaint = new Paint();
        private final int size = (int)(getResources().getDimension(R.dimen.thumbSize));
        private final int margin = (int) Math.ceil(getResources().getDimension(R.dimen.thumbLineWidth) * 3);
        private final int sizeMinusMargins = size - 2 * margin;

        private double minLatitude;
        private double minLongitude;

        double distanceProportion;
        double drawScale;
        double latOffset;
        double lonOffset;

        /**
         * Generates the Thumbnail of the Track with the given id into FilesDir/Thumbnails/.
         * The id will be used also to name the output png file.
         *
         * @param id The id of the Track
         */
        public Thumbnailer(long id) {

            Track track = gpsDataBase.getTrack(id);
            //Log.w("myApp", "[#] GPSApplication.java - Bitmap Size = " + Size);

            if ((track.getNumberOfLocations() > 2) && (track.getDistance() >= 15) && (track.getValidMap() != 0)) {
                this.id = track.getId();
                numberOfLocations = track.getNumberOfLocations();

                // Setup Paints
                drawPaint.setColor(getResources().getColor(R.color.colorThumbnailLineColor));
                drawPaint.setAntiAlias(true);
                drawPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth));
                //drawPaint.setStrokeWidth(2);
                drawPaint.setStyle(Paint.Style.STROKE);
                drawPaint.setStrokeJoin(Paint.Join.ROUND);
                drawPaint.setStrokeCap(Paint.Cap.ROUND);

                bgPaint.setColor(Color.BLACK);
                bgPaint.setAntiAlias(true);
                bgPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth) * 3);
                //BGPaint.setStrokeWidth(6);
                bgPaint.setStyle(Paint.Style.STROKE);
                bgPaint.setStrokeJoin(Paint.Join.ROUND);
                bgPaint.setStrokeCap(Paint.Cap.ROUND);

                endDotdrawPaint.setColor(getResources().getColor(R.color.colorThumbnailLineColor));
                endDotdrawPaint.setAntiAlias(true);
                endDotdrawPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth) * 2.5f);
                endDotdrawPaint.setStyle(Paint.Style.STROKE);
                endDotdrawPaint.setStrokeJoin(Paint.Join.ROUND);
                endDotdrawPaint.setStrokeCap(Paint.Cap.ROUND);

                endDotBGPaint.setColor(Color.BLACK);
                endDotBGPaint.setAntiAlias(true);
                endDotBGPaint.setStrokeWidth(getResources().getDimension(R.dimen.thumbLineWidth) * 4.5f);
                endDotBGPaint.setStyle(Paint.Style.STROKE);
                endDotBGPaint.setStrokeJoin(Paint.Join.ROUND);
                endDotBGPaint.setStrokeCap(Paint.Cap.ROUND);

                // Calculate the drawing scale
                double midLatitude = (track.getLatitudeMax() + track.getLatitudeMin()) / 2;
                double angleFromEquator = Math.abs(midLatitude);

                distanceProportion = Math.cos(Math.toRadians(angleFromEquator));
                //Log.w("myApp", "[#] GPSApplication.java - Distance_Proportion = " + Distance_Proportion);

                drawScale = Math.max(track.getLatitudeMax() - track.getLatitudeMin(), distanceProportion * (track.getLongitudeMax() - track.getLongitudeMin()));
                latOffset = sizeMinusMargins * (1 - (track.getLatitudeMax() - track.getLatitudeMin()) / drawScale) / 2;
                lonOffset = sizeMinusMargins * (1 - (distanceProportion * (track.getLongitudeMax() - track.getLongitudeMin()) / drawScale)) / 2;

                minLatitude = track.getLatitudeMin();
                minLongitude = track.getLongitudeMin();

                final AsyncThumbnailThreadClass asyncThumbnailThreadClass = new AsyncThumbnailThreadClass();
                asyncThumbnailThreadClass.start();
            }
        }

        private class AsyncThumbnailThreadClass extends Thread {

            public AsyncThumbnailThreadClass() {}

            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                String fname = id + ".png";
                File file = new File(getApplicationContext().getFilesDir() + "/Thumbnails/", fname);
                if (file.exists()) file.delete();

                if (drawScale > 0) {
                    int groupOfLocations = 200;
                    Path path = new Path();
                    List<LatLng> latlngList = new ArrayList<>();

                    //Log.w("myApp", "[#] GPSApplication.java - Thumbnailer Thread started");
                    for (int i = 0; i < numberOfLocations; i += groupOfLocations) {
                        latlngList.addAll(gpsDataBase.getLatLngList(id, i, i + groupOfLocations - 1));
                    }
                    //Log.w("myApp", "[#] GPSApplication.java - Added " + latlngList.size() + " items to Path");
                    if (!latlngList.isEmpty()) {
                        Bitmap thumbBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        Canvas thumbCanvas = new Canvas(thumbBitmap);

                        for (int i = 0; i < latlngList.size(); i++) {
                            if (i == 0)
                                path.moveTo((float) (lonOffset + margin + sizeMinusMargins * ((latlngList.get(i).longitude - minLongitude) * distanceProportion / drawScale)),
                                        (float) (-latOffset + size - (margin + sizeMinusMargins * ((latlngList.get(i).latitude - minLatitude) / drawScale))));
                            else
                                path.lineTo((float) (lonOffset + margin + sizeMinusMargins * ((latlngList.get(i).longitude - minLongitude) * distanceProportion / drawScale)),
                                        (float) (-latOffset + size - (margin + sizeMinusMargins * ((latlngList.get(i).latitude - minLatitude) / drawScale))));
                        }
                        thumbCanvas.drawPath(path, bgPaint);
                        thumbCanvas.drawPoint((float) (lonOffset + margin + sizeMinusMargins * ((latlngList.get(latlngList.size()-1).longitude - minLongitude) * distanceProportion / drawScale)),
                                (float) (-latOffset + size - (margin + sizeMinusMargins * ((latlngList.get(latlngList.size()-1).latitude - minLatitude) / drawScale))), endDotBGPaint);
                        thumbCanvas.drawPath(path, drawPaint);
                        thumbCanvas.drawPoint((float) (lonOffset + margin + sizeMinusMargins * ((latlngList.get(latlngList.size()-1).longitude - minLongitude) * distanceProportion / drawScale)),
                                (float) (-latOffset + size - (margin + sizeMinusMargins * ((latlngList.get(latlngList.size()-1).latitude - minLatitude) / drawScale))), endDotdrawPaint);

                        try {
                            FileOutputStream out = new FileOutputStream(file);
                            //Log.w("myApp", "[#] GPSApplication.java - FileOutputStream out = new FileOutputStream(file)");
                            //boolean res = thumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
                            thumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
                            //Log.w("myApp", "[#] GPSApplication.java - thumbBitmap.compress(Bitmap.CompressFormat.PNG, 60, out): " + res);
                            out.flush();
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            //Log.w("myApp", "[#] GPSApplication.java - Unable to save: " + DIRECTORY_TEMP + "/" + fname);
                        }

                        EventBus.getDefault().post(EventBusMSG.REFRESH_TRACKLIST);
                    }
                }
            }
        }
    }
}
