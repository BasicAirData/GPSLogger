/*
 * Exporter - Java Class for Android
 * Created by G.Capelli on 16/7/2016
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

import android.net.Uri;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.getInstance;

import androidx.documentfile.provider.DocumentFile;

/**
 * A Thread that performs the exportation of a Track in KML, GPX, and/or TXT format.
 * The files exported and the destination folder depend on the input parameters.
 */
class Exporter extends Thread {

    private final Track track;
    private final ExportingTask exportingTask;
    private final boolean exportKML;
    private final boolean exportGPX;
    private final boolean exportTXT;
    private final String saveIntoFolder;
    private final double altitudeManualCorrection;
    private final boolean egmAltitudeCorrection;
    private final int getPrefKMLAltitudeMode;
    private final int getPrefGPXVersion;
    private boolean txtFirstTrackpointFlag = true;

    private DocumentFile kmlFile;
    private DocumentFile gpxFile;
    private DocumentFile txtFile;

    int groupOfLocations;                           // Reads and writes location grouped by this number;

    private final ArrayBlockingQueue<LocationExtended> arrayGeopoints = new ArrayBlockingQueue<>(3500);
    private final AsyncGeopointsLoader asyncGeopointsLoader = new AsyncGeopointsLoader();

    /**
     * Converts a String in a format suitable for GPX/KML files,
     * by replacing the invalid characters with the corresponding HTML sequences.
     *
     * @param str The input String
     * @return the string formatted for XML files
     */
    private String stringToXML(String str) {
        if (str == null) return "";
        return str.replace("<","&lt;")
                  .replace("&","&amp;")
                  .replace(">","&gt;")
                  .replace("\"","&quot;")
                  .replace("'","&apos;");
    }

    /**
     * Converts a String in a format suitable for the CDATA fields on KML files,
     * by replacing the invalid characters with the corresponding HTML sequences.
     *
     * @param str The input String
     * @return the string formatted for CDATA fields of KML files
     */
    private String stringToCDATA(String str) {
        if (str == null) return "";
        return str.replace("[","(")
                .replace("]",")")
                .replace("<","(")
                .replace(">",")");
    }

    /**
     * Creates the files that will be written by the Exporter.
     * The method try to assign the name specified as input,
     *
     * @param fName The file name (without path and .extension)
     * @return true if the operation succeed
     */
    private boolean tryToInitFiles(String fName) {
        // Create files, deleting old version if exists

        try {
            DocumentFile pickedDir;
            if (saveIntoFolder.startsWith("content")) {
                Uri uri = Uri.parse(saveIntoFolder);
                pickedDir = DocumentFile.fromTreeUri(getInstance(), uri);
            } else {
                pickedDir = DocumentFile.fromFile(new File(saveIntoFolder));
            }
            if (!pickedDir.exists()) {
                Log.w("myApp", "[#] Exporter.java - UNABLE TO CREATE THE FOLDER");
                exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
                return false;
            }

            if (exportKML) {
                kmlFile = pickedDir.findFile(fName + ".kml");
                if ((kmlFile != null) && (kmlFile.exists())) kmlFile.delete();
                kmlFile = pickedDir.createFile("", fName + ".kml");
                Log.w("myApp", "[#] Exporter.java - Export " + kmlFile.getUri().toString());
            }
            if (exportGPX) {
                gpxFile = pickedDir.findFile(fName + ".gpx");
                if ((gpxFile != null) && (gpxFile.exists())) gpxFile.delete();
                gpxFile = pickedDir.createFile("", fName + ".gpx");
                Log.w("myApp", "[#] Exporter.java - Export " + gpxFile.getUri().toString());
            }
            if (exportTXT) {
                txtFile = pickedDir.findFile(fName + ".txt");
                if ((txtFile != null) && (txtFile.exists())) txtFile.delete();
                txtFile = pickedDir.createFile("", fName + ".txt");
                Log.w("myApp", "[#] Exporter.java - Export " + txtFile.getUri().toString());
            }
        } catch (SecurityException e) {
            Log.w("myApp", "[#] Exporter.java - Unable to write the file: SecurityException");
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            return false;
        } catch (NullPointerException e) {
            Log.w("myApp", "[#] Exporter.java - Unable to write the file: IOException");
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            return false;
        }
        return true;
    }

