package org.lastrix.collagemaker.app;

import android.view.View;

/**
 * Compatibility class to handle item selection on android devices older than API 11.
 * Created by lastrix on 8/26/14.
 */
public class AdapterCompat {

    private final static int SELECTED_COLOR = 0xff33b5e5;
    private final static int BACKGROUND_COLOR = 0x00000000;

    /**
     * Change view background based flag
     *
     * @param view    -- the view to change
     * @param checked -- state
     */
    public static void state(View view, boolean checked) {
        if (checked) {
            view.setBackgroundColor(SELECTED_COLOR);
        } else {
            view.setBackgroundColor(BACKGROUND_COLOR);
        }
    }
}
