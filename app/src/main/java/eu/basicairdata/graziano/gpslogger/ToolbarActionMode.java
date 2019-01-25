package eu.basicairdata.graziano.gpslogger;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ToolbarActionMode implements ActionMode.Callback {

    private Menu actionmenu;
    private GPSApplication gpsApplication = GPSApplication.getInstance();

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.card_menu, menu);
        EventBus.getDefault().register(this);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        actionmenu = menu;
        actionmenu.findItem(R.id.cardmenu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionmenu.findItem(R.id.cardmenu_view).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionmenu.findItem(R.id.cardmenu_export).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionmenu.findItem(R.id.cardmenu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        EvaluateVisibility();
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.cardmenu_delete:
                EventBus.getDefault().post(EventBusMSG.ACTION_BULK_DELETE_TRACKS);
                break;
            case R.id.cardmenu_export:
                EventBus.getDefault().post(EventBusMSG.ACTION_BULK_EXPORT_TRACKS);
                break;
            case R.id.cardmenu_view:
                EventBus.getDefault().post(EventBusMSG.ACTION_BULK_VIEW_TRACKS);
                break;
            case R.id.cardmenu_share:
                EventBus.getDefault().post(EventBusMSG.ACTION_BULK_SHARE_TRACKS);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        EventBus.getDefault().unregister(this);
        if ((gpsApplication.getNumberOfSelectedTracks() > 0) && gpsApplication.getGPSActivity_activeTab() == 2) GPSApplication.getInstance().DeselectAllTracks();
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusMSGNormal msg) {
        switch (msg.MSGType) {
            case EventBusMSG.TRACKLIST_SELECTION:
                EvaluateVisibility();
        }
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        switch (msg) {
            case EventBusMSG.UPDATE_TRACKLIST:
                EvaluateVisibility();
        }
    }

    public void EvaluateVisibility() {
        if (GPSApplication.getInstance().getNumberOfSelectedTracks() > 0) {
            actionmenu.findItem(R.id.cardmenu_view).setVisible((gpsApplication.getNumberOfSelectedTracks() <= 1) && (gpsApplication.isContextMenuViewVisible()));
            actionmenu.findItem(R.id.cardmenu_share).setVisible(gpsApplication.isContextMenuShareVisible());
            actionmenu.findItem(R.id.cardmenu_export).setVisible(gpsApplication.getPrefExportGPX() || gpsApplication.getPrefExportKML() || gpsApplication.getPrefExportTXT());
            actionmenu.findItem(R.id.cardmenu_delete).setVisible(!gpsApplication.getSelectedTracks().contains(gpsApplication.getCurrentTrack()));
            if (!gpsApplication.getViewInApp().equals(""))
                actionmenu.findItem(R.id.cardmenu_view).setTitle(gpsApplication.getString(R.string.card_menu_view, gpsApplication.getViewInApp()));
            else
                actionmenu.findItem(R.id.cardmenu_view).setTitle(gpsApplication.getString(R.string.card_menu_view_selector));
        }
    }
}
