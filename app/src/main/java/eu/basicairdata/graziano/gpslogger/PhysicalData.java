/*
 * PhysicalData - Java Class for Android
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

/**
 * The data structure that defines a physical measurement.
 * A physical data includes a number and a unit of measurement.
 */
class PhysicalData {

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

    String value;       // The Numerical value of the Physical Quantity
    String um;          // The Unit of Measurement
}