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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
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

    private TrackAdapter adapter;
    private List<Track> data = Collections.synchronizedList(new ArrayList<Track>());

    private View view;
    private TextView TVTracklistEmpty;


    public FragmentTracklist() {
        // Required empty public constructor
    }

    private boolean FileExists(String filename) {
        File file = new File(filename);
        return file.exists ();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_tracklist, container, false);

        TVTracklistEmpty    = view.findViewById(R.id.id_textView_TracklistEmpty);
        recyclerView        = view.findViewById(R.id.my_recycler_view);

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.getItemAnimator().setChangeDuration(0);
        adapter = new TrackAdapter(data);

        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Night mode is not active, we're in day time
                adapter.isLightTheme = true;
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // Night mode is active, we're at night!
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // We don't know what mode we're in, assume notnight
                adapter.isLightTheme = false;
                break;
        }

        recyclerView.setAdapter(adapter);

        return view;
    }

    public boolean CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.w("myApp", "[#] FragmentTracklist.java - WRITE_EXTERNAL_STORAGE = Permission GRANTED");
            return true;    // Permission Granted
        } else {
            Log.w("myApp", "[#] FragmentTracklist.java - WRITE_EXTERNAL_STORAGE = Permission DENIED");
            List<String> listPermissionsNeeded = new ArrayList<>();
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
            ActivityCompat.requestPermissions(getActivity(), listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]) , REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] FragmentTracklist.java - EventBus: FragmentTracklist already registered");
            EventBus.getDefault().unregister(this);
        }

        EventBus.getDefault().register(this);
        Update();
    }


    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }


    @Subscribe
    public void onEvent(final EventBusMSGNormal msg) {
        switch (msg.MSGType) {
            case EventBusMSG.TRACKLIST_SELECT:
            case EventBusMSG.TRACKLIST_DESELECT:
                int i = 0;
                boolean found = false;
                synchronized (data) {
                    do {
                        if (data.get(i).getId() == msg.id) {
                            found = true;
                            data.get(i).setSelected(msg.MSGType == EventBusMSG.TRACKLIST_SELECT);
                        }
                        i++;
                    } while ((i < data.size()) && !found);
                }
        }
    }


    @Subscribe
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_TRACK) {
            if (!data.isEmpty() && GPSApplication.getInstance().isCurrentTrackVisible()) {
                final Track trk = GPSApplication.getInstance().getCurrentTrack();
                synchronized (data) {
                    if (data.get(0).getId() == trk.getId()) {
                        //data.set(0, trk);
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
            return;
        }
        if (msg == EventBusMSG.REFRESH_TRACKLIST) {
            try {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (NullPointerException e) {
                //Log.w("myApp", "[#] FragmentTracklist.java - Unable to manage UI");
            }
            return;
        }
        if (msg == EventBusMSG.NOTIFY_TRACKS_DELETED) {
            DeleteSomeTracks();
            return;
        }
        if (msg == EventBusMSG.UPDATE_TRACKLIST) {
            Update();
            return;
        }
        if (msg == EventBusMSG.ACTION_BULK_SHARE_TRACKS) {
            GPSApplication.getInstance().LoadJob(GPSApplication.JOB_TYPE_SHARE);
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                CheckStoragePermission();   // Ask for storage permission
            } else GPSApplication.getInstance().ExecuteJob();
            GPSApplication.getInstance().DeselectAllTracks();
            return;
        }
        if (msg == EventBusMSG.ACTION_BULK_VIEW_TRACKS) {
            final ArrayList<AppInfo> ail = new ArrayList<>(GPSApplication.getInstance().getExternalViewerChecker().getAppInfoList());

            if (!ail.isEmpty()) {
                if (ail.size() == 1) {
                    // 1 Viewer installed, let's use it
                    GPSApplication.getInstance().setTrackViewer(ail.get(0));
                    OpenTrack();
                }
                else {
                    // 2 or more viewers installed
                    // Search the Default Track Viewer selected on Preferences

                    String pn = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("prefTracksViewer", "");
                    boolean foundDefault = false;
                    for (AppInfo ai : ail) {
                        if (ai.PackageName.equals(pn)) {
                            // Default Viewer available!
                            GPSApplication.getInstance().setTrackViewer(ai);
                            foundDefault = true;
                        }
                    }
                    if (!foundDefault) {
                        // The default Track Viewer hasn't been found
                        final Dialog dialog = new Dialog(getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        View view = getLayoutInflater().inflate(R.layout.appdialog_list, null);
                        ListView lv = (ListView) view.findViewById(R.id.id_appdialog_list);

                        AppDialogList clad = new AppDialogList(getActivity(), ail);

                        lv.setAdapter(clad);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                GPSApplication.getInstance().setTrackViewer(ail.get(position));
                                OpenTrack();
                                dialog.dismiss();
                            }
                        });
                        dialog.setContentView(view);
                        dialog.show();
                    } else {
                        // Default Track Viewer found! Let's use it.
                        OpenTrack();
                    }
                }
            }
            return;
        }
        if (msg == EventBusMSG.ACTION_BULK_EXPORT_TRACKS) {
            GPSApplication.getInstance().LoadJob(GPSApplication.JOB_TYPE_EXPORT);
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                CheckStoragePermission();   // Ask for storage permission
            } else GPSApplication.getInstance().ExecuteJob();
            GPSApplication.getInstance().DeselectAllTracks();
            return;
        }
        if (msg == EventBusMSG.ACTION_BULK_DELETE_TRACKS) {
            final ArrayList<Track> selectedTracks = GPSApplication.getInstance().getSelectedTracks();

            // Check if exist at least one exported file:
            boolean fileexist = false;
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                for (Track track : selectedTracks) {
                    fileexist |= FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".kml")
                            || FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".gpx")
                            || FileExists(Environment.getExternalStorageDirectory() + "/GPSLogger/" + track.getName() + ".txt");
                }
            }
            if (fileexist) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.StyledDialog));
                builder.setMessage(getResources().getString(R.string.card_message_delete_also_exported));
                builder.setIcon(android.R.drawable.ic_menu_info_details);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        GPSApplication.getInstance().setDeleteAlsoExportedFiles(true); // Delete also exported files
                        GPSApplication.getInstance().LoadJob(GPSApplication.JOB_TYPE_DELETE);
                        GPSApplication.getInstance().ExecuteJob();
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        GPSApplication.getInstance().setDeleteAlsoExportedFiles(false); // Don't delete exported files
                        GPSApplication.getInstance().LoadJob(GPSApplication.JOB_TYPE_DELETE);
                        GPSApplication.getInstance().ExecuteJob();
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
                        dialog.dismiss();
                        GPSApplication.getInstance().setDeleteAlsoExportedFiles(false); // Don't delete exported files
                        GPSApplication.getInstance().LoadJob(GPSApplication.JOB_TYPE_DELETE);
                        GPSApplication.getInstance().ExecuteJob();
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
            return;
        }
        if (msg == EventBusMSG.INTENT_SEND) {
            final List<ExportingTask> selectedTracks = GPSApplication.getInstance().getExportingTaskList(); // The list of shared tracks
            ArrayList<Uri> files = new ArrayList<>();                                               // The list of URI to be attached to intent
            File file;

            StringBuilder extraSubject = new StringBuilder(getString(R.string.app_name) + " - ");
            StringBuilder extraText = new StringBuilder();
            int i = 0;                                                                              // A service counter for string building

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType("text/xml");

            for (ExportingTask ET : selectedTracks) {

                Track track = GPSApplication.getInstance().GPSDataBase.getTrack(ET.getId());
                if (track == null) return;

                if (i > 0) {
                    extraSubject.append(" + ");
                    extraText.append("\n\n----------------------------\n");
                }
                extraSubject.append(track.getName());

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
                    extraText.append(getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName()
                            + "\n" + track.getNumberOfLocations() + " " + getString(R.string.trackpoints)
                            + "\n" + track.getNumberOfPlacemarks() + " " + getString(R.string.placemarks));
                } else {
                    extraText.append(getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName()
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
                            + "\n" + getString(R.string.pref_track_stats) + ": " + getString(R.string.pref_track_stats_totaltime) + " | " + getString(R.string.pref_track_stats_movingtime));
                }

                String fname = track.getName() + ".kml";
                file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", fname);
                if (file.exists () && GPSApplication.getInstance().getPrefExportKML()) {
                    Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                    files.add(uri);
                }
                fname = track.getName() + ".gpx";
                file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", fname);
                if (file.exists ()  && GPSApplication.getInstance().getPrefExportGPX()) {
                    Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                    files.add(uri);
                }
                fname = track.getName() + ".txt";
                file = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/", fname);
                if (file.exists ()  && GPSApplication.getInstance().getPrefExportTXT()) {
                    Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                    files.add(uri);
                }
                i++;
            }
            intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject.toString());
            intent.putExtra(Intent.EXTRA_TEXT, (CharSequence) (extraText.toString()));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            // Grant Read and Write permissions for all the app that handle the intent for all files of the list
            // it must be done manually because it is the compat version of FileProvider
            List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                for (Uri U : files) {
                    GPSApplication.getInstance().getApplicationContext().grantUriPermission(packageName, U, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            // Create intent to show chooser
            Intent chooser = Intent.createChooser(intent, getString(R.string.card_menu_share));
            // Verify the intent will resolve to at least one activity
            try {
                if ((intent.resolveActivity(getContext().getPackageManager()) != null) && (!files.isEmpty())) {
                    startActivity(chooser);
                }
            } catch (NullPointerException e) {
                //Log.w("myApp", "[#] FragmentTracklist.java - Unable to start the Activity");
            }
        }
    }


    public void OpenTrack() {
        GPSApplication.getInstance().LoadJob(GPSApplication.JOB_TYPE_VIEW);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            CheckStoragePermission();   // Ask for storage permission
        } else GPSApplication.getInstance().ExecuteJob();
        GPSApplication.getInstance().DeselectAllTracks();
    }


    public void Update() {
        if (isAdded()) {
            Log.w("myApp", "[#] FragmentTracklist.java - Updating Tracklist");
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
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TVTracklistEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                            adapter.notifyDataSetChanged();
                        }
                    });
                } catch (NullPointerException e) {
                    //Log.w("myApp", "[#] FragmentTracklist.java - Unable to manage UI");
                }
            }
        }
    }


    public void DeleteSomeTracks() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<Track> TI = GPSApplication.getInstance().getTrackList();
                    synchronized (data) {
                        for (int i = data.size() - 1; i >= 0; i--) {
                            if (!TI.contains(data.get(i))) {
                                data.remove(i);
                                adapter.notifyItemRemoved(i);
                            }
                        }
                        TVTracklistEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        } catch (NullPointerException e) {
            //Log.w("myApp", "[#] FragmentTracklist.java - Unable to manage UI");
            Update();
        }
    }
}
