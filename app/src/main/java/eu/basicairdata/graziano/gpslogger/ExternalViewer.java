/*
 * ExternalViewer - Java Class for Android
 * Created by G.Capelli on 23/9/2020
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

import android.graphics.drawable.Drawable;

/**
 * The data structure that describes a Track Viewer.
 */
public class ExternalViewer {
    String label = "";                      // The name of the app
    String packageName = "";                // The full package name
    String mimeType = "";                   // The mimetype to use with the ACTION_VIEW intent (for example "application/gpx+xml")
    String fileType = "";                   // "GPX" or "KML"
    Drawable icon = null;                   // The app's icon
}