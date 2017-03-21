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
	private static final int UM_METRIC_MS    = 0;
	private static final int UM_METRIC_KMH   = 1;
	private static final int UM_IMPERIAL_FPS = 8;
	private static final int UM_IMPERIAL_MPH = 9;

	public static final byte FORMAT_LATITUDE  = 1;
	public static final byte FORMAT_LONGITUDE = 2;
	public static final byte FORMAT_ALTITUDE  = 3;

	private static final float M_TO_FT = 3.280839895f;
	private static final float MS_TO_MPH = 2.2369363f;

	private PhysicalData _PhysicalData;
	
			
	public PhysicalData format(long Number, byte Format) {
		switch (Format) {
			case FORMAT_LATITUDE:
				
		}
	}
}
    
    
    
