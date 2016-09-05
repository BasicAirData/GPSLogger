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

    public static final int NOT_AVAILABLE = -100000;

    public static final int UM_METRIC_MS = 0;
    public static final int UM_METRIC_KMH = 1;
    public static final int UM_IMPERIAL_FPS = 8;
    public static final int UM_IMPERIAL_MPH = 9;

    public static final float M_TO_FT = 3.280839895f;
    public static final float MS_TO_MPH = 2.2369363f;

    private Location _Location;
    private String _Description = "";
    private double _AltitudeEGM96Correction = NOT_AVAILABLE;
    private int _NumberOfSatellites = NOT_AVAILABLE;


    // Constructor
    public LocationExtended(Location location) {
        _Location = location;
        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (egm96.isEGMGridLoaded()) _AltitudeEGM96Correction = egm96.getEGMCorrection(_Location.getLatitude(), _Location.getLongitude());
        }
    }

    public Location getLocation() {
        return _Location;
    }


    // Getters and Setters -------------------------------------------------------------------------

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


    public double getAltitudeEGM96Correction(){
        if (_AltitudeEGM96Correction == NOT_AVAILABLE) {
            //Log.w("myApp", "[#] LocationExtended.java - _AltitudeEGM96Correction == NOT_AVAILABLE");
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isEGMGridLoaded()) _AltitudeEGM96Correction = egm96.getEGMCorrection(_Location.getLatitude(), _Location.getLongitude());
            }
        }
        return this._AltitudeEGM96Correction;
    }


    // ---------------------------------------------------------------------------------------------

    public String getFormattedLatitude() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        boolean prefShowDecimalCoordinates = gpsApplication.getPrefShowDecimalCoordinates();
        if (_Location != null) {
            if (prefShowDecimalCoordinates) return String.format("%.9f", Math.abs(_Location.getLatitude()));
            else return Location.convert(Math.abs(_Location.getLatitude()), Location.FORMAT_SECONDS);
        }
        return "";
    }

    public String getFormattedLongitude() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        boolean prefShowDecimalCoordinates = gpsApplication.getPrefShowDecimalCoordinates();
        if (_Location != null) {
            if (prefShowDecimalCoordinates) return String.format("%.9f", Math.abs(_Location.getLongitude()));
            else return Location.convert(Math.abs(_Location.getLongitude()), Location.FORMAT_SECONDS);
        }
        return "";
    }

    // NOT USED
    /*
    public String getFormattedAltitude() {
        if (_Location != null) {
            GPSApplication gpsApplication = GPSApplication.getInstance();
            int UM = gpsApplication.getPrefUM();
            switch (UM) {
                case UM_METRIC_KMH:     return _Location.hasAltitude() ? String.valueOf(Math.round(_Location.getAltitude())) : "";
                case UM_METRIC_MS:      return _Location.hasAltitude() ? String.valueOf(Math.round(_Location.getAltitude())) : "";
                case UM_IMPERIAL_MPH:   return _Location.hasAltitude() ? String.valueOf(Math.round(_Location.getAltitude() * M_TO_FT)) : "";
                case UM_IMPERIAL_FPS:   return _Location.hasAltitude() ? String.valueOf(Math.round(_Location.getAltitude() * M_TO_FT)) : "";
            }
        }
        return "";
    }
    */

    public String getFormattedSpeed() {
        if (_Location != null) {
            GPSApplication gpsApplication = GPSApplication.getInstance();
            int UM = gpsApplication.getPrefUM();
            switch (UM) {
                case UM_METRIC_KMH:     return _Location.hasSpeed() ? String.valueOf(Math.round(_Location.getSpeed() * 3.6)) : "";
                case UM_METRIC_MS:      return _Location.hasSpeed() ? String.valueOf(Math.round(_Location.getSpeed())) : "";
                case UM_IMPERIAL_MPH:   return _Location.hasSpeed() ? String.valueOf(Math.round(_Location.getSpeed() * MS_TO_MPH)) : "";
                case UM_IMPERIAL_FPS:   return _Location.hasSpeed() ? String.valueOf(Math.round(_Location.getSpeed() * M_TO_FT)) : "";
            }
        }
        return "";
    }

    public String getFormattedBearing() {
        if (_Location != null) {
            GPSApplication gpsApplication = GPSApplication.getInstance();
            int pDir = gpsApplication.getPrefShowDirections();
            if (_Location.hasBearing()) {

                switch (pDir) {
                    case 0:         // NSWE
                        final String N = gpsApplication.getString(R.string.north);
                        final String S = gpsApplication.getString(R.string.south);
                        final String W = gpsApplication.getString(R.string.west);
                        final String E = gpsApplication.getString(R.string.east);
                        int dr = (int) Math.round(_Location.getBearing() / 22.5);
                        switch (dr) {
                            case 0:     return N;
                            case 1:     return N + N + E;
                            case 2:     return N + E;
                            case 3:     return E + N + E;
                            case 4:     return E;
                            case 5:     return E + S + E;
                            case 6:     return S + E;
                            case 7:     return S + S + E;
                            case 8:     return S;
                            case 9:     return S + S + W;
                            case 10:    return S + W;
                            case 11:    return W + S + W;
                            case 12:    return W;
                            case 13:    return W + N + W;
                            case 14:    return N + W;
                            case 15:    return N + N + W;
                            case 16:    return N;
                            default:    return "";
                        }
                    case 1:         // Angle
                        return String.valueOf(Math.round(_Location.getBearing()));
                    default:
                        return String.valueOf(Math.round(_Location.getBearing()));
                }
            }
        }
        return "";
    }

    public String getFormattedAccuracy() {
        if (_Location != null) {
            GPSApplication gpsApplication = GPSApplication.getInstance();
            int UM = gpsApplication.getPrefUM();
            switch (UM) {
                case UM_METRIC_KMH:     return _Location.hasAccuracy() ? String.valueOf(Math.round(_Location.getAccuracy())) : "";
                case UM_METRIC_MS:      return _Location.hasAccuracy() ? String.valueOf(Math.round(_Location.getAccuracy())) : "";
                case UM_IMPERIAL_MPH:   return _Location.hasAccuracy() ? String.valueOf(Math.round(_Location.getAccuracy() * M_TO_FT)) : "";
                case UM_IMPERIAL_FPS:   return _Location.hasAccuracy() ? String.valueOf(Math.round(_Location.getAccuracy() * M_TO_FT)) : "";
            }
        }
        return "";
    }

    public String getFormattedAltitudeCorrected(double AltitudeManualCorrection, boolean EGMCorrection) {
        if (_Location != null) {
            double correctedaltitude = _Location.getAltitude();
            if ((EGMCorrection) && (getAltitudeEGM96Correction() != NOT_AVAILABLE)) correctedaltitude -= getAltitudeEGM96Correction();
            correctedaltitude += AltitudeManualCorrection;
            GPSApplication gpsApplication = GPSApplication.getInstance();
            int UM = gpsApplication.getPrefUM();
            switch (UM) {
                case UM_METRIC_KMH:     return _Location.hasAltitude() ? String.valueOf(Math.round(correctedaltitude)) : "";
                case UM_METRIC_MS:      return _Location.hasAltitude() ? String.valueOf(Math.round(correctedaltitude)) : "";
                case UM_IMPERIAL_MPH:   return _Location.hasAltitude() ? String.valueOf(Math.round(correctedaltitude * M_TO_FT)) : "";
                case UM_IMPERIAL_FPS:   return _Location.hasAltitude() ? String.valueOf(Math.round(correctedaltitude * M_TO_FT)) : "";
            }
        }
        return "";
    }

    public String getFormattedSpeedUM() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        int UM = gpsApplication.getPrefUM();
        switch (UM) {
            case UM_METRIC_KMH:     return "km/h";
            case UM_METRIC_MS:      return "m/s";
            case UM_IMPERIAL_MPH:   return "mph";
            case UM_IMPERIAL_FPS:   return "fps";
            default:                return "";
        }
    }

    public String getFormattedAltitudeUM() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        int UM = gpsApplication.getPrefUM();
        switch (UM) {
            case UM_METRIC_KMH:     return "m";
            case UM_METRIC_MS:      return "m";
            case UM_IMPERIAL_MPH:   return "ft";
            case UM_IMPERIAL_FPS:   return "ft";
            default:                return "";
        }
    }

    public String getFormattedLatitudeUM() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        final String N = gpsApplication.getString(R.string.north);
        final String S = gpsApplication.getString(R.string.south);
        if (_Location.getLatitude() >= 0) return N;
        else return S;
    }

    public String getFormattedLongitudeUM() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        final String E = gpsApplication.getString(R.string.east);
        final String W = gpsApplication.getString(R.string.west);
        if (_Location.getLongitude() >= 0) return E;
        else return W;
    }

}

