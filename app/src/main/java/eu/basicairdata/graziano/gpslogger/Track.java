/*
 * Track - Java Class for Android
 * Created by G.Capelli on 1/5/2016
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

import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * Describes and manages a Track.
 */
public class Track {

    // Constants
    private static final double MIN_ALTITUDE_STEP           = 8.0;
    private static final float  MOVEMENT_SPEED_THRESHOLD    = 0.5f;     // The minimum speed (in m/s) to consider the user in movement
    private static final float  STANDARD_ACCURACY           = 10.0f;
    private static final float  SECURITY_COEFFICIENT        = 1.7f;

    public static final int TRACK_TYPE_STEADY           = 0;
    public static final int TRACK_TYPE_WALK             = 1;
    public static final int TRACK_TYPE_MOUNTAIN         = 2;
    public static final int TRACK_TYPE_RUN              = 3;
    public static final int TRACK_TYPE_BICYCLE          = 4;
    public static final int TRACK_TYPE_CAR              = 5;
    public static final int TRACK_TYPE_FLIGHT           = 6;
    public static final int TRACK_TYPE_HIKING           = 7;
    public static final int TRACK_TYPE_NORDICWALKING    = 8;
    public static final int TRACK_TYPE_SWIMMING         = 9;
    public static final int TRACK_TYPE_SCUBADIVING      = 10;
    public static final int TRACK_TYPE_ROWING           = 11;
    public static final int TRACK_TYPE_KAYAKING         = 12;
    public static final int TRACK_TYPE_SURFING          = 13;
    public static final int TRACK_TYPE_KITESURFING      = 14;
    public static final int TRACK_TYPE_SAILING          = 15;
    public static final int TRACK_TYPE_BOAT             = 16;
    public static final int TRACK_TYPE_DOWNHILLSKIING   = 17;
    public static final int TRACK_TYPE_SNOWBOARDING     = 18;
    public static final int TRACK_TYPE_SLEDDING         = 19;
    public static final int TRACK_TYPE_SNOWMOBILE       = 20;
    public static final int TRACK_TYPE_SNOWSHOEING      = 21;
    public static final int TRACK_TYPE_ICESKATING       = 22;
    public static final int TRACK_TYPE_HELICOPTER       = 23;
    public static final int TRACK_TYPE_ROCKET           = 24;
    public static final int TRACK_TYPE_PARAGLIDING      = 25;
    public static final int TRACK_TYPE_AIRBALLOON       = 26;
    public static final int TRACK_TYPE_SKATEBOARDING    = 27;
    public static final int TRACK_TYPE_ROLLERSKATING    = 28;
    public static final int TRACK_TYPE_WHEELCHAIR       = 29;
    public static final int TRACK_TYPE_ELECTRICSCOOTER  = 30;
    public static final int TRACK_TYPE_MOPED            = 31;
    public static final int TRACK_TYPE_MOTORCYCLE       = 32;
    public static final int TRACK_TYPE_TRUCK            = 33;
    public static final int TRACK_TYPE_BUS              = 34;
    public static final int TRACK_TYPE_TRAIN            = 35;
    public static final int TRACK_TYPE_AGRICULTURE      = 36;
    public static final int TRACK_TYPE_CITY             = 37;
    public static final int TRACK_TYPE_FOREST           = 38;
    public static final int TRACK_TYPE_WORK             = 39;
    public static final int TRACK_TYPE_PHOTOGRAPHY      = 40;
    public static final int TRACK_TYPE_RESEARCH         = 41;
    public static final int TRACK_TYPE_SOCCER           = 42;
    public static final int TRACK_TYPE_GOLF             = 43;
    public static final int TRACK_TYPE_PETS             = 44;
    public static final int TRACK_TYPE_MAP              = 45;
    public static final int TRACK_TYPE_ND               = NOT_AVAILABLE;

