/*
 * EGM96 - Singleton Java Class for Android
 * Created by G.Capelli (BasicAirData) on 24/4/2016
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


import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class EGM96 {

    // ---------------------------------------------------------------------------- Singleton Class
    private static EGM96 instance = new EGM96();

    private EGM96(){}

    public static EGM96 getInstance(){
        return instance;
    }

    // ------------------------------------------------------------------------ Listeners interface
    // Not used
    /*
    public interface EGM96Listener {
        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        public void onEGMGridLoaded(boolean isConnected);
    }

    // This variable represents the listener passed in by the owning object
    // The listener must implement the events interface and passes messages up to the parent.
    private EGM96Listener listener;

    // Assign the listener implementing events interface that will receive the events
    public void setBluetoothHelperListener(EGM96Listener listener) {
        this.listener = listener;
    }
    */


    private int BOUNDARY = 3; // The grid extensions (in each of the 4 sides) of the real 721 x 1440 grid
    private short[][] EGMGrid = new short[BOUNDARY + 1440 + BOUNDARY][BOUNDARY + 721 + BOUNDARY];
    private boolean isEGMGridLoaded = false;
    private boolean isEGMGridLoading = false;
    private String EGMFileName;

    public double EGM96_VALUE_INVALID = -100000;

    public void LoadGridFromFile(String FileName) {
        if (!isEGMGridLoaded && !isEGMGridLoading) {
            isEGMGridLoading = true;
            EGMFileName = FileName;
            new Thread(new LoadEGM96Grid()).start();
        } else {
            if (isEGMGridLoading) Log.w("myApp", "[#] EGM96.java - Grid is already loading, please wait");
            if (isEGMGridLoaded) Log.w("myApp", "[#] EGM96.java - Grid already loaded");
        }
    }

    public void UnloadGrid() {
        isEGMGridLoaded = false;
        isEGMGridLoading = false;
        //listener.onEGMGridLoaded(isEGMGridLoaded);
        EventBus.getDefault().post("UPDATE_FIX");
        EventBus.getDefault().post("UPDATE_TRACK");
        EventBus.getDefault().post("UPDATE_TRACKLIST");
    }

    public boolean isEGMGridLoaded() {
        return isEGMGridLoaded;
    }

    public boolean isEGMGridLoading() {
        return isEGMGridLoading;
    }

    public double getEGMCorrection(double Latitude, double Longitude) {
    // This function calculates and return the EGM96 altitude correction value of the input coords, in m;
    // Imput coords are: -90 < Latitude < 90; -180 < Longitude < 360 (android range -180 < Longitude < 180);
        if (isEGMGridLoaded) {
            double Lat = 90.0 - Latitude;
            double Lon = Longitude;
            if (Lon < 0) Lon += 360.0;

            int ilon = (int) (Lon / 0.25) + BOUNDARY;
            int ilat = (int) (Lat / 0.25) + BOUNDARY;

            // Creating points for interpolation
            short hc11 = EGMGrid[ilon][ilat];
            short hc12 = EGMGrid[ilon][ilat + 1];
            short hc21 = EGMGrid[ilon + 1][ilat];
            short hc22 = EGMGrid[ilon + 1][ilat + 1];

            // Interpolation:
            // Latitude
            double hc1 = hc11 + (hc12 - hc11) * (Lat % 0.25) / 0.25;
            double hc2 = hc21 + (hc22 - hc21) * (Lat % 0.25) / 0.25;
            // Longitude
            double hc = (hc1 + (hc2 - hc1) * (Lon % 0.25) / 0.25) / 100;

            //Log.w("myApp", "[#] EGM96.java - getEGMCorrection(" + Latitude + ", " + Longitude + ") = " + hc);

            return hc;
        }
        else return EGM96_VALUE_INVALID;
    }



    // The Thread that loads the grid in background ------------------------------------------------

    class LoadEGM96Grid implements Runnable {
        // Thread: Load EGM grid

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            Log.w("myApp", "[#] EGM96.java - Start loading grid");
            File file = new File(EGMFileName);
            Log.w("myApp", "[#] EGM96.java - From file: " + file.getAbsolutePath());
            if (file.exists() && (file.length() == 2076480)) {
                Log.w("myApp", "[#] EGM96.java - Valid file");
                FileInputStream fin;
                try {
                    fin = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    isEGMGridLoaded = false;
                    isEGMGridLoading = false;
                    Log.w("myApp", "[#] EGM96.java - FileNotFoundException");
                    //Toast.makeText(getApplicationContext(), "Oops", Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    return;
                }
                BufferedInputStream bin = new BufferedInputStream(fin);
                DataInputStream din = new DataInputStream(bin);
                int i;
                int i_lon = BOUNDARY;
                int i_lat = BOUNDARY;
                int count = (int) ((file.length() / 2));

                for (i = 0; (i < count); i++) {
                    try {
                        EGMGrid[i_lon][i_lat] = din.readShort();
                        i_lon++;
                        if (i_lon >= (1440 + BOUNDARY)) {
                            i_lat++;
                            i_lon = BOUNDARY;
                        }
                    } catch (IOException e) {
                        isEGMGridLoaded = false;
                        isEGMGridLoading = false;
                        Log.w("myApp", "[#] EGM96.java - IOException");
                        return;
                        //Toast.makeText(getApplicationContext(), "Oops", Toast.LENGTH_SHORT).show();
                        //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

                if (BOUNDARY > 0) {
                    // Fill boundaries with correct data, in order to speed up retrieving for interpolation;
                    // fill left + right boundaries
                    //Log.w("myApp", "[#] EGM96.java - LR BOUNDARIES");
                    for (int ix = 0; (ix < BOUNDARY); ix++) {
                        for (int iy = BOUNDARY; (iy < BOUNDARY + 721); iy++) {
                            EGMGrid[ix][iy] = EGMGrid[ix + 1440][iy];
                            EGMGrid[BOUNDARY + ix + 1440][iy] = EGMGrid[BOUNDARY + ix][iy];
                        }
                    }
                    // fill top + bottom boundaries
                    //Log.w("myApp", "[#] EGM96.java - TOP DOWN BOUNDARIES");
                    for (int iy = 0; (iy < BOUNDARY); iy++) {
                        for (int ix = 0; (ix < BOUNDARY + 1440 + BOUNDARY); ix++) {
                            if (ix > 720) {
                                EGMGrid[ix][iy] = EGMGrid[ix - 720][BOUNDARY + BOUNDARY - iy];
                                EGMGrid[ix][BOUNDARY + iy + 721] = EGMGrid[ix - 720][BOUNDARY + 721-2 - iy];
                            }
                            else {
                                EGMGrid[ix][iy] = EGMGrid[ix + 720][BOUNDARY + BOUNDARY - iy];
                                EGMGrid[ix][BOUNDARY + iy + 721] = EGMGrid[ix + 720][BOUNDARY + 721-2 - iy];
                            }
                        }
                    }
                }

                isEGMGridLoading = false;
                isEGMGridLoaded = true;
                Log.w("myApp", "[#] EGM96.java - Grid Successfully Loaded");
                //Toast.makeText(getApplicationContext(), "EGM96 correction grid loaded", Toast.LENGTH_SHORT).show();

            } else {
                isEGMGridLoading = false;
                isEGMGridLoaded = false;
                if (!file.exists()) { Log.w("myApp", "[#] EGM96.java - File not found"); }
                if (!file.canRead()) { Log.w("myApp", "[#] EGM96.java - Cannot read file"); }
                if (file.length() != 2076480) {Log.w("myApp", "[#] EGM96.java - File has invalid length: " + file.length());}
                //Toast.makeText(getApplicationContext(), "EGM96 correction not available", Toast.LENGTH_SHORT).show();
            }
            EventBus.getDefault().post("UPDATE_FIX");
            EventBus.getDefault().post("UPDATE_TRACK");
            EventBus.getDefault().post("UPDATE_TRACKLIST");
            //listener.onEGMGridLoaded(isEGMGridLoaded);
        }
    }
}
