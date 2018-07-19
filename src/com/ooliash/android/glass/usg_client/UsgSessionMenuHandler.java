package com.ooliash.android.glass.usg_client;

import android.util.Log;

import com.google.android.glass.media.Sounds;

public class UsgSessionMenuHandler {

    private static final String LOG_TAG = "USG";
    private final UsgSessionActivity context;

    UsgSessionMenuHandler(UsgSessionActivity context) {
        this.context = context;
    }

    public void HandleItem(int itemId) {
        context.playSoundEffect(Sounds.TAP);
        switch (itemId) {
            case R.id.freeze:
                context.commandFreeze();
                break;
            case R.id.save_picture:
                //TODO: Implement save picture.
                context.errorMessage("Not implemented yet.");
                break;
            case R.id.options:
                //TODO: Implement options menu.
                context.errorMessage("Not implemented yet.");
                break;
            case R.id.close_session:
                context.finish();
                break;
            default:
                Log.e(LOG_TAG, "Unknown session menu item ID."); // + item.getTitle()
        }
    }
}
