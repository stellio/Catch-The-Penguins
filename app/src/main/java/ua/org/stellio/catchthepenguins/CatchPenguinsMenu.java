/*
 * Copyright (C) Igor Lisovoy - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Igor Lisovoy <igor.lisovoy@yandex.ru>, Oct 17, 2014
 *
 * Web-page: http://stellio.org.ua/
 */

package ua.org.stellio.catchthepenguins;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.SyncStateContract.Helpers;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


public class CatchPenguinsMenu extends Activity implements OnClickListener {

	private Typeface tf; // uses to load custom font
	private TextView appTitle; // app title widget
	private TextView startGameButton; // button to start game
	private TextView rulesButton; // show help

	// constructor
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set landscape orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		// set full screen
	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// without title
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// inflate view
		setContentView(R.layout.startmenu);

		// get font
		tf = Typeface.createFromAsset(getAssets(), "fonts/IceAge.ttf");

		// get links to GUI elements
		appTitle = (TextView) findViewById(R.id.appName);
		startGameButton = (TextView) findViewById(R.id.startGame);
		rulesButton = (TextView) findViewById(R.id.rulesShow);

		// set typeface
		appTitle.setTypeface(tf);
		startGameButton.setTypeface(tf);
		rulesButton.setTypeface(tf);

		// connect OnClickListener
		startGameButton.setOnClickListener(this);
		rulesButton.setOnClickListener(this);
	}

	private void showRules() {

		Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.rules);
        dialogBuilder.setMessage(R.string.rules_content);

        dialogBuilder.setPositiveButton(R.string.help_ok,
            new DialogInterface.OnClickListener() {

        		@Override
            	public void onClick(DialogInterface dialog, int which) {
//                  displayScores(); // update values on screen
//                  isDialogDisplayed = false;
//                  resetGame(); // start new game
                }
            }
        );
        dialogBuilder.show();

	}

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.startGame: {
				// create intent to start main activity
				Intent i = new Intent(this, CatchPenguins.class);
				startActivity(i); // start CatchPenguinsView
				finish(); // close CathcPegnuinsMenu
			}	break;

			case R.id.rulesShow: {
				this.showRules();
			}	break;

			default:
				break;
		}
	}
}
