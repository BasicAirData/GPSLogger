/*
 * PhysicalDataFormatter - Java Class for Android
 * Created by G.Capelli on 21/3/2017
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

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * A helper Class for the formatting of the physical data.
 * It returns the data formatted basing on the given criteria and on the Preferences.
 */
class PhysicalDataFormatter {

    // Units of measurement
    public static final int UM_METRIC   = 0;
    public static final int UM_IMPERIAL = 8;
    public static final int UM_NAUTICAL = 16;

    // Units of measurement for speeds
    public static final int UM_SPEED_MS  = 0;
    public static final int UM_SPEED_KMH = 1;
    public static final int UM_SPEED_FPS = 2;
    public static final int UM_SPEED_MPH = 3;
    public static final int UM_SPEED_KN  = 4;

    // Formatting constants
    public static final byte FORMAT_LATITUDE    = 1;
    public static final byte FORMAT_LONGITUDE   = 2;
    public static final byte FORMAT_ALTITUDE    = 3;
    public static final byte FORMAT_SPEED       = 4;
    public static final byte FORMAT_ACCURACY    = 5;
    public static final byte FORMAT_BEARING     = 6;
    public static final byte FORMAT_DURATION    = 7;
    public static final byte FORMAT_SPEED_AVG   = 8;
    public static final byte FORMAT_DISTANCE    = 9;
    public static final byte FORMAT_TIME        = 10;

    public static final float M_TO_FT   = 3.280839895f;
    public static final float M_TO_NM   = 0.000539957f;
    public static final float MS_TO_MPH = 2.2369363f;
    public static final float MS_TO_KMH = 3.6f;
    public static final float MS_TO_KN  = 1.943844491f;
    public static final float KM_TO_MI  = 0.621371192237f;

    private final GPSApplication gpsApp = GPSApplication.getInstance();

