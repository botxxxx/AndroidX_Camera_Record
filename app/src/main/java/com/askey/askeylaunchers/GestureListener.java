package com.askey.askeylaunchers;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final String DEBUG_TAG = "GestureListener";

    protected static int MIN_SWIPE_DISTANCE_X = 50;
    protected static int MIN_SWIPE_DISTANCE_Y = 100;

    protected static int MAX_SWIPE_DISTANCE_X = 3000;
    protected static int MAX_SWIPE_DISTANCE_Y = 3000;

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float deltaX = e1.getX() - e2.getX();

        float deltaY = e1.getY() - e2.getY();

        float deltaXAbs = Math.abs(deltaX);
        float deltaYAbs = Math.abs(deltaY);

        if ((deltaXAbs >= MIN_SWIPE_DISTANCE_X)) //&& (deltaXAbs <= MAX_SWIPE_DISTANCE_X)
        {
            if (deltaX > 0) {
                swipeLeft();
            } else {
                swipeRight();
            }
        }

        if ((deltaYAbs >= MIN_SWIPE_DISTANCE_Y) && (deltaYAbs <= MAX_SWIPE_DISTANCE_Y)) {
            if (deltaY > 0) {
                swipeUp();
            } else {
                swipeDown();
            }
        }

        return true;
    }

    protected void swipeRight() {
    }

    protected void swipeLeft() {
    }

    protected void swipeUp() {
    }

    protected void swipeDown() {
    }
}
