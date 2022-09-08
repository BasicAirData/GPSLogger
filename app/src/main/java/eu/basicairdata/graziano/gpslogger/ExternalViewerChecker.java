/*
 * ExternalViewerChecker - Java Class for Android
 * Created by G.Capelli on 12/7/2020
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
 *
 */

package eu.basicairdata.graziano.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILE_EMPTY_GPX;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILE_EMPTY_KML;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_KML;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_GPX;

/**
 * A class that makes and stores the list of External Viewers available on the device.
 * The list is made of ExternalViewer items.
 */
public class ExternalViewerChecker {

    private final Context context;
    private ArrayList<ExternalViewer> externalViewerList = new ArrayList<>();         // The list of Viewers available
    private final CustomComparator comparator = new CustomComparator(); // The comparator for the ExternalViewer list sorting

    /**
     * The comparator used to orders the ExternalViewer items alphabetically by label (app name)
     */
    static private class CustomComparator implements Comparator<ExternalViewer> {
        @Override
        public int compare(ExternalViewer o1, ExternalViewer o2) {
            return o1.label.compareTo(o2.label);
        }
    }

    /**
     * The class that defines a filtered research.
     */
    static private class FileType {

        /**
         * Creates a new filtered research.
         *
         * @param packages the ArrayList of string that contains the packages to research.
         *                 A package, for example, could be "com.vecturagames.android.app.gpxviewer".
         *                 Pass null to search all packages (no package filter).
         * @param mimeType The Mime Type of the research (for example "application/gpx+xml").
         *                 A Mime Type must be specified.
         * @param fileType The type of file that will be associated to the apps found by the research.
         *                 The valid values are GPSApplication.FILETYPE_KML (the viewer supports KML)
         *                 and GPSApplication.FILETYPE_GPX (the viewer supports GPX.
         */
        FileType (ArrayList<String> packages, String mimeType, String fileType) {
            this.packages = packages;
            this.fileType = fileType;
            this.mimeType = mimeType;
        }

        ArrayList<String> packages;
        String mimeType;
        String fileType;
    }

    public ArrayList<ExternalViewer> getExternalViewersList() {
        return externalViewerList;
    }

    public ExternalViewerChecker(Context context) {
        this.context = context;
    }

    public int size() {
        return externalViewerList.size();
    }

    public boolean isEmpty() {
        return (externalViewerList.isEmpty());
    }

    /**
     * Creates the ExternalViewer list basing on the research criteria.
     * The criteria are defined into this method.
     * The list of ExternalViewer are stored here into this ExternalViewerChecker class,
     * and can be obtained from outside using the getExternalViewersList() method.
     */
    public void makeExternalViewersList() {
        final PackageManager pm = context.getPackageManager();

        externalViewerList = new ArrayList<>();
        ArrayList<FileType> fileTypeList = new ArrayList<>();

        fileTypeList.add(new FileType(null, "application/gpx+xml", FILETYPE_GPX));                      // The preferred format first
        fileTypeList.add(new FileType(null, "application/vnd.google-earth.kml+xml",  FILETYPE_KML));

        // We can add new MimeTypes, also with filtered lists!
        // for example this one:
//        ArrayList<String> gpxList = new ArrayList<>();
//        gpxList.add("com.vecturagames.android.app.gpxviewer");
//        fileTypeList.add(new FileType(gpxList, "application/gpx+xml", FILETYPE_GPX));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        for (FileType ft : fileTypeList) {
            //intent.setType(ft.mimeType);

            File file = new File(ft.mimeType.equals(FILETYPE_GPX) ? FILE_EMPTY_GPX : FILE_EMPTY_KML);
            Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
            intent.setDataAndType(uri, ft.mimeType);

//            File file = new File(ft.mimeType.equals(FILETYPE_GPX) ? EMPTY_GPX : EMPTY_KML);
//            intent.setDataAndType(Uri.fromFile(file), ft.mimeType);

            //Log.w("myApp", "[#] ExternalViewerChecker.java - " + ft.mimeType);
            //Log.w("myApp", "[#] GPSApplication.java - Open with: " + ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
            List<ResolveInfo> kmlLRI = pm.queryIntentActivities(intent, 0);
            //Log.w("myApp", "[#] ExternalViewerChecker.java - Found " + kmlLRI.size() + " viewers:");
            for (ResolveInfo tmpRI : kmlLRI) {
                boolean isPackageInList = false;
                if (ft.packages != null) {
                    for (String s : ft.packages) {
                        if (s.equals(tmpRI.activityInfo.applicationInfo.packageName)) {
                            isPackageInList = true;
                            break;
                        }
                    }
                } else isPackageInList = true;

                if (isPackageInList) {
                    ExternalViewer aInfo = new ExternalViewer();
                    aInfo.packageName = tmpRI.activityInfo.applicationInfo.packageName;
                    aInfo.label = tmpRI.activityInfo.applicationInfo.loadLabel(pm).toString();

                    boolean found = false;

                    for (ExternalViewer a : externalViewerList) {
                        if (a.label.equals(aInfo.label) && a.packageName.equals(aInfo.packageName)) {
                            found = true;
                            //Log.w("myApp", "[#] ExternalViewerChecker.java -   " + tmpRI.activityInfo.applicationInfo.packageName);
                            break;
                        }
                    }
                    if (!found) {
                        aInfo.mimeType = ft.mimeType;
                        aInfo.fileType = ft.fileType;
                        aInfo.icon = tmpRI.activityInfo.applicationInfo.loadIcon(pm);
                        externalViewerList.add(aInfo);
                        //Log.w("myApp", "[#] ExternalViewerChecker.java - + " + tmpRI.activityInfo.applicationInfo.packageName);
                    }
                }
            }
        }

        // Sort List by Package Name
        Collections.sort(externalViewerList, comparator);

        // Apply Exceptions
        for (ExternalViewer a : externalViewerList) {
            if (a.packageName.equals("at.xylem.mapin")) {
                // MAPinr is not opening GPX correctly!
                a.fileType = FILETYPE_KML;
                a.mimeType = "application/vnd.google-earth.kml+xml";
            }

            if (a.packageName.equals("com.mapswithme.maps.pro")) {
                // MAPS.ME v.14 is not opening GPX anymore!
                a.fileType = FILETYPE_KML;
                a.mimeType = "application/vnd.google-earth.kml+xml";
            }

            if (a.packageName.equals("app.organicmaps")) {
                // Organic Maps does not support GPX, only KML
                a.fileType = FILETYPE_KML;
                a.mimeType = "application/vnd.google-earth.kml+xml";
            }

//            if (a.packageName.equals("com.vecturagames.android.app.gpxviewer")) {
//                // GPX Viewer saves a copy of the file if passed via FileProvider
//                a.requiresFileProvider = false;
//            }

//            if (a.packageName.equals("com.google.earth")) {
//                // Google Earth opens file with fileProvider only
//                a.requiresFileProvider = true;
//            }
        }
    }
}
