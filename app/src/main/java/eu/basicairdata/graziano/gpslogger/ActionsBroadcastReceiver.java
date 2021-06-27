/*
 * ActionsBroadcastReceiver - Java Class for Android
 * Created by G.Capelli on 25/9/2020
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The Broadcast Receiver that reacts when one of the following events occur:
 * <ul>
 *     <li>the screen is turned on</li>
 *     <li>the screen is turned off</li>
 *     <li>The system is shut down</li>
 * </ul>
 */
public class ActionsBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        //Log.w("myApp", "[#] EVENT");
        String broadcastedAction = intent.getAction();
        if (broadcastedAction != null) {  // https://github.com/BasicAirData/GPSLogger/issues/132
            switch (broadcastedAction) {
                case Intent.ACTION_SCREEN_OFF:
                    // Turns off the EventBus Signals of the Recording Thread
                    // in order to save Battery
                    GPSApplication.getInstance().onScreenOff();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    // Turns on the EventBus Signals of the Recording Thread
                    GPSApplication.getInstance().onScreenOn();
                    break;
                case Intent.ACTION_SHUTDOWN:
                    // Gracefully finish to write data and close the Database
                    if (GPSApplication.getInstance() != null) {  // https://github.com/BasicAirData/GPSLogger/issues/146
                        GPSApplication.getInstance().onShutdown();
                    }
                    break;
            }
        }
    }
}