    public static final int[] ACTIVITY_DRAWABLE_RESOURCE = {    // The indexes must match the Track Types previously defined:
            R.drawable.ic_tracktype_place_24dp,                 // STEADY           = 0
            R.drawable.ic_tracktype_walk_24dp,                  // WALK             = 1
            R.drawable.ic_tracktype_mountain_24dp,              // MOUNTAIN         = 2
            R.drawable.ic_tracktype_run_24dp,                   // RUN              = 3
            R.drawable.ic_tracktype_bike_24dp,                  // BICYCLE          = 4
            R.drawable.ic_tracktype_car_24dp,                   // CAR              = 5
            R.drawable.ic_tracktype_flight_24dp,                // FLIGHT           = 6
            R.drawable.ic_tracktype_hiking_24,                  // HIKING           = 7
            R.drawable.ic_tracktype_nordic_walking_24,          // NORDICWALKING    = 8
            R.drawable.ic_tracktype_pool_24,                    // SWIMMING         = 9
            R.drawable.ic_tracktype_scuba_diving_24,            // SCUBADIVING      = 10
            R.drawable.ic_tracktype_rowing_24,                  // ROWING           = 11
            R.drawable.ic_tracktype_kayaking_24,                // KAYAKING         = 12
            R.drawable.ic_tracktype_surfing_24,                 // SURFING          = 13
            R.drawable.ic_tracktype_kitesurfing_24,             // KITESURFING      = 14
            R.drawable.ic_tracktype_sailing_24,                 // SAILING          = 15
            R.drawable.ic_tracktype_directions_boat_24,         // BOAT             = 16
            R.drawable.ic_tracktype_downhill_skiing_24,         // DOWNHILLSKIING   = 17
            R.drawable.ic_tracktype_snowboarding_24,            // SNOWBOARDING     = 18
            R.drawable.ic_tracktype_sledding_24,                // SLEDDING         = 19
            R.drawable.ic_tracktype_snowmobile_24,              // SNOWMOBILE       = 20
            R.drawable.ic_tracktype_snowshoeing_24,             // SNOWSHOEING      = 21
            R.drawable.ic_tracktype_ice_skating_24,             // ICESKATING       = 22
            R.drawable.ic_tracktype_helicopter_24,              // HELICOPTER       = 23
            R.drawable.ic_tracktype_rocket_24,                  // ROCKET           = 24
            R.drawable.ic_tracktype_paragliding_24,             // PARAGLIDING      = 25
            R.drawable.ic_tracktype_airballoon_24,              // AIRBALLOON       = 26
            R.drawable.ic_tracktype_skateboarding_24,           // SKATEBOARDING    = 27
            R.drawable.ic_tracktype_roller_skating_24,          // ROLLERSKATING    = 28
            R.drawable.ic_tracktype_wheelchair_24,              // WHEELCHAIR       = 29
            R.drawable.ic_tracktype_electric_scooter_24,        // ELECTRICSCOOTER  = 30
            R.drawable.ic_tracktype_moped_24,                   // MOPED            = 31
            R.drawable.ic_tracktype_sports_motorsports_24,      // MOTORCYCLE       = 32
            R.drawable.ic_tracktype_truck_24,                   // TRUCK            = 33
            R.drawable.ic_tracktype_directions_bus_24,          // BUS              = 34
            R.drawable.ic_tracktype_train_24,                   // TRAIN            = 35
            R.drawable.ic_tracktype_agriculture_24,             // AGRICULTURE      = 36
            R.drawable.ic_tracktype_city_24,                    // CITY             = 37
            R.drawable.ic_tracktype_forest_24,                  // FOREST           = 38
            R.drawable.ic_tracktype_work_24,                    // WORK             = 39
            R.drawable.ic_tracktype_camera_24,                  // PHOTOGRAPHY      = 40
            R.drawable.ic_tracktype_search_24,                  // RESEARCH         = 41
            R.drawable.ic_tracktype_sports_soccer_24,           // SOCCER           = 42
            R.drawable.ic_tracktype_golf_24,                    // GOLF             = 43
            R.drawable.ic_tracktype_pets_24,                    // PETS             = 44
            R.drawable.ic_tracktype_map_24                      // MAP              = 45
    };

    public static final String[] ACTIVITY_DESCRIPTION = {       // The indexes must match the Track Types previously defined:
            "steady",                                           // STEADY           = 0
            "walking",                                          // WALK             = 1
            "mountaineering",                                   // MOUNTAIN         = 2
            "running",                                          // RUN              = 3
            "cycling",                                          // BICYCLE          = 4
            "car",                                              // CAR              = 5
            "flying",                                           // FLIGHT           = 6
            "hiking",                                           // HIKING           = 7
            "nordic_walking",                                   // NORDICWALKING    = 8
            "swimming",                                         // SWIMMING         = 9
            "scuba_diving",                                     // SCUBADIVING      = 10
            "rowing",                                           // ROWING           = 11
            "kayaking",                                         // KAYAKING         = 12
            "surfing",                                          // SURFING          = 13
            "kitesurfing",                                      // KITESURFING      = 14
            "sailing",                                          // SAILING          = 15
            "boat",                                             // BOAT             = 16
            "downhill_skiing",                                  // DOWNHILLSKIING   = 17
            "snowboarding",                                     // SNOWBOARDING     = 18
            "sledding",                                         // SLEDDING         = 19
            "snowmobile",                                       // SNOWMOBILE       = 20
            "snowshoeing",                                      // SNOWSHOEING      = 21
            "ice_skating",                                      // ICESKATING       = 22
            "helicopter",                                       // HELICOPTER       = 23
            "rocket",                                           // ROCKET           = 24
            "paragliding",                                      // PARAGLIDING      = 25
            "air_balloon",                                      // AIRBALLOON       = 26
            "skateboarding",                                    // SKATEBOARDING    = 27
            "roller_skating",                                   // ROLLERSKATING    = 28
            "wheelchair",                                       // WHEELCHAIR       = 29
            "electric_scooter",                                 // ELECTRICSCOOTER  = 30
            "moped",                                            // MOPED            = 31
            "motorcycle",                                       // MOTORCYCLE       = 32
            "truck",                                            // TRUCK            = 33
            "bus",                                              // BUS              = 34
            "train",                                            // TRAIN            = 35
            "agriculture",                                      // AGRICULTURE      = 36
            "city",                                             // CITY             = 37
            "forest",                                           // FOREST           = 38
            "work",                                             // WORK             = 39
            "photography",                                      // PHOTOGRAPHY      = 40
            "research",                                         // RESEARCH         = 41
            "soccer",                                           // SOCCER           = 42
            "golf",                                             // GOLF             = 43
            "pets",                                             // PETS             = 44
            "map"                                               // MAP              = 45
    };

