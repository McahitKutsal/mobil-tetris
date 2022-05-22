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
        checkPermission();
        getSupportActionBar().hide();
        findViewById(R.id.easy_menu_button).setOnClickListener(this);
        findViewById(R.id.hard_menu_button).setOnClickListener(this);
        findViewById(R.id.ranking_button).setOnClickListener(view -> {
            Intent AccessRanking = new Intent(this, RankingActivity.class);
            startActivity(AccessRanking);
        });
    }
    private void checkPermission() {
        if (!hasPermissions()) {
            requestPermissions(PERMISSIONS, REQ_PERMISSION);
        } else {
            checkPermission(true);
        }
    }
    private boolean hasPermissions() {
        int result;
        // Check permission status in string array
        for (String perms : MainActivity.PERMISSIONS) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                // When if unauthorized permission found
                return false;
            }
        }

        // When if all permission allowed
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (isGranted) {
            permissionStatus = true;
        } else {
            permissionStatus = false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0) {
                boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
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

        switch (view.getId()){
            case R.id.easy_menu_button:
                difficulty = DIFFICULTY_EASY;
                break;
            case R.id.hard_menu_button:
                difficulty = DIFFICULTY_HARD;
                break;
        }

        Intent StartGame = new Intent(this, GameActivity.class);

        // do not add to the stack history
        StartGame.setFlags(StartGame.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        StartGame.putExtra("difficulty", difficulty);

        if(permissionStatus){
            startActivity(StartGame);
        }else{
            checkPermission();
        }

    }
}