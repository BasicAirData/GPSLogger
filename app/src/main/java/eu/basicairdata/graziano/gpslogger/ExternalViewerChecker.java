/**
 * ExternalViewerChecker - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 12/7/2020
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ExternalViewerChecker {

    private final Context context;

    private ArrayList<AppInfo> appInfoList = new ArrayList<>();

    static private class CustomComparator implements Comparator<AppInfo> {
        @Override
        public int compare(AppInfo o1, AppInfo o2) {
            return o1.Label.compareTo(o2.Label);
        }
    }


    public ArrayList<AppInfo> getAppInfoList() {
        return appInfoList;
    }

    private final CustomComparator Comparator = new CustomComparator();


    public ExternalViewerChecker(Context context) {
        this.context = context;
    }


    public int size() {
        return appInfoList.size();
    }


    public boolean isEmpty() {
        return (appInfoList.isEmpty());
    }


    public void makeAppInfoList() {
        final PackageManager pm = context.getPackageManager();

        appInfoList = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.setType("application/gpx+xml");  // GPX

        //Log.w("myApp", "[#] GPSApplication.java - Open with: " + ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
        List<ResolveInfo> GPXlri = pm.queryIntentActivities(intent, 0);
        //Log.w("myApp", "[#] GPSApplication.java - Found " + KMLlri.size() + " viewers:");
        for (ResolveInfo tmpri : GPXlri) {
            //Log.w("myApp", "[#] " + ri.activityInfo.applicationInfo.packageName + " - " + tmpri.activityInfo.applicationInfo.packageName);
            AppInfo ainfo = new AppInfo();
            ainfo.PackageName = tmpri.activityInfo.applicationInfo.packageName;
            ainfo.Label = tmpri.activityInfo.applicationInfo.loadLabel(pm).toString();
            ainfo.Icon =  tmpri.activityInfo.applicationInfo.loadIcon(pm);
            ainfo.KML = false;
            ainfo.GPX = true;

            appInfoList.add(ainfo);
        }

        intent.setType("application/vnd.google-earth.kml+xml");     // KML

        //Log.w("myApp", "[#] GPSApplication.java - Open with: " + ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
        List<ResolveInfo> KMLlri = pm.queryIntentActivities(intent, 0);
        //Log.w("myApp", "[#] GPSApplication.java - Found " + KMLlri.size() + " viewers:");
        for (ResolveInfo tmpri : KMLlri) {
            //Log.w("myApp", "[#] " + ri.activityInfo.applicationInfo.packageName + " - " + tmpri.activityInfo.applicationInfo.packageName);
            AppInfo ainfo = new AppInfo();
            ainfo.PackageName = tmpri.activityInfo.applicationInfo.packageName;
            ainfo.Label = tmpri.activityInfo.applicationInfo.loadLabel(pm).toString();
            ainfo.Icon =  tmpri.activityInfo.applicationInfo.loadIcon(pm);
            ainfo.KML = true;
            ainfo.GPX = false;

            boolean found = false;

            for (AppInfo a : appInfoList) {
                if (a.Label.equals(ainfo.Label) && a.PackageName.equals(ainfo.PackageName)) {
                    found = true;
                    //a.KML = true;
                    break;
                }
            }
            if (!found) {
                appInfoList.add(ainfo);
            }
        }

        // Sort List by Package Name
        if (appInfoList.size() > 1) {
            Collections.sort(appInfoList, Comparator);
        }

        // Apply Exceptions
        if (appInfoList.size() > 0) {
            for (AppInfo a : appInfoList) {
                if (a.PackageName.equals("at.xylem.mapin")) {
                    // MAPinr is not opening GPX correctly!
                    a.GPX = false;
                    a.KML = true;
                }
                if (a.PackageName.equals("com.google.earth")) {
                    // Google Earth opens file with fileProvider only
                    a.requiresFileProvider = true;
                }
            }
        }
    }
}
