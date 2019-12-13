package com.askey.askeylaunchers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class BaseActivity extends AppCompatActivity {
    protected static final long DEF_DOUBLE_KEY_INTERVAL = 750;
    private static final String TAG = "BaseActivity";
    private long lastKeyUp = -1;
    private boolean hasDoubleClicked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setFullScreen();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(MainActivity.TAG, "BaseActivity::onKeyUp: keyCdoe = " + keyCode + ", " + event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:  //goto previous page
            {
                goBack();
                return true;
            }
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    protected void goBack() {
        finish();
    }

    protected void setFullScreen() {
        setFullScreen(getWindow().getDecorView());
    }

    protected void setFullScreen(View view) {
        final View decorView = view;
        final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);

        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                Log.d(MainActivity.TAG, "BaseActivity::onSystemUiVisibilityChange: visibility = " + visibility);
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(uiOptions);
                }
            }
        });
    }
}