    /**
     * The Thread that performs the exportation of the Track in KML, GPX, and/or TXT format.
     * The files exported and the destination folder depend on the input parameters.
     *
     * @param exportingTask the associated ExportingTask
     * @param exportKML if true, the KML file will be created
     * @param exportGPX if true, the GPX file will be created
     * @param exportTXT if true, the TXT file will be created
     * @param saveIntoFolder The folder where the created files will be placed
     */
    public Exporter(ExportingTask exportingTask, boolean exportKML, boolean exportGPX, boolean exportTXT, String saveIntoFolder) {
        this.exportingTask = exportingTask;
        this.exportingTask.setNumberOfPoints_Processed(0);
        this.exportingTask.setStatus(ExportingTask.STATUS_RUNNING);
        this.track = GPSApplication.getInstance().gpsDataBase.getTrack(exportingTask.getId());
        this.altitudeManualCorrection = GPSApplication.getInstance().getPrefAltitudeCorrection();
        this.egmAltitudeCorrection = GPSApplication.getInstance().getPrefEGM96AltitudeCorrection();
        this.getPrefKMLAltitudeMode = GPSApplication.getInstance().getPrefKMLAltitudeMode();
        this.getPrefGPXVersion = GPSApplication.getInstance().getPrefGPXVersion();
        this.exportTXT = exportTXT;
        this.exportGPX = exportGPX;
        this.exportKML = exportKML;
        this.saveIntoFolder = saveIntoFolder;

        int formats = 0;
        if (exportKML) formats++;
        if (exportGPX) formats++;
        if (exportTXT) formats++;
        if (formats == 1) groupOfLocations = 1500;
        else {
            groupOfLocations = 1900;
            if (exportKML) groupOfLocations -= 200;     // KML is a light format, less time to write file
            if (exportTXT) groupOfLocations -= 800;     //
            if (exportGPX) groupOfLocations -= 600;     // GPX is the heavier format, more time to write the file
        }
    }


    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        Log.w("myApp", "[#] Exporter.java - STARTED");

        kmlFile = null;
        gpxFile = null;
        txtFile = null;

        final int GPX1_0 = 100;
        final int GPX1_1 = 110;

        Date creationTime;
        long elements_total;
        String versionName = BuildConfig.VERSION_NAME;
        GPSApplication gpsApp = GPSApplication.getInstance();
        if (gpsApp == null) return;

        elements_total = track.getNumberOfLocations() + track.getNumberOfPlacemarks();
        long startTime = System.currentTimeMillis();

        // ----------------------------------------------------------------------------------------

        if (track == null) {
            //Log.w("myApp", "[#] Exporter.java - Track = null!!");
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            return;
        }
        if (track.getNumberOfLocations() + track.getNumberOfPlacemarks() == 0) {
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            return;
        }

        //EventBus.getDefault().post(new EventBusMSGLong(EventBusMSG.TRACK_SETPROGRESS, track.getId(), 1));

        if (egmAltitudeCorrection && EGM96.getInstance().isLoading()) {
            try {
                Log.w("myApp", "[#] Exporter.java - Wait, EGMGrid is loading");
                do {
                    Thread.sleep(200);
                    // Lazy polling until EGM grid finish to load
                } while (EGM96.getInstance().isLoading());
            } catch (InterruptedException e) {
                Log.w("myApp", "[#] Exporter.java - Cannot wait!!");
            }
        }

