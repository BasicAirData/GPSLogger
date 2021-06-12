/*
 * SpikesChecker - Java Class for Android
 * Created by G.Capelli on 15/9/2016
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

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * Checks the evolution of the altitudes with the purpose to detect the altitude spikes.
 */
class SpikesChecker {

    private long    goodTime            = NOT_AVAILABLE;    // The time of the last good value

    private double  prevAltitude        = NOT_AVAILABLE;    // the previous data loaded
    private long    prevTime            = NOT_AVAILABLE;
    private float   prevVerticalSpeed   = NOT_AVAILABLE;

    private double  newAltitude         = NOT_AVAILABLE;    // the new (current) data loaded
    private long    newTime             = NOT_AVAILABLE;
    private float   newVerticalSpeed    = NOT_AVAILABLE;

    private long    timeInterval        = NOT_AVAILABLE;    // Interval between fixes (in seconds)
    private float   verticalAcceleration;

    private final float MAX_ACCELERATION;                   // The maximum vertical acceleration allowed
    private final int   STABILIZATION_TIME;                 // Stabilization window, in seconds. It must be > 0

    /**
     * Creates a SpikesChecker with the given parameters.
     *
     * @param maxAcceleration The maximum valid acceleration
     * @param stabilizationTime The time that passes from an invalid value to the next valid one
     */
    SpikesChecker(float maxAcceleration, int stabilizationTime) {
        MAX_ACCELERATION = maxAcceleration;
        STABILIZATION_TIME = stabilizationTime;
    }

    /**
     * Loads a new sample into the checker.
     *
     * @param time The time of the sample
     * @param altitude The related altitude in meters
     */
    void load(long time, double altitude) {
        if (time > newTime) {
            prevTime = newTime;
            newTime = time;
            prevAltitude = newAltitude;
            prevVerticalSpeed = newVerticalSpeed;
        }
        timeInterval = prevTime != NOT_AVAILABLE ? (newTime - prevTime) / 1000 : NOT_AVAILABLE;
        newAltitude = altitude;
        if ((timeInterval > 0) && (prevAltitude != NOT_AVAILABLE)) {
            newVerticalSpeed = (float) (newAltitude - prevAltitude) / timeInterval;
            if (prevVerticalSpeed != NOT_AVAILABLE) {
                if (timeInterval > 1000) verticalAcceleration = NOT_AVAILABLE; // Prevent Vertical Acceleration value from exploding
                else verticalAcceleration = 2 * (-prevVerticalSpeed * timeInterval + (float)(newAltitude - prevAltitude)) / (timeInterval * timeInterval);
            }
        }
        if (Math.abs(verticalAcceleration) >= MAX_ACCELERATION) goodTime = newTime;
        //Log.w("myApp", "[#] SpikesChecker.java - Vertical Acceleration = " + VerticalAcceleration);
        //Log.w("myApp", "[#] SpikesChecker.java - Validation window = " + (New_Time - Good_Time) / 1000);
    }

    /**
     * @return true if the last altitude loaded is valid (is not a spike).
     */
    boolean isValid() {
        return (newTime - goodTime) / 1000 >= STABILIZATION_TIME;
    }
}
