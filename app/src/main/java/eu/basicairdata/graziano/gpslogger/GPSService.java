/**
 * GPSService - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 2/11/2016
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class GPSService extends Service {
    // Singleton instance
    private static GPSService singleton;
    private String oldNotificationText = "";
    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    final String CHANNEL_ID = "GPSLoggerServiceChannel";
    final int ID = 1;

    private boolean RecordingState = false;

    public static GPSService getInstance(){
        return singleton;
    }
    // IBinder
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {                                   //returns the instance of the service
        public GPSService getServiceInstance(){
            return GPSService.this;
        }
    }

    // PARTIAL_WAKELOCK
    private PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private Notification getNotification() {

        RecordingState = isIconRecording();

        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        //builder.setSmallIcon(R.drawable.ic_notification_24dp)
        builder.setSmallIcon(RecordingState ? R.mipmap.ic_notify_recording_24dp : R.mipmap.ic_notify_24dp)
                .setColor(getResources().getColor(R.color.colorPrimaryLight))
                .setContentTitle(getString(R.string.app_name))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentText(composeContentText());

        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        //    builder.setPriority(NotificationCompat.PRIORITY_LOW);
        //}

        final Intent startIntent = new Intent(getApplicationContext(), GPSActivity.class);
        startIntent.setAction(Intent.ACTION_MAIN);
        startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, startIntent, 0);
        builder.setContentIntent(contentIntent);
        return builder.build();
    }

    /* THREAD FOR DEBUG PURPOSE
    Thread t = new Thread() {
        public void run() {
            boolean i = true;
            while (i) {
                try {
                    sleep(1000);
                    Log.w("myApp", "[#] GPSService.java - ** RUNNING **");
                } catch (InterruptedException e) {
                    i = false;
                }
            }
        }
    }; */

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        // THREAD FOR DEBUG PURPOSE
        //if (!t.isAlive()) {
        //    t.start();
        //}

        // PARTIAL_WAKELOCK
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
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
        mNotificationManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        startForeground(ID, getNotification());
        Log.w("myApp", "[#] GPSService.java - START = onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.w("myApp", "[#] GPSService.java - WAKELOCK acquired");
        }
        Log.w("myApp", "[#] GPSService.java - BIND = onBind");
        return mBinder;
        //return null;
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

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if ((msg == EventBusMSG.UPDATE_FIX) && (builder != null)) {
            String notificationText = composeContentText();
            if (!oldNotificationText.equals(notificationText)) {
                builder.setContentText(notificationText);

                if (isIconRecording() != RecordingState) {
                    RecordingState = isIconRecording();
                    builder.setSmallIcon(RecordingState ? R.mipmap.ic_notify_recording_24dp : R.mipmap.ic_notify_24dp);
                }

                mNotificationManager.notify(ID, builder.build());
                oldNotificationText = notificationText;
                //Log.w("myApp", "[#] GPSService.java - Update Notification Text");
            }
        }
    }


    private boolean isIconRecording () {
        return ((GPSApplication.getInstance().getGPSStatus() == GPSApplication.GPS_OK) && GPSApplication.getInstance().getRecording());
    }


    private String composeContentText () {
        String notificationText = "";

        int GPSStatus = GPSApplication.getInstance().getGPSStatus();
        switch (GPSStatus) {
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
                if (GPSApplication.getInstance().getRecording()) {
                    PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
                    PhysicalData phdDuration;
                    phdDuration = phdformatter.format(GPSApplication.getInstance().getCurrentTrack().getDuration(), PhysicalDataFormatter.FORMAT_DURATION);
                    PhysicalData phdDistance;
                    phdDistance = phdformatter.format(GPSApplication.getInstance().getCurrentTrack().getDistance(), PhysicalDataFormatter.FORMAT_DISTANCE);
                    notificationText = getString(R.string.duration) + ": " + phdDuration.Value + " - "
                            + getString(R.string.distance) + ": " + phdDistance.Value + " " + phdDistance.UM;
                } else {
                    notificationText = getString(R.string.notification_contenttext);
                }
        }
        return notificationText;
    }
}
