/*
 * Copyright (C) Igor Lisovoy - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Igor Lisovoy <igor.lisovoy@yandex.ru>, Oct 17, 2014
 *
 * Web-page: http://stellio.org.ua/
 */

package ua.org.stellio.catchthepenguins;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CatchPenguinsView extends View {

	// get HIGH_SCORE from SharedPreference
	private static final String HIGH_SCORE = "HIGH_SCORE";
	private SharedPreferences preferences; // stores the high score

	// variables for managing the game
	private int penguinsTouched; // number of touched penguins
	private int score; // current score
	private int level; // current level
	private int viewWidth; // stores the width of this View
	private int viewHeight; // stores the height of this view
	private long animationTime; // how long each penguins remains on the screen
	private boolean isGameOver; // whether the game has ended
	private boolean isGamePaused; // whether the game has ended
	private boolean isDialogDisplayed; // whether the game has ended
	private int highScore; // the game's all time high score

	// collections of penguins (ImageViews) and Animators
	private final Queue<ImageView> penguins =
      new ConcurrentLinkedQueue<ImageView>();
	private final Queue<Animator> animators =
			new ConcurrentLinkedQueue<Animator>();

	private Typeface tf; // uses to load custom font
	private TextView highScoreTextView; // displays high score
	private TextView currentScoreTextView; // displays current score
	private TextView levelTextView; // displays current level
	private LinearLayout livesLinearLayout; // displays lives remaining
	private RelativeLayout relativeLayout; // displays spots
	private Resources resources; // used to load resources
	private LayoutInflater layoutInflater; // used to inflate GUIs

	// time in milliseconds for penguin and touched penguin animations
	private static final int INITIAL_ANIMATION_DURATION = 6000;
	private static final Random random = new Random(); // for random cords
	private static final int PENGUIN_DIAMETER = 100; // initial spot size
	private static final float SCALE_X = 0.25f; // end animation x scale
	private static final float SCALE_Y = 0.25f; // end animation y scale
	private static final int INITIAL_PENGUINS = 5; // initial # of penguins
	private static final int PENGUIN_DELAY = 500; // delay in milliseconds
	private static final int LIVES = 3; // start with 3 lives
	private static final int MAX_LIVES = 7; // maximum # of total lives
	private static final int LEVELS = 10;
	private static final int NEW_LEVEL = 10; // penguins to reach new level
	private Handler penguinHandler; // adds new penguins to the game

	// sound IDs, constants and variables for the game's sounds
	private static final int PENGUIN_APPEAR = 1;
	private static final int PENGUIN_CATCH = 2;
	private static final int PENGUIN_DISAPPEAR = 3;
	private static final int GAME_OVER = 4;
	private static final int MISSED_TOUCH = 5;
	private static final int NEW_LIFE = 6;
	private static final int GAME_WIN = 7;
	private static final int SOUND_PRIORITY = 1;
	private static final int SOUND_QUALITY = 100;
	private static final int MAX_STREAMS = 4;
	private SoundPool soundPool; // plays sound effects
	private int volume; // sound effect volume
	private Map<Integer, Integer> soundMap; // maps ID to soundpool

	// main constructor
   	public CatchPenguinsView(Context context, SharedPreferences sharedPreferences, RelativeLayout parentLayout) {
		super(context);

		// load the high score
		preferences = sharedPreferences;
		highScore = preferences.getInt(HIGH_SCORE, 0);

		// save the Resources
		resources = context.getResources();

		// save layout inflater
		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// set parent layout to our relative layout
		relativeLayout = parentLayout;

		// background image
		relativeLayout.setBackgroundResource(R.drawable.background);

		// load custom font
		tf = Typeface.createFromAsset(context.getAssets(), "fonts/IceAge.ttf");

		// get references to various GUI components
		livesLinearLayout = (LinearLayout) relativeLayout.findViewById(R.id.lifeLinearLayout);
		highScoreTextView = (TextView) relativeLayout.findViewById(R.id.highScoreTextView);
		currentScoreTextView = (TextView) relativeLayout.findViewById(R.id.scoreTextView);
		levelTextView = (TextView) relativeLayout.findViewById(R.id.levelTextView);

	    // set custom typeface
		highScoreTextView.setTypeface(tf);
		currentScoreTextView.setTypeface(tf);
		levelTextView.setTypeface(tf);

		penguinHandler = new Handler(); // used to add penguins when game start
	}

	// save width/height of view
	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
	  	viewWidth = width; // save the new width
	  	viewHeight = height; // save the new height
	}

	// called by main Activity when on Pause
	public void pause() {
		isGamePaused = true;
		soundPool.release(); // release audio resources
		soundPool = null;
		cancelAnimations(); // stop animations
	}

	private void showWinnerDialog() {

		// if last score is greater, save it
        if (score > highScore) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(HIGH_SCORE, score);
            editor.commit(); // save high score
            highScore = score;
        }

        // build winner dialog
		Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle(R.string.win_title);
        dialogBuilder.setMessage(resources.getString(R.string.score) + " " + score);
        dialogBuilder.setCancelable(false);


        // Start new game
        dialogBuilder.setPositiveButton(R.string.reset_game,
            new DialogInterface.OnClickListener() {

        		@Override
            	public void onClick(DialogInterface dialog, int which) {
                  displayScores(); // update values on screen
                  isDialogDisplayed = false;
                  resetGame(); // start new game
                }
            }
        );

        // Set share button
        dialogBuilder.setNeutralButton(R.string.share,
        	new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {

					String message = String.format(resources.getString(R.string.win_text), score);
					Intent share = new Intent(Intent.ACTION_SEND);
					share.setType("text/plain");
					share.putExtra(Intent.EXTRA_TEXT, message);

					getContext().startActivity(Intent.createChooser(share, getContext().getString(R.string.share_title)));
					Activity activity = (Activity)getContext();
					activity.finish();
				}
			}
        );

        // Set exit button
        dialogBuilder.setNegativeButton(R.string.exit,
        	new DialogInterface.OnClickListener() {

        		@Override
				public void onClick(DialogInterface dialog, int which) {
					Activity activity = (Activity)getContext();
					activity.finish();
				}
			}
	    );

        dialogBuilder.show(); // display reset game dialog

	}

	// stop animations and remove ImageViews with penguins
	private void cancelAnimations() {
		// cancel all animations
		for (Animator animator : animators)
			animator.cancel();

		// remove all penguins view from screen
		for (ImageView view : penguins)
			relativeLayout.removeView(view);

		penguinHandler.removeCallbacks(addPenguinRunnable);
		animators.clear();
		penguins.clear();
	}

	// called by main Activity when it receives onResume
	public void resume(Context context) {
	  isGamePaused = false;
	  initializeSoundEffects(context); // initialize app's SoundPool

	  if (!isDialogDisplayed)
	    	resetGame(); // start game
	}

	// prepare new game
   	public void resetGame() {
		penguins.clear(); // clean list of penguins
		animators.clear(); // clearn list of animators
		livesLinearLayout.removeAllViews();

		animationTime = INITIAL_ANIMATION_DURATION; // init animation length
		penguinsTouched = 0; // reset number of thouched penguins
		score = 0; // reset the score
		level = 1; // reset the level
		isGameOver = false; //
		displayScores(); // display scores

		// add lives
		for (int i = 0; i < LIVES; i++)
		{
			// add life indicator to screen
		 	livesLinearLayout.addView((ImageView) layoutInflater.inflate(R.layout.life, null));
		}

		// add INITIAL_PENGUINS
		for (int i = 1; i <= INITIAL_PENGUINS; ++i)
			penguinHandler.postDelayed(addPenguinRunnable, i * PENGUIN_DELAY);
   }

	// init soundPool
	private void initializeSoundEffects(Context context) {
		// initialize SoundPool to play game effects
		soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, SOUND_QUALITY);

		// set volume
		AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

		// create sound effects map
		soundMap = new HashMap<Integer, Integer>();

		// add each sound effect to the SoundPool
		soundMap.put(PENGUIN_APPEAR,
			soundPool.load(context, R.raw.appear, SOUND_PRIORITY));
		soundMap.put(PENGUIN_CATCH,
			soundPool.load(context, R.raw.catch_penguin, SOUND_PRIORITY));
		soundMap.put(PENGUIN_DISAPPEAR,
			soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
		soundMap.put(GAME_OVER,
			soundPool.load(context, R.raw.game_over, SOUND_PRIORITY));
		soundMap.put(MISSED_TOUCH,
			soundPool.load(context, R.raw.missed, SOUND_PRIORITY));
		soundMap.put(NEW_LIFE,
				soundPool.load(context, R.raw.new_life, SOUND_PRIORITY));
		soundMap.put(GAME_WIN,
				soundPool.load(context, R.raw.win_game, SOUND_PRIORITY));
	}

	// display current score highest score and level
   	private void displayScores() {
      	levelTextView.setText(resources.getString(R.string.level) + " " + level);
      	currentScoreTextView.setText(resources.getString(R.string.score) + " " + score);
    	highScoreTextView.setText(resources.getString(R.string.high_score) + " " + highScore);
   	}

   	// Runnable used to add new spots to the game at the start
	private Runnable addPenguinRunnable = new Runnable() {
    	public void run() {
        	addNewPenguin(); // add new penguin to game
      }
    };

    // adds new penguin to the lyaout and set animation
   	public void addNewPenguin() {
		// get random coordinate for start and end of points
		int x = random.nextInt(viewWidth - PENGUIN_DIAMETER);
		int y = random.nextInt(viewHeight - PENGUIN_DIAMETER);
		int x2 = random.nextInt(viewWidth - PENGUIN_DIAMETER);
		int y2 = random.nextInt(viewHeight - PENGUIN_DIAMETER);

		// create new penguin
		final ImageView penguin = (ImageView) layoutInflater.inflate(R.layout.untoched, null);
		penguins.add(penguin); // add penguin to list of penguins

		penguin.setLayoutParams(new RelativeLayout.LayoutParams(PENGUIN_DIAMETER, PENGUIN_DIAMETER));


		// set resource to ImageView. Two variants 'black penguin or blue'
		penguin.setImageResource(
				random.nextInt(2) == 0 ? R.drawable.black_penguin : R.drawable.blue_penguin);
		penguin.setX(x); // set coordinates
		penguin.setY(y);

		// add onClickListener for view
		penguin.setOnClickListener(
			new OnClickListener() {
		    	public void onClick(View v) {
		       		touchedPenguin(penguin); // handle touched penguin
		    	}
		 	}
		);

		relativeLayout.addView(penguin); // add penguin to layout (screen)
		// play the appear sound
      	if (soundPool != null)
        	soundPool.play(PENGUIN_APPEAR, volume, volume, SOUND_PRIORITY, 0, 1f);


		// prepare and init penguin animation
		penguin.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y).setDuration(animationTime).setListener(
			new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					animators.add(animation); // save animator, to get posibility terminate this animation
				}

				public void onAnimationEnd(Animator animation) {
					animators.remove(animation); // remove animato

					if (!isGamePaused && penguins.contains(penguin)) // not touched
						missedPenguin(penguin); // lose a life

				}
			}
		);
    }

    // called when user touches the screen, not penguin
   	@Override
   	public boolean onTouchEvent(MotionEvent event) {
    	// play the missed sound
      	if (soundPool != null)
        	soundPool.play(MISSED_TOUCH, volume, volume, SOUND_PRIORITY, 0, 1f);

      	score -= 5 * level; // remove some points
      	score = Math.max(score, 0); // do not let the score go below zero
      	displayScores(); // update values
      	return true;
   }

	// called when a penguin is touched
   	private void touchedPenguin(ImageView penguin) {
    	relativeLayout.removeView(penguin); // remove touched penguin from screen
      	penguins.remove(penguin); // remove old penguin from list

      	++penguinsTouched; // increment the number of touched penguins
      	score += 10 * level; // increment the score


      	// play the hit effects
      	if (soundPool != null)
        	soundPool.play(PENGUIN_CATCH, volume, volume, SOUND_PRIORITY, 0, 1f);

      	// go to next level if user touched 10 penguins
      	if (penguinsTouched % NEW_LEVEL == 0) {
        	++level;
         	animationTime *= 0.95; // make game 5% faster

         	// check if user past last level
         	if (level > LEVELS) {
         		isGameOver = true;
         		soundPool.play(GAME_WIN, volume, volume, SOUND_PRIORITY, 0, 1f);
         		this.showWinnerDialog();

         	} else {

         	// if maximum number of lives has not been reached
            	if (livesLinearLayout.getChildCount() < MAX_LIVES) {
            		ImageView life = (ImageView) layoutInflater.inflate(R.layout.life, null);
                	livesLinearLayout.addView(life); // add life to screen
                	// play new life added effect
                	if (soundPool != null)
                		soundPool.play(NEW_LIFE, volume, volume, SOUND_PRIORITY, 0, 1f);
            	}

         	}


     	}

     	displayScores(); // update values

      	if (!isGameOver)
        	addNewPenguin(); // add another penguin
   	}

   	// called when a spot finishes its animation without being touched
   	public void missedPenguin(ImageView penguin) {
    	penguins.remove(penguin); // remove penguin from list
      	relativeLayout.removeView(penguin); // and from sreen

      	if (isGameOver) // exit
        	return;

//      	 play the disappear effect
      	if (soundPool != null)
        	soundPool.play(PENGUIN_DISAPPEAR, volume, volume, SOUND_PRIORITY, 0, 1f);

      	// if the game has been lost
      	if (livesLinearLayout.getChildCount() == 0) {
         	isGameOver = true; // Game Over )

        	// if last score is greater, save it
	        if (score > highScore) {
	            SharedPreferences.Editor editor = preferences.edit();
	            editor.putInt(HIGH_SCORE, score);
	            editor.commit(); // save high score
	            highScore = score;
	        }

	        cancelAnimations();

	        // display a "Game Over" dialog
	        Builder dialogBuilder = new AlertDialog.Builder(getContext());
	        dialogBuilder.setTitle(R.string.game_over);
	        dialogBuilder.setMessage(resources.getString(R.string.score) + " " + score);
	        dialogBuilder.setCancelable(false);


	        // Start new game
	        dialogBuilder.setPositiveButton(R.string.reset_game,
	            new DialogInterface.OnClickListener() {

	        		@Override
	            	public void onClick(DialogInterface dialog, int which) {
	                  displayScores(); // update values on screen
	                  isDialogDisplayed = false;
	                  resetGame(); // start new game
	                }
	            }
	        );

	        // Set share button
	        dialogBuilder.setNeutralButton(R.string.share,
	        	new DialogInterface.OnClickListener() {
//
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
//
						String message = String.format(resources.getString(R.string.get_score), score);
						Intent share = new Intent(Intent.ACTION_SEND);
						share.setType("text/plain");
						share.putExtra(Intent.EXTRA_TEXT, message);
//
						getContext().startActivity(Intent.createChooser(share, getContext().getString(R.string.share_title)));
						Activity activity = (Activity)getContext();
						activity.finish();
					}
				}
	        );

	        // Set exit button
	        dialogBuilder.setNegativeButton(R.string.exit,
	        	new DialogInterface.OnClickListener() {

	        		@Override
					public void onClick(DialogInterface dialog, int which) {
						Activity activity = (Activity)getContext();
						activity.finish();
					}
				}
		    );




	        isDialogDisplayed = true;
	        dialogBuilder.show(); // display reset game dialog
	        // play game over sound effect
	        if (soundPool != null)
	        	soundPool.play(GAME_OVER, volume, volume, SOUND_PRIORITY, 0, 1f);
	    }
	    else { // remove one life
   	    	livesLinearLayout.removeViewAt( // remove from screen
            livesLinearLayout.getChildCount() - 1);
        	addNewPenguin(); // add another penguin to the screen
       }
    }
}
