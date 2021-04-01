package com.d160.wa034;

import android.annotation.*;
import android.content.*;
import android.os.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

    public static class BootUpReceiver extends BroadcastReceiver {
        @SuppressLint("UnsafeProtectedBroadcastReceiver")
        public void onReceive(Context context, Intent intent) {
            context.startActivity(RestartActivity.createIntent(Objects.requireNonNull(context)));
        }
    }
}
