/*
 * TrackInfoAdapter - Java Class for Android
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

//import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;


class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

    private final static int NOT_AVAILABLE = -100000;
    private final static String FilesDir = GPSApplication.getInstance().getApplicationContext().getFilesDir().toString();

    private final static int CARDTYPE_CURRENTTRACK = 0;
    private final static int CARDTYPE_TRACK = 1;

    private ArrayList<Track> dataSet;
    private int selectedItem = -1;

    //private final int[] Icons = {R.mipmap.ic_place_white_24dp, R.mipmap.ic_directions_walk_white_24dp, R.mipmap.ic_terrain_white_24dp,
    //        R.mipmap.ic_directions_run_white_24dp, R.mipmap.ic_directions_bike_white_24dp, R.mipmap.ic_directions_car_white_24dp,
    //        R.mipmap.ic_flight_white_24dp};

    private static final Bitmap[] bmpTrackType = {
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_place_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_walk_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_terrain_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_run_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_bike_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_car_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_flight_white_24dp)
    };

    class TrackHolder extends RecyclerView.ViewHolder {

        private Bitmap bmp;
        private long numberOfPoints;
        private int TT;

        private final TextView textViewTrackName;
        private final TextView textViewTrackLength;
        private final TextView textViewTrackDuration;
        private final TextView textViewTrackAltitudeGap;
        private final TextView textViewTrackMaxSpeed;
        private final TextView textViewTrackAverageSpeed;
        private final TextView textViewTrackGeopoints;
        private final TextView textViewTrackPlacemarks;
        private final ImageView imageViewThumbnail;
        private final ImageView imageViewIcon;
        private final ProgressBar progressBar;

        private PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
        private PhysicalData phdDuration;
        private PhysicalData phdSpeedMax;
        private PhysicalData phdSpeedAvg;
        private PhysicalData phdDistance;
        private PhysicalData phdAltitudeGap;

        private Track track;


        TrackHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedItem = getLayoutPosition();
                    if (selectedItem >= 0) {
                        if (track.getProgress() == 0) {
                            EventBus.getDefault().post("TRACKLIST_SELECTION " + track.getId());
                            Log.w("myApp", "[#] TrackAdapter.java - Selected track: " + track.getName() + " (id = " + track.getId() + ")");
                        }
                    }
                }
            });
            //itemView.setClickable(true);

            textViewTrackName           = (TextView) itemView.findViewById(R.id.id_textView_card_TrackName);
            textViewTrackLength         = (TextView) itemView.findViewById(R.id.id_textView_card_length);
            textViewTrackDuration       = (TextView) itemView.findViewById(R.id.id_textView_card_duration);
            textViewTrackAltitudeGap    = (TextView) itemView.findViewById(R.id.id_textView_card_altitudegap);
            textViewTrackMaxSpeed       = (TextView) itemView.findViewById(R.id.id_textView_card_maxspeed);
            textViewTrackAverageSpeed   = (TextView) itemView.findViewById(R.id.id_textView_card_averagespeed);
            textViewTrackGeopoints      = (TextView) itemView.findViewById(R.id.id_textView_card_geopoints);
            textViewTrackPlacemarks     = (TextView) itemView.findViewById(R.id.id_textView_card_placemarks);
            imageViewThumbnail          = (ImageView) itemView.findViewById(R.id.id_imageView_card_minimap);
            imageViewIcon               = (ImageView) itemView.findViewById(R.id.id_imageView_card_tracktype);
            progressBar                 = (ProgressBar) itemView.findViewById(R.id.id_progressBar_card);
        }

        void SetProgress(int newprogress) {
            track.setProgress(newprogress);
            progressBar.setProgress(newprogress);
        }

        void BindTrack(Track trk) {

            track = trk;
            numberOfPoints = track.getNumberOfLocations();
            textViewTrackName.setText(track.getName());
            //textViewTrackName.setText(track.getId() + " - " + track.getName());
            if (numberOfPoints > 1) {
                phdDistance = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phdDistance.Value + " " + phdDistance.UM);
                phdDuration = phdformatter.format(track.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phdDuration.Value);
                phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phdAltitudeGap.Value + " " + phdAltitudeGap.UM);
                phdSpeedMax = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phdSpeedMax.Value + " " + phdSpeedMax.UM);
                phdSpeedAvg = phdformatter.format(track.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phdSpeedAvg.Value + " " + phdSpeedAvg.UM);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(numberOfPoints));
            textViewTrackPlacemarks.setText(String.valueOf(track.getNumberOfPlacemarks()));
            progressBar.setProgress(track.getProgress());
            TT = track.getTrackType();
            if (TT != NOT_AVAILABLE) {
                imageViewIcon.setVisibility(View.VISIBLE);
                imageViewIcon.setImageBitmap(bmpTrackType[TT]);
            }
            else imageViewIcon.setVisibility(View.INVISIBLE);
            String Filename = FilesDir + "/Thumbnails/" + track.getId() + ".png";
            File file = new File(Filename);
            if (file.exists ()) {
                bmp = BitmapFactory.decodeFile(Filename);
                imageViewThumbnail.setImageBitmap(bmp);
            } else imageViewThumbnail.setImageDrawable(null);
            //Picasso.with(GPSApplication.getInstance().getApplicationContext()).load(file).into(imageViewThumbnail);
        }
    }


    TrackAdapter(ArrayList<Track> data) {
        this.dataSet = data;
    }


    @Override
    public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.card_currenttrackinfo : R.layout.card_trackinfo, parent, false);
        return new TrackHolder(view);
    }

    public int getItemViewType (int position) {
        //Some logic to know which type will come next;
        //return (position == 0) && (dataSet.get(0).getId() == GPSApplication.getInstance().getCurrentTrack().getId()) ? CARDTYPE_CURRENTTRACK : CARDTYPE_TRACK;
        return (position == 0) && GPSApplication.getInstance().isCurrentTrackVisible() ? CARDTYPE_CURRENTTRACK : CARDTYPE_TRACK;
    }


    @Override
    public void onBindViewHolder(final TrackHolder holder, final int listPosition) {
        holder.BindTrack(dataSet.get(listPosition));
    }


    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}