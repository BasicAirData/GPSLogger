/*
 * FragmentTracklist - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 19/6/2016
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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FragmentTracklist extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    private static RecyclerView.Adapter adapter;
    private static ArrayList<Track> data;

    private View view;
    private TextView TVTracklistEmpty;
    private long selectedtrackID = -1;

    public FragmentTracklist() {
        // Required empty public constructor
    }

    private void DeleteFile(String filename) {
        File file = new File(filename);
        if (file.exists ()) file.delete();
    }

    private boolean FileExists(String filename) {
        File file = new File(filename);
        return file.exists ();
    }

    private boolean isPackageInstalled(String uri) {
        PackageManager pm = getContext().getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }


    public void setProgress(int listPosition, int progress) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(listPosition);
        if (holder != null) {
            ((TrackAdapter.TrackHolder)holder).SetProgress(progress);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        view = inflater.inflate(R.layout.fragment_tracklist, container, false);
        TVTracklistEmpty = (TextView) view.findViewById(R.id.id_textView_TracklistEmpty);
        recyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.getItemAnimator().setChangeDuration(0);
        data = new ArrayList<>();
        adapter = new TrackAdapter(data);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        GPSApplication gpsApplication = GPSApplication.getInstance();
        final boolean expTXT = gpsApplication.getPrefExportTXT();
        final boolean expGPX = gpsApplication.getPrefExportGPX();
        final boolean expKML = gpsApplication.getPrefExportKML();
        final long OpenInViewer = gpsApplication.getOpenInViewer();
        final long Share = gpsApplication.getShare();
        //PackageManager pm = getContext().getPackageManager();
        //menu.setHeaderTitle("Track " + data.get(selectedtrackID).getName());
        if ((expGPX || expKML || expTXT) && (Share == -1)) {
            int i = 0;
            for (Track T : data) {
                if (T.getId() == selectedtrackID) {
                    final int ii = i;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyItemChanged(ii);
                        }
                    });

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "GPS Logger - Track " + T.getName());

                    intent.putExtra(Intent.EXTRA_TEXT, (CharSequence) ("GPS Logger"));
                    intent.setType("text/xml");

                    //String title = getString(R.string.card_menu_share);
                    // Create intent to show chooser
                    //Intent chooser = Intent.createChooser(intent, title);

                    // Verify the intent will resolve to at least one activity
                    if ((intent.resolveActivity(getContext().getPackageManager()) != null)) {
                        menu.add(0, v.getId(), 0, R.string.card_menu_share);
                    }
                }
                i++;
            }
        }
        // TODO: Finish implementation of "View in..."
        //if ((isPackageInstalled("com.google.earth") && (OpenInViewer == -1))) menu.add(0, v.getId(), 0, R.string.card_menu_view);
        if (OpenInViewer == -1){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType("application/vnd.google-earth.kml+xml");
            // Find default app
            ResolveInfo ri = getContext().getPackageManager().resolveActivity(intent, 0);
            if (ri != null) {
                //Log.w("myApp", "[#] FragmentTracklist.java - Open with: " + ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
                List<ResolveInfo> lri = getContext().getPackageManager().queryIntentActivities(intent, 0);
                //Log.w("myApp", "[#] FragmentTracklist.java - Found " + lri.size() + " viewers:");
                boolean founddefaultapp = false;
                for (ResolveInfo tmpri : lri) {
                    //Log.w("myApp", "[#] " + ri.activityInfo.applicationInfo.packageName + " - " + tmpri.activityInfo.applicationInfo.packageName);
                    if (ri.activityInfo.applicationInfo.packageName.equals(tmpri.activityInfo.applicationInfo.packageName)) {
                        founddefaultapp = true;
                        //Log.w("myApp", "[#]                              DEFAULT --> " + tmpri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
                    } //else Log.w("myApp", "[#]                                          " + tmpri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager()));
                }
                menu.add(0, 100, 0, founddefaultapp ? getResources().getString(R.string.card_menu_view, ri.activityInfo.applicationInfo.loadLabel(getContext().getPackageManager())) :
                        getResources().getString(R.string.card_menu_view_selector));
            }
        }

        if (expGPX || expKML || expTXT) menu.add(0, v.getId(), 0, R.string.card_menu_export);
        menu.add(0, v.getId(), 0, R.string.card_menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(getResources().getString(R.string.card_menu_delete))) {
            if (!data.isEmpty() && (selectedtrackID >= 0)) {
                int i = 0;
                boolean found = false;
                do {
                    if (data.get(i).getId() == selectedtrackID) {
                        final int ii = i;
                        found = true;

                        boolean fileexist = FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + data.get(i).getName() +".kml")
                                         || FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + data.get(i).getName() +".gpx")
                                         || FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + data.get(i).getName() +".txt");

                        if (fileexist) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.StyledDialog));
                            builder.setMessage(getResources().getString(R.string.card_message_delete_also_exported));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String name = data.get(ii).getName();
                                    String nameID = String.valueOf(data.get(ii).getId());

                                    EventBus.getDefault().post("DELETE_TRACK " + data.get(ii).getId());
                                    data.remove(ii);
                                    // Delete exported files
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/" + name + ".txt");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/" + name + ".kml");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/" + name + ".gpx");
                                    // Delete track files
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".txt");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".kml");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".gpx");
                                    DeleteFile(getContext().getFilesDir() + "/Thumbnails/" + nameID + ".png");

                                    dialog.dismiss();
                                }
                            });
                            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String name = data.get(ii).getName();
                                    String nameID = String.valueOf(data.get(ii).getId());

                                    EventBus.getDefault().post("DELETE_TRACK " + data.get(ii).getId());
                                    data.remove(ii);
                                    // Delete track files
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".txt");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".kml");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".gpx");
                                    DeleteFile(getContext().getFilesDir() + "/Thumbnails/" + nameID + ".png");

                                    dialog.dismiss();
                                }
                            });
                            builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.StyledDialog));
                            builder.setMessage(getResources().getString(R.string.card_message_delete_confirmation));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String name = data.get(ii).getName();
                                    String nameID = String.valueOf(data.get(ii).getId());

                                    EventBus.getDefault().post("DELETE_TRACK " + data.get(ii).getId());
                                    data.remove(ii);
                                    // Delete track files
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".txt");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".kml");
                                    DeleteFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/" + name + ".gpx");
                                    DeleteFile(getContext().getFilesDir() + "/Thumbnails/" + nameID + ".png");

                                    dialog.dismiss();
                                }
                            });
                            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    i++;
                } while ((i < data.size()) && !found);
            }
        } else if (item.getTitle().equals(getResources().getString(R.string.card_menu_export))) {
            if (!data.isEmpty() && (selectedtrackID >= 0)) {
                int i = 0;
                boolean found = false;
                do {
                    if (data.get(i).getId() == selectedtrackID) {
                        found = true;
                        EventBus.getDefault().post("EXPORT_TRACK " + data.get(i).getId());
                    }
                    i++;
                } while ((i < data.size()) && !found);
            }
        } else if (item.getItemId() == 100) {
            // TODO: Set ID in xml !!!!!!
            if (!data.isEmpty() && (selectedtrackID >= 0)) {
                int i = 0;
                boolean found = false;
                do {
                    if (data.get(i).getId() == selectedtrackID) {
                        found = true;
                        EventBus.getDefault().post("VIEW_TRACK " + data.get(i).getId());
                    }
                    i++;
                } while ((i < data.size()) && !found);
            }
        } else if (item.getTitle().equals(getResources().getString(R.string.card_menu_share))) {
            if (!data.isEmpty() && (selectedtrackID >= 0)) {
                int i = 0;
                boolean found = false;
                do {
                    if (data.get(i).getId() == selectedtrackID) {
                        found = true;
                        EventBus.getDefault().post("SHARE_TRACK " + data.get(i).getId());
                    }
                    i++;
                } while ((i < data.size()) && !found);
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        Update();
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onEvent(String msg) {
        if (msg.equals("UPDATE_TRACKLIST")) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Update();
                }
            });
        }
        if (msg.contains("TRACKLIST_SELECTION")) {
            final int selID = Integer.valueOf(msg.split(" ")[1]);
            if (selID >= 0) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        selectedtrackID = selID;
                        registerForContextMenu(view);
                        getActivity().openContextMenu(view);
                        unregisterForContextMenu(view);
                    }
                });
            }
        }
        if (msg.contains("TRACK_SETPROGRESS")) {
            final long trackid = Long.valueOf(msg.split(" ")[1]);
            final int progress = Integer.valueOf(msg.split(" ")[2]);
            if ((trackid > 0) && (progress >= 0)) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        for (Track T : data) {
                            if (T.getId() == trackid) {
                                setProgress(i, T.getProgress());
                                //adapter.notifyItemChanged(i);
                            }
                            i++;
                        }
                    }
                });
            }
        }
        if (msg.contains("INTENT_SEND")) {
            final long trackid = Long.valueOf(msg.split(" ")[1]);
            if (trackid > 0) {
                int i = 0;
                for (Track track : data) {
                    if (track.getId() == trackid) {
                        final int ii = i;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyItemChanged(ii);
                            }
                        });

                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Intent.EXTRA_SUBJECT, "GPS Logger - Track " + track.getName());

                        PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
                        PhysicalData phdDuration;
                        PhysicalData phdSpeedMax;
                        PhysicalData phdSpeedAvg;
                        PhysicalData phdDistance;
                        PhysicalData phdAltitudeGap;
                        PhysicalData phdOverallDirection;
                        phdDuration = phdformatter.format(track.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                        phdSpeedMax = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                        phdSpeedAvg = phdformatter.format(track.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                        phdDistance = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                        phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                        phdOverallDirection = phdformatter.format(track.getBearing(),PhysicalDataFormatter.FORMAT_BEARING);

                        intent.putExtra(Intent.EXTRA_TEXT, (CharSequence) ("GPS Logger - Track " + track.getName()
                                + "\n" + track.getNumberOfLocations() + " " + getString(R.string.trackpoints)
                                + "\n" + track.getNumberOfPlacemarks() + " " + getString(R.string.placemarks)
                                + "\n"
                                + "\n" + getString(R.string.pref_track_stats) + " " + (GPSApplication.getInstance().getPrefShowTrackStatsType() == 0 ? getString(R.string.pref_track_stats_totaltime) : getString(R.string.pref_track_stats_movingtime)) + ":"
                                + "\n" + getString(R.string.distance) + " = " + phdDistance.Value + " " + phdDistance.UM
                                + "\n" + getString(R.string.duration) + " = " + phdDuration.Value
                                + "\n" + getString(R.string.altitude_gap) + " = " + phdAltitudeGap.Value + " " + phdAltitudeGap.UM
                                + "\n" + getString(R.string.max_speed) + " = " + phdSpeedMax.Value + " " + phdSpeedMax.UM
                                + "\n" + getString(R.string.average_speed) + " = " + phdSpeedAvg.Value + " " + phdSpeedAvg.UM
                                + "\n" + getString(R.string.overall_direction) + " = " + phdOverallDirection.Value + " " + phdOverallDirection.UM));
                        intent.setType("text/xml");

                        ArrayList<Uri> files = new ArrayList<>();
                        String fname = track.getName() + ".kml";
                        File file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", fname);
                        if (file.exists () && GPSApplication.getInstance().getPrefExportKML()) {
                            Uri uri = Uri.fromFile(file);
                            files.add(uri);
                        }
                        fname = track.getName() + ".gpx";
                        file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", fname);
                        if (file.exists ()  && GPSApplication.getInstance().getPrefExportGPX()) {
                            Uri uri = Uri.fromFile(file);
                            files.add(uri);
                        }
                        fname = track.getName() + ".txt";
                        file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", fname);
                        if (file.exists ()  && GPSApplication.getInstance().getPrefExportTXT()) {
                            Uri uri = Uri.fromFile(file);
                            files.add(uri);
                        }

                        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);

                        String title = getString(R.string.card_menu_share);
                        // Create intent to show chooser
                        Intent chooser = Intent.createChooser(intent, title);

                        // Verify the intent will resolve to at least one activity
                        if ((intent.resolveActivity(getContext().getPackageManager()) != null) && (!files.isEmpty())) {
                            startActivity(chooser);
                        }
                    }
                    i++;
                }
            }
        }
    }

    public void Update() {
        if (isAdded()) {
            final GPSApplication GlobalVariables = (GPSApplication) getActivity().getApplicationContext();
            final List<Track> TI = GlobalVariables.getTrackList();
            if (data != null) data.clear();
            if (!TI.isEmpty()) {
                TVTracklistEmpty.setVisibility(View.GONE);
                data.addAll(TI);
            } else {
                TVTracklistEmpty.setVisibility(View.VISIBLE);
            }
            adapter.notifyDataSetChanged();
        }
    }
}
