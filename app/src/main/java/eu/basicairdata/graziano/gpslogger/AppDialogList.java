/**
 * AppDialogList - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 23/9/2020
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

public class AppDialogList extends BaseAdapter {

    private ArrayList<AppInfo> listData;

    private LayoutInflater layoutInflater;

    public AppDialogList(Context context, ArrayList<AppInfo> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
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

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.appdialog_list_row, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.id_appdialog_row_imageView_icon);
            holder.description = (TextView) convertView.findViewById(R.id.id_appdialog_row_textView_description);
            holder.format = (TextView) convertView.findViewById(R.id.id_appdialog_row_textView_format);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.icon.setImageDrawable(listData.get(position).Icon);
        holder.description.setText(listData.get(position).Label);
        holder.format.setText(listData.get(position).GPX ? "GPX" : listData.get(position).KML ? "KML" : "");

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView description;
        TextView format;
    }

}