    /**
     * It returns a PhysicalData formatted basing on the given criteria and on the Preferences.
     *
     * @param number The float number to format as Physical Data
     * @param format The desired Format (FORMAT_LATITUDE, FORMAT_LONGITUDE, FORMAT_ALTITUDE...)
     * @return The Physical Data containing number and unit of measurement
     */
    public PhysicalData format(float number, byte format) {
        PhysicalData physicalData = new PhysicalData();
        physicalData.value = "";
        physicalData.um = "";
        
        if (number == NOT_AVAILABLE) return(physicalData);     // Returns empty fields if the data is not available
        
        switch (format) {
            case FORMAT_SPEED:  // Speed
                switch (gpsApp.getPrefUMOfSpeed()) {
                    case UM_SPEED_KMH:
                        physicalData.value = String.valueOf(Math.round(number * MS_TO_KMH));
                        physicalData.um = gpsApp.getString(R.string.UM_km_h);
                        return(physicalData);
                    case UM_SPEED_MS:
                        physicalData.value = String.valueOf(Math.round(number));
                        physicalData.um = gpsApp.getString(R.string.UM_m_s);
                        return(physicalData);
                    case UM_SPEED_MPH:
                        physicalData.value = String.valueOf(Math.round(number * MS_TO_MPH));
                        physicalData.um = gpsApp.getString(R.string.UM_mph);
                        return(physicalData);
                    case UM_SPEED_FPS:
                        physicalData.value = String.valueOf(Math.round(number * M_TO_FT));
                        physicalData.um = gpsApp.getString(R.string.UM_fps);
                        return(physicalData);
                    case UM_SPEED_KN:
                        physicalData.value = String.valueOf(Math.round(number * MS_TO_KN));
                        physicalData.um = gpsApp.getString(R.string.UM_kn);
                        return(physicalData);
                }

            case FORMAT_SPEED_AVG:  // Average Speed, formatted with 1 decimal
                switch (gpsApp.getPrefUMOfSpeed()) {
                    case UM_SPEED_KMH:
                        physicalData.value = String.format(Locale.getDefault(), "%.1f", (number * MS_TO_KMH));
                        physicalData.um = gpsApp.getString(R.string.UM_km_h);
                        return(physicalData);
                    case UM_SPEED_MS:
                        physicalData.value = String.format(Locale.getDefault(), "%.1f", (number));
                        physicalData.um = gpsApp.getString(R.string.UM_m_s);
                        return(physicalData);
                    case UM_SPEED_MPH:
                        physicalData.value = String.format(Locale.getDefault(), "%.1f", (number * MS_TO_MPH));
                        physicalData.um = gpsApp.getString(R.string.UM_mph);
                        return(physicalData);
                    case UM_SPEED_FPS:
                        physicalData.value = String.format(Locale.getDefault(), "%.1f", (number * M_TO_FT));
                        physicalData.um = gpsApp.getString(R.string.UM_fps);
                        return(physicalData);
                    case UM_SPEED_KN:
                        physicalData.value = String.format(Locale.getDefault(), "%.1f", (number * MS_TO_KN));
                        physicalData.um = gpsApp.getString(R.string.UM_kn);
                        return(physicalData);
                }

            case FORMAT_ACCURACY:   // Accuracy
                switch (gpsApp.getPrefUM()) {
                    case UM_METRIC:
                        if (GPSApplication.getInstance().isAccuracyDecimal()) {
                            if (Math.round(number) >= 10)
                                physicalData.value = String.valueOf(Math.round(number));
                            else if (Math.round(number * 10) >= 10)
                                physicalData.value = String.format(Locale.getDefault(), "%.1f", (Math.round(number * 10.0)) / 10.0);
                            else
                                physicalData.value = String.format(Locale.getDefault(), "%.2f", (Math.floor(number * 100.0)) / 100.0);
                        } else {
                            physicalData.value = String.valueOf(Math.round(number));
                        }
                        physicalData.um = gpsApp.getString(R.string.UM_m);
                        return(physicalData);
                    case UM_IMPERIAL:
                    case UM_NAUTICAL:
                        if (GPSApplication.getInstance().isAccuracyDecimal()) {
                            if (Math.round(number * M_TO_FT) >= 10)
                                physicalData.value = String.valueOf(Math.round(number * M_TO_FT));
                            else if (Math.round(number * M_TO_FT * 10) >= 10)
                                physicalData.value = String.format(Locale.getDefault(), "%.1f", (Math.round(number * M_TO_FT * 10.0)) / 10.0);
                            else
                                physicalData.value = String.format(Locale.getDefault(), "%.2f", (Math.floor(number * M_TO_FT * 100.0)) / 100.0);
                        } else {
                            physicalData.value = String.valueOf(Math.round(number * M_TO_FT));
                        }
                        physicalData.um = gpsApp.getString(R.string.UM_ft);
                        return(physicalData);
                }

            case FORMAT_BEARING:    // Bearing (Direction)
                switch (gpsApp.getPrefShowDirections()) {
                    case 0:         // NSWE
                        int dr = (int) Math.round(number / 22.5);
                        switch (dr) {
                            case 0:     physicalData.value = gpsApp.getString(R.string.north);             return(physicalData);
                            case 1:     physicalData.value = gpsApp.getString(R.string.north_northeast);   return(physicalData);
                            case 2:     physicalData.value = gpsApp.getString(R.string.northeast);         return(physicalData);
                            case 3:     physicalData.value = gpsApp.getString(R.string.east_northeast);    return(physicalData);
                            case 4:     physicalData.value = gpsApp.getString(R.string.east);              return(physicalData);
                            case 5:     physicalData.value = gpsApp.getString(R.string.east_southeast);    return(physicalData);
                            case 6:     physicalData.value = gpsApp.getString(R.string.southeast);         return(physicalData);
                            case 7:     physicalData.value = gpsApp.getString(R.string.south_southeast);   return(physicalData);
                            case 8:     physicalData.value = gpsApp.getString(R.string.south);             return(physicalData);
                            case 9:     physicalData.value = gpsApp.getString(R.string.south_southwest);   return(physicalData);
                            case 10:    physicalData.value = gpsApp.getString(R.string.southwest);         return(physicalData);
                            case 11:    physicalData.value = gpsApp.getString(R.string.west_southwest);    return(physicalData);
                            case 12:    physicalData.value = gpsApp.getString(R.string.west);              return(physicalData);
                            case 13:    physicalData.value = gpsApp.getString(R.string.west_northwest);    return(physicalData);
                            case 14:    physicalData.value = gpsApp.getString(R.string.northwest);         return(physicalData);
                            case 15:    physicalData.value = gpsApp.getString(R.string.north_northwest);   return(physicalData);
                            case 16:    physicalData.value = gpsApp.getString(R.string.north);             return(physicalData);
                        }
                    case 1:         // Angle
                        physicalData.value = String.valueOf(Math.round(number)).concat("°");
                        return(physicalData);
                }

            case FORMAT_DISTANCE:   // Distance
                switch (gpsApp.getPrefUM()) {
                    case UM_METRIC:
                        if (number < 1000) {
                            physicalData.value = String.format(Locale.getDefault(), "%.0f", (Math.floor(number)));
                            physicalData.um = gpsApp.getString(R.string.UM_m);
                        }
                        else {
                            if (number < 10000) physicalData.value = String.format(Locale.getDefault(), "%.2f" , ((Math.floor(number / 10.0)))/100.0);
                            else physicalData.value = String.format(Locale.getDefault(), "%.1f" , ((Math.floor(number / 100.0)))/10.0);
                            physicalData.um = gpsApp.getString(R.string.UM_km);
                        }
                        return(physicalData);
                    case UM_IMPERIAL:
                        if ((number * M_TO_FT) < 1000) {
                            physicalData.value = String.format(Locale.getDefault(), "%.0f", (Math.floor(number * M_TO_FT)));
                            physicalData.um = gpsApp.getString(R.string.UM_ft);
                        }
                        else {
                            if ((number * KM_TO_MI) < 10000) physicalData.value = String.format(Locale.getDefault(), "%.2f", ((Math.floor((number * KM_TO_MI) / 10.0)))/100.0);
                            else physicalData.value = String.format(Locale.getDefault(), "%.1f", ((Math.floor((number * KM_TO_MI) / 100.0)))/10.0);
                            physicalData.um = gpsApp.getString(R.string.UM_mi);
                        }
                        return(physicalData);
                    case UM_NAUTICAL:
                        if ((number * M_TO_NM) < 100) physicalData.value = String.format(Locale.getDefault(), "%.2f", ((Math.floor((number * M_TO_NM) * 100.0))) / 100.0);
                        else physicalData.value = String.format(Locale.getDefault(), "%.1f", ((Math.floor((number * M_TO_NM) * 10.0))) / 10.0);
                        physicalData.um = gpsApp.getString(R.string.UM_nm);
                        return(physicalData);
                }
        }
        return(physicalData);
    }

