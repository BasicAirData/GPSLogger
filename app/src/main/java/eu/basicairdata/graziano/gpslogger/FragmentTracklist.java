/**
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

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FragmentTracklist extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    private RecyclerView.Adapter adapter;
    private List<Track> data = Collections.synchronizedList(new ArrayList<Track>());

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
        adapter = new TrackAdapter(data);
        recyclerView.setAdapter(adapter);
        return view;
    }

    public boolean CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return true;    // Permission Granted
        else {
            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (showRationale || !GPSApplication.getInstance().isStoragePermissionChecked()) {
                List<String> listPermissionsNeeded = new ArrayList<>();
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
                ActivityCompat.requestPermissions(getActivity(), listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]) , REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
            return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.card_menu, menu);

        final GPSApplication gpsApplication = GPSApplication.getInstance();

        //menu.setHeaderTitle("Track " + data.get(selectedtrackID).getName());
        //Log.w("myApp", "[#] FragmentTracklist.java - share + export");
        if (gpsApplication.getPrefExportGPX() || gpsApplication.getPrefExportKML() || gpsApplication.getPrefExportTXT()) {
            menu.findItem(R.id.cardmenu_export).setVisible(true);   // menu export
            if ((gpsApplication.getShare() == -1) && gpsApplication.isContextMenuShareVisible())
                menu.findItem(R.id.cardmenu_share).setVisible(true);   // menu share
        }
        //Log.w("myApp", "[#] FragmentTracklist.java - view");
        if ((gpsApplication.getOpenInViewer() == -1) && gpsApplication.isContextMenuViewVisible()) {                                    // menu view
            if (!gpsApplication.getViewInApp().equals(""))
                menu.findItem(R.id.cardmenu_view).setTitle(getResources().getString(R.string.card_menu_view, gpsApplication.getViewInApp())).setVisible(true);
            else menu.findItem(R.id.cardmenu_view).setVisible(true);
        }
        //Log.w("myApp", "[#] FragmentTracklist.java - delete");
        if (selectedtrackID == gpsApplication.getCurrentTrack().getId()) menu.findItem(R.id.cardmenu_delete).setVisible(false);

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (!shouldShowRationale && gpsApplication.isStoragePermissionChecked()) {
                menu.findItem(R.id.cardmenu_share).setEnabled(false);
                menu.findItem(R.id.cardmenu_view).setEnabled(false);
                menu.findItem(R.id.cardmenu_export).setEnabled(false);
            }
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.cardmenu_delete:
                if (!data.isEmpty() && (selectedtrackID >= 0)) {
                    final Track track = GPSApplication.getInstance().GPSDataBase.getTrack(selectedtrackID);
                    if (track != null) {
                        boolean fileexist = FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".kml")
                                || FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".gpx")
                                || FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".txt");

                        if (fileexist) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.StyledDialog));
                            builder.setMessage(getResources().getString(R.string.card_message_delete_also_exported));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String name = track.getName();
                                    String nameID = String.valueOf(track.getId());

                                    EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.DELETE_TRACK, track.getId()));

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int i = 0;
                                            boolean found = false;
                                            synchronized (data) {
                                                do {
                                                    if (data.get(i).getId() == selectedtrackID) {
                                                        found = true;
                                                        data.remove(i);
                                                        adapter.notifyItemRemoved(i);
                                                        if (data.isEmpty())
                                                            TVTracklistEmpty.setVisibility(View.VISIBLE);
                                                    }
                                                    i++;
                                                } while ((i < data.size()) && !found);
                                            }
                                        }
                                    });


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
                                    String name = track.getName();
                                    String nameID = String.valueOf(track.getId());

                                    EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.DELETE_TRACK, track.getId()));

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int i = 0;
                                            boolean found = false;
                                            synchronized (data) {
                                                do {
                                                    if (data.get(i).getId() == selectedtrackID) {
                                                        found = true;
                                                        data.remove(i);
                                                        adapter.notifyItemRemoved(i);
                                                        if (data.isEmpty())
                                                            TVTracklistEmpty.setVisibility(View.VISIBLE);
                                                    }
                                                    i++;
                                                } while ((i < data.size()) && !found);
                                            }
                                        }
                                    });

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
                                    String name = track.getName();
                                    String nameID = String.valueOf(track.getId());

                                    EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.DELETE_TRACK, track.getId()));

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int i = 0;
                                            boolean found = false;
                                            synchronized (data) {
                                                do {
                                                    if (data.get(i).getId() == selectedtrackID) {
                                                        found = true;
                                                        data.remove(i);
                                                        adapter.notifyItemRemoved(i);
                                                        if (data.isEmpty())
                                                            TVTracklistEmpty.setVisibility(View.VISIBLE);
                                                    }
                                                    i++;
                                                } while ((i < data.size()) && !found);
                                            }
                                        }
                                    });

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
                    } else Update();    // Update the Tracklist!!
                }
                break;
            case R.id.cardmenu_export:
                if (!data.isEmpty() && (selectedtrackID >= 0)) {
                    int i = 0;
                    boolean found = false;
                    synchronized(data) {
                        do {
                            if (data.get(i).getId() == selectedtrackID) {
                                found = true;
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    // Store the message to send in case the storage permission will be granted
                                    GPSApplication.getInstance().setDoIfGrantStoragePermission(new EventBusMSGNormal(EventBusMSG.EXPORT_TRACK, data.get(i).getId()));
                                    // Ask for storage permission
                                    CheckStoragePermission();
                                }
                                else EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.EXPORT_TRACK, data.get(i).getId()));
                            }
                            i++;
                        } while ((i < data.size()) && !found);
                    }
                }
                break;
            case R.id.cardmenu_view:
                if (!data.isEmpty() && (selectedtrackID >= 0)) {
                    int i = 0;
                    boolean found = false;
                    synchronized(data) {
                        do {
                            if (data.get(i).getId() == selectedtrackID) {
                                found = true;
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    // Store the message to send in case the storage permission will be granted
                                    GPSApplication.getInstance().setDoIfGrantStoragePermission(new EventBusMSGNormal(EventBusMSG.VIEW_TRACK, data.get(i).getId()));
                                    // Ask for storage permission
                                    CheckStoragePermission();
                                } else EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.VIEW_TRACK, data.get(i).getId()));
                            }
                            i++;
                        } while ((i < data.size()) && !found);
                    }
                }
                break;
            case R.id.cardmenu_share:
                if (!data.isEmpty() && (selectedtrackID >= 0)) {
                    int i = 0;
                    boolean found = false;
                    synchronized(data) {
                        do {
                            if (data.get(i).getId() == selectedtrackID) {
                                found = true;
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    // Store the message to send in case the storage permission will be granted
                                    GPSApplication.getInstance().setDoIfGrantStoragePermission(new EventBusMSGNormal(EventBusMSG.SHARE_TRACK, data.get(i).getId()));
                                    // Ask for storage permission
                                    CheckStoragePermission();
                                } else EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.SHARE_TRACK, data.get(i).getId()));
                            }
                            i++;
                        } while ((i < data.size()) && !found);
                    }
                }
                break;
            default:
                //TODO: selectedtrackID = -1;
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
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_TRACK) {
            if (!data.isEmpty() && GPSApplication.getInstance().isCurrentTrackVisible()) {
                final Track trk = GPSApplication.getInstance().getCurrentTrack();
                synchronized (data) {
                    if (data.get(0).getId() == trk.getId()) {
                        data.set(0, trk);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Update Current Track Card Statistics
                                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(0);
                                if (holder != null) {
                                    ((TrackAdapter.TrackHolder) holder).UpdateTrackStats(data.get(0));
                                }
                            }
                        });
                    }
                }
            }
        }
        if (msg == EventBusMSG.UPDATE_TRACKLIST) {
            Update();
        }
    }


    @Subscribe
    public void onEvent(EventBusMSGNormal msg) {
        if (msg.MSGType == EventBusMSG.TRACKLIST_SELECTION) {
            final long selID = msg.id;
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
        if (msg.MSGType == EventBusMSG.INTENT_SEND) {
            final long trackid = msg.id;
            if (trackid > 0) {
                Track track = GPSApplication.getInstance().GPSDataBase.getTrack(trackid);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName());

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
                phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                phdOverallDirection = phdformatter.format(track.getBearing(),PhysicalDataFormatter.FORMAT_BEARING);
                if (track.getNumberOfLocations() <= 1) {
                    intent.putExtra(Intent.EXTRA_TEXT, (CharSequence) (getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName()
                            + "\n" + track.getNumberOfLocations() + " " + getString(R.string.trackpoints)
                            + "\n" + track.getNumberOfPlacemarks() + " " + getString(R.string.placemarks)));
                } else {
                    intent.putExtra(Intent.EXTRA_TEXT, (CharSequence) (getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName()
                            + "\n" + track.getNumberOfLocations() + " " + getString(R.string.trackpoints)
                            + "\n" + track.getNumberOfPlacemarks() + " " + getString(R.string.placemarks)
                            + "\n"
                            + "\n" + getString(R.string.distance) + " = " + phdDistance.Value + " " + phdDistance.UM
                            + "\n" + getString(R.string.duration) + " = " + phdDuration.Value + " | " + phdDurationMoving.Value
                            + "\n" + getString(R.string.altitude_gap) + " = " + phdAltitudeGap.Value + " " + phdAltitudeGap.UM
                            + "\n" + getString(R.string.max_speed) + " = " + phdSpeedMax.Value + " " + phdSpeedMax.UM
                            + "\n" + getString(R.string.average_speed) + " = " + phdSpeedAvg.Value + " | " + phdSpeedAvgMoving.Value + " " + phdSpeedAvg.UM
                            + "\n" + getString(R.string.overall_direction) + " = " + phdOverallDirection.Value + " " + phdOverallDirection.UM
                            + "\n"
                            + "\n" + getString(R.string.pref_track_stats) + ": " + getString(R.string.pref_track_stats_totaltime) + " | " + getString(R.string.pref_track_stats_movingtime)));
                }
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
        }
    }


    @Subscribe
    public void onEvent(EventBusMSGLong msg) {
        if (msg.MSGType == EventBusMSG.TRACK_SETPROGRESS) {
            final long trackid = msg.id;
            final long progress = msg.Value;
            if ((trackid > 0) && (progress >= 0)) {
                int i = 0;
                synchronized(data) {
                    for (Track T : data) {
                        if (T.getId() == trackid) setProgress(i, T.getProgress());
                        i++;
                    }
                }
            }
        }
    }


    public void Update() {
        if (isAdded()) {
            //Log.w("myApp", "[#] FragmentTracklist.java - Updating Tracklist");
            final List<Track> TI = GPSApplication.getInstance().getTrackList();
            //Log.w("myApp", "[#] FragmentTracklist.java - The element 0 has id = " + TI.get(0).getId());
            synchronized(data) {
                if (data != null) data.clear();
                if (!TI.isEmpty()) {
                    data.addAll(TI);
                    if (data.get(0).getId() == GPSApplication.getInstance().getCurrentTrack().getId()) {
                        GPSApplication.getInstance().setisCurrentTrackVisible(true);
                        //Log.w("myApp", "[#] FragmentTracklist.java - current track, VISIBLE into the tracklist ("
                        //    + GPSApplication.getInstance().getCurrentTrack().getId() + ")");
                    } else {
                        GPSApplication.getInstance().setisCurrentTrackVisible(false);
                        //Log.w("myApp", "[#] FragmentTracklist.java - current track empty, NOT VISIBLE into the tracklist");
                    }
                } else {
                    GPSApplication.getInstance().setisCurrentTrackVisible(false);
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TVTracklistEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }
}
