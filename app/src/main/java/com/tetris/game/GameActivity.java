package com.tetris.game;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import android.util.Log;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    //Gerekli değişkenlerin tanımlanması
    private GazeTracker gazeTracker = null;
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
    private float delayFactor;
    private float delay;
    private float delayLowerLimit;
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

        // Media Player ayarları yapılır ve müzik başlatılır.
        tetris_sound= MediaPlayer.create(GameActivity.this, R.raw.tetris);
        tetris_sound.setLooping(true);
        tetris_sound.setVolume(100, 100);
        tetris_sound.start();

        // Oyun durumu sıfırlanır.
        gameState.reset();

        // Uygulamalarda default olarak gelen toolbarın gizlenmesi
        getSupportActionBar().hide();

        // Skor değişkenine 0 atanır.
        tempScore = 0;

        // Tasarım ile değişkenlerin bağlantısının yapılması
        tetrisView = findViewById(R.id.tetris_view);
        left = findViewById(R.id.button_left);
        turn = findViewById(R.id.button_turn);
        right = findViewById(R.id.button_right);
        pause = findViewById(R.id.button_pause);
        score = findViewById(R.id.game_score);
        eye_icon = findViewById(R.id.eye_icon);

        //Gece modu ve gündüz modu için tema renk ayarlarının yapılması
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


        // Butonlar için tıklama fonksiyonları atanır.
        left.setOnClickListener(this);
        turn.setOnClickListener(this);
        right.setOnClickListener(this);
        pause.setOnClickListener(this);

        //Oyunun hızını ayarlayan değişkenlere atama yapılması
        delay = 500;
        delayLowerLimit = 200;
        delayFactor = 1.25f;

        Intent i = getIntent();
        int difficulty = i.getIntExtra("difficulty", 0);

        // Oyunun zorluğunu ayarlayan delay kullancının seçimine göre düzenlenir
        if (difficulty == 2) {
            delay = delay/delayFactor;
        } else {
            delay = delay * delayFactor;
        }

        // Göz tarama başlatılır
        initGaze();

        // Oyunun akış döngüsü tanımlanır.
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
                            // Kullanıcı aşağıya bakınca gelen obje döndürülür.
                            gameState.rotateFallingTetrisFigureAntiClock();
                            Log.i("SeeSo", "Aşağı");
                        }
                        if(right==x_array.length){
                            // Kullanıcı sağa bakınca gelen obje sağa gider.
                            gameState.moveFallingTetrisFigureRight();
                            Log.i("SeeSo", "Sağa");
                        }
                        if(left==x_array.length){
                            // Kullanıcı sola bakınca gelen obje sola gider.
                            gameState.moveFallingTetrisFigureLeft();
                            Log.i("SeeSo", "Sola");
                        }
                        // Objenin aşağıya hareketi sağlanır.
                        boolean success = gameState.moveFallingTetrisFigureDown();
                        if (!success) {
                            gameState.paintTetrisFigure(gameState.falling);
                            gameState.lineRemove();

                            gameState.pushNewTetrisFigure(TetrisFigureType.getRandomTetrisFigure());

                            // Skor arttıkça oyun hızlandırılır.
                            if (gameState.score % 10 == 9 && delay >= delayLowerLimit) {
                                delay = delay / delayFactor + 1;
                            }
                            gameState.incrementScore();

                            // Skor arttılır ve güncellenir
                            ++tempScore;
                            String stringScore = Integer.toString(gameState.score);
                            score.setText(stringScore);


                        }

                        tetrisView.invalidate();
                    }
                    handler.postDelayed(this, (long) delay);
                } else {
                    //Oyun bitince müzik durduruluyor.
                    tetris_sound.stop();

                    //Dialog oluşturulur ve skor ekrana yazdırılır.
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
        // Oyunun akış döngüsü başlatılır.
        loop.run();

    }


    @Override
    public void onClick(View action) {
        if (action == left) {

            //Sol butona tıklayınca sola hareket ettirilir.
            gameState.moveFallingTetrisFigureLeft();

        } else if (action == right) {

            //Sağ butona tıklayınca sağa hareket ettirilir.
            gameState.moveFallingTetrisFigureRight();

        } else if (action == turn) {

            //Döndürme butona tıklayınca obje döndürülür.
            gameState.rotateFallingTetrisFigureAntiClock();

        } else if (action == pause) {

            if (gameState.status) {
                //Pause butonuna tıklayınca oyun ve müzik durdurulur.
                if (gameState.pause) {
                    gameState.pause = false;
                    pause.setText(R.string.pause);
                    tetris_sound.start();

                } else {
                    //Oyun durmuşken pause butonuna tıklayınca oyun ve müzik devam ettirilir.
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
        //Oyundan çıkılınca oyun, göz takibi ve müzik durdurulur.

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
        //Oyun devam ettirince göz takibi başlatılır.
        if(gazeTracker!=null && !gazeTracker.isTracking()){
            gazeTracker.startTracking();
        }

    }


    private void initGaze() {
        //Göz takip kütüphanesinin lisansının tanımlanması ve tanımlanması
        String licenseKey = "dev_41qcrirm06ytujj97umrc5c5fog9yz4w16er2xys";
        GazeTracker.initGazeTracker(GameActivity.this, licenseKey, initializationCallback);
    }

    private InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            //Göz takip kütüphanesinin sorunsuz başlaması için kontrol yapılır.
            if (gazeTracker != null) {
                initSuccess(gazeTracker);
            } else {
                initFail(error);
            }


        }
    };


    private void initSuccess(GazeTracker gazeTracker) {
        //Başarılı olunca tanımlama yapılır.
        this.gazeTracker = gazeTracker;
        this.gazeTracker.setGazeCallback(gazeCallback);
        this.gazeTracker.startTracking();
    }
    //Göz takip için filtre tanımlanır.
    private OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);

    public GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            //Göz takibinden gelen x ve y değerleri filtreden geçirdikten sonra diziye aktarılır.
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
                    //Eğer kullancı ekranın içerisine bakıyorsa göz ikonu yeşil yapılır.
                    focusable = true;
                    eye_icon.setColorFilter(ContextCompat.getColor(GameActivity.this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }else{
                if(focusable){
                    //Eğer kullancı ekranın dışına bakıyorsa göz ikonu mavi yapılır.
                    focusable = false;
                    eye_icon.setColorFilter(ContextCompat.getColor(GameActivity.this, R.color.ocean), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

        }
    };

    private void initFail(InitializationErrorType error) {
        //Göz takip kütüphanesinde hata olur ve başlatılamazsa hata log atılır.
        String err = "";
        if (error == InitializationErrorType.ERROR_INIT) {
            err = "Initialization failed";
        } else if (error == InitializationErrorType.ERROR_CAMERA_PERMISSION) {
            err = "Required permission not granted";
        } else {
            err = "init gaze library fail";
        }
        Log.w("SeeSo", "error description: " + err);
    }
}