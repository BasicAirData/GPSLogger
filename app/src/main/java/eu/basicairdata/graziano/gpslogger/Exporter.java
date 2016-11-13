/*
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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class Exporter extends Thread {

    public static final int NOT_AVAILABLE = -100000;

    Track track = null;
    int GroupOfLocations = 200; // Reads and writes location grouped by 200;
    boolean ExportKML = true;
    boolean ExportGPX = true;
    String SaveIntoFolder = "/";
    double AltitudeManualCorrection = 0;
    boolean EGMAltitudeCorrection = false;
    int getPrefKMLAltitudeMode = 0;

    String versionName = BuildConfig.VERSION_NAME;

    public Exporter(long ID, boolean ExportKML, boolean ExportGPX, String SaveIntoFolder) {
        track = GPSApplication.getInstance().GPSDataBase.getTrack(ID);
        AltitudeManualCorrection = GPSApplication.getInstance().getPrefAltitudeCorrection();
        EGMAltitudeCorrection = GPSApplication.getInstance().getPrefEGM96AltitudeCorrection();
        getPrefKMLAltitudeMode = GPSApplication.getInstance().getPrefKMLAltitudeMode();

        this.ExportGPX = ExportGPX;
        this.ExportKML = ExportKML;
        this.SaveIntoFolder = SaveIntoFolder;
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

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
        if (track.getNumberOfLocations() + track.getNumberOfPlacemarks() == 0) return;

        EventBus.getDefault().post("TRACK_SETPROGRESS " + track.getId() + " 1");

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

        SimpleDateFormat dfdt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  // date and time formatter for GPX timestamp
        dfdt.setTimeZone(TimeZone.getTimeZone("GMT"));

        File KMLfile = null;
        File GPXfile = null;
        final String newLine = System.getProperty("line.separator");

        // Verify if Folder exists
        sd = new File(SaveIntoFolder);
        boolean success = true;
        if (!sd.exists()) {
            success = sd.mkdir();
        }
        if (!success) return;

        // Create files, deleting old version if exists
        if (ExportKML) {
            KMLfile = new File(sd, (track.getName() + ".kml"));
            if (KMLfile.exists()) KMLfile.delete();
        }
        if (ExportGPX) {
            GPXfile = new File(sd, (track.getName() + ".gpx"));
            if (GPXfile.exists()) GPXfile.delete();
        }

        // Create buffers for Write operations
        PrintWriter KMLfw = null;
        BufferedWriter KMLbw = null;
        PrintWriter GPXfw = null;
        BufferedWriter GPXbw = null;


        try {
            if (ExportKML) {
                KMLfw = new PrintWriter(KMLfile);
                KMLbw = new BufferedWriter(KMLfw);
            }
            if (ExportGPX) {
                GPXfw = new PrintWriter(GPXfile);
                GPXbw = new BufferedWriter(GPXfw);
            }

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
                KMLbw.write("  <name>Paths</name>" + newLine);
                KMLbw.write("  <description>GPS Logger: " + track.getName() + newLine);
                KMLbw.write("  </description>" + newLine);

                KMLbw.write("  <Style id=\"TrackLineAndPoly\">" + newLine);
                KMLbw.write("   <LineStyle>" + newLine);
                KMLbw.write("    <color>ff0000ff</color>" + newLine);
                KMLbw.write("    <width>4</width>" + newLine);
                KMLbw.write("   </LineStyle>" + newLine);
                KMLbw.write("   <PolyStyle>" + newLine);
                KMLbw.write("    <color>7f0000ff</color>" + newLine);
                KMLbw.write("   </PolyStyle>" + newLine);
                KMLbw.write("  </Style>" + newLine);

                KMLbw.write("  <Style id=\"Bookmark_Style\">" + newLine);
                KMLbw.write("   <IconStyle>" + newLine);
                KMLbw.write("    <Icon> <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle_highlight.png</href> </Icon>" + newLine);
                KMLbw.write("   </IconStyle>" + newLine);
                KMLbw.write("  </Style>" + newLine + newLine);
            }

            if (ExportGPX) {
                // Writing head of GPX file

                GPXbw.write("<?xml version=\"1.0\"?>" + newLine);
                GPXbw.write("<!-- Created with BasicAirData GPS Logger for Android - ver. " + versionName + " -->" + newLine);
                GPXbw.write("<!-- Track " + String.valueOf(track.getId()) + " = " + String.valueOf(track.getNumberOfLocations())
                        + " TrackPoints + " + String.valueOf(track.getNumberOfPlacemarks()) + " Placemarks -->" + newLine);
                GPXbw.write("<gpx creator=\"BasicAirData GPS Logger\" version=\"" + versionName + "\" xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" + newLine + newLine);
            }


            // ---------------------------------------------------------------- Writing Trackpoints
            // Approximation: 0.00000001 = 0° 0' 0.000036"
            // On equator 1" ~= 31 m  ->  0.000036" ~= 1.1 mm
            // We'll use 1 mm also for approx. altitudes!
            Log.w("myApp", "[#] Exporter.java - Writing Trackpoints");

            if (track.getNumberOfLocations() > 0) {
                String formattedLatitude = "";
                String formattedLongitude = "";
                String formattedAltitude = "";

                // Writes track headings
                if (ExportKML) {
                    KMLbw.write("  <Placemark>" + newLine);
                    KMLbw.write("   <name>GPS Logger</name>" + newLine);
                    KMLbw.write("   <description>Track: " + track.getName() + " </description>" + newLine);
                    KMLbw.write("   <styleUrl>#TrackLineAndPoly</styleUrl>" + newLine);
                    KMLbw.write("   <LineString>" + newLine);
                    KMLbw.write("    <extrude>0</extrude>" + newLine);
                    KMLbw.write("    <tessellate>0</tessellate>" + newLine);
                    KMLbw.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
                    KMLbw.write("    <coordinates>" + newLine);
                }
                if (ExportGPX) {
                    GPXbw.write("<trk>" + newLine);
                    GPXbw.write(" <name>" + track.getName() + "</name>" + newLine);
                    GPXbw.write(" <desc>GPS Logger: " + track.getName() + "</desc>" + newLine);
                    GPXbw.write(" <trkseg>" + newLine);
                }

                List<LocationExtended> locationList = new ArrayList<LocationExtended>();

                for (int i = 0; i <= track.getNumberOfLocations(); i += GroupOfLocations) {
                    //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                    if (!locationList.isEmpty()) locationList.clear();
                    //Log.w("myApp", "[#] Exporter.java - DB query + list.addall(...)");
                    locationList.addAll(GPSApplication.getInstance().GPSDataBase.getLocationsList(track.getId(), i, i + GroupOfLocations - 1));
                    //Log.w("myApp", "[#] Exporter.java - Write files");

                    if (!locationList.isEmpty()) {
                        for (LocationExtended loc : locationList) {
                            // Create formatted strings
                            if (ExportKML || ExportGPX) {
                                formattedLatitude = String.format(Locale.US, "%.8f", loc.getLocation().getLatitude());
                                formattedLongitude = String.format(Locale.US, "%.8f", loc.getLocation().getLongitude());
                                if (loc.getLocation().hasAltitude()) formattedAltitude = String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction()));
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
                                if (loc.getLocation().hasSpeed()) {
                                    GPXbw.write("<speed>");     // Speed
                                    GPXbw.write(String.format(Locale.US, "%.3f", loc.getLocation().getSpeed()));
                                    GPXbw.write("</speed>");
                                }
                                GPXbw.write("<time>");     // Time
                                GPXbw.write(dfdt.format(loc.getLocation().getTime()));
                                GPXbw.write("</time>");
                                //if (loc.getNumberOfSatellites() > 0) {                            // NOT YET IMPLEMENTED: GPX standards requires sats used for FIX.
                                //    GPXbw.write("<sat>");                                         // but those are the number of satellites in view!!!
                                //    GPXbw.write(String.valueOf(loc.getNumberOfSatellites()));     // TODO: Save the satellites used in FIX
                                //    GPXbw.write("</sat>");
                                //}
                                GPXbw.write("</trkpt>" + newLine);
                            }
                        }
                    }

                    long progress = 100L * (i + GroupOfLocations) / (track.getNumberOfLocations() + track.getNumberOfPlacemarks());
                    if (progress > 99) progress = 99;
                    if (progress < 1) progress = 1;
                    EventBus.getDefault().post("TRACK_SETPROGRESS " + track.getId() + " " + progress);
                }

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


            // ---------------------------------------------------------------- Writing Placemarks
            Log.w("myApp", "[#] Exporter.java - Writing Placemarks");

            if (track.getNumberOfPlacemarks() > 0) {
                // Writes track headings

                List<LocationExtended> placemarkList = new ArrayList<LocationExtended>();

                for (int i = 0; i <= track.getNumberOfPlacemarks(); i += GroupOfLocations) {
                    //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                    if (!placemarkList.isEmpty()) placemarkList.clear();
                    placemarkList.addAll(GPSApplication.getInstance().GPSDataBase.getPlacemarksList(track.getId(), i, i + GroupOfLocations - 1));

                    if (!placemarkList.isEmpty()) {
                        for (LocationExtended loc : placemarkList) {

                            // KML
                            if (ExportKML) {
                                KMLbw.write("  <Placemark>" + newLine);
                                KMLbw.write("   <name>");
                                KMLbw.write(loc.getDescription()
                                        .replace("<","&lt;")
                                        .replace("&","&amp;")
                                        .replace(">","&gt;")
                                        .replace("\"","&quot;")
                                        .replace("'","&apos;"));
                                KMLbw.write("</name>" + newLine);
                                KMLbw.write("   <styleUrl>#Bookmark_Style</styleUrl>" + newLine);
                                KMLbw.write("   <Point>" + newLine);
                                KMLbw.write("    <altitudeMode>" + (getPrefKMLAltitudeMode == 1 ? "clampToGround" : "absolute") + "</altitudeMode>" + newLine);
                                KMLbw.write("    <coordinates>");
                                if (loc.getLocation().hasAltitude()) {
                                    KMLbw.write(String.format(Locale.US, "%.8f", loc.getLocation().getLongitude()) + "," +
                                            String.format(Locale.US, "%.8f", loc.getLocation().getLatitude()) + "," +
                                            String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction())));
                                } else {
                                    KMLbw.write(String.format(Locale.US, "%.8f", loc.getLocation().getLongitude()) + "," +
                                            String.format(Locale.US, "%.8f", loc.getLocation().getLatitude()) + "," +
                                            "0");
                                }
                                KMLbw.write("</coordinates>" + newLine);
                                KMLbw.write("    <extrude>1</extrude>" + newLine);
                                KMLbw.write("   </Point>" + newLine);
                                KMLbw.write("  </Placemark>" + newLine + newLine);
                            }


                            // GPX
                            if (ExportGPX) {
                                GPXbw.write("<wpt lat=\"");
                                GPXbw.write(String.format(Locale.US, "%.8f", loc.getLocation().getLatitude()) + "\" lon=\"" +
                                        String.format(Locale.US, "%.8f", loc.getLocation().getLongitude()) + "\">");

                                if (loc.getLocation().hasAltitude()) {
                                    GPXbw.write("<ele>");     // Elevation
                                    GPXbw.write(String.format(Locale.US, "%.3f", loc.getLocation().getAltitude() + AltitudeManualCorrection - (((loc.getAltitudeEGM96Correction() == NOT_AVAILABLE) || (!EGMAltitudeCorrection)) ? 0 : loc.getAltitudeEGM96Correction())));
                                    GPXbw.write("</ele>");
                                }

                                GPXbw.write("<time>");     // Time
                                GPXbw.write(dfdt.format(loc.getLocation().getTime()));
                                GPXbw.write("</time>");


                                //if (loc.getNumberOfSatellites() > 0) {
                                //    GPXbw.write("<sat>");
                                //    GPXbw.write(String.valueOf(loc.getNumberOfSatellites()));
                                //    GPXbw.write("</sat>");
                                ///

                                GPXbw.write("<name>");     // Name
                                GPXbw.write(loc.getDescription()
                                        .replace("<","&lt;")
                                        .replace("&","&amp;")
                                        .replace(">","&gt;")
                                        .replace("\"","&quot;")
                                        .replace("'","&apos;"));
                                GPXbw.write("</name></wpt>" + newLine + newLine);
                            }
                        }
                    }

                    long progress = 100L * (track.getNumberOfLocations() + i + GroupOfLocations) / (track.getNumberOfLocations() + track.getNumberOfPlacemarks());
                    if (progress > 99) progress = 99;
                    if (progress < 1) progress = 1;
                    EventBus.getDefault().post("TRACK_SETPROGRESS " + track.getId() + " " + progress);
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

            Log.w("myApp", "[#] Exporter.java - Files exported!");

            EventBus.getDefault().post("TRACK_SETPROGRESS " + track.getId() + " 100");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Log.w("myApp", "[#] Exporter.java - Cannot wait!!");
            }

            EventBus.getDefault().post("TRACK_EXPORTED " + track.getId());

        } catch (IOException e) {
            //e.printStackTrace();
            //Toast.makeText(context, "File not saved",Toast.LENGTH_SHORT).show();
        }
    }
}