        SimpleDateFormat dfdtGPX = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);        // date and time formatter for GPX timestamp (with millis)
        dfdtGPX.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat dfdtGPX_NoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);   // date and time formatter for GPX timestamp (without millis)
        dfdtGPX_NoMillis.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat dfdtTXT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS", Locale.US);           // date and time formatter for TXT timestamp (with millis)
        dfdtTXT.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat dfdtTXT_NoMillis = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);      // date and time formatter for TXT timestamp (without millis)
        dfdtTXT_NoMillis.setTimeZone(TimeZone.getTimeZone("GMT"));

        //final String newLine = System.getProperty("line.separator"); //\n\r
        final String newLine = "\r\n";

        // If the file is not writable abort exportation:
        boolean fileWritable = tryToInitFiles(gpsApp.getFileName(track));               // Try to use the name with the description
        //if (!fileWritable) fileWritable = tryToInitFiles(track.getName());  // else try to use the name without description
        if (!fileWritable) {
            Log.w("myApp", "[#] Exporter.java - Unable to write the file!!");
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            return;
        }

        // Create buffers for Write operations
        BufferedWriter kmlBW = null;
        BufferedWriter gpxBW = null;
        BufferedWriter txtBW = null;

        asyncGeopointsLoader.start();

        try {
            if (exportKML) {
                OutputStream outputStream = GPSApplication.getInstance().getContentResolver().openOutputStream(kmlFile.getUri(), "rw");
                kmlBW = new BufferedWriter(new OutputStreamWriter(outputStream));
            }
            if (exportGPX) {
                OutputStream outputStream = GPSApplication.getInstance().getContentResolver().openOutputStream(gpxFile.getUri(), "rw");
                gpxBW = new BufferedWriter(new OutputStreamWriter(outputStream));
            }
            if (exportTXT) {
                OutputStream outputStream = GPSApplication.getInstance().getContentResolver().openOutputStream(txtFile.getUri(), "rw");
                txtBW = new BufferedWriter(new OutputStreamWriter(outputStream));
            }

            creationTime = Calendar.getInstance().getTime();

            // ---------------------------------------------------------------------- Writing Heads
            Log.w("myApp", "[#] Exporter.java - Writing Heads");

            if (exportKML) {
                // Writing head of KML file

                kmlBW.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
                kmlBW.write("<!-- Created with BasicAirData GPS Logger for Android - ver. " + versionName + " -->" + newLine);
                kmlBW.write("<!-- Track " + String.valueOf(track.getId()) + " = " + String.valueOf(track.getNumberOfLocations())
                        + " TrackPoints + " + String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks -->" + newLine);
                kmlBW.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">" + newLine);
                kmlBW.write(" <Document>" + newLine);
                kmlBW.write("  <name>GPS Logger " + track.getName() + "</name>" + newLine);
                kmlBW.write("  <description><![CDATA[" + (track.getDescription().isEmpty() ? "" : stringToCDATA(track.getDescription()) + newLine));
                kmlBW.write(String.valueOf(track.getNumberOfLocations()) + " Trackpoints + " + String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks]]></description>" + newLine);
                if (track.getNumberOfLocations() > 0) {
                    kmlBW.write("  <Style id=\"TrackStyle\">" + newLine);
                    kmlBW.write("   <LineStyle>" + newLine);
                    kmlBW.write("    <color>ff0000ff</color>" + newLine);
                    kmlBW.write("    <width>3</width>" + newLine);
                    kmlBW.write("   </LineStyle>" + newLine);
                    kmlBW.write("   <PolyStyle>" + newLine);
                    kmlBW.write("    <color>7f0000ff</color>" + newLine);
                    kmlBW.write("   </PolyStyle>" + newLine);
                    kmlBW.write("   <BalloonStyle>" + newLine);
                    kmlBW.write("    <text><![CDATA[<p style=\"color:red;font-weight:bold\">$[name]</p><p style=\"font-size:11px\">$[description]</p><p style=\"font-size:7px\">" +
                            gpsApp.getApplicationContext().getString(R.string.pref_track_stats) + ": " +
                            gpsApp.getApplicationContext().getString(R.string.pref_track_stats_totaltime) + " | " +
                            gpsApp.getApplicationContext().getString(R.string.pref_track_stats_movingtime) + "</p>]]></text>" + newLine);
                    kmlBW.write("   </BalloonStyle>" + newLine);
                    kmlBW.write("  </Style>" + newLine);
                }
                if (track.getNumberOfPlacemarks() > 0) {
                    kmlBW.write("  <Style id=\"PlacemarkStyle\">" + newLine);
                    kmlBW.write("   <IconStyle>" + newLine);
                    kmlBW.write("    <Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle_highlight.png</href></Icon>" + newLine);
                    kmlBW.write("   </IconStyle>" + newLine);
                    kmlBW.write("  </Style>" + newLine);
                }
                kmlBW.write(newLine);
            }

            if (exportGPX) {
                // Writing head of GPX file

                gpxBW.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
                gpxBW.write("<!-- Created with BasicAirData GPS Logger for Android - ver. " + versionName + " -->" + newLine);
                gpxBW.write("<!-- Track " + String.valueOf(track.getId()) + " = " + String.valueOf(track.getNumberOfLocations())
                        + " TrackPoints + " + String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks -->" + newLine + newLine);

                if (track.getNumberOfLocations() > 0) {
                    gpxBW.write("<!-- Track Statistics (based on Total Time | Time in Movement): -->" + newLine);
                    PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
                    PhysicalData phdDuration;
                    PhysicalData phdDurationMoving;
                    PhysicalData phdSpeedMax;
                    PhysicalData phdSpeedAvg;
                    PhysicalData phdSpeedAvgMoving;
                    PhysicalData phdDistance;
                    PhysicalData phdAltitudeGap;
                    PhysicalData phdOverallDirection;
                    phdDuration = phdformatter.format(track.getDuration(), PhysicalDataFormatter.FORMAT_DURATION);
                    phdDurationMoving = phdformatter.format(track.getDurationMoving(), PhysicalDataFormatter.FORMAT_DURATION);
                    phdSpeedMax = phdformatter.format(track.getSpeedMax(), PhysicalDataFormatter.FORMAT_SPEED);
                    phdSpeedAvg = phdformatter.format(track.getSpeedAverage(), PhysicalDataFormatter.FORMAT_SPEED_AVG);
                    phdSpeedAvgMoving = phdformatter.format(track.getSpeedAverageMoving(), PhysicalDataFormatter.FORMAT_SPEED_AVG);
                    phdDistance = phdformatter.format(track.getEstimatedDistance(), PhysicalDataFormatter.FORMAT_DISTANCE);
                    phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(gpsApp.getPrefEGM96AltitudeCorrection()), PhysicalDataFormatter.FORMAT_ALTITUDE);
                    phdOverallDirection = phdformatter.format(track.getBearing(), PhysicalDataFormatter.FORMAT_BEARING);

                    if (!phdDistance.value.isEmpty())
                        gpxBW.write("<!--  Distance = " + phdDistance.value + " " + phdDistance.um + " -->" + newLine);
                    if (!phdDuration.value.isEmpty())
                        gpxBW.write("<!--  Duration = " + phdDuration.value + " | " + phdDurationMoving.value + " -->" + newLine);
                    if (!phdAltitudeGap.value.isEmpty())
                        gpxBW.write("<!--  Altitude Gap = " + phdAltitudeGap.value + " " + phdAltitudeGap.um + " -->" + newLine);
                    if (!phdSpeedMax.value.isEmpty())
                        gpxBW.write("<!--  Max Speed = " + phdSpeedMax.value + " " + phdSpeedMax.um + " -->" + newLine);
                    if (!phdSpeedAvg.value.isEmpty())
                        gpxBW.write("<!--  Avg Speed = " + phdSpeedAvg.value + " | " + phdSpeedAvgMoving.value + " " + phdSpeedAvg.um + " -->" + newLine);
                    if (!phdOverallDirection.value.isEmpty())
                        gpxBW.write("<!--  Direction = " + phdOverallDirection.value + phdOverallDirection.um + " -->" + newLine);
                    if (track.getEstimatedTrackType() != NOT_AVAILABLE)
                        gpxBW.write("<!--  Activity = " + Track.ACTIVITY_DESCRIPTION[track.getEstimatedTrackType()] + " -->" + newLine);

                    gpxBW.write("<!--  Altitudes = "
                            + (egmAltitudeCorrection ? "Corrected using EGM96 grid (bilinear interpolation)" : "Raw")
                            + (altitudeManualCorrection == 0 ? "" : (", " + (String.format(Locale.US, "%+.3f", altitudeManualCorrection) + "m of manual offset")))
                            + " -->" + newLine);

                    gpxBW.write(newLine);
                }

                if (getPrefGPXVersion == GPX1_0) {     // GPX 1.0
                    gpxBW.write("<gpx version=\"1.0\"" + newLine
                              + "     creator=\"BasicAirData GPS Logger " + versionName + "\"" + newLine
                              + "     xmlns=\"http://www.topografix.com/GPX/1/0\"" + newLine
                              + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine
                              + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" + newLine);
                    gpxBW.write("<name>GPS Logger " + track.getName() + "</name>" + newLine);
                    if (!track.getDescription().isEmpty()) gpxBW.write("<desc>" + stringToXML(track.getDescription()) + "</desc>" + newLine);
                    gpxBW.write("<time>" + dfdtGPX_NoMillis.format(creationTime) + "</time>" + newLine);
                    if (track.getEstimatedTrackType() != NOT_AVAILABLE) gpxBW.write("<keywords>" + Track.ACTIVITY_DESCRIPTION[track.getEstimatedTrackType()] + "</keywords>" + newLine);
                    if ((track.getValidMap() != 0)
                            && (track.getLatitudeMin() != NOT_AVAILABLE) && (track.getLongitudeMin() != NOT_AVAILABLE)
                            && (track.getLatitudeMax() != NOT_AVAILABLE) && (track.getLongitudeMax() != NOT_AVAILABLE)) {
                        gpxBW.write("<bounds minlat=\"" + String.format(Locale.US, "%.8f", track.getLatitudeMin())
                                + "\" minlon=\"" + String.format(Locale.US, "%.8f", track.getLongitudeMin())
                                + "\" maxlat=\"" + String.format(Locale.US, "%.8f", track.getLatitudeMax())
                                + "\" maxlon=\"" + String.format(Locale.US, "%.8f", track.getLongitudeMax())
                                + "\" />" + newLine);
                    }
                    gpxBW.write(newLine);
                }

                if (getPrefGPXVersion == GPX1_1) {    // GPX 1.1
                    gpxBW.write("<gpx version=\"1.1\"" + newLine
                              + "     creator=\"BasicAirData GPS Logger " + versionName + "\"" + newLine
                              + "     xmlns=\"http://www.topografix.com/GPX/1/1\"" + newLine
                              + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine
                              + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">" + newLine);
                    //          + "     xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"" + newLine           // Garmin extension to include speeds
                    //          + "     xmlns:gpxtrkx=\"http://www.garmin.com/xmlschemas/TrackStatsExtension/v1\"" + newLine  //
                    //          + "     xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">" + newLine); //
                    gpxBW.write("<metadata> " + newLine);    // GPX Metadata
                    gpxBW.write(" <name>GPS Logger " + track.getName() + "</name>" + newLine);
                    if (!track.getDescription().isEmpty()) gpxBW.write(" <desc>" + stringToXML(track.getDescription()) + "</desc>" + newLine);
                    gpxBW.write(" <time>" + dfdtGPX_NoMillis.format(creationTime) + "</time>" + newLine);
                    if (track.getEstimatedTrackType() != NOT_AVAILABLE) gpxBW.write(" <keywords>" + Track.ACTIVITY_DESCRIPTION[track.getEstimatedTrackType()] + "</keywords>" + newLine);
                    if ((track.getValidMap() != 0)
                            && (track.getLatitudeMin() != NOT_AVAILABLE) && (track.getLongitudeMin() != NOT_AVAILABLE)
                            && (track.getLatitudeMax() != NOT_AVAILABLE) && (track.getLongitudeMax() != NOT_AVAILABLE)) {
                        gpxBW.write(" <bounds minlat=\"" + String.format(Locale.US, "%.8f", track.getLatitudeMin())
                                + "\" minlon=\"" + String.format(Locale.US, "%.8f", track.getLongitudeMin())
                                + "\" maxlat=\"" + String.format(Locale.US, "%.8f", track.getLatitudeMax())
                                + "\" maxlon=\"" + String.format(Locale.US, "%.8f", track.getLongitudeMax())
                                + "\" />" + newLine);
                    }
                    gpxBW.write("</metadata>" + newLine + newLine);
                }
            }

            if (exportTXT) {
                // Writing head of TXT file
                txtBW.write("type,date time,latitude,longitude,accuracy(m),altitude(m),geoid_height(m),speed(m/s),bearing(deg),sat_used,sat_inview,name,desc" + newLine);
            }

            String formattedLatitude = "";
            String formattedLongitude = "";
            String formattedAltitude = "";
            String formattedSpeed = "";

            // ---------------------------------------------------------------- Writing Placemarks
            Log.w("myApp", "[#] Exporter.java - Writing Placemarks");

            if (track.getNumberOfPlacemarks() > 0) {
                int placemark_id = 1;                   // It is used to add a progressive "id" to Placemarks

                // Writes track headings

                List<LocationExtended> placemarkList = new ArrayList<>(groupOfLocations);

                for (int i = 0; i <= track.getNumberOfPlacemarks(); i += groupOfLocations) {
                    //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                    placemarkList.addAll(gpsApp.gpsDataBase.getPlacemarksList(track.getId(), i, i + groupOfLocations - 1));

                    if (!placemarkList.isEmpty()) {
                        for (LocationExtended loc : placemarkList) {
                            formattedLatitude = String.format(Locale.US, "%.8f", loc.getLocation().getLatitude());
                            formattedLongitude = String.format(Locale.US, "%.8f", loc.getLocation().getLongitude());
                            if (loc.getLocation().hasAltitude()) formattedAltitude = String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + altitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!egmAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction()));
                            if(exportGPX || exportTXT) {
                                if (loc.getLocation().hasSpeed())
                                    formattedSpeed = String.format(Locale.US, "%.3f", loc.getLocation().getSpeed());
                            }

                            // KML
                            if (exportKML) {
                                kmlBW.write("  <Placemark id=\"" + placemark_id + "\">" + newLine);
                                kmlBW.write("   <name>");
                                kmlBW.write(stringToXML(loc.getDescription()));
                                kmlBW.write("</name>" + newLine);
                                kmlBW.write("   <styleUrl>#PlacemarkStyle</styleUrl>" + newLine);
                                kmlBW.write("   <Point>" + newLine);
                                kmlBW.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
                                kmlBW.write("    <coordinates>");
                                if (loc.getLocation().hasAltitude()) {
                                    kmlBW.write(formattedLongitude + "," + formattedLatitude + "," + formattedAltitude);
                                } else {
                                    kmlBW.write(formattedLongitude + "," + formattedLatitude + ",0");
                                }
                                kmlBW.write("</coordinates>" + newLine);
                                kmlBW.write("    <extrude>1</extrude>" + newLine);
                                kmlBW.write("   </Point>" + newLine);
                                kmlBW.write("  </Placemark>" + newLine + newLine);
                            }

                            // GPX
                            if (exportGPX) {
                                gpxBW.write("<wpt lat=\"" + formattedLatitude + "\" lon=\"" + formattedLongitude + "\">");
                                if (loc.getLocation().hasAltitude()) {
                                    gpxBW.write("<ele>");     // Elevation
                                    gpxBW.write(formattedAltitude);
                                    gpxBW.write("</ele>");
                                }
                                gpxBW.write("<time>");     // Time
                                //gpxBW.write(dfdtGPX.format(loc.getLocation().getTime()));
                                gpxBW.write(((loc.getLocation().getTime() % 1000L) == 0L) ?
                                        dfdtGPX_NoMillis.format(loc.getLocation().getTime()) :
                                        dfdtGPX.format(loc.getLocation().getTime()));
                                gpxBW.write("</time>");
                                gpxBW.write("<name>");     // Name
                                gpxBW.write(stringToXML(loc.getDescription()));
                                gpxBW.write("</name>");
                                if (loc.getNumberOfSatellitesUsedInFix() > 0) {     // Satellites used in fix
                                    gpxBW.write("<sat>");
                                    gpxBW.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                                    gpxBW.write("</sat>");
                                }
                                gpxBW.write("</wpt>" + newLine + newLine);
                            }

                            // TXT
                            if (exportTXT) {
                                //type,time,latitude,longitude,altitude (m),geoid_height (m),speed (m/s),sat_used,sat_inview,name,desc
                                //txtBW.write("W," + dfdtTXT.format(loc.getLocation().getTime()) + "," + formattedLatitude + "," + formattedLongitude + ",");
                                txtBW.write("W," + (((loc.getLocation().getTime() % 1000L) == 0L) ?
                                          dfdtTXT_NoMillis.format(loc.getLocation().getTime()) :
                                          dfdtTXT.format(loc.getLocation().getTime()))
                                        + "," + formattedLatitude + "," + formattedLongitude + ",");
                                if (loc.getLocation().hasAccuracy())
                                    txtBW.write(String.format(Locale.US, "%.2f", loc.getLocation().getAccuracy()));
                                txtBW.write(",");
                                if (loc.getLocation().hasAltitude())
                                    txtBW.write(formattedAltitude);
                                txtBW.write(",");
                                if ((loc.getAltitudeEGM96Correction() != NOT_AVAILABLE) && (egmAltitudeCorrection))
                                    txtBW.write(String.format(Locale.US, "%.3f",loc.getAltitudeEGM96Correction()));
                                txtBW.write(",");
                                if (loc.getLocation().hasSpeed())
                                    txtBW.write(formattedSpeed);
                                txtBW.write(",");
                                if (loc.getLocation().hasBearing())
                                    txtBW.write(String.format(Locale.US, "%.0f", loc.getLocation().getBearing()));
                                txtBW.write(",");
                                if (loc.getNumberOfSatellitesUsedInFix() > 0)
                                    txtBW.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                                txtBW.write(",");
                                if (loc.getNumberOfSatellites() > 0)
                                    txtBW.write(String.valueOf(loc.getNumberOfSatellites()));
                                txtBW.write(",");
                                // Name is an empty field
                                txtBW.write(",");
                                txtBW.write(loc.getDescription().replace(",","_"));
                                txtBW.write(newLine);
                            }

                            placemark_id++;
                            exportingTask.setNumberOfPoints_Processed(exportingTask.getNumberOfPoints_Processed() + 1);
                        }
                        placemarkList.clear();
                    }
                }

                exportingTask.setNumberOfPoints_Processed(track.getNumberOfPlacemarks());
            }



            // ---------------------------------------------------------------- Writing Track
            // Approximation: 0.00000001 = 0° 0' 0.000036"
            // On equator 1" ~= 31 m  ->  0.000036" ~= 1.1 mm
            // We'll use 1 mm also for approx. altitudes!
            Log.w("myApp", "[#] Exporter.java - Writing Trackpoints");

            if (track.getNumberOfLocations() > 0) {

                // Writes track headings
                if (exportKML) {
                    PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
                    PhysicalData phdDuration;
                    PhysicalData phdDurationMoving;
                    PhysicalData phdSpeedMax;
                    PhysicalData phdSpeedAvg;
                    PhysicalData phdSpeedAvgMoving;
                    PhysicalData phdDistance;
                    PhysicalData phdAltitudeGap;
                    PhysicalData phdOverallDirection;
                    phdDuration = phdformatter.format(track.getDuration(),PhysicalDataFormatter.FORMAT_DURATION);
                    phdDurationMoving = phdformatter.format(track.getDurationMoving(),PhysicalDataFormatter.FORMAT_DURATION);
                    phdSpeedMax = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                    phdSpeedAvg = phdformatter.format(track.getSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                    phdSpeedAvgMoving = phdformatter.format(track.getSpeedAverageMoving(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                    phdDistance = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                    phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(gpsApp.getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                    phdOverallDirection = phdformatter.format(track.getBearing(),PhysicalDataFormatter.FORMAT_BEARING);

                    String TrackDesc = (track.getDescription().isEmpty() ? "" : "<b>" + stringToCDATA(track.getDescription()) + "</b><br><br>")
                            + gpsApp.getApplicationContext().getString(R.string.distance) + " = " + phdDistance.value + " " + phdDistance.um +
                            "<br>" + gpsApp.getApplicationContext().getString(R.string.duration) + " = " + phdDuration.value + " | " + phdDurationMoving.value +
                            "<br>" + gpsApp.getApplicationContext().getString(R.string.altitude_gap) + " = " + phdAltitudeGap.value + " " + phdAltitudeGap.um +
                            "<br>" + gpsApp.getApplicationContext().getString(R.string.max_speed) + " = " + phdSpeedMax.value + " " + phdSpeedMax.um +
                            "<br>" + gpsApp.getApplicationContext().getString(R.string.average_speed) + " = " + phdSpeedAvg.value + " | " + phdSpeedAvgMoving.value + " " + phdSpeedAvg.um +
                            "<br>" + gpsApp.getApplicationContext().getString(R.string.direction) + " = " + phdOverallDirection.value + " " + phdOverallDirection.um +
                            "<br><br><i>" + track.getNumberOfLocations() + " " + gpsApp.getApplicationContext().getString(R.string.trackpoints) + "</i>" ;

                    kmlBW.write("  <Placemark id=\"" + track.getName() + "\">" + newLine);
                    kmlBW.write("   <name>" + gpsApp.getApplicationContext().getString(R.string.tab_track) + " " + track.getName() + "</name>" + newLine);
                    kmlBW.write("   <description><![CDATA[" + TrackDesc + "]]></description>" + newLine);
                    kmlBW.write("   <styleUrl>#TrackStyle</styleUrl>" + newLine);
                    kmlBW.write("   <LineString>" + newLine);
                    kmlBW.write("    <extrude>0</extrude>" + newLine);
                    kmlBW.write("    <tessellate>0</tessellate>" + newLine);
                    kmlBW.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
                    kmlBW.write("    <coordinates>" + newLine);
                }
                if (exportGPX) {
                    gpxBW.write("<trk>" + newLine);
                    gpxBW.write(" <name>" + gpsApp.getApplicationContext().getString(R.string.tab_track) + " " + track.getName() + "</name>" + newLine);
                    gpxBW.write(" <trkseg>" + newLine);
                }

                LocationExtended loc;

                for (int i = 0; i < track.getNumberOfLocations(); i++) {

                    loc = arrayGeopoints.take();

                                        // Create formatted strings
                    formattedLatitude = String.format(Locale.US, "%.8f", loc.getLocation().getLatitude());
                    formattedLongitude = String.format(Locale.US, "%.8f", loc.getLocation().getLongitude());
                    if (loc.getLocation().hasAltitude()) formattedAltitude = String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + altitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!egmAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction()));
                    if(exportGPX || exportTXT) {
                        if (loc.getLocation().hasSpeed())
                            formattedSpeed = String.format(Locale.US, "%.3f", loc.getLocation().getSpeed());
                    }

                    // KML
                    if (exportKML) {
                        if (loc.getLocation().hasAltitude()) kmlBW.write("     " + formattedLongitude + "," + formattedLatitude + "," + formattedAltitude + newLine);
                        else kmlBW.write("     " + formattedLongitude + "," + formattedLatitude + ",0" + newLine);
                    }

                    // GPX
                    if (exportGPX) {
                        gpxBW.write("  <trkpt lat=\"" + formattedLatitude + "\" lon=\"" + formattedLongitude + "\">");
                        if (loc.getLocation().hasAltitude()) {
                            gpxBW.write("<ele>");     // Elevation
                            gpxBW.write(formattedAltitude);
                            gpxBW.write("</ele>");
                        }
                        gpxBW.write("<time>");     // Time
                        //gpxBW.write(dfdtGPX.format(loc.getLocation().getTime()));
                        gpxBW.write(((loc.getLocation().getTime() % 1000L) == 0L) ?
                                dfdtGPX_NoMillis.format(loc.getLocation().getTime()) :
                                dfdtGPX.format(loc.getLocation().getTime()));
                        gpxBW.write("</time>");
                        if (getPrefGPXVersion == GPX1_0) {
                            if (loc.getLocation().hasSpeed()) {
                                gpxBW.write("<speed>");     // Speed
                                gpxBW.write(formattedSpeed);
                                gpxBW.write("</speed>");
                            }
                        }
                        if (loc.getNumberOfSatellitesUsedInFix() > 0) {                   // GPX standards requires sats used for FIX.
                            gpxBW.write("<sat>");                                         // and NOT the number of satellites in view!!!
                            gpxBW.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                            gpxBW.write("</sat>");
                        }
                        /*
                        if (getPrefGPXVersion == GPX1_1) {                                // GPX 1.1 doesn't support speed tags. Let's switch to Garmin extensions :(
                            if (loc.getLocation().hasSpeed()) {
                                gpxBW.write("<extensions><gpxtpx:TrackPointExtension><gpxtpx:speed>");     // Speed (as Garmin extension)
                                gpxBW.write(formattedSpeed);
                                gpxBW.write("</gpxtpx:speed></gpxtpx:TrackPointExtension></extensions>");
                            }
                        } */
                        gpxBW.write("</trkpt>" + newLine);
                    }

                    // TXT
                    if (exportTXT) {
                        //type,time,latitude,longitude,altitude (m),geoid_height (m),speed (m/s),sat_used,sat_inview,name,desc
                        //txtBW.write("T," + dfdtTXT.format(loc.getLocation().getTime()) + "," + formattedLatitude + "," + formattedLongitude + ",");
                        txtBW.write("T," + (((loc.getLocation().getTime() % 1000L) == 0L) ?
                                  dfdtTXT_NoMillis.format(loc.getLocation().getTime()) :
                                  dfdtTXT.format(loc.getLocation().getTime()))
                                + "," + formattedLatitude + "," + formattedLongitude + ",");
                        if (loc.getLocation().hasAccuracy())
                            txtBW.write(String.format(Locale.US, "%.2f", loc.getLocation().getAccuracy()));
                        txtBW.write(",");
                        if (loc.getLocation().hasAltitude())
                            txtBW.write(formattedAltitude);
                        txtBW.write(",");
                        if ((loc.getAltitudeEGM96Correction() != NOT_AVAILABLE) && (egmAltitudeCorrection))
                            txtBW.write(String.format(Locale.US, "%.3f",loc.getAltitudeEGM96Correction()));
                        txtBW.write(",");
                        if (loc.getLocation().hasSpeed())
                            txtBW.write(formattedSpeed);
                        txtBW.write(",");
                        if (loc.getLocation().hasBearing())
                            txtBW.write(String.format(Locale.US, "%.0f", loc.getLocation().getBearing()));
                        txtBW.write(",");
                        if (loc.getNumberOfSatellitesUsedInFix() > 0)
                            txtBW.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                        txtBW.write(",");
                        if (loc.getNumberOfSatellites() > 0)
                            txtBW.write(String.valueOf(loc.getNumberOfSatellites()));
                        txtBW.write(",");
                        if (txtFirstTrackpointFlag) {           // First trackpoint of the track: add the description
                            if (track.getDescription().isEmpty()) txtBW.write(track.getName() + ",GPS Logger: " + track.getName());
                            else txtBW.write(track.getName() + ",GPS Logger: " + track.getName() + " - " + track.getDescription().replace(",", "_"));
                            txtFirstTrackpointFlag = false;
                        } else txtBW.write(",");
                        txtBW.write(newLine);
                    }

                    exportingTask.setNumberOfPoints_Processed(exportingTask.getNumberOfPoints_Processed() + 1);
                }

                exportingTask.setNumberOfPoints_Processed(track.getNumberOfPlacemarks() + track.getNumberOfLocations());
                arrayGeopoints.clear();

                if (exportKML) {
                    kmlBW.write("    </coordinates>" + newLine);
                    kmlBW.write("   </LineString>" + newLine);
                    kmlBW.write("  </Placemark>" + newLine + newLine);
                }
                if (exportGPX) {
                    gpxBW.write(" </trkseg>" + newLine);
                    gpxBW.write("</trk>" + newLine + newLine);
                }
            }



            // ------------------------------------------------------------ Writing tails and close
            Log.w("myApp", "[#] Exporter.java - Writing Tails and close files");

            if (exportKML) {
                kmlBW.write(" </Document>" + newLine);
                kmlBW.write("</kml>" + newLine + " ");
                kmlBW.flush();
                kmlBW.close();
            }
            if (exportGPX) {
                gpxBW.write("</gpx>" + newLine + " ");
                gpxBW.flush();
                gpxBW.close();
            }
            if (exportTXT) {
                txtBW.flush();
                txtBW.close();
            }

            Log.w("myApp", "[#] Exporter.java - Track "+ track.getId() +" exported in " + (System.currentTimeMillis() - startTime) + " ms (" + elements_total + " pts @ " + ((1000L * elements_total) / (System.currentTimeMillis() - startTime)) + " pts/s)");
            //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACK_EXPORTED, track.getId()));
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_SUCCESS);
        } catch (IOException e) {
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            asyncGeopointsLoader.interrupt();
            Log.w("myApp", "[#] Exporter.java - Unable to write the file: " + e);
        } catch (InterruptedException e) {
            exportingTask.setStatus(ExportingTask.STATUS_ENDED_FAILED);
            asyncGeopointsLoader.interrupt();
            Log.w("myApp", "[#] Exporter.java - Interrupted: " + e);
        }
    }

    /**
     * This Thread feeds the arrayGeopoints list with the GeoPoints, by querying
     * small blocks of points from the DB and keeping the list as full as possible.
     * The thread is started as soon as the Exported is started in order to fill the
     * list meanwhile the Exporter is initializing the files and is writing the file headers.
     * <p>
     * The Database reading and the file writing are decoupled into two separate Threads
     * in order to optimise the Exporter's performance.
     */
    private class AsyncGeopointsLoader extends Thread {

        public AsyncGeopointsLoader() {
        }

        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            List<LocationExtended> lList = new ArrayList<>(groupOfLocations);

            for (int i = 0; i <= track.getNumberOfLocations(); i += groupOfLocations) {
                //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                lList.addAll(GPSApplication.getInstance().gpsDataBase.getLocationsList(track.getId(), i, i + groupOfLocations - 1));
                if (!lList.isEmpty()) {
                    for (LocationExtended loc : lList) {
                        try {
                            arrayGeopoints.put(loc);
                            //Log.w("myApp", "[#] Exporter.java - " + ArrayGeopoints.size());
                        } catch (InterruptedException e) {
                            Log.w("myApp", "[#] Exporter.java - Interrupted: " + e);
                        }
                    }
                    lList.clear();
                }
            }
        }
    }
}
