/*
 * FragmentTracklist - Java Class for Android
 * Created by G.Capelli on 19/6/2016
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * The Fragment that displays and manages the list of the archived Tracks
 * on the third tab (Tracklist) of the main Activity (GPSActivity).
 */
public class FragmentTracklist extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    private TrackAdapter adapter;
    private final List<Track> data = Collections.synchronizedList(new ArrayList<Track>());
    private View view;
    private TextView tvTracklistEmpty;

    public FragmentTracklist() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_tracklist, container, false);

        tvTracklistEmpty = view.findViewById(R.id.id_textView_TracklistEmpty);
        recyclerView = view.findViewById(R.id.my_recycler_view);
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
        update();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    /**
     * The EventBus receiver for Normal Messages.
     */
    @Subscribe
    public void onEvent(final EventBusMSGNormal msg) {
        int i = 0;
        boolean found = false;
        switch (msg.eventBusMSG) {
            case EventBusMSG.TRACKLIST_SELECT:
            case EventBusMSG.TRACKLIST_DESELECT:
                synchronized (data) {
                    do {
                        if (data.get(i).getId() == msg.trackID) {
                            found = true;
                            data.get(i).setSelected(msg.eventBusMSG == EventBusMSG.TRACKLIST_SELECT);
                        }
                        i++;
                    } while ((i < data.size()) && !found);
                }
                break;
            case EventBusMSG.TRACKLIST_RANGE_SELECTION:
                if (GPSApplication.getInstance().getLastClickId() != NOT_AVAILABLE) {
                    synchronized (data) {
                        do {
                            if (data.get(i).getId() == GPSApplication.getInstance().getLastClickId()) {
                                data.get(i).setSelected(GPSApplication.getInstance().getLastClickState());
                                found = !found;
                            }
                            if (data.get(i).getId() == msg.trackID) {
                                data.get(i).setSelected(GPSApplication.getInstance().getLastClickState());
                                found = !found;
                            }
                            if (found) {
                                // into the range
                                data.get(i).setSelected(GPSApplication.getInstance().getLastClickState());
                            }
                            i++;
                        } while (i < data.size());
                    }
                    EventBus.getDefault().post(EventBusMSG.UPDATE_TRACKLIST);
                }
        }
    }

    /**
     * The EventBus receiver for Short Messages.
     */
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
            deleteSomeTracks();
            return;
        }
        if (msg == EventBusMSG.UPDATE_TRACKLIST) {
            update();
            return;
        }
        if (msg == EventBusMSG.ACTION_BULK_SHARE_TRACKS) {
            GPSApplication.getInstance().loadJob(GPSApplication.JOB_TYPE_SHARE);
            GPSApplication.getInstance().executeJob();
            GPSApplication.getInstance().deselectAllTracks();
            return;
        }
        if (msg == EventBusMSG.ACTION_EDIT_TRACK) {
            for (Track T : GPSApplication.getInstance().getTrackList()) {
                if (T.isSelected()) {
                    GPSApplication.getInstance().setTrackToEdit(T);
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentTrackPropertiesDialog tpDialog = new FragmentTrackPropertiesDialog();
                    tpDialog.setTitleResource(R.string.card_menu_edit);
                    tpDialog.setFinalizeTrackWithOk(false);
                    tpDialog.show(fm, "");
                    break;
                }
            }
        }
        if (msg == EventBusMSG.ACTION_BULK_VIEW_TRACKS) {
            final ArrayList<ExternalViewer> evList = new ArrayList<>(GPSApplication.getInstance().getExternalViewerChecker().getExternalViewersList());

            if (!evList.isEmpty()) {
                if (evList.size() == 1) {
                    // 1 Viewer installed, let's use it
                    GPSApplication.getInstance().setTrackViewer(evList.get(0));
                    openTrack();
                }
                else {
                    // 2 or more viewers installed
                    // Search the Default Track Viewer selected on Preferences

                    String pn = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("prefTracksViewer", "");
                    boolean foundDefault = false;
                    for (ExternalViewer ev : evList) {
                        if (ev.packageName.equals(pn)) {
                            // Default Viewer available!
                            GPSApplication.getInstance().setTrackViewer(ev);
                            foundDefault = true;
                        }
                    }
                    if (!foundDefault) {
                        // The default Track Viewer hasn't been found
                        final Dialog dialog = new Dialog(getActivity());
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        View view = getLayoutInflater().inflate(R.layout.appdialog_list, null);
                        ListView lv = (ListView) view.findViewById(R.id.id_appdialog_list);

                        ExternalViewerAdapter clad = new ExternalViewerAdapter(getActivity(), evList);

                        lv.setAdapter(clad);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                GPSApplication.getInstance().setTrackViewer(evList.get(position));
                                openTrack();
                                dialog.dismiss();
                            }
                        });
                        dialog.setContentView(view);
                        dialog.show();
                    } else {
                        // Default Track Viewer found! Let's use it.
                        openTrack();
                    }
                }
            }
            return;
        }
        if (msg == EventBusMSG.ACTION_BULK_DELETE_TRACKS) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getResources().getString(R.string.card_message_delete_confirmation));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    GPSApplication.getInstance().loadJob(GPSApplication.JOB_TYPE_DELETE);
                    GPSApplication.getInstance().executeJob();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
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

                Track track = GPSApplication.getInstance().gpsDataBase.getTrack(ET.getId());
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
                phdDurationMoving = phdformatter.format(track.getDurationMoving(),PhysicalDataFormatter.FORMAT_DURATION);
                phdSpeedMax = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                phdSpeedAvg = phdformatter.format(track.getSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                phdSpeedAvgMoving = phdformatter.format(track.getSpeedAverageMoving(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                phdDistance = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                phdOverallDirection = phdformatter.format(track.getBearing(),PhysicalDataFormatter.FORMAT_BEARING);
                if (track.getNumberOfLocations() <= 1) {
                    extraText.append(getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName()
                            + (track.getDescription().isEmpty() ? "\n" + track.getDescription() + "\n" : "")
                            + "\n" + track.getNumberOfLocations() + " " + getString(R.string.trackpoints)
                            + "\n" + track.getNumberOfPlacemarks() + " " + getString(R.string.annotations));
                } else {
                    extraText.append(getString(R.string.app_name) + " - " + getString(R.string.tab_track) + " " + track.getName()
                            + (!track.getDescription().isEmpty() ? "\n" + track.getDescription() : "")
                            + "\n" + track.getNumberOfLocations() + " " + getString(R.string.trackpoints)
                            + "\n" + track.getNumberOfPlacemarks() + " " + getString(R.string.annotations)
                            + "\n"
                            + "\n" + getString(R.string.distance) + " = " + phdDistance.value + " " + phdDistance.um
                            + "\n" + getString(R.string.duration) + " = " + phdDuration.value + " | " + phdDurationMoving.value
                            + "\n" + getString(R.string.altitude_gap) + " = " + phdAltitudeGap.value + " " + phdAltitudeGap.um
                            + "\n" + getString(R.string.max_speed) + " = " + phdSpeedMax.value + " " + phdSpeedMax.um
                            + "\n" + getString(R.string.average_speed) + " = " + phdSpeedAvg.value + " | " + phdSpeedAvgMoving.value + " " + phdSpeedAvg.um
                            + "\n" + getString(R.string.overall_direction) + " = " + phdOverallDirection.value + " " + phdOverallDirection.um
                            + "\n"
                            + "\n" + getString(R.string.pref_track_stats) + ": " + getString(R.string.pref_track_stats_totaltime) + " | " + getString(R.string.pref_track_stats_movingtime));
                }

                String fname = GPSApplication.getInstance().getFileName(track) + ".kml";
                file = new File(GPSApplication.DIRECTORY_TEMP + "/", fname);
                if (file.exists () && GPSApplication.getInstance().getPrefExportKML()) {
                    Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                    files.add(uri);
                }
                fname = GPSApplication.getInstance().getFileName(track) + ".gpx";
                file = new File(GPSApplication.DIRECTORY_TEMP + "/", fname);
                if (file.exists ()  && GPSApplication.getInstance().getPrefExportGPX()) {
                    Uri uri = FileProvider.getUriForFile(GPSApplication.getInstance(), "eu.basicairdata.graziano.gpslogger.fileprovider", file);
                    files.add(uri);
                }
                fname = GPSApplication.getInstance().getFileName(track) + ".txt";
                file = new File(GPSApplication.DIRECTORY_TEMP + "/", fname);
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

    /**
     * Opens a Track with an external viewer using the GPSApplication Job executor.
     */
    public void openTrack() {
        GPSApplication.getInstance().loadJob(GPSApplication.JOB_TYPE_VIEW);
        GPSApplication.getInstance().executeJob();
        GPSApplication.getInstance().deselectAllTracks();
    }

    /**
     * Updates the user interface of the fragment.
     */
    public void update() {
        if (isAdded()) {
            Log.w("myApp", "[#] FragmentTracklist.java - Updating Tracklist");
            final List<Track> TI = GPSApplication.getInstance().getTrackList();
            //Log.w("myApp", "[#] FragmentTracklist.java - The element 0 has id = " + TI.get(0).getId());
            synchronized(data) {
                if (data != null) data.clear();
                if (!TI.isEmpty()) {
                    data.addAll(TI);
                    if (data.get(0).getId() == GPSApplication.getInstance().getCurrentTrack().getId()) {
                        GPSApplication.getInstance().setCurrentTrackVisible(true);
                        //Log.w("myApp", "[#] FragmentTracklist.java - current track, VISIBLE into the tracklist ("
                        //    + GPSApplication.getInstance().getCurrentTrack().getId() + ")");
                    } else {
                        GPSApplication.getInstance().setCurrentTrackVisible(false);
                        //Log.w("myApp", "[#] FragmentTracklist.java - current track empty, NOT VISIBLE into the tracklist");
                    }
                } else {
                    GPSApplication.getInstance().setCurrentTrackVisible(false);
                }
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvTracklistEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                            adapter.notifyDataSetChanged();
                        }
                    });
                } catch (NullPointerException e) {
                    //Log.w("myApp", "[#] FragmentTracklist.java - Unable to manage UI");
                }
            }
        }
    }

    /**
     * Deletes some tracks from the CardView Adapter via notification,
     * in order to show a graceful animation of the deletion.
     */
    public void deleteSomeTracks() {
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
                        tvTracklistEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            });
        } catch (NullPointerException e) {
            //Log.w("myApp", "[#] FragmentTracklist.java - Unable to manage UI");
            update();
        }
    }
}
