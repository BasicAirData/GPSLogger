/**
 * Exporter - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 16/7/2016
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

import android.os.Environment;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;

class Exporter extends Thread {

    private static final int NOT_AVAILABLE = -100000;

    private Track track = null;
    private boolean ExportKML = true;
    private boolean ExportGPX = true;
    private boolean ExportTXT = true;
    private String SaveIntoFolder = "/";
    private double AltitudeManualCorrection = 0;
    private boolean EGMAltitudeCorrection = false;
    private int getPrefKMLAltitudeMode = 0;
    private int getPrefGPXVersion = 0;
    private boolean TXTFirstTrackpointFlag = true;

    private boolean UnableToWriteFile = false;
    int GroupOfLocations = 300; // Reads and writes location grouped by 300;

    private ArrayBlockingQueue<LocationExtended> ArrayGeopoints = new ArrayBlockingQueue<>(1200);
    private AsyncGeopointsLoader asyncGeopointsLoader = new AsyncGeopointsLoader();


    public Exporter(long ID, boolean ExportKML, boolean ExportGPX, boolean ExportTXT, String SaveIntoFolder) {
        track = GPSApplication.getInstance().GPSDataBase.getTrack(ID);
        AltitudeManualCorrection = GPSApplication.getInstance().getPrefAltitudeCorrection();
        EGMAltitudeCorrection = GPSApplication.getInstance().getPrefEGM96AltitudeCorrection();
        getPrefKMLAltitudeMode = GPSApplication.getInstance().getPrefKMLAltitudeMode();
        getPrefGPXVersion = GPSApplication.getInstance().getPrefGPXVersion();

        this.ExportTXT = ExportTXT;
        this.ExportGPX = ExportGPX;
        this.ExportKML = ExportKML;
        this.SaveIntoFolder = SaveIntoFolder;
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        final int GPX1_0 = 100;
        final int GPX1_1 = 110;

        Date creationTime;
        long elements_total;
        String versionName = BuildConfig.VERSION_NAME;
        GPSApplication GPSApp = GPSApplication.getInstance();
        if (GPSApp == null) return;

        elements_total = track.getNumberOfLocations() + track.getNumberOfPlacemarks();
        long start_Time = System.currentTimeMillis();

        // ------------------------------------------------- Create the Directory tree if not exist
        File sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger");
        if (!sd.exists()) {
            sd.mkdir();
        }
        sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
        if (!sd.exists()) {
            sd.mkdir();
        }
        // ----------------------------------------------------------------------------------------

        if (track == null) {
            //Log.w("myApp", "[#] Exporter.java - Track = null!!");
            return;
        }
        if (track.getNumberOfLocations() + track.getNumberOfPlacemarks() == 0) {
            EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            return;
        }

        EventBus.getDefault().post(new EventBusMSGLong(EventBusMSG.TRACK_SETPROGRESS, track.getId(), 1));

        if (EGMAltitudeCorrection && EGM96.getInstance().isEGMGridLoading()) {
            try {
                Log.w("myApp", "[#] Exporter.java - Wait, EGMGrid is loading");
                do {
                    Thread.sleep(200);
                    // Lazy polling until EGM grid finish to load
                } while (EGM96.getInstance().isEGMGridLoading());
            } catch (InterruptedException e) {
                Log.w("myApp", "[#] Exporter.java - Cannot wait!!");
            }
        }

        SimpleDateFormat dfdtGPX = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  // date and time formatter for GPX timestamp
        dfdtGPX.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat dfdtTXT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");  // date and time formatter for TXT timestamp
        dfdtTXT.setTimeZone(TimeZone.getTimeZone("GMT"));

        File KMLfile = null;
        File GPXfile = null;
        File TXTfile = null;
        //final String newLine = System.getProperty("line.separator"); //\n\r
        final String newLine = "\r\n";

        // Verify if Folder exists
        sd = new File(SaveIntoFolder);
        boolean success = true;
        if (!sd.exists()) {
            success = sd.mkdir();
        }
        if (!success) {
            EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            return;
        }

        // Create files, deleting old version if exists
        if (ExportKML) {
            KMLfile = new File(sd, (track.getName() + ".kml"));
            if (KMLfile.exists()) KMLfile.delete();
        }
        if (ExportGPX) {
            GPXfile = new File(sd, (track.getName() + ".gpx"));
            if (GPXfile.exists()) GPXfile.delete();
        }
        if (ExportTXT) {
            TXTfile = new File(sd, (track.getName() + ".txt"));
            if (TXTfile.exists()) TXTfile.delete();
        }

        // Create buffers for Write operations
        PrintWriter KMLfw = null;
        BufferedWriter KMLbw = null;
        PrintWriter GPXfw = null;
        BufferedWriter GPXbw = null;
        PrintWriter TXTfw = null;
        BufferedWriter TXTbw = null;

        // Check if all the files are writable:
        try {
            if ((ExportGPX && !(GPXfile.createNewFile())) || (ExportKML && !(KMLfile.createNewFile())) || (ExportTXT && !(TXTfile.createNewFile()))) {
                UnableToWriteFile = true;
                Log.w("myApp", "[#] Exporter.java - Unable to write the file");
            }
        } catch (SecurityException e) {
            UnableToWriteFile = true;
            Log.w("myApp", "[#] Exporter.java - Unable to write the file: SecurityException");
        } catch (IOException e) {
            UnableToWriteFile = true;
            Log.w("myApp", "[#] Exporter.java - Unable to write the file: IOException");
        }

        // If the file is not writable abort exportation:
        if (UnableToWriteFile) {
            EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            return;
        }

        asyncGeopointsLoader.start();

        try {
            if (ExportKML) {
                KMLfw = new PrintWriter(KMLfile);
                KMLbw = new BufferedWriter(KMLfw);
            }
            if (ExportGPX) {
                GPXfw = new PrintWriter(GPXfile);
                GPXbw = new BufferedWriter(GPXfw);
            }
            if (ExportTXT) {
                TXTfw = new PrintWriter(TXTfile);
                TXTbw = new BufferedWriter(TXTfw);
            }

            creationTime = Calendar.getInstance().getTime();

            // ---------------------------------------------------------------------- Writing Heads
            Log.w("myApp", "[#] Exporter.java - Writing Heads");

            if (ExportKML) {
                // Writing head of KML file

                KMLbw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
                KMLbw.write("<!-- Created with BasicAirData GPS Logger for Android - ver. " + versionName + " -->" + newLine);
                KMLbw.write("<!-- Track " + String.valueOf(track.getId()) + " = " + String.valueOf(track.getNumberOfLocations())
                        + " TrackPoints + " + String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks -->" + newLine);
                KMLbw.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">" + newLine);
                KMLbw.write(" <Document>" + newLine);
                KMLbw.write("  <name>GPS Logger " + track.getName() + "</name>" + newLine);
                KMLbw.write("  <description><![CDATA[" + String.valueOf(track.getNumberOfLocations()) + " Trackpoints<br>" +
                        String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks]]></description>" + newLine);
                if (track.getNumberOfLocations() > 0) {
                    KMLbw.write("  <Style id=\"TrackStyle\">" + newLine);
                    KMLbw.write("   <LineStyle>" + newLine);
                    KMLbw.write("    <color>ff0000ff</color>" + newLine);
                    KMLbw.write("    <width>3</width>" + newLine);
                    KMLbw.write("   </LineStyle>" + newLine);
                    KMLbw.write("   <PolyStyle>" + newLine);
                    KMLbw.write("    <color>7f0000ff</color>" + newLine);
                    KMLbw.write("   </PolyStyle>" + newLine);
                    KMLbw.write("   <BalloonStyle>" + newLine);
                    KMLbw.write("    <text><![CDATA[<p style=\"color:red;font-weight:bold\">$[name]</p><p style=\"font-size:11px\">$[description]</p><p style=\"font-size:7px\">" +
                            GPSApp.getApplicationContext().getString(R.string.pref_track_stats) + ": " +
                            GPSApp.getApplicationContext().getString(R.string.pref_track_stats_totaltime) + " | " +
                            GPSApp.getApplicationContext().getString(R.string.pref_track_stats_movingtime) + "</p>]]></text>" + newLine);
                    KMLbw.write("   </BalloonStyle>" + newLine);
                    KMLbw.write("  </Style>" + newLine);
                }
                if (track.getNumberOfPlacemarks() > 0) {
                    KMLbw.write("  <Style id=\"PlacemarkStyle\">" + newLine);
                    KMLbw.write("   <IconStyle>" + newLine);
                    KMLbw.write("    <Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle_highlight.png</href></Icon>" + newLine);
                    KMLbw.write("   </IconStyle>" + newLine);
                    KMLbw.write("  </Style>" + newLine);
                }
                KMLbw.write(newLine);
            }

            if (ExportGPX) {
                // Writing head of GPX file

                GPXbw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
                GPXbw.write("<!-- Created with BasicAirData GPS Logger for Android - ver. " + versionName + " -->" + newLine);
                GPXbw.write("<!-- Track " + String.valueOf(track.getId()) + " = " + String.valueOf(track.getNumberOfLocations())
                        + " TrackPoints + " + String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks -->" + newLine);
                if (getPrefGPXVersion == GPX1_0) {     // GPX 1.0
                    GPXbw.write("<gpx version=\"1.0\"" + newLine
                              + "     creator=\"BasicAirData GPS Logger " + versionName + "\"" + newLine
                              + "     xmlns=\"http://www.topografix.com/GPX/1/0\"" + newLine
                              + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine
                              + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" + newLine);
                    GPXbw.write("<name>GPS Logger " + track.getName() + "</name>" + newLine);
                    GPXbw.write("<time>" + dfdtGPX.format(creationTime) + "</time>" + newLine + newLine);
                }
                if (getPrefGPXVersion == GPX1_1) {    // GPX 1.1
                    GPXbw.write("<gpx version=\"1.1\"" + newLine
                              + "     creator=\"BasicAirData GPS Logger " + versionName + "\"" + newLine
                              + "     xmlns=\"http://www.topografix.com/GPX/1/1\"" + newLine
                              + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine
                              + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">" + newLine);
                    //          + "     xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"" + newLine           // Garmin extension to include speeds
                    //          + "     xmlns:gpxtrkx=\"http://www.garmin.com/xmlschemas/TrackStatsExtension/v1\"" + newLine  //
                    //          + "     xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">" + newLine); //
                    GPXbw.write("<metadata> " + newLine);    // GPX Metadata
                    GPXbw.write(" <name>GPS Logger " + track.getName() + "</name>" + newLine);
                    GPXbw.write(" <time>" + dfdtGPX.format(creationTime) + "</time>" + newLine);
                    GPXbw.write("</metadata>" + newLine + newLine);
                }
            }

            if (ExportTXT) {
                // Writing head of TXT file
                TXTbw.write("type,time,latitude,longitude,accuracy (m),altitude (m),geoid_height (m),speed (m/s),bearing (deg),sat_used,sat_inview,name,desc" + newLine);
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

                List<LocationExtended> placemarkList = new ArrayList<>(GroupOfLocations);

                for (int i = 0; i <= track.getNumberOfPlacemarks(); i += GroupOfLocations) {
                    //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                    placemarkList.addAll(GPSApp.GPSDataBase.getPlacemarksList(track.getId(), i, i + GroupOfLocations - 1));

                    if (!placemarkList.isEmpty()) {
                        for (LocationExtended loc : placemarkList) {
                            formattedLatitude = String.format(Locale.US, "%.8f", loc.getLocation().getLatitude());
                            formattedLongitude = String.format(Locale.US, "%.8f", loc.getLocation().getLongitude());
                            if (loc.getLocation().hasAltitude()) formattedAltitude = String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction()));
                            if(ExportGPX || ExportTXT) {
                                if (loc.getLocation().hasSpeed())
                                    formattedSpeed = String.format(Locale.US, "%.3f", loc.getLocation().getSpeed());
                            }

                            // KML
                            if (ExportKML) {
                                KMLbw.write("  <Placemark id=\"" + placemark_id + "\">" + newLine);
                                KMLbw.write("   <name>");
                                KMLbw.write(loc.getDescription()
                                        .replace("<","&lt;")
                                        .replace("&","&amp;")
                                        .replace(">","&gt;")
                                        .replace("\"","&quot;")
                                        .replace("'","&apos;"));
                                KMLbw.write("</name>" + newLine);
                                KMLbw.write("   <styleUrl>#PlacemarkStyle</styleUrl>" + newLine);
                                KMLbw.write("   <Point>" + newLine);
                                KMLbw.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
                                KMLbw.write("    <coordinates>");
                                if (loc.getLocation().hasAltitude()) {
                                    KMLbw.write(formattedLongitude + "," + formattedLatitude + "," + formattedAltitude);
                                } else {
                                    KMLbw.write(formattedLongitude + "," + formattedLatitude + ",0");
                                }
                                KMLbw.write("</coordinates>" + newLine);
                                KMLbw.write("    <extrude>1</extrude>" + newLine);
                                KMLbw.write("   </Point>" + newLine);
                                KMLbw.write("  </Placemark>" + newLine + newLine);
                            }

                            // GPX
                            if (ExportGPX) {
                                GPXbw.write("<wpt lat=\"" + formattedLatitude + "\" lon=\"" + formattedLongitude + "\">");
                                if (loc.getLocation().hasAltitude()) {
                                    GPXbw.write("<ele>");     // Elevation
                                    GPXbw.write(formattedAltitude);
                                    GPXbw.write("</ele>");
                                }
                                GPXbw.write("<time>");     // Time
                                GPXbw.write(dfdtGPX.format(loc.getLocation().getTime()));
                                GPXbw.write("</time>");
                                GPXbw.write("<name>");     // Name
                                GPXbw.write(loc.getDescription()
                                        .replace("<","&lt;")
                                        .replace("&","&amp;")
                                        .replace(">","&gt;")
                                        .replace("\"","&quot;")
                                        .replace("'","&apos;"));
                                GPXbw.write("</name>");
                                if (loc.getNumberOfSatellitesUsedInFix() > 0) {     // Satellites used in fix
                                    GPXbw.write("<sat>");
                                    GPXbw.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                                    GPXbw.write("</sat>");
                                }
                                GPXbw.write("</wpt>" + newLine + newLine);
                            }

                            // TXT
                            if (ExportTXT) {
                                //type,time,latitude,longitude,altitude (m),geoid_height (m),speed (m/s),sat_used,sat_inview,name,desc
                                TXTbw.write("W," + dfdtTXT.format(loc.getLocation().getTime()) + "," + formattedLatitude + "," + formattedLongitude + ",");
                                if (loc.getLocation().hasAccuracy())
                                    TXTbw.write(String.format(Locale.US, "%.0f", loc.getLocation().getAccuracy()));
                                TXTbw.write(",");
                                if (loc.getLocation().hasAltitude())
                                    TXTbw.write(formattedAltitude);
                                TXTbw.write(",");
                                if ((loc.getAltitudeEGM96Correction() != NOT_AVAILABLE) && (EGMAltitudeCorrection))
                                    TXTbw.write(String.format(Locale.US, "%.3f",loc.getAltitudeEGM96Correction()));
                                TXTbw.write(",");
                                if (loc.getLocation().hasSpeed())
                                    TXTbw.write(formattedSpeed);
                                TXTbw.write(",");
                                if (loc.getLocation().hasBearing())
                                    TXTbw.write(String.format(Locale.US, "%.0f", loc.getLocation().getBearing()));
                                TXTbw.write(",");
                                if (loc.getNumberOfSatellitesUsedInFix() > 0)
                                    TXTbw.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                                TXTbw.write(",");
                                if (loc.getNumberOfSatellites() > 0)
                                    TXTbw.write(String.valueOf(loc.getNumberOfSatellites()));
                                TXTbw.write(",");
                                // Name is an empty field
                                TXTbw.write(",");
                                TXTbw.write(loc.getDescription().replace(",","_"));
                                TXTbw.write(newLine);
                            }

                            placemark_id++;
                        }
                        placemarkList.clear();
                    }
                }
            }



            // ---------------------------------------------------------------- Writing Track
            // Approximation: 0.00000001 = 0° 0' 0.000036"
            // On equator 1" ~= 31 m  ->  0.000036" ~= 1.1 mm
            // We'll use 1 mm also for approx. altitudes!
            Log.w("myApp", "[#] Exporter.java - Writing Trackpoints");

            if (track.getNumberOfLocations() > 0) {

                // Writes track headings
                if (ExportKML) {
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
                    phdDurationMoving = phdformatter.format(track.getDuration_Moving(),PhysicalDataFormatter.FORMAT_DURATION);
                    phdSpeedMax = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                    phdSpeedAvg = phdformatter.format(track.getSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                    phdSpeedAvgMoving = phdformatter.format(track.getSpeedAverageMoving(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                    phdDistance = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                    phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(GPSApp.getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                    phdOverallDirection = phdformatter.format(track.getBearing(),PhysicalDataFormatter.FORMAT_BEARING);

                    String TrackDesc = GPSApp.getApplicationContext().getString(R.string.distance) + " = " + phdDistance.Value + " " + phdDistance.UM +
                            "<br>" + GPSApp.getApplicationContext().getString(R.string.duration) + " = " + phdDuration.Value + " | " + phdDurationMoving.Value +
                            "<br>" + GPSApp.getApplicationContext().getString(R.string.altitude_gap) + " = " + phdAltitudeGap.Value + " " + phdAltitudeGap.UM +
                            "<br>" + GPSApp.getApplicationContext().getString(R.string.max_speed) + " = " + phdSpeedMax.Value + " " + phdSpeedMax.UM +
                            "<br>" + GPSApp.getApplicationContext().getString(R.string.average_speed) + " = " + phdSpeedAvg.Value + " | " + phdSpeedAvgMoving.Value + " " + phdSpeedAvg.UM +
                            "<br>" + GPSApp.getApplicationContext().getString(R.string.direction) + " = " + phdOverallDirection.Value + " " + phdOverallDirection.UM +
                            "<br><br><i>" + track.getNumberOfLocations() + " " + GPSApp.getApplicationContext().getString(R.string.trackpoints) + "</i>" ;

                    KMLbw.write("  <Placemark id=\"" + track.getName() + "\">" + newLine);
                    KMLbw.write("   <name>" + GPSApp.getApplicationContext().getString(R.string.tab_track) + " " + track.getName() + "</name>" + newLine);
                    KMLbw.write("   <description><![CDATA[" + TrackDesc + "]]></description>" + newLine);
                    KMLbw.write("   <styleUrl>#TrackStyle</styleUrl>" + newLine);
                    KMLbw.write("   <LineString>" + newLine);
                    KMLbw.write("    <extrude>0</extrude>" + newLine);
                    KMLbw.write("    <tessellate>0</tessellate>" + newLine);
                    KMLbw.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
                    KMLbw.write("    <coordinates>" + newLine);
                }
                if (ExportGPX) {
                    GPXbw.write("<trk>" + newLine);
                    GPXbw.write(" <name>" + GPSApp.getApplicationContext().getString(R.string.tab_track) + " " + track.getName() + "</name>" + newLine);
                    GPXbw.write(" <trkseg>" + newLine);
                }

                int n = 1000;
                long progress = 0;
                long oldProgress = 0;
                LocationExtended loc;

                for (int i = 0; i < track.getNumberOfLocations(); i++) {

                    loc = ArrayGeopoints.take();

                                        // Create formatted strings
                    formattedLatitude = String.format(Locale.US, "%.8f", loc.getLocation().getLatitude());
                    formattedLongitude = String.format(Locale.US, "%.8f", loc.getLocation().getLongitude());
                    if (loc.getLocation().hasAltitude()) formattedAltitude = String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction()));
                    if(ExportGPX || ExportTXT) {
                        if (loc.getLocation().hasSpeed())
                            formattedSpeed = String.format(Locale.US, "%.3f", loc.getLocation().getSpeed());
                    }

                    // KML
                    if (ExportKML) {
                        if (loc.getLocation().hasAltitude()) KMLbw.write("     " + formattedLongitude + "," + formattedLatitude + "," + formattedAltitude + newLine);
                        else KMLbw.write("     " + formattedLongitude + "," + formattedLatitude + ",0" + newLine);
                    }

                    // GPX
                    if (ExportGPX) {
                        GPXbw.write("  <trkpt lat=\"" + formattedLatitude + "\" lon=\"" + formattedLongitude + "\">");
                        if (loc.getLocation().hasAltitude()) {
                            GPXbw.write("<ele>");     // Elevation
                            GPXbw.write(formattedAltitude);
                            GPXbw.write("</ele>");
                        }
                        GPXbw.write("<time>");     // Time
                        GPXbw.write(dfdtGPX.format(loc.getLocation().getTime()));
                        GPXbw.write("</time>");
                        if (getPrefGPXVersion == GPX1_0) {
                            if (loc.getLocation().hasSpeed()) {
                                GPXbw.write("<speed>");     // Speed
                                GPXbw.write(formattedSpeed);
                                GPXbw.write("</speed>");
                            }
                        }
                        if (loc.getNumberOfSatellitesUsedInFix() > 0) {                   // GPX standards requires sats used for FIX.
                            GPXbw.write("<sat>");                                         // and NOT the number of satellites in view!!!
                            GPXbw.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                            GPXbw.write("</sat>");
                        }
                        /*
                        if (getPrefGPXVersion == GPX1_1) {                                // GPX 1.1 doesn't support speed tags. Let's switch to Garmin extensions :(
                            if (loc.getLocation().hasSpeed()) {
                                GPXbw.write("<extensions><gpxtpx:TrackPointExtension><gpxtpx:speed>");     // Speed (as Garmin extension)
                                GPXbw.write(formattedSpeed);
                                GPXbw.write("</gpxtpx:speed></gpxtpx:TrackPointExtension></extensions>");
                            }
                        } */
                        GPXbw.write("</trkpt>" + newLine);
                    }

                    // TXT
                    if (ExportTXT) {
                        //type,time,latitude,longitude,altitude (m),geoid_height (m),speed (m/s),sat_used,sat_inview,name,desc
                        TXTbw.write("T," + dfdtTXT.format(loc.getLocation().getTime()) + "," + formattedLatitude + "," + formattedLongitude + ",");
                        if (loc.getLocation().hasAccuracy())
                            TXTbw.write(String.format(Locale.US, "%.0f", loc.getLocation().getAccuracy()));
                        TXTbw.write(",");
                        if (loc.getLocation().hasAltitude())
                            TXTbw.write(formattedAltitude);
                        TXTbw.write(",");
                        if ((loc.getAltitudeEGM96Correction() != NOT_AVAILABLE) && (EGMAltitudeCorrection))
                            TXTbw.write(String.format(Locale.US, "%.3f",loc.getAltitudeEGM96Correction()));
                        TXTbw.write(",");
                        if (loc.getLocation().hasSpeed())
                            TXTbw.write(formattedSpeed);
                        TXTbw.write(",");
                        if (loc.getLocation().hasBearing())
                            TXTbw.write(String.format(Locale.US, "%.0f", loc.getLocation().getBearing()));
                        TXTbw.write(",");
                        if (loc.getNumberOfSatellitesUsedInFix() > 0)
                            TXTbw.write(String.valueOf(loc.getNumberOfSatellitesUsedInFix()));
                        TXTbw.write(",");
                        if (loc.getNumberOfSatellites() > 0)
                            TXTbw.write(String.valueOf(loc.getNumberOfSatellites()));
                        TXTbw.write(",");
                        if (TXTFirstTrackpointFlag) {           // First trackpoint of the track: add the description
                            TXTbw.write(track.getName() + ",GPS Logger: " + track.getName());
                            TXTFirstTrackpointFlag = false;
                        } else TXTbw.write(",");
                        TXTbw.write(newLine);
                    }

                    n++;
                    if (n > 30) {     // Evaluate the progress every n elements
                        progress = 100L * (track.getNumberOfPlacemarks() + i + GroupOfLocations) / (track.getNumberOfLocations() + track.getNumberOfPlacemarks());
                        if (progress > 99) progress = 99;
                        if (progress < 1) progress = 1;
                        if (progress - oldProgress >= 1) {
                            EventBus.getDefault().post(new EventBusMSGLong(EventBusMSG.TRACK_SETPROGRESS, track.getId(), progress));
                            oldProgress = progress;
                            n = 0;
                        }
                    }
                }

                EventBus.getDefault().post(new EventBusMSGLong(EventBusMSG.TRACK_SETPROGRESS, track.getId(), 100));
                ArrayGeopoints.clear();

                if (ExportKML) {
                    KMLbw.write("    </coordinates>" + newLine);
                    KMLbw.write("   </LineString>" + newLine);
                    KMLbw.write("  </Placemark>" + newLine + newLine);
                }
                if (ExportGPX) {
                    GPXbw.write(" </trkseg>" + newLine);
                    GPXbw.write("</trk>" + newLine + newLine);
                }
            }



            // ------------------------------------------------------------ Writing tails and close
            Log.w("myApp", "[#] Exporter.java - Writing Tails and close files");

            if (ExportKML) {
                KMLbw.write(" </Document>" + newLine);
                KMLbw.write("</kml>");

                KMLbw.close();
                KMLfw.close();
            }
            if (ExportGPX) {
                GPXbw.write("</gpx>");

                GPXbw.close();
                GPXfw.close();
            }
            if (ExportTXT) {
                TXTbw.close();
                TXTfw.close();
            }

            Log.w("myApp", "[#] Exporter.java - Track "+ track.getId() +" exported in " + (System.currentTimeMillis() - start_Time) + " ms (" + elements_total + " pts @ " + ((1000L * elements_total) / (System.currentTimeMillis() - start_Time)) + " pts/s)");

            //EventBus.getDefault().post(new EventBusMSGLong(EventBusMSG.TRACK_SETPROGRESS, track.getId(), 100));
            //try {
            //    Thread.sleep(300);
            //} catch (InterruptedException e) {
            //    Log.w("myApp", "[#] Exporter.java - Cannot wait!!");
            //}

            EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACK_EXPORTED, track.getId()));

        } catch (IOException e) {
            EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.getId()));
            asyncGeopointsLoader.interrupt();
            Log.w("myApp", "[#] Exporter.java - Unable to write the file: " + e);
        } catch (InterruptedException e) {
            asyncGeopointsLoader.interrupt();
            Log.w("myApp", "[#] Exporter.java - Interrupted: " + e);
        }
    }


    private class AsyncGeopointsLoader extends Thread {

        public AsyncGeopointsLoader() {
        }

        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            List<LocationExtended> lList = new ArrayList<>(GroupOfLocations);

            for (int i = 0; i <= track.getNumberOfLocations(); i += GroupOfLocations) {
                //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                lList.addAll(GPSApplication.getInstance().GPSDataBase.getLocationsList(track.getId(), i, i + GroupOfLocations - 1));
                if (!lList.isEmpty()) {
                    for (LocationExtended loc : lList) {
                        try {
                            ArrayGeopoints.put(loc);
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
