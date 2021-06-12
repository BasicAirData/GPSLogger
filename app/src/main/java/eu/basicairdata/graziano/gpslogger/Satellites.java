/*
 * Satellites - Java Class for Android
 * Created by G.Capelli on 26/10/2020
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

import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.os.Build;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * Stores and manages the updating of the status of the satellites constellations.
 */
public class Satellites {
    //private static final float NO_DATA = 0.0f;

    private int numSatsTotal = NOT_AVAILABLE;
    //private int numSatsInView = NOT_AVAILABLE;
    private int numSatsUsedInFix = NOT_AVAILABLE;

    /**
     * The data structure that describes a satellite
     */
    private static class Satellite {
        public int svid;
        public int constellationType;
        public boolean isUsedInFix = false;
        //public boolean isInView = false;
    }

    /**
     * @return The total number of available satellites
     */
    public int getNumSatsTotal() {
        return numSatsTotal;
    }

    /**
     * @return The number of satellites that are used to obtain the FIX
     */
    public int getNumSatsUsedInFix() {
        return numSatsUsedInFix;
    }

//    public int getNumSatsInView() {
//        return numSatsInView;
//    }

    /**
     * Updates the status of the satellites using the Legacy GpsStatus.
     */
    public void updateStatus(GpsStatus gpsStatus) {
        if (gpsStatus != null) {
            int satsTotal = 0;     // Total number of Satellites;
            //int satsInView = 0;    // Satellites in view;
            int satsUsed = 0;      // Satellites used in fix;
            Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
            for (GpsSatellite sat : sats) {
                satsTotal++;
                if (sat.usedInFix()) satsUsed++;
                //if (sat.getSnr() != NO_DATA) satsInView++;
            }
            numSatsTotal = satsTotal;
            //numSatsInView = satsInView;
            numSatsUsedInFix = satsUsed;
        } else {
            numSatsTotal = NOT_AVAILABLE;
            //numSatsInView = NOT_AVAILABLE;
            numSatsUsedInFix = NOT_AVAILABLE;
        }
    }

    /**
     * Updates the status of the satellites using the new GnssStatus.
     * For Android VERSION_CODES.N and above.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateStatus(GnssStatus gnssStatus) {
        if (gnssStatus != null) {
            ArrayList <Satellite> satellites = new ArrayList<>();
            for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
                // The Satellite is present

                Satellite sat = new Satellite();
                sat.svid = gnssStatus.getSvid(i);
                sat.constellationType = gnssStatus.getConstellationType(i);
                sat.isUsedInFix = gnssStatus.usedInFix(i);
                //if (gnssStatus.getCn0DbHz(i) != NO_DATA) sat.isInView = true;   // The Satellite is in View

                boolean found = false;
                for (Satellite s : satellites) {
                    if ((s.svid == sat.svid) && (s.constellationType == sat.constellationType)) {
                        // Another signal from the same Satellite found (dual freq GNSS)
                        found = true;
                        if (sat.isUsedInFix) s.isUsedInFix = true;
                        //if (sat.isInView) s.isInView = true;
                    }
                }
                if (!found) {
                    // Add the new Satellite to the List
                    satellites.add(sat);
                }
            }
            numSatsTotal = satellites.size();
            //numSatsInView = 0;
            numSatsUsedInFix = 0;
            for (Satellite s : satellites) {
                //if (s.isInView) numSatsInView++;
                if (s.isUsedInFix) numSatsUsedInFix++;
            }
        } else {
            numSatsTotal = NOT_AVAILABLE;
            //numSatsInView = NOT_AVAILABLE;
            numSatsUsedInFix = NOT_AVAILABLE;
        }
    }
}
