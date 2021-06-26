/*
 * ExternalViewerAdapter - Java Class for Android
 * Created by G.Capelli on 23/9/2020
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_KML;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_GPX;

/**
 * The Adapter for the menu that lists the Track Viewers.
 */
public class ExternalViewerAdapter extends BaseAdapter {
    private final ArrayList<ExternalViewer> listData;
    private final LayoutInflater layoutInflater;

    /**
     * Creates a new ExternalViewerAdapter using the specified ArrayList of ExternalViewer.
     *
     * @param context the base context
     * @param listData the ArrayList of ExternalViewer to be used as adapter data.
     */
    public ExternalViewerAdapter(Context context, ArrayList<ExternalViewer> listData) {
        this.listData = listData;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Returns the View of the given position.
     *
     * @param position the position of the view
     * @param convertView the view
     * @param parent the parent ViewGroup
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.appdialog_list_row, null);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.id_appdialog_row_imageView_icon);
            holder.description = convertView.findViewById(R.id.id_appdialog_row_textView_description);
            holder.format = convertView.findViewById(R.id.id_appdialog_row_textView_format);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.icon.setImageDrawable(listData.get(position).icon);
        holder.description.setText(listData.get(position).label);
        holder.format.setText(listData.get(position).fileType.equals(FILETYPE_GPX) ? "GPX" : listData.get(position).fileType.equals(FILETYPE_KML) ? "KML" : "");
        return convertView;
    }

    /**
     * The class used into the ExternalViewerAdapter Class.
     */
    static class ViewHolder {
        ImageView icon;
        TextView description;
        TextView format;
    }
}
