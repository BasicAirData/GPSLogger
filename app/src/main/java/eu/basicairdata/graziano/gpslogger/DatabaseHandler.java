/*
 * DatabaseHandler - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 1/5/2016
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;


class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 2;          // Updated to 2 in v2.1.3 (code 14)
    private static final int LOCATION_TYPE_LOCATION = 1;
    private static final int LOCATION_TYPE_PLACEMARK = 2;

    // Database Name
    private static final String DATABASE_NAME = "GPSLogger";

    // -------------------------------------------------------------------------------- Table names
    private static final String TABLE_LOCATIONS = "locations";
    private static final String TABLE_TRACKS = "tracks";
    private static final String TABLE_PLACEMARKS = "placemarks";

    // ----------------------------------------------------------------------- Common Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TRACK_ID = "track_id";

    // --------------------------------------------------------------- Location Table Columns names
    private static final String KEY_LOCATION_NUMBER = "nr";
    private static final String KEY_LOCATION_LATITUDE = "latitude";
    private static final String KEY_LOCATION_LONGITUDE = "longitude";
    private static final String KEY_LOCATION_ALTITUDE = "altitude";
    private static final String KEY_LOCATION_SPEED = "speed";
    private static final String KEY_LOCATION_ACCURACY = "accuracy";
    private static final String KEY_LOCATION_BEARING = "bearing";
    private static final String KEY_LOCATION_TIME = "time";
    private static final String KEY_LOCATION_NUMBEROFSATELLITES = "number_of_satellites";
    private static final String KEY_LOCATION_TYPE = "type";
    private static final String KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX = "number_of_satellites_used_in_fix";

    // ---------------------------------------------------------------------------- Placemarks adds
    private static final String KEY_LOCATION_NAME = "name";

    // ------------------------------------------------------------------ Track Table Columns names
    private static final String KEY_TRACK_NAME = "name";
    private static final String KEY_TRACK_FROM = "location_from";
    private static final String KEY_TRACK_TO = "location_to";


    private static final String KEY_TRACK_START_LATITUDE        = "start_latitude";
    private static final String KEY_TRACK_START_LONGITUDE       = "start_longitude";
    private static final String KEY_TRACK_START_ALTITUDE        = "start_altitude";
    private static final String KEY_TRACK_START_ACCURACY        = "start_accuracy";
    private static final String KEY_TRACK_START_SPEED           = "start_speed";
    private static final String KEY_TRACK_START_TIME            = "start_time";

    private static final String KEY_TRACK_LASTFIX_TIME = "lastfix_time";

    private static final String KEY_TRACK_END_LATITUDE = "end_latitude";
    private static final String KEY_TRACK_END_LONGITUDE = "end_longitude";
    private static final String KEY_TRACK_END_ALTITUDE = "end_altitude";
    private static final String KEY_TRACK_END_ACCURACY = "end_accuracy";
    private static final String KEY_TRACK_END_SPEED = "end_speed";
    private static final String KEY_TRACK_END_TIME = "end_time";

    private static final String KEY_TRACK_LASTSTEPDST_LATITUDE = "laststepdst_latitude";
    private static final String KEY_TRACK_LASTSTEPDST_LONGITUDE = "laststepdst_longitude";
    private static final String KEY_TRACK_LASTSTEPDST_ACCURACY = "laststepdst_accuracy";

    private static final String KEY_TRACK_LASTSTEPALT_ALTITUDE = "laststepalt_altitude";
    private static final String KEY_TRACK_LASTSTEPALT_ACCURACY = "laststepalt_accuracy";

    private static final String KEY_TRACK_MIN_LATITUDE = "min_latitude";
    private static final String KEY_TRACK_MIN_LONGITUDE = "min_longitude";

    private static final String KEY_TRACK_MAX_LATITUDE = "max_latitude";
    private static final String KEY_TRACK_MAX_LONGITUDE = "max_longitude";

    private static final String KEY_TRACK_DURATION = "duration";
    private static final String KEY_TRACK_DURATION_MOVING = "duration_moving";

    private static final String KEY_TRACK_DISTANCE = "distance";
    private static final String KEY_TRACK_DISTANCE_INPROGRESS = "distance_in_progress";
    private static final String KEY_TRACK_DISTANCE_LASTALTITUDE = "distance_last_altitude";

    private static final String KEY_TRACK_ALTITUDE_UP = "altitude_up";
    private static final String KEY_TRACK_ALTITUDE_DOWN = "altitude_down";
    private static final String KEY_TRACK_ALTITUDE_INPROGRESS = "altitude_in_progress";

    private static final String KEY_TRACK_SPEED_MAX = "speed_max";
    private static final String KEY_TRACK_SPEED_AVERAGE = "speed_average";
    private static final String KEY_TRACK_SPEED_AVERAGEMOVING = "speed_average_moving";

    private static final String KEY_TRACK_NUMBEROFLOCATIONS = "number_of_locations";
    private static final String KEY_TRACK_NUMBEROFPLACEMARKS = "number_of_placemarks";
    private static final String KEY_TRACK_TYPE = "type";

    private static final String KEY_TRACK_VALIDMAP = "validmap";



    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TRACKS_TABLE = "CREATE TABLE " + TABLE_TRACKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"    // 0
                + KEY_TRACK_NAME + " TEXT,"                         // 1
                + KEY_TRACK_FROM + " TEXT,"                         // 2
                + KEY_TRACK_TO + " TEXT,"                           // 3
                + KEY_TRACK_START_LATITUDE + " REAL,"               // 4
                + KEY_TRACK_START_LONGITUDE + " REAL,"              // 5
                + KEY_TRACK_START_ALTITUDE + " REAL,"               // 6
                + KEY_TRACK_START_ACCURACY + " REAL,"               // 7
                + KEY_TRACK_START_SPEED + " REAL,"                  // 8
                + KEY_TRACK_START_TIME + " REAL,"                   // 9
                + KEY_TRACK_LASTFIX_TIME + " REAL,"                 // 10
                + KEY_TRACK_END_LATITUDE + " REAL,"                 // 11
                + KEY_TRACK_END_LONGITUDE + " REAL,"                // 12
                + KEY_TRACK_END_ALTITUDE + " REAL,"                 // 13
                + KEY_TRACK_END_ACCURACY + " REAL,"                 // 14
                + KEY_TRACK_END_SPEED + " REAL,"                    // 15
                + KEY_TRACK_END_TIME + " REAL,"                     // 16
                + KEY_TRACK_LASTSTEPDST_LATITUDE + " REAL,"         // 17
                + KEY_TRACK_LASTSTEPDST_LONGITUDE + " REAL,"        // 18
                + KEY_TRACK_LASTSTEPDST_ACCURACY + " REAL,"         // 19
                + KEY_TRACK_LASTSTEPALT_ALTITUDE + " REAL,"         // 20
                + KEY_TRACK_LASTSTEPALT_ACCURACY + " REAL,"         // 21
                + KEY_TRACK_MIN_LATITUDE + " REAL,"                 // 22
                + KEY_TRACK_MIN_LONGITUDE + " REAL,"                // 23
                + KEY_TRACK_MAX_LATITUDE + " REAL,"                 // 24
                + KEY_TRACK_MAX_LONGITUDE + " REAL,"                // 25
                + KEY_TRACK_DURATION + " REAL,"                     // 26
                + KEY_TRACK_DURATION_MOVING + " REAL,"              // 27
                + KEY_TRACK_DISTANCE + " REAL,"                     // 28
                + KEY_TRACK_DISTANCE_INPROGRESS + " REAL,"          // 29
                + KEY_TRACK_DISTANCE_LASTALTITUDE + " REAL,"        // 30
                + KEY_TRACK_ALTITUDE_UP + " REAL,"                  // 31
                + KEY_TRACK_ALTITUDE_DOWN + " REAL,"                // 32
                + KEY_TRACK_ALTITUDE_INPROGRESS + " REAL,"          // 33
                + KEY_TRACK_SPEED_MAX + " REAL,"                    // 34
                + KEY_TRACK_SPEED_AVERAGE + " REAL,"                // 35
                + KEY_TRACK_SPEED_AVERAGEMOVING + " REAL,"          // 36
                + KEY_TRACK_NUMBEROFLOCATIONS + " INTEGER,"         // 37
                + KEY_TRACK_NUMBEROFPLACEMARKS + " INTEGER,"        // 38
                + KEY_TRACK_VALIDMAP + " INTEGER,"                  // 39
                + KEY_TRACK_TYPE + " INTEGER " + ")";               // 40
        db.execSQL(CREATE_TRACKS_TABLE);

        String CREATE_LOCATIONS_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"                // 0
                + KEY_TRACK_ID + " INTEGER,"                                    // 1
                + KEY_LOCATION_NUMBER + " INTEGER,"                             // 2
                + KEY_LOCATION_LATITUDE + " REAL,"                              // 3
                + KEY_LOCATION_LONGITUDE + " REAL,"                             // 4
                + KEY_LOCATION_ALTITUDE + " REAL,"                              // 5
                + KEY_LOCATION_SPEED + " REAL,"                                 // 6
                + KEY_LOCATION_ACCURACY + " REAL,"                              // 7
                + KEY_LOCATION_BEARING + " REAL,"                               // 8
                + KEY_LOCATION_TIME + " REAL,"                                  // 9
                + KEY_LOCATION_NUMBEROFSATELLITES + " INTEGER,"                 // 10
                + KEY_LOCATION_TYPE + " INTEGER,"                               // 11
                + KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX + " INTEGER" + ")";  // 12
        db.execSQL(CREATE_LOCATIONS_TABLE);

        String CREATE_PLACEMARKS_TABLE = "CREATE TABLE " + TABLE_PLACEMARKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"                // 0
                + KEY_TRACK_ID + " INTEGER,"                                    // 1
                + KEY_LOCATION_NUMBER + " INTEGER,"                             // 2
                + KEY_LOCATION_LATITUDE + " REAL,"                              // 3
                + KEY_LOCATION_LONGITUDE + " REAL,"                             // 4
                + KEY_LOCATION_ALTITUDE + " REAL,"                              // 5
                + KEY_LOCATION_SPEED + " REAL,"                                 // 6
                + KEY_LOCATION_ACCURACY + " REAL,"                              // 7
                + KEY_LOCATION_BEARING + " REAL,"                               // 8
                + KEY_LOCATION_TIME + " REAL,"                                  // 9
                + KEY_LOCATION_NUMBEROFSATELLITES + " INTEGER,"                 // 10
                + KEY_LOCATION_TYPE + " INTEGER,"                               // 11
                + KEY_LOCATION_NAME + " TEXT,"                                  // 12
                + KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX + " INTEGER" + ")";  // 13
        db.execSQL(CREATE_PLACEMARKS_TABLE);
    }


    private static final int NOT_AVAILABLE = -100000;

    private static final String DATABASE_ALTER_TABLE_LOCATIONS_TO_V2 = "ALTER TABLE "
            + TABLE_LOCATIONS + " ADD COLUMN " + KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX + " INTEGER DEFAULT " +  NOT_AVAILABLE + ";";
    private static final String DATABASE_ALTER_TABLE_PLACEMARKS_TO_V2 = "ALTER TABLE "
            + TABLE_PLACEMARKS + " ADD COLUMN " + KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX + " INTEGER DEFAULT " +  NOT_AVAILABLE + ";";

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Use this function in case of DB version upgrade.

        // Drop older table if existed
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACEMARKS);
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKS);

        // Create tables again
        //onCreate(db);

        switch (oldVersion)
        {
            case 1:
                //upgrade from version 1 to 2
                //Log.w("myApp", "[#] DatabaseHandler.java - onUpgrade: from version 1 to 2 ...");
                db.execSQL(DATABASE_ALTER_TABLE_LOCATIONS_TO_V2);
                db.execSQL(DATABASE_ALTER_TABLE_PLACEMARKS_TO_V2);
            //case 2:
                //upgrade from version 2 to 3
            //    db.execSQL(DATABASE_ALTER_TEAM_TO_V3);

                //and so on.. do not add breaks so that switch will
                //start at oldVersion, and run straight through to the latest
        }
        //Log.w("myApp", "[#] DatabaseHandler.java - onUpgrade: DB upgraded to version " + newVersion);
    }

// ----------------------------------------------------------------------- LOCATIONS AND PLACEMARKS

    // Add new Location and update the corresponding track
    public void addLocationToTrack(LocationExtended location, Track track) {
        SQLiteDatabase db = this.getWritableDatabase();

        Location loc = location.getLocation();

        ContentValues locvalues = new ContentValues();
        locvalues.put(KEY_TRACK_ID, track.getId());
        locvalues.put(KEY_LOCATION_NUMBER, track.getNumberOfLocations());
        locvalues.put(KEY_LOCATION_LATITUDE, loc.getLatitude());
        locvalues.put(KEY_LOCATION_LONGITUDE, loc.getLongitude());
        locvalues.put(KEY_LOCATION_ALTITUDE, loc.hasAltitude() ? loc.getAltitude() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_SPEED, loc.hasSpeed() ? loc.getSpeed() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_ACCURACY, loc.hasAccuracy() ? loc.getAccuracy() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_BEARING, loc.hasBearing() ? loc.getBearing() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_TIME, loc.getTime());
        locvalues.put(KEY_LOCATION_NUMBEROFSATELLITES, location.getNumberOfSatellites());
        locvalues.put(KEY_LOCATION_TYPE, LOCATION_TYPE_LOCATION);
        locvalues.put(KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX, location.getNumberOfSatellitesUsedInFix());

        ContentValues trkvalues = new ContentValues();
        trkvalues.put(KEY_TRACK_NAME, track.getName());
        trkvalues.put(KEY_TRACK_FROM, "");
        trkvalues.put(KEY_TRACK_TO, "");

        trkvalues.put(KEY_TRACK_START_LATITUDE, track.getStart_Latitude());
        trkvalues.put(KEY_TRACK_START_LONGITUDE, track.getStart_Longitude());
        trkvalues.put(KEY_TRACK_START_ALTITUDE, track.getStart_Altitude());
        trkvalues.put(KEY_TRACK_START_ACCURACY, track.getStart_Accuracy());
        trkvalues.put(KEY_TRACK_START_SPEED, track.getStart_Speed());
        trkvalues.put(KEY_TRACK_START_TIME, track.getStart_Time());

        trkvalues.put(KEY_TRACK_LASTFIX_TIME, track.getLastFix_Time());

        trkvalues.put(KEY_TRACK_END_LATITUDE, track.getEnd_Latitude());
        trkvalues.put(KEY_TRACK_END_LONGITUDE, track.getEnd_Longitude());
        trkvalues.put(KEY_TRACK_END_ALTITUDE, track.getEnd_Altitude());
        trkvalues.put(KEY_TRACK_END_ACCURACY, track.getEnd_Accuracy());
        trkvalues.put(KEY_TRACK_END_SPEED, track.getEnd_Speed());
        trkvalues.put(KEY_TRACK_END_TIME, track.getEnd_Time());

        trkvalues.put(KEY_TRACK_LASTSTEPDST_LATITUDE, track.getLastStepDistance_Latitude());
        trkvalues.put(KEY_TRACK_LASTSTEPDST_LONGITUDE, track.getLastStepDistance_Longitude());
        trkvalues.put(KEY_TRACK_LASTSTEPDST_ACCURACY, track.getLastStepDistance_Accuracy());

        trkvalues.put(KEY_TRACK_LASTSTEPALT_ALTITUDE, track.getLastStepAltitude_Altitude());
        trkvalues.put(KEY_TRACK_LASTSTEPALT_ACCURACY, track.getLastStepAltitude_Accuracy());

        trkvalues.put(KEY_TRACK_MIN_LATITUDE, track.getMin_Latitude());
        trkvalues.put(KEY_TRACK_MIN_LONGITUDE, track.getMin_Longitude());

        trkvalues.put(KEY_TRACK_MAX_LATITUDE, track.getMax_Latitude());
        trkvalues.put(KEY_TRACK_MAX_LONGITUDE, track.getMax_Longitude());

        trkvalues.put(KEY_TRACK_DURATION, track.getDuration());
        trkvalues.put(KEY_TRACK_DURATION_MOVING, track.getDuration_Moving());

        trkvalues.put(KEY_TRACK_DISTANCE, track.getDistance());
        trkvalues.put(KEY_TRACK_DISTANCE_INPROGRESS, track.getDistanceInProgress());
        trkvalues.put(KEY_TRACK_DISTANCE_LASTALTITUDE, track.getDistanceLastAltitude());

        trkvalues.put(KEY_TRACK_ALTITUDE_UP, track.getAltitude_Up());
        trkvalues.put(KEY_TRACK_ALTITUDE_DOWN, track.getAltitude_Down());
        trkvalues.put(KEY_TRACK_ALTITUDE_INPROGRESS, track.getAltitude_InProgress());

        trkvalues.put(KEY_TRACK_SPEED_MAX, track.getSpeedMax());
        trkvalues.put(KEY_TRACK_SPEED_AVERAGE, track.getSpeedAverage());
        trkvalues.put(KEY_TRACK_SPEED_AVERAGEMOVING, track.getSpeedAverageMoving());

        trkvalues.put(KEY_TRACK_NUMBEROFLOCATIONS, track.getNumberOfLocations());
        trkvalues.put(KEY_TRACK_NUMBEROFPLACEMARKS, track.getNumberOfPlacemarks());
        trkvalues.put(KEY_TRACK_TYPE, track.getType());

        trkvalues.put(KEY_TRACK_VALIDMAP, track.getValidMap());

        db.beginTransaction();
        db.insert(TABLE_LOCATIONS, null, locvalues);                // Insert the new Location
        db.update(TABLE_TRACKS, trkvalues, KEY_ID + " = ?",
                new String[] { String.valueOf(track.getId()) });    // Update the corresponding Track
        db.setTransactionSuccessful();
        db.endTransaction();

        //Log.w("myApp", "[#] DatabaseHandler.java - addLocation: Location " + track.getNumberOfLocations() + " added into track " + track.getID());
    }


    // Add new Placemark and update the corresponding track
    public void addPlacemarkToTrack(LocationExtended placemark, Track track) {
        SQLiteDatabase db = this.getWritableDatabase();

        Location loc = placemark.getLocation();

        ContentValues locvalues = new ContentValues();
        locvalues.put(KEY_TRACK_ID, track.getId());
        locvalues.put(KEY_LOCATION_NUMBER, track.getNumberOfPlacemarks());
        locvalues.put(KEY_LOCATION_LATITUDE, loc.getLatitude());
        locvalues.put(KEY_LOCATION_LONGITUDE, loc.getLongitude());
        locvalues.put(KEY_LOCATION_ALTITUDE, loc.hasAltitude() ? loc.getAltitude() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_SPEED, loc.hasSpeed() ? loc.getSpeed() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_ACCURACY, loc.hasAccuracy() ? loc.getAccuracy() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_BEARING, loc.hasBearing() ? loc.getBearing() : NOT_AVAILABLE);
        locvalues.put(KEY_LOCATION_TIME, loc.getTime());
        locvalues.put(KEY_LOCATION_NUMBEROFSATELLITES, placemark.getNumberOfSatellites());
        locvalues.put(KEY_LOCATION_TYPE, LOCATION_TYPE_PLACEMARK);
        locvalues.put(KEY_LOCATION_NAME, placemark.getDescription());
        locvalues.put(KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX, placemark.getNumberOfSatellitesUsedInFix());

        ContentValues trkvalues = new ContentValues();
        trkvalues.put(KEY_TRACK_NAME, track.getName());
        trkvalues.put(KEY_TRACK_FROM, "");
        trkvalues.put(KEY_TRACK_TO, "");

        trkvalues.put(KEY_TRACK_START_LATITUDE, track.getStart_Latitude());
        trkvalues.put(KEY_TRACK_START_LONGITUDE, track.getStart_Longitude());
        trkvalues.put(KEY_TRACK_START_ALTITUDE, track.getStart_Altitude());
        trkvalues.put(KEY_TRACK_START_ACCURACY, track.getStart_Accuracy());
        trkvalues.put(KEY_TRACK_START_SPEED, track.getStart_Speed());
        trkvalues.put(KEY_TRACK_START_TIME, track.getStart_Time());

        trkvalues.put(KEY_TRACK_LASTFIX_TIME, track.getLastFix_Time());

        trkvalues.put(KEY_TRACK_END_LATITUDE, track.getEnd_Latitude());
        trkvalues.put(KEY_TRACK_END_LONGITUDE, track.getEnd_Longitude());
        trkvalues.put(KEY_TRACK_END_ALTITUDE, track.getEnd_Altitude());
        trkvalues.put(KEY_TRACK_END_ACCURACY, track.getEnd_Accuracy());
        trkvalues.put(KEY_TRACK_END_SPEED, track.getEnd_Speed());
        trkvalues.put(KEY_TRACK_END_TIME, track.getEnd_Time());

        trkvalues.put(KEY_TRACK_LASTSTEPDST_LATITUDE, track.getLastStepDistance_Latitude());
        trkvalues.put(KEY_TRACK_LASTSTEPDST_LONGITUDE, track.getLastStepDistance_Longitude());
        trkvalues.put(KEY_TRACK_LASTSTEPDST_ACCURACY, track.getLastStepDistance_Accuracy());

        trkvalues.put(KEY_TRACK_LASTSTEPALT_ALTITUDE, track.getLastStepAltitude_Altitude());
        trkvalues.put(KEY_TRACK_LASTSTEPALT_ACCURACY, track.getLastStepAltitude_Accuracy());

        trkvalues.put(KEY_TRACK_MIN_LATITUDE, track.getMin_Latitude());
        trkvalues.put(KEY_TRACK_MIN_LONGITUDE, track.getMin_Longitude());

        trkvalues.put(KEY_TRACK_MAX_LATITUDE, track.getMax_Latitude());
        trkvalues.put(KEY_TRACK_MAX_LONGITUDE, track.getMax_Longitude());

        trkvalues.put(KEY_TRACK_DURATION, track.getDuration());
        trkvalues.put(KEY_TRACK_DURATION_MOVING, track.getDuration_Moving());

        trkvalues.put(KEY_TRACK_DISTANCE, track.getDistance());
        trkvalues.put(KEY_TRACK_DISTANCE_INPROGRESS, track.getDistanceInProgress());
        trkvalues.put(KEY_TRACK_DISTANCE_LASTALTITUDE, track.getDistanceLastAltitude());

        trkvalues.put(KEY_TRACK_ALTITUDE_UP, track.getAltitude_Up());
        trkvalues.put(KEY_TRACK_ALTITUDE_DOWN, track.getAltitude_Down());
        trkvalues.put(KEY_TRACK_ALTITUDE_INPROGRESS, track.getAltitude_InProgress());

        trkvalues.put(KEY_TRACK_SPEED_MAX, track.getSpeedMax());
        trkvalues.put(KEY_TRACK_SPEED_AVERAGE, track.getSpeedAverage());
        trkvalues.put(KEY_TRACK_SPEED_AVERAGEMOVING, track.getSpeedAverageMoving());

        trkvalues.put(KEY_TRACK_NUMBEROFLOCATIONS, track.getNumberOfLocations());
        trkvalues.put(KEY_TRACK_NUMBEROFPLACEMARKS, track.getNumberOfPlacemarks());
        trkvalues.put(KEY_TRACK_TYPE, track.getType());

        trkvalues.put(KEY_TRACK_VALIDMAP, track.getValidMap());

        db.beginTransaction();
        db.insert(TABLE_PLACEMARKS, null, locvalues);                // Insert the new Location
        db.update(TABLE_TRACKS, trkvalues, KEY_ID + " = ?",
                new String[] { String.valueOf(track.getId()) });    // Update the corresponding Track
        db.setTransactionSuccessful();
        db.endTransaction();

        //Log.w("myApp", "[#] DatabaseHandler.java - addLocation: Location " + track.getNumberOfLocations() + " added into track " + track.getID());
    }


    // Get single Location
    public LocationExtended getLocation(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        LocationExtended extdloc = null;
        double lcdata_double;
        float lcdata_float;

        Cursor cursor = db.query(TABLE_LOCATIONS, new String[] {KEY_ID,
                        KEY_LOCATION_LATITUDE,
                        KEY_LOCATION_LONGITUDE,
                        KEY_LOCATION_ALTITUDE,
                        KEY_LOCATION_SPEED,
                        KEY_LOCATION_ACCURACY,
                        KEY_LOCATION_BEARING,
                        KEY_LOCATION_TIME,
                        KEY_LOCATION_NUMBEROFSATELLITES,
                        KEY_LOCATION_NUMBEROFSATELLITESUSEDINFIX}, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            Location lc = new Location("DB");
            lc.setLatitude(cursor.getDouble(1));
            lc.setLongitude(cursor.getDouble(2));

            lcdata_double = cursor.getDouble(3);
            if (lcdata_double != NOT_AVAILABLE) lc.setAltitude(lcdata_double);
            //else lc.removeAltitude();

            lcdata_float = cursor.getFloat(4);
            if (lcdata_float != NOT_AVAILABLE) lc.setSpeed(lcdata_float);
            //else lc.removeSpeed();

            lcdata_float = cursor.getFloat(5);
            if (lcdata_float != NOT_AVAILABLE) lc.setAccuracy(lcdata_float);
            //else lc.removeAccuracy();

            lcdata_float = cursor.getFloat(6);
            if (lcdata_float != NOT_AVAILABLE) lc.setBearing(lcdata_float);
            //else lc.removeBearing();

            lc.setTime(cursor.getLong(7));


            extdloc = new LocationExtended(lc);
            extdloc.setNumberOfSatellites(cursor.getInt(8));
            extdloc.setNumberOfSatellitesUsedInFix(cursor.getInt(9));

            cursor.close();
        }
        return extdloc != null ? extdloc : null;
    }



    // Getting a list of Locations associated to a specified track, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<LocationExtended> getLocationsList(long TrackID, long startNumber, long endNumber) {

        List<LocationExtended> locationList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_LOCATIONS + " WHERE "
                + KEY_TRACK_ID + " = " + TrackID + " AND "
                + KEY_LOCATION_NUMBER + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_LOCATION_NUMBER;

        //Log.w("myApp", "[#] DatabaseHandler.java - getLocationList(" + TrackID + ", " + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        double lcdata_double;
        float lcdata_float;

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Location lc = new Location("DB");
                    lc.setLatitude(cursor.getDouble(3));
                    lc.setLongitude(cursor.getDouble(4));

                    lcdata_double = cursor.getDouble(5);
                    if (lcdata_double != NOT_AVAILABLE) lc.setAltitude(lcdata_double);
                    //else lc.removeAltitude();

                    lcdata_float = cursor.getFloat(6);
                    if (lcdata_float != NOT_AVAILABLE) lc.setSpeed(lcdata_float);
                    //else lc.removeSpeed();

                    lcdata_float = cursor.getFloat(7);
                    if (lcdata_float != NOT_AVAILABLE) lc.setAccuracy(lcdata_float);
                    //else lc.removeAccuracy();

                    lcdata_float = cursor.getFloat(8);
                    if (lcdata_float != NOT_AVAILABLE) lc.setBearing(lcdata_float);
                    //else lc.removeBearing();

                    lc.setTime(cursor.getLong(9));

                    LocationExtended extdloc = new LocationExtended(lc);
                    extdloc.setNumberOfSatellites(cursor.getInt(10));
                    extdloc.setNumberOfSatellitesUsedInFix(cursor.getInt(12));

                    locationList.add(extdloc); // Add Location to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return locationList;
    }

    // Getting a list of Locations associated to a specified track, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<LocationExtended> getPlacemarksList(long TrackID, long startNumber, long endNumber) {

        List<LocationExtended> placemarkList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_PLACEMARKS + " WHERE "
                + KEY_TRACK_ID + " = " + TrackID + " AND "
                + KEY_LOCATION_NUMBER + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_LOCATION_NUMBER;

        //Log.w("myApp", "[#] DatabaseHandler.java - getLocationList(" + TrackID + ", " + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        double lcdata_double;
        float lcdata_float;

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Location lc = new Location("DB");
                    lc.setLatitude(cursor.getDouble(3));
                    lc.setLongitude(cursor.getDouble(4));

                    lcdata_double = cursor.getDouble(5);
                    if (lcdata_double != NOT_AVAILABLE) lc.setAltitude(lcdata_double);
                    //else lc.removeAltitude();

                    lcdata_float = cursor.getFloat(6);
                    if (lcdata_float != NOT_AVAILABLE) lc.setSpeed(lcdata_float);
                    //else lc.removeSpeed();

                    lcdata_float = cursor.getFloat(7);
                    if (lcdata_float != NOT_AVAILABLE) lc.setAccuracy(lcdata_float);
                    //else lc.removeAccuracy();

                    lcdata_float = cursor.getFloat(8);
                    if (lcdata_float != NOT_AVAILABLE) lc.setBearing(lcdata_float);
                    //else lc.removeBearing();

                    lc.setTime(cursor.getLong(9));

                    LocationExtended extdloc = new LocationExtended(lc);
                    extdloc.setNumberOfSatellites(cursor.getInt(10));
                    extdloc.setNumberOfSatellitesUsedInFix(cursor.getInt(13));
                    extdloc.setDescription(cursor.getString(12));

                    placemarkList.add(extdloc); // Add Location to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return placemarkList;
    }


    // Getting a list of Locations associated to a specified track, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<LatLng> getLatLngList(long TrackID, long startNumber, long endNumber) {

        List<LatLng> latlngList = new ArrayList<>();

        String selectQuery = "SELECT " + KEY_TRACK_ID + "," + KEY_LOCATION_LATITUDE + "," + KEY_LOCATION_LONGITUDE + "," + KEY_LOCATION_NUMBER
                + " FROM " + TABLE_LOCATIONS + " WHERE "
                + KEY_TRACK_ID + " = " + TrackID + " AND "
                + KEY_LOCATION_NUMBER + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_LOCATION_NUMBER;

        //Log.w("myApp", "[#] DatabaseHandler.java - getLocationList(" + TrackID + ", " + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    LatLng latlng = new LatLng();
                    latlng.Latitude = cursor.getDouble(1);
                    latlng.Longitude = cursor.getDouble(2);

                    latlngList.add(latlng); // Add Location to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return latlngList;
    }


    // Get the total amount of Locations stored in the DB
    public long getLocationsTotalCount() {
        String countQuery = "SELECT  * FROM " + TABLE_LOCATIONS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        long result = cursor.getCount();
        cursor.close();
        return result;
    }


    // Get the number of Locations of a Track
    public long getLocationsCount(long TrackID) {
        String countQuery = "SELECT  * FROM " + TABLE_LOCATIONS + " WHERE " + KEY_TRACK_ID + " = " + TrackID;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        long result = cursor.getCount();
        cursor.close();
        return result;
    }


    // Get last Location ID
    public long getLastLocationID(long TrackID) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = 0;

        String query = "SELECT " + KEY_ID + " FROM " + TABLE_LOCATIONS +
                " WHERE " + KEY_TRACK_ID + " = " + TrackID + " ORDER BY " + KEY_ID + " DESC LIMIT 1";

        //Log.w("myApp", "[#] DatabaseHandler.java - getLastLocationID(): " + query);

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            //Log.w("myApp", "[#] !null");
            if (cursor.moveToFirst()) {
                result = cursor.getLong(0);
            } //else Log.w("myApp", "[#] DatabaseHandler.java - Location table is empty");
            cursor.close();
        }
        //Log.w("myApp", "[#] RETURN getLastTrack()");
        return result;
    }




// ----------------------------------------------------------------------------------------- TRACKS

    // Delete the track with the specified ID;
    // The method deletes also Placemarks and Locations associated to the specified track
    public void DeleteTrack(long TrackID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        db.delete(TABLE_PLACEMARKS, KEY_TRACK_ID + " = ?",
                new String[] { String.valueOf(TrackID) });    // Delete track's Placemarks
        db.delete(TABLE_LOCATIONS, KEY_TRACK_ID + " = ?",
                new String[] { String.valueOf(TrackID) });    // Delete track's Locations
        db.delete(TABLE_TRACKS, KEY_ID + " = ?",
                new String[] { String.valueOf(TrackID) });    // Delete track
        db.setTransactionSuccessful();
        db.endTransaction();

        //Log.w("myApp", "[#] DatabaseHandler.java - addLocation: Location " + track.getNumberOfLocations() + " added into track " + track.getID());
    }


    // Add a new Track, returns the TrackID
    public long addTrack(Track track) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues trkvalues = new ContentValues();
        trkvalues.put(KEY_TRACK_NAME, track.getName());
        trkvalues.put(KEY_TRACK_FROM, "");
        trkvalues.put(KEY_TRACK_TO, "");

        trkvalues.put(KEY_TRACK_START_LATITUDE, track.getStart_Latitude());
        trkvalues.put(KEY_TRACK_START_LONGITUDE, track.getStart_Longitude());
        trkvalues.put(KEY_TRACK_START_ALTITUDE, track.getStart_Altitude());
        trkvalues.put(KEY_TRACK_START_ACCURACY, track.getStart_Accuracy());
        trkvalues.put(KEY_TRACK_START_SPEED, track.getStart_Speed());
        trkvalues.put(KEY_TRACK_START_TIME, track.getStart_Time());

        trkvalues.put(KEY_TRACK_LASTFIX_TIME, track.getLastFix_Time());

        trkvalues.put(KEY_TRACK_END_LATITUDE, track.getEnd_Latitude());
        trkvalues.put(KEY_TRACK_END_LONGITUDE, track.getEnd_Longitude());
        trkvalues.put(KEY_TRACK_END_ALTITUDE, track.getEnd_Altitude());
        trkvalues.put(KEY_TRACK_END_ACCURACY, track.getEnd_Accuracy());
        trkvalues.put(KEY_TRACK_END_SPEED, track.getEnd_Speed());
        trkvalues.put(KEY_TRACK_END_TIME, track.getEnd_Time());

        trkvalues.put(KEY_TRACK_LASTSTEPDST_LATITUDE, track.getLastStepDistance_Latitude());
        trkvalues.put(KEY_TRACK_LASTSTEPDST_LONGITUDE, track.getLastStepDistance_Longitude());
        trkvalues.put(KEY_TRACK_LASTSTEPDST_ACCURACY, track.getLastStepDistance_Accuracy());

        trkvalues.put(KEY_TRACK_LASTSTEPALT_ALTITUDE, track.getLastStepAltitude_Altitude());
        trkvalues.put(KEY_TRACK_LASTSTEPALT_ACCURACY, track.getLastStepAltitude_Accuracy());

        trkvalues.put(KEY_TRACK_MIN_LATITUDE, track.getMin_Latitude());
        trkvalues.put(KEY_TRACK_MIN_LONGITUDE, track.getMin_Longitude());

        trkvalues.put(KEY_TRACK_MAX_LATITUDE, track.getMax_Latitude());
        trkvalues.put(KEY_TRACK_MAX_LONGITUDE, track.getMax_Longitude());

        trkvalues.put(KEY_TRACK_DURATION, track.getDuration());
        trkvalues.put(KEY_TRACK_DURATION_MOVING, track.getDuration_Moving());

        trkvalues.put(KEY_TRACK_DISTANCE, track.getDistance());
        trkvalues.put(KEY_TRACK_DISTANCE_INPROGRESS, track.getDistanceInProgress());
        trkvalues.put(KEY_TRACK_DISTANCE_LASTALTITUDE, track.getDistanceLastAltitude());

        trkvalues.put(KEY_TRACK_ALTITUDE_UP, track.getAltitude_Up());
        trkvalues.put(KEY_TRACK_ALTITUDE_DOWN, track.getAltitude_Down());
        trkvalues.put(KEY_TRACK_ALTITUDE_INPROGRESS, track.getAltitude_InProgress());

        trkvalues.put(KEY_TRACK_SPEED_MAX, track.getSpeedMax());
        trkvalues.put(KEY_TRACK_SPEED_AVERAGE, track.getSpeedAverage());
        trkvalues.put(KEY_TRACK_SPEED_AVERAGEMOVING, track.getSpeedAverageMoving());

        trkvalues.put(KEY_TRACK_NUMBEROFLOCATIONS, track.getNumberOfLocations());
        trkvalues.put(KEY_TRACK_NUMBEROFPLACEMARKS, track.getNumberOfPlacemarks());
        trkvalues.put(KEY_TRACK_TYPE, track.getType());

        trkvalues.put(KEY_TRACK_VALIDMAP, track.getValidMap());

        long TrackID;
        // Inserting Row
        TrackID = (db.insert(TABLE_TRACKS, null, trkvalues));

        //Log.w("myApp", "[#] DatabaseHandler.java - addTrack " + TrackID);

        return TrackID; // Insert this in the track ID !!!
    }


    // Get Track
    public Track getTrack(long TrackID) {

        Track track = null;

        String selectQuery = "SELECT  * FROM " + TABLE_TRACKS + " WHERE "
                + KEY_ID + " = " + TrackID;

        //Log.w("myApp", "[#] DatabaseHandler.java - getTrackList(" + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        //Log.w("myApp", "[#] DatabaseHandler.java - getTrack(" + TrackID + ")");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                track = new Track();
                track.FromDB(cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),

                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getDouble(6),
                        cursor.getFloat(7),
                        cursor.getFloat(8),
                        cursor.getLong(9),

                        cursor.getLong(10),

                        cursor.getDouble(11),
                        cursor.getDouble(12),
                        cursor.getDouble(13),
                        cursor.getFloat(14),
                        cursor.getFloat(15),
                        cursor.getLong(16),

                        cursor.getDouble(17),
                        cursor.getDouble(18),
                        cursor.getFloat(19),

                        cursor.getDouble(20),
                        cursor.getFloat(21),

                        cursor.getDouble(22),
                        cursor.getDouble(23),
                        cursor.getDouble(24),
                        cursor.getDouble(25),

                        cursor.getLong(26),
                        cursor.getLong(27),

                        cursor.getFloat(28),
                        cursor.getFloat(29),
                        cursor.getLong(30),

                        cursor.getDouble(31),
                        cursor.getDouble(32),
                        cursor.getDouble(33),

                        cursor.getFloat(34),
                        cursor.getFloat(35),
                        cursor.getFloat(36),

                        cursor.getLong(37),
                        cursor.getLong(38),

                        cursor.getInt(39),
                        cursor.getInt(40));
            }
            cursor.close();
        }
        return track;
    }


    // Get last TrackID
    public long getLastTrackID() {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = 0;

        String query = "SELECT " + KEY_ID + " FROM " + TABLE_TRACKS + " ORDER BY " + KEY_ID + " DESC LIMIT 1";

        //Log.w("myApp", "[#] DatabaseHandler.java - getLastTrackID(): " + query);

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            //Log.w("myApp", "[#] !null");
            if (cursor.moveToFirst()) {
                result = cursor.getLong(0);
            } //else Log.w("myApp", "[#] Tracks table is empty");
            cursor.close();
        }
        //Log.w("myApp", "[#] RETURN getLastTrack()");
        //Log.w("myApp", "[#] DatabaseHandler.java - LastTrackID = " + result);
        return result;
    }


    // Get last TrackID
    public Track getLastTrack() {
        return getTrack(getLastTrackID());
    }


    // Getting a list of Tracks, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<Track> getTracksList(long startNumber, long endNumber) {

        List<Track> trackList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_TRACKS + " WHERE "
                + KEY_ID + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_ID + " DESC";

        //Log.w("myApp", "[#] DatabaseHandler.java - getTrackList(" + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Track trk = new Track();
                    trk.FromDB(cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),

                            cursor.getDouble(4),
                            cursor.getDouble(5),
                            cursor.getDouble(6),
                            cursor.getFloat(7),
                            cursor.getFloat(8),
                            cursor.getLong(9),

                            cursor.getLong(10),

                            cursor.getDouble(11),
                            cursor.getDouble(12),
                            cursor.getDouble(13),
                            cursor.getFloat(14),
                            cursor.getFloat(15),
                            cursor.getLong(16),

                            cursor.getDouble(17),
                            cursor.getDouble(18),
                            cursor.getFloat(19),

                            cursor.getDouble(20),
                            cursor.getFloat(21),

                            cursor.getDouble(22),
                            cursor.getDouble(23),
                            cursor.getDouble(24),
                            cursor.getDouble(25),

                            cursor.getLong(26),
                            cursor.getLong(27),

                            cursor.getFloat(28),
                            cursor.getFloat(29),
                            cursor.getLong(30),

                            cursor.getDouble(31),
                            cursor.getDouble(32),
                            cursor.getDouble(33),

                            cursor.getFloat(34),
                            cursor.getFloat(35),
                            cursor.getFloat(36),

                            cursor.getLong(37),
                            cursor.getLong(38),

                            cursor.getInt(39),
                            cursor.getInt(40));

                    trackList.add(trk);             // Add Track to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return trackList;
    }


    // Getting the list of all Tracks in the DB
    /* NOT USED, COMMENTED OUT !!
    public List<Track> getTracksList() {

        List<Track> trackList = new ArrayList<Track>();

        String selectQuery = "SELECT  * FROM " + TABLE_TRACKS
                + " ORDER BY " + KEY_ID + " DESC";

        //Log.w("myApp", "[#] DatabaseHandler.java - getTrackList() ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    Track trk = new Track();
                    trk.FromDB(cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),

                            cursor.getDouble(4),
                            cursor.getDouble(5),
                            cursor.getDouble(6),
                            cursor.getFloat(7),
                            cursor.getFloat(8),
                            cursor.getLong(9),

                            cursor.getLong(10),

                            cursor.getDouble(11),
                            cursor.getDouble(12),
                            cursor.getDouble(13),
                            cursor.getFloat(14),
                            cursor.getFloat(15),
                            cursor.getLong(16),

                            cursor.getDouble(17),
                            cursor.getDouble(18),
                            cursor.getFloat(19),

                            cursor.getDouble(20),
                            cursor.getFloat(21),

                            cursor.getDouble(22),
                            cursor.getDouble(23),
                            cursor.getDouble(24),
                            cursor.getDouble(25),

                            cursor.getLong(26),
                            cursor.getLong(27),

                            cursor.getFloat(28),
                            cursor.getFloat(29),
                            cursor.getLong(30),

                            cursor.getDouble(31),
                            cursor.getDouble(32),
                            cursor.getDouble(33),

                            cursor.getFloat(34),
                            cursor.getFloat(35),
                            cursor.getFloat(36),

                            cursor.getLong(37),
                            cursor.getLong(38),

                            cursor.getInt(39),
                            cursor.getInt(40));
                    trackList.add(trk);             // Add Track to list
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return trackList;
    } */
}