    // Variables
    private long    id;                                             // Saved in DB
    private String  name                        = "";               // Saved in DB
    private String  description                 = "";               // Saved in DB
    // The data related to the Start Point
    private double  latitudeStart               = NOT_AVAILABLE;    // Saved in DB
    private double  longitudeStart              = NOT_AVAILABLE;    // Saved in DB
    private double  altitudeStart               = NOT_AVAILABLE;    // Saved in DB
    private double  egmAltitudeCorrectionStart  = NOT_AVAILABLE;
    private float   accuracyStart               = STANDARD_ACCURACY;// Saved in DB
    private float   speedStart                  = NOT_AVAILABLE;    // Saved in DB
    private long    timeStart                   = NOT_AVAILABLE;    // Saved in DB
    // The data related to the Last FIX
    // added to the track
    private long    timeLastFix                 = NOT_AVAILABLE;    // Saved in DB
    // The data related to the End Point
    private double  latitudeEnd                 = NOT_AVAILABLE;    // Saved in DB
    private double  longitudeEnd                = NOT_AVAILABLE;    // Saved in DB
    private double  altitudeEnd                 = NOT_AVAILABLE;    // Saved in DB
    private double  egmAltitudeCorrectionEnd    = NOT_AVAILABLE;
    private float   accuracyEnd                 = STANDARD_ACCURACY;// Saved in DB
    private float   speedEnd                    = NOT_AVAILABLE;    // Saved in DB
    private long    timeEnd                     = NOT_AVAILABLE;    // Saved in DB
    // The data related to the Point
    // stored as last Step for Distance calculation
    private double  latitudeLastStepDistance    = NOT_AVAILABLE;    // Saved in DB
    private double  longitudeLastStepDistance   = NOT_AVAILABLE;    // Saved in DB
    private float   accuracyLastStepDistance    = STANDARD_ACCURACY;// Saved in DB
    // The data related to the Point
    // stored as last Step for Altitude
    private double  altitudeLastStepAltitude    = NOT_AVAILABLE;    // Saved in DB
    private float   accuracyLastStepAltitude    = STANDARD_ACCURACY;// Saved in DB

    private double  latitudeMin                 = NOT_AVAILABLE;    // Saved in DB
    private double  longitudeMin                = NOT_AVAILABLE;    // Saved in DB

    private double  latitudeMax                 = NOT_AVAILABLE;    // Saved in DB
    private double  longitudeMax                = NOT_AVAILABLE;    // Saved in DB

    private long    duration                    = NOT_AVAILABLE;    // Saved in DB
    private long    durationMoving              = NOT_AVAILABLE;    // Saved in DB

    private float   distance                    = NOT_AVAILABLE;    // Saved in DB
    private float   distanceInProgress          = NOT_AVAILABLE;    // Saved in DB
    private long    distanceLastAltitude        = NOT_AVAILABLE;    // Saved in DB

    private double  altitudeUp                  = NOT_AVAILABLE;    // Saved in DB
    private double  altitudeDown                = NOT_AVAILABLE;    // Saved in DB
    private double  altitudeInProgress          = NOT_AVAILABLE;    // Saved in DB

    private float   speedMax                    = NOT_AVAILABLE;    // Saved in DB
    private float   speedAverage                = NOT_AVAILABLE;    // Saved in DB
    private float   speedAverageMoving          = NOT_AVAILABLE;    // Saved in DB

    private long    numberOfLocations           = 0;                // Saved in DB
    private long    numberOfPlacemarks          = 0;                // Saved in DB

    private int     validMap                    = 1;                // Saved in DB
    // 1 = Map extents valid, OK generation of Thumb
    // 0 = Do not generate thumb (track crosses anti-meridian)

    private int     type                        = TRACK_TYPE_ND;    // Saved in DB

    // True if the card view is selected
    private boolean isSelected = false;

    // The altitude validator (the anti spikes filter):
    // - Max Acceleration = 12 m/s^2
    // - Stabilization time = 4 s
    private final SpikesChecker altitudeFilter = new SpikesChecker(12, 4);

