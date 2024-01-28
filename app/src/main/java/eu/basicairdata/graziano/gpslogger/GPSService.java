/*
 * GPSService - Java Class for Android
 * Created by G.Capelli on 2/11/2016
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
 */

package eu.basicairdata.graziano.gpslogger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * The Foreground Service that keeps alive the app in background when recording.
 * It shows a notification that shows the status of the recording, the traveled distance
 * and the related duration.
 */
public class GPSService extends Service {

    private static final int ID = 1;                        // The id of the notification

    private String oldNotificationText = "";
    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private boolean recordingState;
    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK

    /**
     * Returns the instance of the service
     */
    public class LocalBinder extends Binder {
        public GPSService getServiceInstance(){
            return GPSService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();      // IBinder

    @Override
    public void onCreate() {
        super.onCreate();
        // PARTIAL_WAKELOCK
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"GPSLogger:wakelock");
        Log.w("myApp", "[#] GPSService.java - CREATE = onCreate");
        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] GPSActivity.java - EventBus: GPSActivity already registered");
            EventBus.getDefault().unregister(this);
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(ID, getNotification());
        Log.w("myApp", "[#] GPSService.java - START = onStartCommand");
        return START_NOT_STICKY;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public IBinder onBind(Intent intent) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.w("myApp", "[#] GPSService.java - WAKELOCK acquired");
        }
        Log.w("myApp", "[#] GPSService.java - BIND = onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        // PARTIAL_WAKELOCK
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.w("myApp", "[#] GPSService.java - WAKELOCK released");
        }
        EventBus.getDefault().unregister(this);
        Log.w("myApp", "[#] GPSService.java - DESTROY = onDestroy");
        // THREAD FOR DEBUG PURPOSE
        //if (t.isAlive()) t.interrupt();
        super.onDestroy();
    }

    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if ((msg == EventBusMSG.UPDATE_FIX) && (builder != null)
                && ((Build.VERSION.SDK_INT < Build.VERSION_CODES.N) || (mNotificationManager.areNotificationsEnabled()))) {
            String notificationText = composeContentText();
            if (!oldNotificationText.equals(notificationText)) {
                builder.setContentText(notificationText);
                builder.setOngoing(true);                   // https://developer.android.com/develop/background-work/services/foreground-services#user-dismiss-notification
                if (isIconRecording() != recordingState) {
                    recordingState = isIconRecording();
                    builder.setSmallIcon(recordingState ? R.mipmap.ic_notify_recording_24dp : R.mipmap.ic_notify_24dp);
                }
                mNotificationManager.notify(ID, builder.build());
                oldNotificationText = notificationText;
                //Log.w("myApp", "[#] GPSService.java - Update Notification Text");
            }
        }
    }

    /**
     * Creates and gets the Notification.
     *
     * @return the Notification
     */
    private Notification getNotification() {
        final String CHANNEL_ID = "GPSLoggerServiceChannel";

        recordingState = isIconRecording();
        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        //builder.setSmallIcon(R.drawable.ic_notification_24dp)
        builder.setSmallIcon(recordingState ? R.mipmap.ic_notify_recording_24dp : R.mipmap.ic_notify_24dp)
                .setColor(getResources().getColor(R.color.colorPrimaryLight))
                .setContentTitle(getString(R.string.app_name))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(composeContentText());

        final Intent startIntent = new Intent(getApplicationContext(), GPSActivity.class);
        startIntent.setAction(Intent.ACTION_MAIN);
        startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, startIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        return builder.build();
    }

    /**
     * @return true if the icon should be the filled one, indicating that the app is recording.
     */
    private boolean isIconRecording () {
        return ((GPSApplication.getInstance().getGPSStatus() == GPSApplication.GPS_OK) && GPSApplication.getInstance().isRecording());
    }

    /**
     * @return The string to use as Notification description.
     */
    private String composeContentText () {
        String notificationText = "";
        int gpsStatus = GPSApplication.getInstance().getGPSStatus();
        switch (gpsStatus) {
            case GPSApplication.GPS_DISABLED:
                notificationText = getString(R.string.gps_disabled);
                break;
            case GPSApplication.GPS_OUTOFSERVICE:
                notificationText = getString(R.string.gps_out_of_service);
                break;
            case GPSApplication.GPS_TEMPORARYUNAVAILABLE:
            case GPSApplication.GPS_SEARCHING:
                notificationText = getString(R.string.gps_searching);
                break;
            case GPSApplication.GPS_STABILIZING:
                notificationText = getString(R.string.gps_stabilizing);
                break;
            case GPSApplication.GPS_OK:
                if (GPSApplication.getInstance().isRecording() && (GPSApplication.getInstance().getCurrentTrack() != null)) {
                    PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
                    PhysicalData phdDuration;
                    PhysicalData phdDistance;

                    // Duration
                    phdDuration = phdformatter.format(GPSApplication.getInstance().getCurrentTrack().getPrefTime(), PhysicalDataFormatter.FORMAT_DURATION);
                    if (phdDuration.value.isEmpty()) phdDuration.value = "00:00";
                    notificationText = getString(R.string.duration) + ": " + phdDuration.value;

                    // Distance (if available)
                    phdDistance = phdformatter.format(GPSApplication.getInstance().getCurrentTrack().getEstimatedDistance(), PhysicalDataFormatter.FORMAT_DISTANCE);
                    if (!phdDistance.value.isEmpty()) {
                        notificationText += " - " + getString(R.string.distance) + ": " + phdDistance.value + " " + phdDistance.um;
                    }
                } else {
                    notificationText = getString(R.string.notification_contenttext);
                }
        }
        return notificationText;
    }
}
