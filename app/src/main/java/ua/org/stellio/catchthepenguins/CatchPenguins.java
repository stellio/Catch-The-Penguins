/*
 * Copyright (C) Igor Lisovoy - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Igor Lisovoy <igor.lisovoy@yandex.ru>, Oct 19, 2014
 *
 * Web-page: http://stellio.org.ua/
 */

package ua.org.stellio.catchthepenguins;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class CatchPenguins extends Activity {

    private CatchPenguinsView penguinsView; // main game view
    private RelativeLayout layout; // main Activity layout

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // set full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // without title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.gameview);

        // create new CatchPenguinsView and add it to the RelativeLayout
        layout = (RelativeLayout) findViewById(R.id.relativeLayout);

        // create view
        penguinsView = new CatchPenguinsView(this, getPreferences(Context.MODE_PRIVATE),layout);

        // add view to layout
        layout.addView(penguinsView, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        penguinsView.pause();
    }

    @Override
    public void onResume(){
        super.onResume();
        penguinsView.resume(this);
    }
}