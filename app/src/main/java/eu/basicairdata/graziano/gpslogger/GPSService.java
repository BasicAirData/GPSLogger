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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class GPSService extends Service {
    // Singleton instance
    private static GPSService singleton;
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        //builder.setSmallIcon(R.drawable.ic_notification_24dp)
        builder.setSmallIcon(R.mipmap.ic_notify_24dp)
                .setColor(getResources().getColor(R.color.colorPrimaryLight))
                .setContentTitle(getString(R.string.app_name))
                .setShowWhen(false)
                .setContentText(getString(R.string.notification_contenttext));

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
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"GPSLogger");
        Log.w("myApp", "[#] GPSService.java - CREATE = onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, getNotification());
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

        Log.w("myApp", "[#] GPSService.java - DESTROY = onDestroy");
        // THREAD FOR DEBUG PURPOSE
        //if (t.isAlive()) t.interrupt();
        super.onDestroy();
    }
}
