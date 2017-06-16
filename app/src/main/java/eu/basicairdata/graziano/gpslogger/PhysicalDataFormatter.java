/*
 * PhysicalDataFormatter - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 21/3/2017
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

class PhysicalDataFormatter {

    private final int NOT_AVAILABLE = -100000;

    private final int UM_METRIC_MS       = 0;
    private final int UM_METRIC_KMH      = 1;
    private final int UM_IMPERIAL_FPS    = 8;
    private final int UM_IMPERIAL_MPH    = 9;
    private final int UM_NAUTICAL_KN     = 16;
    private final int UM_NAUTICAL_MPH    = 17;

    static final byte FORMAT_LATITUDE    = 1;
    static final byte FORMAT_LONGITUDE   = 2;
    static final byte FORMAT_ALTITUDE    = 3;
    static final byte FORMAT_SPEED       = 4;
    static final byte FORMAT_ACCURACY    = 5;
    static final byte FORMAT_BEARING     = 6;
    static final byte FORMAT_DURATION    = 7;
    static final byte FORMAT_SPEED_AVG   = 8;
    static final byte FORMAT_DISTANCE    = 9;
    
    private final float M_TO_FT   = 3.280839895f;
    private final float M_TO_NM   = 0.000539957f;
    private final float MS_TO_MPH = 2.2369363f;
    private final float MS_TO_KMH = 3.6f;
    private final float MS_TO_KN  = 1.943844491f;
    private final float KM_TO_MI  = 0.621371192237f;
    
    //private PhysicalData _PhysicalData = new PhysicalData();
    private GPSApplication gpsApplication = GPSApplication.getInstance();
        
    
    public PhysicalData format(float Number, byte Format) {
        PhysicalData _PhysicalData = new PhysicalData();
        _PhysicalData.Value = "";
        _PhysicalData.UM = "";
        
        if (Number == NOT_AVAILABLE) return(_PhysicalData);     // Returns empty fields if the data is not available
        
        switch (Format) {
            case FORMAT_SPEED:  // Speed
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * MS_TO_KMH));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_km_h);
                        return(_PhysicalData);
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m_s);
                        return(_PhysicalData);
                    case UM_IMPERIAL_MPH:
                    case UM_NAUTICAL_MPH:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * MS_TO_MPH));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_mph);
                        return(_PhysicalData);
                    case UM_IMPERIAL_FPS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_fps);
                        return(_PhysicalData);
                    case UM_NAUTICAL_KN:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * MS_TO_KN));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_kn);
                        return(_PhysicalData);
                }

            case FORMAT_SPEED_AVG:  // Average Speed, formatted with 1 decimal
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                        _PhysicalData.Value = String.format("%.1f", (Number * MS_TO_KMH));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_km_h);
                        return(_PhysicalData);
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.format("%.1f", (Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m_s);
                        return(_PhysicalData);
                    case UM_IMPERIAL_MPH:
                    case UM_NAUTICAL_MPH:
                        _PhysicalData.Value = String.format("%.1f", (Number * MS_TO_MPH));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_mph);
                        return(_PhysicalData);
                    case UM_IMPERIAL_FPS:
                        _PhysicalData.Value = String.format("%.1f", (Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_fps);
                        return(_PhysicalData);
                    case UM_NAUTICAL_KN:
                        _PhysicalData.Value = String.format("%.1f", (Number * MS_TO_KN));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_kn);
                        return(_PhysicalData);
                }

            case FORMAT_ACCURACY:   // Accuracy
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m);
                        return(_PhysicalData);
                    case UM_IMPERIAL_MPH:
                    case UM_IMPERIAL_FPS:
                    case UM_NAUTICAL_MPH:
                    case UM_NAUTICAL_KN:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_ft);
                        return(_PhysicalData);
                }

            case FORMAT_BEARING:    // Bearing (Direction)
                switch (gpsApplication.getPrefShowDirections()) {
                    case 0:         // NSWE
                        final String N = gpsApplication.getString(R.string.north);
                        final String S = gpsApplication.getString(R.string.south);
                        final String W = gpsApplication.getString(R.string.west);
                        final String E = gpsApplication.getString(R.string.east);
                        int dr = (int) Math.round(Number / 22.5);
                        switch (dr) {
                            case 0:     _PhysicalData.Value = N;            return(_PhysicalData);
                            case 1:     _PhysicalData.Value = N + N + E;    return(_PhysicalData);
                            case 2:     _PhysicalData.Value = N + E;        return(_PhysicalData);
                            case 3:     _PhysicalData.Value = E + N + E;    return(_PhysicalData);
                            case 4:     _PhysicalData.Value = E;            return(_PhysicalData);
                            case 5:     _PhysicalData.Value = E + S + E;    return(_PhysicalData);
                            case 6:     _PhysicalData.Value = S + E;        return(_PhysicalData);
                            case 7:     _PhysicalData.Value = S + S + E;    return(_PhysicalData);
                            case 8:     _PhysicalData.Value = S;            return(_PhysicalData);
                            case 9:     _PhysicalData.Value = S + S + W;    return(_PhysicalData);
                            case 10:    _PhysicalData.Value = S + W;        return(_PhysicalData);
                            case 11:    _PhysicalData.Value = W + S + W;    return(_PhysicalData);
                            case 12:    _PhysicalData.Value = W;            return(_PhysicalData);
                            case 13:    _PhysicalData.Value = W + N + W;    return(_PhysicalData);
                            case 14:    _PhysicalData.Value = N + W;        return(_PhysicalData);
                            case 15:    _PhysicalData.Value = N + N + W;    return(_PhysicalData);
                            case 16:    _PhysicalData.Value = N;            return(_PhysicalData);
                        }
                    case 1:         // Angle
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        return(_PhysicalData);
                }

            case FORMAT_DISTANCE:   // Distance
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                    case UM_METRIC_MS:
                        if (Number < 1000) {
                            _PhysicalData.Value = String.format("%.0f", (Math.floor(Number)));
                            _PhysicalData.UM = gpsApplication.getString(R.string.UM_m);
                        }
                        else {
                            if (Number < 10000) _PhysicalData.Value = String.format("%.2f" , ((Math.floor(Number / 10.0)))/100.0);
                            else _PhysicalData.Value = String.format("%.1f" , ((Math.floor(Number / 100.0)))/10.0);
                            _PhysicalData.UM = gpsApplication.getString(R.string.UM_km);
                        }
                        return(_PhysicalData);
                    case UM_IMPERIAL_MPH:
                    case UM_IMPERIAL_FPS:
                        if ((Number * M_TO_FT) < 1000) {
                            _PhysicalData.Value = String.format("%.0f", (Math.floor(Number * M_TO_FT)));
                            _PhysicalData.UM = gpsApplication.getString(R.string.UM_ft);
                        }
                        else {
                            if ((Number * KM_TO_MI) < 10000) _PhysicalData.Value = String.format("%.2f", ((Math.floor((Number * KM_TO_MI) / 10.0)))/100.0);
                            else _PhysicalData.Value = String.format("%.1f", ((Math.floor((Number * KM_TO_MI) / 100.0)))/10.0);
                            _PhysicalData.UM = gpsApplication.getString(R.string.UM_mi);
                        }
                        return(_PhysicalData);
                    case UM_NAUTICAL_KN:
                    case UM_NAUTICAL_MPH:
                        if ((Number * M_TO_NM) < 100) _PhysicalData.Value = String.format("%.2f", ((Math.floor((Number * M_TO_NM) * 100.0))) / 100.0);
                        else _PhysicalData.Value = String.format("%.1f", ((Math.floor((Number * M_TO_NM) * 10.0))) / 10.0);
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_nm);
                        return(_PhysicalData);
                }
        }
        return(_PhysicalData);
    }
    
    public PhysicalData format(double Number, byte Format) {
        PhysicalData _PhysicalData = new PhysicalData();
        _PhysicalData.Value = "";
        _PhysicalData.UM = "";
        
        if (Number == NOT_AVAILABLE) return(_PhysicalData);     // Returns empty fields if the data is not available
        
        switch (Format) {
            case FORMAT_LATITUDE:   // Latitude
                _PhysicalData.Value = gpsApplication.getPrefShowDecimalCoordinates() ? 
                    String.format("%.9f", Math.abs(Number)) : Location.convert(Math.abs(Number), Location.FORMAT_SECONDS);
                _PhysicalData.UM = Number >= 0 ? gpsApplication.getString(R.string.north) : gpsApplication.getString(R.string.south);
                return(_PhysicalData);
                
            case FORMAT_LONGITUDE:  // Longitude
                _PhysicalData.Value = gpsApplication.getPrefShowDecimalCoordinates() ?
                    String.format("%.9f", Math.abs(Number)) : Location.convert(Math.abs(Number), Location.FORMAT_SECONDS);
                _PhysicalData.UM = Number >= 0 ?
                    gpsApplication.getString(R.string.east) : gpsApplication.getString(R.string.west);
                return(_PhysicalData);
                
            case FORMAT_ALTITUDE:   // Altitude
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m);
                        return(_PhysicalData);
                    case UM_IMPERIAL_MPH:
                    case UM_IMPERIAL_FPS:
                    case UM_NAUTICAL_KN:
                    case UM_NAUTICAL_MPH:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_ft);
                        return(_PhysicalData);
                }
            }
        return(_PhysicalData);
    }

    public PhysicalData format(long Number, byte Format) {
        PhysicalData _PhysicalData = new PhysicalData();
        _PhysicalData.Value = "";
        _PhysicalData.UM = "";

        if (Number == NOT_AVAILABLE) return(_PhysicalData);     // Returns empty fields if the data is not available

        switch (Format) {
            case FORMAT_DURATION:   // Durations
                long time = Number / 1000;
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
                _PhysicalData.Value = hours.equals("00") ? minutes + ":" + seconds : hours + ":" + minutes + ":" + seconds;
                return(_PhysicalData);
        }
        return(_PhysicalData);
    }
}