    /**
     * It returns a PhysicalData formatted basing on the given criteria and on the Preferences.
     *
     * @param number The double number to format as Physical Data
     * @param format The desired format (FORMAT_LATITUDE, FORMAT_LONGITUDE, FORMAT_ALTITUDE...)
     * @return The Physical Data containing number and unit of measurement
     */
    public PhysicalData format(double number, byte format) {
        PhysicalData physicalData = new PhysicalData();
        physicalData.value = "";
        physicalData.um = "";
        
        if (number == NOT_AVAILABLE) return(physicalData);     // Returns empty fields if the data is not available
        
        switch (format) {
            case FORMAT_LATITUDE:   // Latitude
                physicalData.value = gpsApp.getPrefShowDecimalCoordinates() ?
                    String.format(Locale.getDefault(), "%.9f", Math.abs(number)) :
                        Location.convert(Math.abs(number), Location.FORMAT_SECONDS).replaceFirst(":", "°").replaceFirst(":", "' ").concat("\"");
                physicalData.um = number >= 0 ? gpsApp.getString(R.string.north) : gpsApp.getString(R.string.south);
                return(physicalData);
            case FORMAT_LONGITUDE:  // Longitude
                physicalData.value = gpsApp.getPrefShowDecimalCoordinates() ?
                    String.format(Locale.getDefault(), "%.9f", Math.abs(number)) :
                        Location.convert(Math.abs(number), Location.FORMAT_SECONDS).replaceFirst(":", "°").replaceFirst(":", "' ").concat("\"");
                physicalData.um = number >= 0 ?
                    gpsApp.getString(R.string.east) : gpsApp.getString(R.string.west);
                return(physicalData);
            case FORMAT_ALTITUDE:   // Altitude
                switch (gpsApp.getPrefUM()) {
                    case UM_METRIC:
                        physicalData.value = String.valueOf(Math.round(number));
                        physicalData.um = gpsApp.getString(R.string.UM_m);
                        return(physicalData);
                    case UM_IMPERIAL:
                    case UM_NAUTICAL:
                        physicalData.value = String.valueOf(Math.round(number * M_TO_FT));
                        physicalData.um = gpsApp.getString(R.string.UM_ft);
                        return(physicalData);
                }
            }
        return(physicalData);
    }

    /**
     * It returns a PhysicalData formatted basing on the given criteria and on the Preferences.
     *
     * @param number The long number to format as Physical Data
     * @param format The desired format (FORMAT_LATITUDE, FORMAT_LONGITUDE, FORMAT_ALTITUDE...)
     * @return The Physical Data containing number and unit of measurement
     */
    public PhysicalData format(long number, byte format) {
        PhysicalData physicalData = new PhysicalData();
        physicalData.value = "";
        physicalData.um = "";

        if (number == NOT_AVAILABLE) return(physicalData);     // Returns empty fields if the data is not available

        switch (format) {
            case FORMAT_DURATION:   // Durations
                long time = number / 1000;
                String seconds = Integer.toString((int) (time % 60));
                String minutes = Integer.toString((int) ((time % 3600) / 60));
                String hours = Integer.toString((int) (time / 3600));
                for (int i = 0; i < 2; i++) {
                    if (seconds.length() < 2) {
                        seconds = "0" + seconds;
                    }
                    if (minutes.length() < 2) {
                        minutes = "0" + minutes;
                    }
                    if (hours.length() < 2) {
                        hours = "0" + hours;
                    }
                }
                physicalData.value = hours.equals("00") ? minutes + ":" + seconds : hours + ":" + minutes + ":" + seconds;
                return(physicalData);
            case FORMAT_TIME:   // Timestamps
                if (gpsApp.getPrefShowLocalTime()) {
                    SimpleDateFormat dfdTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());        // date and time formatter
                    SimpleDateFormat dfdTimeZone = new SimpleDateFormat("ZZZZZ", Locale.getDefault());       // timezone formatter
                    physicalData.value = dfdTime.format(number);
                    physicalData.um = dfdTimeZone.format(number);
                    return (physicalData);
                } else {
                    SimpleDateFormat dfdTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());        // date and time formatter
                    dfdTime.setTimeZone(TimeZone.getTimeZone("GMT"));
                    physicalData.value = dfdTime.format(number);
                    return (physicalData);
                }
        }
        return(physicalData);
    }
}