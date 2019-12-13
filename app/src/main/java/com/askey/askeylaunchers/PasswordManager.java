package com.askey.askeylaunchers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

abstract class PasswordManager {
    private static final String TAG = "PasswordManager";

    private static final String KEY_ALIAS = "askey.launcher.pwd";

    private static final String DEF_PASSWORD = "1234";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String FIXED_IV = "AnalogClocIV";
    private static int pwdlength = 4;
    private static KeyStore keyStore;
    private static Cipher cipher;
    public PopupWindow password_popup;
    private View.OnClickListener clicklistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView pwd = (TextView) password_popup.getContentView().findViewById(R.id.popup_password_display);

            CharSequence currpwd = pwd.getText();
            CharSequence newpwd;

            String tag = (String) v.getTag();
            switch (tag) {
                case "-1":
                    if (currpwd.length() > 0) {
                        newpwd = currpwd.subSequence(0, currpwd.length() - 1);
                    } else {
                        newpwd = "";
                    }
                    pwd.setText(newpwd);
                    break;
                default:
                    if (currpwd.length() < pwdlength) {
                        newpwd = currpwd + tag;
                        pwd.setText(newpwd);
                        if (newpwd.length() == pwdlength) {
                            inputAction(newpwd);
                        }
                    }
                    break;
            }
        }
    };

    private static void setFullScreen(View view) {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_VISIBLE;
        view.setSystemUiVisibility(uiOptions);
    }

    public static boolean isLocked(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME_ASKEYLAUNCHED, Context.MODE_PRIVATE);
            boolean locked = prefs.getBoolean(MainActivity.KEY_LAUNCH_LOCKED, false);

            return locked;
        } catch (Exception e) {
        }

        return false;
    }

    public static void setLocked(Context context, boolean locked) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME_ASKEYLAUNCHED, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(MainActivity.KEY_LAUNCH_LOCKED, locked);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initPassword(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME_ASKEYLAUNCHED, Context.MODE_PRIVATE);
            String keyvalue = prefs.getString(MainActivity.KEY_LAUNCH_PASSWORD, null);

            if (keyvalue == null) {
                setPassword(context, DEF_PASSWORD);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean setPassword(Context context, CharSequence pwd) {
        if (pwd.length() == pwdlength) {
            try {
                String encryptedpwd = encryptKey(pwd);

                SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME_ASKEYLAUNCHED, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(MainActivity.KEY_LAUNCH_PASSWORD, encryptedpwd);
                editor.commit();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private static boolean verifyPassword(Context context, CharSequence pwd) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME_ASKEYLAUNCHED, Context.MODE_PRIVATE);
            String keyvalue = prefs.getString(MainActivity.KEY_LAUNCH_PASSWORD, null);

            if (keyvalue != null) {
                String encryptedkey = keyvalue.replaceAll("\\r|\\n", "").trim();
                String encryptedpwd = encryptKey(pwd).replaceAll("\\r|\\n", "").trim();
                if (encryptedkey.equals(encryptedpwd)) {
                    return true;
                }
            } else {
                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }

    static void initKeyStore() {
        try {
            keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false)
                                .build());
                keyGenerator.generateKey();
            }
        } catch (Exception e) {
        }
    }

    private static String encryptKey(CharSequence input) throws Exception {
        byte[] inputByte = input.toString().getBytes();

        if (cipher == null) {
            cipher = Cipher.getInstance(AES_MODE);
        }
        cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_ALIAS, null),
                new GCMParameterSpec(128, FIXED_IV.getBytes()));

        byte[] encodedBytes = cipher.doFinal(inputByte);
        String encodedString = Base64.encodeToString(encodedBytes, Base64.DEFAULT);

        return encodedString;
    }

    public void showPasswordInput(Activity activity) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_password_input, null);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        boolean focusable = true;

        View decorView = activity.getWindow().getDecorView();

        password_popup = new PopupWindow(popupView, width, height, focusable);

        TextView pwd = (TextView) password_popup.getContentView().findViewById(R.id.popup_password_display);
        pwd.setText("");

        password_popup.setFocusable(false);
        password_popup.update();
        password_popup.showAtLocation(decorView, Gravity.CENTER, 0, 0);
        setFullScreen(password_popup.getContentView());
        password_popup.setFocusable(true);
        password_popup.update();

        Button btnCancel = (Button) password_popup.getContentView().findViewById(R.id.popup_password_btn_x);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                password_popup.dismiss();
            }
        });

        ImageButton btnBackspace = (ImageButton) password_popup.getContentView().findViewById(R.id.popup_password_btn_backspace);
        btnBackspace.setOnClickListener(clicklistener);
        int[] btnIds = {
                R.id.popup_password_btn_0,
                R.id.popup_password_btn_1,
                R.id.popup_password_btn_2,
                R.id.popup_password_btn_3,
                R.id.popup_password_btn_4,
                R.id.popup_password_btn_5,
                R.id.popup_password_btn_6,
                R.id.popup_password_btn_7,
                R.id.popup_password_btn_8,
                R.id.popup_password_btn_9
        };

        for (int btnId : btnIds) {
            Button btn = (Button) password_popup.getContentView().findViewById(btnId);
            btn.setOnClickListener(clicklistener);
        }
    }

    private void inputAction(CharSequence txt) {
        if (verifyPassword(password_popup.getContentView().getContext().getApplicationContext(), txt)) {
            password_popup.dismiss();
            verifiedAction();
        } else {
            wrongAction();
        }
    }

    abstract void verifiedAction();

    private void wrongAction() {
        TextView pwd = (TextView) password_popup.getContentView().findViewById(R.id.popup_password_display);

        int color = pwd.getCurrentTextColor();
        pwd.setTextColor(Color.RED);
        pwd.startAnimation(shakeError());
        pwd.setTextColor(color);
    }

    private TranslateAnimation shakeError() {
        TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(7));
        return shake;
    }
}
