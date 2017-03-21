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

public class PhysicalDataFormatter {
    private static final int NOT_AVAILABLE = -100000;
    
    private static final int UM_METRIC_MS       = 0;
    private static final int UM_METRIC_KMH      = 1;
    private static final int UM_IMPERIAL_FPS    = 8;
    private static final int UM_IMPERIAL_MPH    = 9;

    public static final byte FORMAT_LATITUDE    = 1;
    public static final byte FORMAT_LONGITUDE   = 2;
    public static final byte FORMAT_ALTITUDE    = 3;
    public static final byte FORMAT_SPEED       = 4;
    public static final byte FORMAT_ACCURACY    = 5;
    public static final byte FORMAT_BEARING     = 6;
    
    private static final float M_TO_FT = 3.280839895f;
    private static final float MS_TO_MPH = 2.2369363f;
    private static final float MS_TO_KMH = 3.6f;
    
    private PhysicalData _PhysicalData;
    private GPSApplication gpsApplication = GPSApplication.getInstance();
        
    
    public PhysicalData format(float Number, byte Format) {
        _PhysicalData.Value = "";
        _PhysicalData.UM = "";
        
        if (Number == NOT_AVAILABLE) return(_PhysicalData);     // Returns empty fields if the data is not available
        
        switch (Format) {
            case FORMAT_SPEED:   // Speed
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * MS_TO_KMH));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_km_h);
                    break;
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m_s);
                    break;
                    case UM_IMPERIAL_MPH:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * MS_TO_MPH));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_mph);
                    break;
                    case UM_IMPERIAL_FPS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_fps);
                    break;
                }
            break;
            case FORMAT_ACCURACY:   // Accuracy
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m);
                    break;
                    case UM_IMPERIAL_MPH:
                    case UM_IMPERIAL_FPS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_ft);
                    break;
                }
            break;
            case FORMAT_BEARING:   // Bearing (Direction)
                switch (gpsApplication.getPrefShowDirections()) {
                    case 0:         // NSWE
                        final String N = gpsApplication.getString(R.string.north);
                        final String S = gpsApplication.getString(R.string.south);
                        final String W = gpsApplication.getString(R.string.west);
                        final String E = gpsApplication.getString(R.string.east);
                        int dr = (int) Math.round(_Location.getBearing() / 22.5);
                        switch (dr) {
                            // TODO - Import code from getFormattedBearing;
                        }
                    break;
                }
            break;
            return(_PhysicalData);
        }
        
        return(_PhysicalData);
    }
    
    public PhysicalData format(double Number, byte Format) {
        _PhysicalData.Value = "";
        _PhysicalData.UM = "";
        
        if (Number == NOT_AVAILABLE) return(_PhysicalData);     // Returns empty fields if the data is not available
        
        switch (Format) {
            case FORMAT_LATITUDE:   // Latitude
                _PhysicalData.Value = gpsApplication.getPrefShowDecimalCoordinates() ? 
                    String.format("%.9f", Math.abs(Number)) : Location.convert(Math.abs(Number), Location.FORMAT_SECONDS);
                _PhysicalData.UM = Number >= 0 ?
                    gpsApplication.getString(R.string.north) : gpsApplication.getString(R.string.south);
            break;
                
            case FORMAT_LONGITUDE:  // Longitude
                _PhysicalData.Value = gpsApplication.getPrefShowDecimalCoordinates() ?
                    String.format("%.9f", Math.abs(Number)) : Location.convert(Math.abs(Number), Location.FORMAT_SECONDS);
                _PhysicalData.UM = Number >= 0 ?
                    gpsApplication.getString(R.string.east) : gpsApplication.getString(R.string.west);
            break;
                
            case FORMAT_ALTITUDE:   // Altitude
                switch (gpsApplication.getPrefUM()) {
                    case UM_METRIC_KMH:
                    case UM_METRIC_MS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_m);
                    break;
                    case UM_IMPERIAL_MPH:
                    case UM_IMPERIAL_FPS:
                        _PhysicalData.Value = String.valueOf(Math.round(Number * M_TO_FT));
                        _PhysicalData.UM = gpsApplication.getString(R.string.UM_ft);
                    break;
                }
            break;
            }
            return(_PhysicalData);
        }
    }
}
