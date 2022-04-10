/*
 * LocationExtended - Java Class for Android
 * Created by G.Capelli on 2/6/2016
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

import android.location.Location;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * The Location Class, including some extra stuff in order to manage the orthometric
 * height using the EGM Correction.
 */
public class LocationExtended {
    private Location location;
    private String description              = "";
    private double altitudeEGM96Correction  = NOT_AVAILABLE;
    private int numberOfSatellites          = NOT_AVAILABLE;
    private int numberOfSatellitesUsedInFix = NOT_AVAILABLE;

    /**
     * The constructor.
     *
     * @param location the base Location
     */
    public LocationExtended(Location location) {
        this.location = location;
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (egm96.isLoaded()) altitudeEGM96Correction = egm96.getEGMCorrection(this.location.getLatitude(), this.location.getLongitude());
        }
    }

    // ------------------------------------------------------------------------- Getters and Setters

    public Location getLocation() {
        return location;
    }

    public double getLatitude() { return location.getLatitude(); }

    public double getLongitude() { return location.getLongitude(); }

    public double getAltitude() { return location.hasAltitude() ? location.getAltitude() : NOT_AVAILABLE; }

    public float getSpeed() { return location.hasSpeed() ? location.getSpeed() : NOT_AVAILABLE; }

    public float getAccuracy() { return location.hasAccuracy() ? location.getAccuracy() : NOT_AVAILABLE; }

    public float getBearing() { return location.hasBearing() ? location.getBearing() : NOT_AVAILABLE; }

    public long getTime() { return location.getTime(); }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNumberOfSatellites(int numberOfSatellites) {
        this.numberOfSatellites = numberOfSatellites;
    }

    public int getNumberOfSatellites() {
        return numberOfSatellites;
    }

    public void setNumberOfSatellitesUsedInFix(int numberOfSatellites) {
        numberOfSatellitesUsedInFix = numberOfSatellites;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return numberOfSatellitesUsedInFix;
    }

    /**
     * @return the altitude correction, in meters, based on EGM96
     */
    public double getAltitudeEGM96Correction(){
        if (altitudeEGM96Correction == NOT_AVAILABLE) {
            //Log.w("myApp", "[#] LocationExtended.java - _AltitudeEGM96Correction == NOT_AVAILABLE");
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isLoaded()) altitudeEGM96Correction = egm96.getEGMCorrection(location.getLatitude(), location.getLongitude());
            }
        }
        return altitudeEGM96Correction;
    }

    /**
     * @return the orthometric altitude in meters
     */
    public double getAltitudeCorrected(double AltitudeManualCorrection, boolean EGMCorrection) {
        if (location != null) {
            if (!location.hasAltitude()) return NOT_AVAILABLE;
            if ((EGMCorrection) && (getAltitudeEGM96Correction() != NOT_AVAILABLE)) return location.getAltitude() - getAltitudeEGM96Correction() + AltitudeManualCorrection;
            else return location.getAltitude() + AltitudeManualCorrection;
        }
        return NOT_AVAILABLE;
    }
}

