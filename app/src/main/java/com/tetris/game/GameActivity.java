package com.tetris.game;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    GazeTracker gazeTracker = null;

    public static GameState gameState = new GameState(24, 20, TetrisFigureType.getRandomTetrisFigure());
    private TetrisView tetrisView;
    private ImageButton left;
    private ImageButton right;
    private ImageButton turn;
    private ImageView eye_icon;
    private Button pause;
    private TextView score;
    private Handler handler;
    private Runnable loop;
    private int delayFactor;
    private int delay;
    private int delayLowerLimit;
    private int tempScore;
    MediaPlayer tetris_sound;
    private float[] x_array = {500,500,500,500,500};
    private float[] y_array = {500,500,500,500,500};
    private boolean focusable = false;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // set sound during playing
        tetris_sound= MediaPlayer.create(GameActivity.this, R.raw.tetris);
        tetris_sound.setLooping(true); // Set looping
        tetris_sound.setVolume(100, 100);
        tetris_sound.start();

        // reset view / gamestate after restart in order to play again
        gameState.reset();

        // hide Action Bar
        getSupportActionBar().hide();

        // score for intent to GameOver activity
        tempScore = 0;

        // config the used views
        tetrisView = findViewById(R.id.tetris_view);

        // button move left
        left = findViewById(R.id.button_left);

        // button turn
        turn = findViewById(R.id.button_turn);

        // button turn right
        right = findViewById(R.id.button_right);

        pause = findViewById(R.id.button_pause);
        score = findViewById(R.id.game_score);

        eye_icon = findViewById(R.id.eye_icon);

        // set dark / light mode colors to buttons
        int nightModeFlags =
                getApplicationContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                left.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.wave));
                right.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.wave));
                turn.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.wave));
                pause.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.ocean));
                pause.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.wave));
                break;

            case Configuration.UI_MODE_NIGHT_NO:

                left.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.deep_aqua));
                right.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.deep_aqua));
                turn.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.deep_aqua));
                pause.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.wave));
                pause.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                break;

            default:
                break;
        }


        // set onclicklisteners
        left.setOnClickListener(this);
        turn.setOnClickListener(this);
        right.setOnClickListener(this);
        pause.setOnClickListener(this);

        delay = 500;
        delayLowerLimit = 200;
        delayFactor = 2;

        // get the intent with the difficulty information from main activity
        Intent i = getIntent();
        int difficulty = i.getIntExtra("difficulty", 0);

        // set the speed of the game based on the difficulty
        if (difficulty == 2) {
            delay = delay/delayFactor;
        } else {
            delay = delay * delayFactor;
        }

        initGaze();
        
        handler = new Handler(Looper.getMainLooper());
        loop = new Runnable() {
            public void run() {
                if (gameState.status) {
                    if (!gameState.pause) {
                        int up=0,down=0,right=0,left=0;

                        for(int i=0;i<x_array.length;i++){
                            float x = x_array[i];
                            float y = y_array[i];

                            if(y>1500){
                                down++;
                            }else if(y<100){
                                up++;
                            }
                            else if(x>750){
                                right++;
                            }
                            else if(x<250){
                                left++;
                            }
                        }

                        if(up==x_array.length){
                            Log.i("SeeSo", "Yukarı");
                        }
                        if(down==x_array.length){
                            gameState.rotateFallingTetrisFigureAntiClock();
                            Log.i("SeeSo", "Aşağı");
                        }
                        if(right==x_array.length){
                            gameState.moveFallingTetrisFigureRight();
                            Log.i("SeeSo", "Sağa");
                        }
                        if(left==x_array.length){
                            gameState.moveFallingTetrisFigureLeft();
                            Log.i("SeeSo", "Sola");
                        }



                        boolean success = gameState.moveFallingTetrisFigureDown();
                        if (!success) {
                            gameState.paintTetrisFigure(gameState.falling);
                            gameState.lineRemove();

                            gameState.pushNewTetrisFigure(TetrisFigureType.getRandomTetrisFigure());

                            // make game faster
                            if (gameState.score % 10 == 9 && delay >= delayLowerLimit) {
                                delay = delay / delayFactor + 1;
                            }
                            gameState.incrementScore();

                            // update score
                            ++tempScore;
                            String stringScore = Integer.toString(gameState.score);
                            score.setText(stringScore);


                        }
                       tetrisView.invalidate();
                    }
                    handler.postDelayed(this, delay);
                } else {
                    tetris_sound.stop();

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(GameActivity.this);
                    alertDialogBuilder.setTitle("Oyun Bitti");
                    alertDialogBuilder.setIcon(R.drawable.ic_game_over);
                    alertDialogBuilder.setMessage("İyi bir oyun çıkardın! Skorun "+tempScore);
                    alertDialogBuilder.setPositiveButton("İlerle", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            Intent i = new Intent(getBaseContext(), MainActivity.class);
                            startActivity(i);

                  }
                    });
                    AlertDialog alert = alertDialogBuilder.create();
                    alert.setCanceledOnTouchOutside(false);
                    alert.setCancelable(false);

                    if(!isFinishing())
                    {
                        alert.show();
                    }


                }
            }

        };
        loop.run();

    }


    @Override
    public void onClick(View action) {
        if (action == left) {

            // change color when click
            gameState.moveFallingTetrisFigureLeft();

        } else if (action == right) {

            // change color when click
            gameState.moveFallingTetrisFigureRight();

        } else if (action == turn) {

            // change color when click
            gameState.rotateFallingTetrisFigureAntiClock();

        } else if (action == pause) {
            if (gameState.status) {
                if (gameState.pause) {
                    gameState.pause = false;
                    pause.setText(R.string.pause);
                    tetris_sound.start();

                } else {
                    pause.setText(R.string.play);
                    gameState.pause = true;
                    tetris_sound.pause();

                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause.setText(R.string.play);
        gameState.pause = true;
        if (tetris_sound.isPlaying()) {
            tetris_sound.pause();
        }
        if(gazeTracker!=null && gazeTracker.isTracking()){
            gazeTracker.stopTracking();
        }

        focusable = false;
        eye_icon.setColorFilter(ContextCompat.getColor(GameActivity.this, R.color.ocean), android.graphics.PorterDuff.Mode.SRC_IN);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(gazeTracker!=null && !gazeTracker.isTracking()){
            gazeTracker.startTracking();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
/*
        GazeTracker.deinitGazeTracker(this.gazeTracker);
        this.gazeTracker = null;*/
    }

    private void initGaze() {
        String licenseKey = "dev_41qcrirm06ytujj97umrc5c5fog9yz4w16er2xys";
        GazeTracker.initGazeTracker(GameActivity.this, licenseKey, initializationCallback);
    }

    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                initSuccess(gazeTracker);
            } else {
                initFail(error);
            }


        }
    };


    private void initSuccess(GazeTracker gazeTracker) {
        this.gazeTracker = gazeTracker;
        this.gazeTracker.setGazeCallback(gazeCallback);
        this.gazeTracker.startTracking();
    }
    private OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);

    public GazeCallback gazeCallback = new GazeCallback() {

        @Override
        public void onGaze(GazeInfo gazeInfo) {

            if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                float[] filteredValues = oneEuroFilterManager.getFilteredValues();
                float x = filteredValues[0];
                float y = filteredValues[1];

                for(int i=0;i<(x_array.length-1);i++){
                    x_array[i+1] = x_array[i];
                }
                for(int i=0;i<(y_array.length-1);i++){
                    y_array[i+1] = y_array[i];
                }

                x_array[0] = x;
                y_array[0] = y;

                if(!focusable){
                    focusable = true;
                    eye_icon.setColorFilter(ContextCompat.getColor(GameActivity.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }else{
                if(focusable){
                    focusable = false;
                    eye_icon.setColorFilter(ContextCompat.getColor(GameActivity.this, R.color.ocean), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

        }
    };

    private void initFail(InitializationErrorType error) {
        String err = "";
        if (error == InitializationErrorType.ERROR_INIT) {
            // When initialization is failed
            err = "Initialization failed";
        } else if (error == InitializationErrorType.ERROR_CAMERA_PERMISSION) {
            // When camera permission doesn not exists
            err = "Required permission not granted";
        } else {
            // Gaze library initialization failure
            // It can ba caused by several reasons(i.e. Out of memory).
            err = "init gaze library fail";
        }
        Log.w("SeeSo", "error description: " + err);
        Toast.makeText(GameActivity.this,"error description: " + err,Toast.LENGTH_LONG).show();
    }
}