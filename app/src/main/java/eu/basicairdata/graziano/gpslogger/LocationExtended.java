/*
 * LocationExtended - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 2/6/2016
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

public class LocationExtended {

    private final int NOT_AVAILABLE = -100000;

    private Location _Location;
    private String _Description = "";
    private double _AltitudeEGM96Correction = NOT_AVAILABLE;
    private int _NumberOfSatellites = NOT_AVAILABLE;
    private int _NumberOfSatellitesUsedInFix = NOT_AVAILABLE;


    // Constructor
    public LocationExtended(Location location) {
        _Location = location;
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (egm96.isEGMGridLoaded()) _AltitudeEGM96Correction = egm96.getEGMCorrection(_Location.getLatitude(), _Location.getLongitude());
        }
    }

    // Getters and Setters -------------------------------------------------------------------------

    public Location getLocation() {
        return _Location;
    }

    public double getLatitude() { return _Location.getLatitude(); }
    public double getLongitude() { return _Location.getLongitude(); }
    public double getAltitude() { return _Location.hasAltitude() ? _Location.getAltitude() : NOT_AVAILABLE; }
    public float getSpeed() { return _Location.hasSpeed() ? _Location.getSpeed() : NOT_AVAILABLE; }
    public float getAccuracy() { return _Location.hasAccuracy() ? _Location.getAccuracy() : NOT_AVAILABLE; }
    public float getBearing() { return _Location.hasBearing() ? _Location.getBearing() : NOT_AVAILABLE; }
    public long getTime() { return _Location.getTime(); }

    public String getDescription() {
        return _Description;
    }

    public void setDescription(String Description) {
        this._Description = Description;
    }

    public void setNumberOfSatellites(int numberOfSatellites) {
        _NumberOfSatellites = numberOfSatellites;
    }

    public int getNumberOfSatellites() {
        return _NumberOfSatellites;
    }

    public void setNumberOfSatellitesUsedInFix(int numberOfSatellites) {
        _NumberOfSatellitesUsedInFix = numberOfSatellites;
    }

    public int getNumberOfSatellitesUsedInFix() {
        return _NumberOfSatellitesUsedInFix;
    }

    public double getAltitudeEGM96Correction(){
        if (_AltitudeEGM96Correction == NOT_AVAILABLE) {
            //Log.w("myApp", "[#] LocationExtended.java - _AltitudeEGM96Correction == NOT_AVAILABLE");
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isEGMGridLoaded()) _AltitudeEGM96Correction = egm96.getEGMCorrection(_Location.getLatitude(), _Location.getLongitude());
            }
        }
        return _AltitudeEGM96Correction;
    }

    public double getAltitudeCorrected(double AltitudeManualCorrection, boolean EGMCorrection) {
        if (_Location != null) {
            if (!_Location.hasAltitude()) return NOT_AVAILABLE;
            if ((EGMCorrection) && (getAltitudeEGM96Correction() != NOT_AVAILABLE)) return _Location.getAltitude() - getAltitudeEGM96Correction() + AltitudeManualCorrection;
            else return _Location.getAltitude() + AltitudeManualCorrection;
        }
        return NOT_AVAILABLE;
    }
}