    /**
     * Add a LocationExtended to the Track, and updates the Track statistics.
     *
     * @param location the location to be added to the Track
     */
    public void add(LocationExtended location) {
        if (numberOfLocations == 0) {
            // Init "Start" variables
            latitudeStart = location.getLocation().getLatitude();
            longitudeStart = location.getLocation().getLongitude();
            if (location.getLocation().hasAltitude()) {
                altitudeStart = location.getLocation().getAltitude();
            } else {
                altitudeStart = NOT_AVAILABLE;
            }
            egmAltitudeCorrectionStart = location.getAltitudeEGM96Correction();
            speedStart = location.getLocation().hasSpeed() ? location.getLocation().getSpeed() : NOT_AVAILABLE;
            accuracyStart = location.getLocation().hasAccuracy() ? location.getLocation().getAccuracy() : STANDARD_ACCURACY;
            timeStart = location.getLocation().getTime();

            latitudeLastStepDistance = latitudeStart;
            longitudeLastStepDistance = longitudeStart;
            accuracyLastStepDistance = accuracyStart;

            latitudeMax = latitudeStart;
            longitudeMax = longitudeStart;
            latitudeMin = latitudeStart;
            longitudeMin = longitudeStart;

            if (name.equals("")) {
                SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
                name = df2.format(timeStart);
            }

            timeLastFix = timeStart;
            timeEnd = timeStart;

            durationMoving = 0;
            duration = 0;
            distance = 0;
        }

        timeLastFix = timeEnd;

        latitudeEnd = location.getLocation().getLatitude();
        longitudeEnd = location.getLocation().getLongitude();
        if (location.getLocation().hasAltitude()) {
            altitudeEnd = location.getLocation().getAltitude();
        } else {
            altitudeEnd = NOT_AVAILABLE;
        }
        egmAltitudeCorrectionEnd = location.getAltitudeEGM96Correction();

        speedEnd = location.getLocation().hasSpeed() ? location.getLocation().getSpeed() : NOT_AVAILABLE;
        accuracyEnd = location.getLocation().hasAccuracy() ? location.getLocation().getAccuracy() : STANDARD_ACCURACY;
        timeEnd = location.getLocation().getTime();

        if (egmAltitudeCorrectionEnd == NOT_AVAILABLE) getEGMAltitudeCorrectionEnd();
        if (egmAltitudeCorrectionStart == NOT_AVAILABLE) getEGMAltitudeCorrectionStart();

        // ---------------------------------------------- Load the new value into antispikes filter
        if (altitudeEnd != NOT_AVAILABLE) altitudeFilter.load(timeEnd, altitudeEnd);

        // ------------------------------------------------------------- Coords for thumb and stats

        if (validMap != 0) {
            if (latitudeEnd > latitudeMax) latitudeMax = latitudeEnd;
            if (longitudeEnd > longitudeMax) longitudeMax = longitudeEnd;
            if (latitudeEnd < latitudeMin) latitudeMin = latitudeEnd;
            if (longitudeEnd < longitudeMin) longitudeMin = longitudeEnd;

            if (Math.abs(longitudeLastStepDistance - longitudeEnd) > 90) validMap = 0;
            //  YOU PASS FROM -180 TO +180, OR REVERSE. iN THE PACIFIC OCEAN.
            //  in that case the app doesn't generate the thumb map.
        }

        // ---------------------------------------------------------------------------------- Times

        duration = timeEnd - timeStart;
        if (speedEnd >= MOVEMENT_SPEED_THRESHOLD) durationMoving += timeEnd - timeLastFix;

        // --------------------------- Spaces (Distances) increment if distance > sum of accuracies

        // -- Temp locations for "DistanceTo"
        Location LastStepDistanceLoc = new Location("TEMP");
        LastStepDistanceLoc.setLatitude(latitudeLastStepDistance);
        LastStepDistanceLoc.setLongitude(longitudeLastStepDistance);

        Location EndLoc = new Location("TEMP");
        EndLoc.setLatitude(latitudeEnd);
        EndLoc.setLongitude(longitudeEnd);
        // -----------------------------------

        distanceInProgress = LastStepDistanceLoc.distanceTo(EndLoc);
        float DeltaDistancePlusAccuracy = distanceInProgress + accuracyEnd;

        if (DeltaDistancePlusAccuracy < distanceInProgress + accuracyEnd) {
            accuracyLastStepDistance = DeltaDistancePlusAccuracy;
            //Log.w("myApp", "[#] Track.java - LastStepDistance_Accuracy updated to " + LastStepDistance_Accuracy );
        }

        if (distanceInProgress > accuracyEnd + accuracyLastStepDistance) {
            distance += distanceInProgress;
            if (distanceLastAltitude != NOT_AVAILABLE) distanceLastAltitude += distanceInProgress;
            distanceInProgress = 0;

            latitudeLastStepDistance = latitudeEnd;
            longitudeLastStepDistance = longitudeEnd;
            accuracyLastStepDistance = accuracyEnd;
        }

        // Found a first fix with altitude!!
        if ((altitudeEnd != NOT_AVAILABLE) && (distanceLastAltitude == NOT_AVAILABLE)) {
            distanceLastAltitude = 0;
            altitudeUp = 0;
            altitudeDown = 0;
            if (altitudeStart == NOT_AVAILABLE) altitudeStart = altitudeEnd;
            altitudeLastStepAltitude = altitudeEnd;
            accuracyLastStepAltitude = accuracyEnd;
        }

        if ((altitudeLastStepAltitude != NOT_AVAILABLE) && (altitudeEnd != NOT_AVAILABLE)) {
            altitudeInProgress = altitudeEnd - altitudeLastStepAltitude;
            // Improve last step accuracy in case of new data elements:
            float DeltaAltitudePlusAccuracy = (float) Math.abs(altitudeInProgress) + accuracyEnd;
            if (DeltaAltitudePlusAccuracy <= accuracyLastStepAltitude) {
                accuracyLastStepAltitude = DeltaAltitudePlusAccuracy;
                distanceLastAltitude = 0;
                //Log.w("myApp", "[#] Track.java - LastStepAltitude_Accuracy updated to " + LastStepAltitude_Accuracy );
            }
            // Evaluate the altitude step convalidation:
            if ((Math.abs(altitudeInProgress) > MIN_ALTITUDE_STEP) && altitudeFilter.isValid()
                    && ((float) Math.abs(altitudeInProgress) > (SECURITY_COEFFICIENT * (accuracyLastStepAltitude + accuracyEnd)))) {
                // Altitude step:
                // increment distance only if the inclination is relevant (assume deltah=20m in max 5000m)
                if (distanceLastAltitude < 5000) {
                    float hypotenuse = (float) Math.sqrt((double) (distanceLastAltitude * distanceLastAltitude) + (altitudeInProgress * altitudeInProgress));
                    distance = distance + hypotenuse - distanceLastAltitude;
                    //Log.w("myApp", "[#] Track.java - Distance += " + (hypotenuse - DistanceLastAltitude));
                }
                //Reset variables
                altitudeLastStepAltitude = altitudeEnd;
                accuracyLastStepAltitude = accuracyEnd;
                distanceLastAltitude = 0;

                if (altitudeInProgress > 0) altitudeUp += altitudeInProgress;    // Increment the correct value of Altitude UP/DOWN
                else altitudeDown -= altitudeInProgress;
                altitudeInProgress = 0;
            }

        }

        // --------------------------------------------------------------------------------- Speeds

        if ((speedEnd != NOT_AVAILABLE) && (speedEnd > speedMax)) speedMax = speedEnd;
        if (duration > 0) speedAverage = (distance + distanceInProgress) / (((float) duration) / 1000f);
        if (durationMoving > 0) speedAverageMoving = (distance + distanceInProgress) / (((float) durationMoving) / 1000f);
        numberOfLocations++;
    }

