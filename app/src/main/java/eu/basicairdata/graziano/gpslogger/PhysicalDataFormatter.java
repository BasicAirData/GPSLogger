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
    public static final int UM_METRIC_MS    = 0;
    public static final int UM_METRIC_KMH   = 1;
    public static final int UM_IMPERIAL_FPS = 8;
    public static final int UM_IMPERIAL_MPH = 9;
    
    private static final float M_TO_FT = 3.280839895f;
    private static final float MS_TO_MPH = 2.2369363f;
    
    public PhysicalData format(long Number, byte ... TODO)
