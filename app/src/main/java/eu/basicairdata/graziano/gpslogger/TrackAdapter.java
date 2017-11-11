/**
 * TrackAdapter - Java Class for Android
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
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
//import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;


class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

    private final static int NOT_AVAILABLE = -100000;
    private final static String FilesDir = GPSApplication.getInstance().getApplicationContext().getFilesDir().toString() + "/Thumbnails/";
    private final static int CARDTYPE_CURRENTTRACK = 0;
    private final static int CARDTYPE_TRACK = 1;

    private List<Track> dataSet;


    private static final Bitmap[] bmpTrackType = {
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_place_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_walk_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_terrain_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_run_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_bike_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_car_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_flight_white_24dp)
    };


    static class TrackHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
        private PhysicalData phd;
        private int TT;

        private long id;
        private Bitmap bmp;

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


        @Override
        public void onClick(View v) {
            if (progressBar.getProgress() == 0) {
                EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACKLIST_SELECTION, id));
                //Log.w("myApp", "[#] TrackAdapter.java - Selected track id = " + id);
            }
        }


        TrackHolder(View itemView) {
            super(itemView);

            //Log.w("myApp", "[#] TrackAdapter.java - TrackHolder");
            itemView.setOnClickListener(this);

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
            progressBar.setProgress(newprogress);
        }


        void UpdateTrackStats(Track trk) {
            textViewTrackName.setText(trk.getName());
            //textViewTrackName.setText(track.getId() + " - " + track.getName());
            if (trk.getNumberOfLocations() > 1) {
                phd = phdformatter.format(trk.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phd.Value);
                phd = phdformatter.format(trk.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phd.Value + " " + phd.UM);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(trk.getNumberOfLocations()));
            textViewTrackPlacemarks.setText(String.valueOf(trk.getNumberOfPlacemarks()));

            TT = trk.getTrackType();
            if (TT != NOT_AVAILABLE) imageViewIcon.setImageBitmap(bmpTrackType[TT]);
            else imageViewIcon.setImageBitmap(null);
        }


        void BindTrack(Track trk) {

            id = trk.getId();
            textViewTrackName.setText(trk.getName());
            //textViewTrackName.setText(track.getId() + " - " + track.getName());
            if (trk.getNumberOfLocations() > 1) {
                phd = phdformatter.format(trk.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phd.Value);
                phd = phdformatter.format(trk.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phd.Value + " " + phd.UM);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(trk.getNumberOfLocations()));
            textViewTrackPlacemarks.setText(String.valueOf(trk.getNumberOfPlacemarks()));

            // ----- This is a Workaround of an Android bug (https://issuetracker.google.com/issues/36923384)
            progressBar.setMax(50);
            progressBar.setMax(100);
            // -----

            progressBar.setProgress(trk.getProgress());

            TT = trk.getTrackType();
            if (TT != NOT_AVAILABLE) imageViewIcon.setImageBitmap(bmpTrackType[TT]);
            else imageViewIcon.setImageBitmap(null);

            /*
            Picasso
                    .with(GPSApplication.getInstance().getApplicationContext())
                    .load("file:///" + FilesDir + id + ".png")
                    .noPlaceholder()
                    .noFade()
                    .into(imageViewThumbnail);
            */

            if (Build.VERSION.SDK_INT >= 11) {
                Glide.clear(imageViewThumbnail);

                Glide
                        .with(GPSApplication.getInstance().getApplicationContext())
                        .load(FilesDir + id + ".png")
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        //.skipMemoryCache(true)
                        .error(null)
                        .dontAnimate()
                        .into(imageViewThumbnail);
            } else {
                String Filename = FilesDir + id + ".png";
                File file = new File(Filename);
                if (file.exists ()) {
                    bmp = BitmapFactory.decodeFile(Filename);
                    imageViewThumbnail.setImageBitmap(bmp);
                }
                else imageViewThumbnail.setImageDrawable(null);
            }
        }
    }


    TrackAdapter(List<Track> data) {
        synchronized(data) {
            this.dataSet = data;
        }
    }


    @Override
    public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TrackHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.card_currenttrackinfo : R.layout.card_trackinfo, parent, false));
    }


    public int getItemViewType (int position) {
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