    /**
     * Creates a void Track.
     */
    public Track(){
    }

    /**
     * Creates a Track with the specified name.
     *
     * @param name The name of the Track
     */
    public Track(String name){
        this.name = name;
    }

    /**
     * Creates a Track with the given data.
     * This method is used to load the Track from the Database.
     *
     * @param id The id of the Track
     * @param name The name of the track
     * @param from The description of the start point
     * @param to The description of the endpoint
     * @param latitudeStart The latitude of the start point
     * @param longitudeStart The longitude of the start point
     * @param altitudeStart The raw altitude of the start point (without any correction)
     * @param accuracyStart The accuracy of the start point
     * @param speedStart The speed of the start point
     * @param timeStart The time of the start point
     * @param timeLastFix The time of the last fix
     * @param latitudeEnd The latitude of the endpoint
     * @param longitudeEnd The longitude of the endpoint
     * @param altitudeEnd The raw altitude of the endpoint (without any correction)
     * @param accuracyEnd The accuracy of the endpoint
     * @param speedEnd The speed of the endpoint
     * @param timeEnd The time of the endpoint
     * @param latitudeLastStepDistance The latitude of the point stored as last step for distance calculation
     * @param longitudeLastStepDistance The longitude of the point stored as last step for distance calculation
     * @param accuracyLastStepDistance The accuracy of the point stored as last step for distance calculation
     * @param altitudeLastStepAltitude The altitude of the point stored as last step for altitude
     * @param accuracyLastStepAltitude The accuracy of the point stored as last step for altitude
     * @param latitudeMin The minimum latitude reached
     * @param longitudeMin The minimum longitude reached
     * @param latitudeMax The maximum latitude reached
     * @param longitudeMax The maximum longitude reached
     * @param duration The duration of the Track
     * @param durationMoving The time in movement of the Track
     * @param distance The distance of the Track
     * @param distanceInProgress The part of the distance of the Track not yet validated
     * @param distanceLastAltitude The distance walked since the last step of altitude
     * @param altitudeUp The total ascending
     * @param altitudeDown The total descending
     * @param altitudeInProgress The altitude gap since the last altitude step
     * @param speedMax The maximum speed reached
     * @param speedAverage The average speed based on total time
     * @param speedAverageMoving The average speed based on the time in movement
     * @param numberOfLocations The number of Locations recorded
     * @param numberOfPlacemarks The number of Placemarks recorded
     * @param validMap 1 if the map should be drawn
     * @param type The type of activity done during the Track recording
     * @param description The description of the Track
     */
    public void fromDB(long id, String name, String from, String to,
                       double latitudeStart, double longitudeStart, double altitudeStart,
                       float accuracyStart, float speedStart, long timeStart, long timeLastFix,
                       double latitudeEnd, double longitudeEnd, double altitudeEnd,
                       float accuracyEnd, float speedEnd, long timeEnd,
                       double latitudeLastStepDistance, double longitudeLastStepDistance, float accuracyLastStepDistance,
                       double altitudeLastStepAltitude, float accuracyLastStepAltitude,
                       double latitudeMin, double longitudeMin,
                       double latitudeMax, double longitudeMax,
                       long duration, long durationMoving, float distance, float distanceInProgress,
                       long distanceLastAltitude, double altitudeUp, double altitudeDown,
                       double altitudeInProgress, float speedMax, float speedAverage,
                       float speedAverageMoving, long numberOfLocations, long numberOfPlacemarks,
                       int validMap, int type, String description) {
        this.id = id;
        this.name = name;
        this.description = description;

        this.latitudeStart = latitudeStart;
        this.longitudeStart = longitudeStart;
        this.altitudeStart = altitudeStart;
        this.accuracyStart = accuracyStart;
        this.speedStart = speedStart;
        this.timeStart = timeStart;

        this.timeLastFix = timeLastFix;

        this.latitudeEnd = latitudeEnd;
        this.longitudeEnd = longitudeEnd;
        this.altitudeEnd = altitudeEnd;
        this.accuracyEnd = accuracyEnd;
        this.speedEnd = speedEnd;
        this.timeEnd = timeEnd;

        this.latitudeLastStepDistance = latitudeLastStepDistance;
        this.longitudeLastStepDistance = longitudeLastStepDistance;
        this.accuracyLastStepDistance = accuracyLastStepDistance;

        this.altitudeLastStepAltitude = altitudeLastStepAltitude;
        this.accuracyLastStepAltitude = accuracyLastStepAltitude;

        this.latitudeMin = latitudeMin;
        this.longitudeMin = longitudeMin;

        this.latitudeMax = latitudeMax;
        this.longitudeMax = longitudeMax;

        this.duration = duration;
        this.durationMoving = durationMoving;

        this.distance = distance;
        this.distanceInProgress = distanceInProgress;
        this.distanceLastAltitude = distanceLastAltitude;

        this.altitudeUp = altitudeUp;
        this.altitudeDown = altitudeDown;
        this.altitudeInProgress = altitudeInProgress;

        this.speedMax = speedMax;
        this.speedAverage = speedAverage;
        this.speedAverageMoving = speedAverageMoving;

        this.numberOfLocations = numberOfLocations;
        this.numberOfPlacemarks = numberOfPlacemarks;

        this.validMap = validMap;
        this.type = type;

        EGM96 egm96 = EGM96.getInstance();
        if (egm96 != null) {
            if (egm96.isLoaded()) {
                if (latitudeStart != NOT_AVAILABLE) egmAltitudeCorrectionStart = egm96.getEGMCorrection(latitudeStart, longitudeStart);
                if (latitudeEnd != NOT_AVAILABLE) egmAltitudeCorrectionEnd = egm96.getEGMCorrection(latitudeEnd, longitudeEnd);
            }
        }
    }

