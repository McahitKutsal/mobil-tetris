package com.tetris.game;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Gerekli değişkenlerin tanımlanması
    private static final String[] PERMISSIONS = new String[]
            {Manifest.permission.CAMERA};
    private static final int REQ_PERMISSION = 1000;

    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_HARD = 2;

    private boolean permissionStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Kamera izni verilmiş mi? Kontol edilmesi
        checkPermission();
        //Uygulamalarda default olarak gelen toolbarın gizlenmesi
        getSupportActionBar().hide();
        //Tasarım ile değişkenlerin bağlantısının yapılması
        findViewById(R.id.easy_menu_button).setOnClickListener(this);
        findViewById(R.id.hard_menu_button).setOnClickListener(this);
    }
    private void checkPermission() {
        //Kamera izin kontrolü
        if (!hasPermissions()) {
            requestPermissions(PERMISSIONS, REQ_PERMISSION);
        } else {
            checkPermission(true);
        }
    }
    private boolean hasPermissions() {
        int result;
        for (String perms : MainActivity.PERMISSIONS) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {

                return false;
            }
        }

        // Kullanıcı izin vermişse true döner
        return true;
    }

    private void checkPermission(boolean isGranted) {
        //Kamera izin kontrolü
        if (isGranted) {
            permissionStatus = true;
        } else {
            permissionStatus = false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // İzin almak için dialog gösterilir ve kullanıcının verdiği yanıta göre fonk geri döndürülür
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0) {
                boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                // Kullanıcı izin verirse true, vermezse false döner
                if (cameraPermissionAccepted) {
                    checkPermission(true);
                } else {
                    checkPermission(false);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        int difficulty = 0;
        //Kolay ve zor mod butonlarına tılayınca difficulty belirlenir
        switch (view.getId()){
            case R.id.easy_menu_button:
                difficulty = DIFFICULTY_EASY;
                break;
            case R.id.hard_menu_button:
                difficulty = DIFFICULTY_HARD;
                break;
        }
        // Ardından Game Activity ye gönderilir ve oyun başlar.
        Intent StartGame = new Intent(this, GameActivity.class);
        StartGame.setFlags(StartGame.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        StartGame.putExtra("difficulty", difficulty);
        // Yine gönderilmeden kamera izni verip vermediği kontrol edilir.
        if(permissionStatus){
            startActivity(StartGame);
        }else{
            checkPermission();
        }

    }
}