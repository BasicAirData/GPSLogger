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

import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_KML;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_GPX;


public class ExternalViewerChecker {

    private final Context context;

    private ArrayList<AppInfo> appInfoList = new ArrayList<>();

    static private class CustomComparator implements Comparator<AppInfo> {
        @Override
        public int compare(AppInfo o1, AppInfo o2) {
            return o1.label.compareTo(o2.label);
        }
    }

    private class FileType {
        FileType (ArrayList<String> _packages, String _mimeType, String _fileType) {
            this.packages = _packages;
            this.fileType = _fileType;
            this.mimeType = _mimeType;
        }
        ArrayList<String> packages;
        String mimeType;
        String fileType;
    }

    private ArrayList<FileType> fileTypeList = new ArrayList<>();

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

        fileTypeList = new ArrayList<>();
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
            intent.setType(ft.mimeType);
            //Log.w("myApp", "[#] ExternalViewerChecker.java - " + ft.mimeType);
            //Log.w("myApp", "[#] GPSApplication.java - Open with: " + ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
            List<ResolveInfo> KMLlri = pm.queryIntentActivities(intent, 0);
            //Log.w("myApp", "[#] ExternalViewerChecker.java - Found " + KMLlri.size() + " viewers:");
            for (ResolveInfo tmpri : KMLlri) {
                boolean isPackageInList = false;
                if (ft.packages != null) {
                    for (String s : ft.packages) {
                        if (s.equals(tmpri.activityInfo.applicationInfo.packageName)) {
                            isPackageInList = true;
                            break;
                        }
                    }
                } else isPackageInList = true;

                if (isPackageInList) {
                    AppInfo ainfo = new AppInfo();
                    ainfo.packageName = tmpri.activityInfo.applicationInfo.packageName;
                    ainfo.label = tmpri.activityInfo.applicationInfo.loadLabel(pm).toString();

                    boolean found = false;

                    for (AppInfo a : appInfoList) {
                        if (a.label.equals(ainfo.label) && a.packageName.equals(ainfo.packageName)) {
                            found = true;
                            //Log.w("myApp", "[#] ExternalViewerChecker.java -   " + tmpri.activityInfo.applicationInfo.packageName);
                            break;
                        }
                    }
                    if (!found) {
                        ainfo.mimeType = ft.mimeType;
                        ainfo.fileType = ft.fileType;
                        ainfo.icon = tmpri.activityInfo.applicationInfo.loadIcon(pm);
                        appInfoList.add(ainfo);
                        //Log.w("myApp", "[#] ExternalViewerChecker.java - + " + tmpri.activityInfo.applicationInfo.packageName);
                    }
                }
            }
        }

        // Sort List by Package Name
        Collections.sort(appInfoList, Comparator);

        // Apply Exceptions
        for (AppInfo a : appInfoList) {
            if (a.packageName.equals("at.xylem.mapin")) {
                // MAPinr is not opening GPX correctly!
                a.fileType = FILETYPE_KML;
                a.mimeType = "application/vnd.google-earth.kml+xml";
            }
            if (a.packageName.equals("com.google.earth")) {
                // Google Earth opens file with fileProvider only
                a.requiresFileProvider = true;
            }
        }
    }
}