    // ------------------------------------------------------------------------ Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitudeStart() {
        return latitudeStart;
    }

    public double getLongitudeStart() {
        return longitudeStart;
    }

    public double getAltitudeStart() {
        return altitudeStart;
    }

    public double getEGMAltitudeCorrectionStart() {
        if (egmAltitudeCorrectionStart == NOT_AVAILABLE) {
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isLoaded()) {
                    if (latitudeStart != NOT_AVAILABLE)
                        egmAltitudeCorrectionStart = egm96.getEGMCorrection(latitudeStart, longitudeStart);
                }
            }
        }
        return egmAltitudeCorrectionStart;
    }

    public float getAccuracyStart() {
        return accuracyStart;
    }

    public float getSpeedStart() {
        return speedStart;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public long getTimeLastFix() {
        return timeLastFix;
    }

    public double getLatitudeEnd() {
        return latitudeEnd;
    }

    public double getLongitudeEnd() {
        return longitudeEnd;
    }

    public double getAltitudeEnd() {
        return altitudeEnd;
    }

    public double getEGMAltitudeCorrectionEnd() {
        if (egmAltitudeCorrectionEnd == NOT_AVAILABLE) {
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isLoaded()) {
                    if (latitudeEnd != NOT_AVAILABLE)
                        egmAltitudeCorrectionEnd = egm96.getEGMCorrection(latitudeEnd, longitudeEnd);
                }
            }
        }
        return egmAltitudeCorrectionEnd;
    }

    public float getAccuracyEnd() {
        return accuracyEnd;
    }

    public float getSpeedEnd() {
        return speedEnd;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public double getLatitudeLastStepDistance() {
        return latitudeLastStepDistance;
    }

    public double getLongitudeLastStepDistance() {
        return longitudeLastStepDistance;
    }

    public float getAccuracyLastStepDistance() {
        return accuracyLastStepDistance;
    }

    public double getAltitudeLastStepAltitude() {
        return altitudeLastStepAltitude;
    }

    public float getAccuracyLastStepAltitude() {
        return accuracyLastStepAltitude;
    }

    public double getLatitudeMin() {
        return latitudeMin;
    }

    public double getLongitudeMin() {
        return longitudeMin;
    }

    public double getLatitudeMax() {
        return latitudeMax;
    }

    public double getLongitudeMax() {
        return longitudeMax;
    }

    public long getDuration() {
        return duration;
    }

    public long getDurationMoving() {
        return durationMoving;
    }

    public float getDistance() {
        return distance;
    }

    public float getDistanceInProgress() {
        return distanceInProgress;
    }

    public long getDistanceLastAltitude() {
        return distanceLastAltitude;
    }

    public double getAltitudeUp() {
        return altitudeUp;
    }

    public double getAltitudeDown() {
        return altitudeDown;
    }

    public double getAltitudeInProgress() {
        return altitudeInProgress;
    }

    public float getSpeedMax() {
        return speedMax;
    }

    public float getSpeedAverage() {
        return speedAverage;
    }

    public float getSpeedAverageMoving() {
        return speedAverageMoving;
    }

    public long getNumberOfLocations() {
        return numberOfLocations;
    }

    public long getNumberOfPlacemarks() {
        return numberOfPlacemarks;
    }

    public int getValidMap() {
        return validMap;
    }

    public int getType() {
        return type;
    }

    public void setType(int type){
        this.type = type;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * @return true if the altitude is valid. false when in the middle of a spike.
     */
    public boolean isValidAltitude() {
        return altitudeFilter.isValid();
    }

    /**
     * Notifies that a Placemark has been added to the Track into the Database
     *
     * @return the number of Placemarks on the Track.
     */
    public long addPlacemark(LocationExtended location) {
        this.numberOfPlacemarks++ ;
        // If the Track name has not yet been set, sets it now.
        // This means that this Placemark is the first item added to the track.
        if (name.equals("")) {
            SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
            name = df2.format(location.getLocation().getTime());
        }
        return numberOfPlacemarks;
    }

    /**
     * @return the total distance, including the in-progress part.
     */
    public float getEstimatedDistance(){
        if (numberOfLocations == 0) return NOT_AVAILABLE;
        if (numberOfLocations == 1) return 0;
        return distance + distanceInProgress;
    }

    /**
     * Returns the estimated ascending altitude.
     *
     * @param egmCorrection if true, it estimates the altitude using also the EGM Correction.
     * @return the estimated ascending altitude.
     */
    public double getEstimatedAltitudeUp(boolean egmCorrection){
        // Retrieve EGM Corrections if available
        if ((egmAltitudeCorrectionStart == NOT_AVAILABLE) || (egmAltitudeCorrectionEnd == NOT_AVAILABLE)) {
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isLoaded()) {
                    if (latitudeStart != NOT_AVAILABLE) egmAltitudeCorrectionStart = egm96.getEGMCorrection(latitudeStart, longitudeStart);
                    if (latitudeEnd != NOT_AVAILABLE) egmAltitudeCorrectionEnd = egm96.getEGMCorrection(latitudeEnd, longitudeEnd);
                }
            }
        }
        double egmcorr = 0;
        if ((egmCorrection) && ((egmAltitudeCorrectionStart != NOT_AVAILABLE) && (egmAltitudeCorrectionEnd != NOT_AVAILABLE))) {
            egmcorr = egmAltitudeCorrectionStart - egmAltitudeCorrectionEnd;
        }
        double dresultUp = altitudeInProgress > 0 ? altitudeUp + altitudeInProgress : altitudeUp;
        dresultUp -= egmcorr < 0 ? egmcorr : 0;
        double dresultDown = altitudeInProgress < 0 ? altitudeDown - altitudeInProgress : altitudeDown;
        dresultDown -= egmcorr > 0 ? egmcorr : 0;

        if (dresultUp < 0) {
            dresultDown -= dresultUp;
            dresultUp = 0;
        }
        if (dresultDown < 0) {
            dresultUp -= dresultDown;
            //dresultDown = 0;
        }
        return dresultUp;
    }

    /**
     * Returns the estimated descending altitude.
     *
     * @param egmCorrection if true, it estimates the altitude using also the EGM Correction.
     * @return the estimated descending altitude.
     */
    public double getEstimatedAltitudeDown(boolean egmCorrection){
        // Retrieve EGM Corrections if available
        if ((egmAltitudeCorrectionStart == NOT_AVAILABLE) || (egmAltitudeCorrectionEnd == NOT_AVAILABLE)) {
            EGM96 egm96 = EGM96.getInstance();
            if (egm96 != null) {
                if (egm96.isLoaded()) {
                    if (latitudeStart != NOT_AVAILABLE) egmAltitudeCorrectionStart = egm96.getEGMCorrection(latitudeStart, longitudeStart);
                    if (latitudeEnd != NOT_AVAILABLE) egmAltitudeCorrectionEnd = egm96.getEGMCorrection(latitudeEnd, longitudeEnd);
                }
            }
        }
        double egmcorr = 0;
        if ((egmCorrection) && ((egmAltitudeCorrectionStart != NOT_AVAILABLE) && (egmAltitudeCorrectionEnd != NOT_AVAILABLE))) {
            egmcorr = egmAltitudeCorrectionStart - egmAltitudeCorrectionEnd;
        }
        double dresultUp = altitudeInProgress > 0 ? altitudeUp + altitudeInProgress : altitudeUp;
        dresultUp -= egmcorr < 0 ? egmcorr : 0;
        double dresultDown = altitudeInProgress < 0 ? altitudeDown - altitudeInProgress : altitudeDown;
        dresultDown -= egmcorr > 0 ? egmcorr : 0;

        if (dresultUp < 0) {
            dresultDown -= dresultUp;
            dresultUp = 0;
        }
        if (dresultDown < 0) {
            //dresultUp -= dresultDown;
            dresultDown = 0;
        }
        return dresultDown;
    }

    /**
     * Returns the estimated gap of altitude.
     * The Altitude Gap is the difference between the current altitude and the
     * altitude of the start point.
     *
     * @param egmCorrection if true, it estimates the altitude using also the EGM Correction.
     * @return the estimated altitude gap.
     */
    public double getEstimatedAltitudeGap(boolean egmCorrection){
        return getEstimatedAltitudeUp(egmCorrection) - getEstimatedAltitudeDown(egmCorrection);
    }

    /**
     * @return the overall direction of the Track.
     */
    public float getBearing() {
        if (latitudeEnd != NOT_AVAILABLE) {
            if (((latitudeStart == latitudeEnd) && (longitudeStart == longitudeEnd)) || (distance == 0))
                return NOT_AVAILABLE;
            Location endLoc = new Location("TEMP");
            endLoc.setLatitude(latitudeEnd);
            endLoc.setLongitude(longitudeEnd);
            Location startLoc = new Location("TEMP");
            startLoc.setLatitude(latitudeStart);
            startLoc.setLongitude(longitudeStart);
            float bTo = startLoc.bearingTo(endLoc);
            if (bTo < 0) bTo += 360f;
            return bTo;
        }
        return NOT_AVAILABLE;
    }

    /**
     * @return the time, based on preferences (Total or Moving).
     */
    public long getPrefTime() {
        GPSApplication gpsApp = GPSApplication.getInstance();
        int pTime = gpsApp.getPrefShowTrackStatsType();
        switch (pTime) {
            case 0:         // Total based
                return duration;
            case 1:         // Moving based
                return durationMoving;
            default:
                return duration;
        }
    }

    /**
     * @return the average speed, based on preferences (Total or Moving)
     */
    public float getPrefSpeedAverage() {
        if (numberOfLocations == 0) return NOT_AVAILABLE;
        GPSApplication gpsApp = GPSApplication.getInstance();
        int pTime = gpsApp.getPrefShowTrackStatsType();
        switch (pTime) {
            case 0:         // Total based
                return speedAverage;
            case 1:         // Moving based
                return speedAverageMoving;
            default:
                return speedAverage;
        }
    }

    /**
     * @return the track Type. If not set, it returns an estimation of the activity Type, basing on Track's data.
     */
    public int getEstimatedTrackType() {
        if (type != TRACK_TYPE_ND) return type;
        if ((distance == NOT_AVAILABLE) || (speedMax == NOT_AVAILABLE)) {
            if (numberOfPlacemarks == 0) return TRACK_TYPE_ND;
            else return TRACK_TYPE_STEADY;
        }
        if ((distance < 15.0f) || (speedMax == 0.0f) || (speedAverageMoving == NOT_AVAILABLE)) return TRACK_TYPE_STEADY;
        if (speedMax < (7.0f / 3.6f)) {
            if ((altitudeUp != NOT_AVAILABLE) && (altitudeDown != NOT_AVAILABLE))
                if ((altitudeDown + altitudeUp > (0.1f * distance)) && (distance > 500.0f)) return TRACK_TYPE_MOUNTAIN;
            else return TRACK_TYPE_WALK;
        }
        if (speedMax < (15.0f / 3.6f)) {
            if (speedAverageMoving > 8.0f / 3.6f) return TRACK_TYPE_RUN;
            else {
                if ((altitudeUp != NOT_AVAILABLE) && (altitudeDown != NOT_AVAILABLE))
                    if ((altitudeDown + altitudeUp > (0.1f * distance)) && (distance > 500.0f)) return TRACK_TYPE_MOUNTAIN;
                else return TRACK_TYPE_WALK;
            }
        }
        if (speedMax < (50.0f / 3.6f)) {
            if ((speedAverageMoving + speedMax) / 2 > 35.0f / 3.6f) return TRACK_TYPE_CAR;
            if ((speedAverageMoving + speedMax) / 2 > 20.0f / 3.6)  return TRACK_TYPE_BICYCLE;
            else if ((speedAverageMoving + speedMax) / 2 > 12.0f / 3.6f) return TRACK_TYPE_RUN;
            else {
                if ((altitudeUp != NOT_AVAILABLE) && (altitudeDown != NOT_AVAILABLE))
                    if ((altitudeDown + altitudeUp > (0.1f * distance)) && (distance > 500.0f))
                        return TRACK_TYPE_MOUNTAIN;
                    else return TRACK_TYPE_WALK;
            }
            /*
            if (SpeedAverageMoving > 20.0f / 3.6f) return TRACK_TYPE_CAR;
            if (SpeedAverageMoving > 12.0f / 3.6) return TRACK_TYPE_BICYCLE;
            else if (SpeedAverageMoving > 8.0f / 3.6f) return TRACK_TYPE_RUN;
            else {
                if ((Altitude_Up != NOT_AVAILABLE) && (Altitude_Down != NOT_AVAILABLE))
                    if ((Altitude_Down + Altitude_Up > (0.1f * Distance)) && (Distance > 500.0f))
                        return TRACK_TYPE_MOUNTAIN;
                else return TRACK_TYPE_WALK;
            }*/
        }
        if ((altitudeUp != NOT_AVAILABLE) && (altitudeDown != NOT_AVAILABLE))
            if ((altitudeDown + altitudeUp > 5000.0) && (speedMax > 300.0f / 3.6f)) return TRACK_TYPE_FLIGHT;
        return TRACK_TYPE_CAR;
    }
}
