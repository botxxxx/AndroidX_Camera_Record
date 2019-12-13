package com.askey.askeylaunchers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeClock extends View {

    private final int TEXT_SIZE = 24;
    private Paint paint;
    private Paint paintNum;
    private Paint paintHour;
    private Paint paintMinute;
    private Paint paintSecond;
    private float x, y;
    private int r;
    private TextView digitalClock;

    public TimeClock(Context context) {
        super(context);
        initPaint();

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    postInvalidate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public TimeClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        postInvalidate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public TimeClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        postInvalidate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public TimeClock(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaint();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        postInvalidate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);

        paintNum = new Paint();
        paintNum.setColor(Color.WHITE);
        paintNum.setAntiAlias(true);
        paintNum.setTextSize(TEXT_SIZE);
        paintNum.setStyle(Paint.Style.STROKE);
        paintNum.setTextAlign(Paint.Align.CENTER);

        paintSecond = new Paint();
        paintSecond.setColor(Color.RED);
        paintSecond.setAntiAlias(true);
        paintSecond.setStrokeWidth(5);
        paintSecond.setStyle(Paint.Style.FILL);

        paintMinute = new Paint();
        paintMinute.setColor(Color.WHITE);
        paintMinute.setAntiAlias(true);
        paintMinute.setStrokeWidth(7);
        paintMinute.setStyle(Paint.Style.FILL);

        paintHour = new Paint();
        paintHour.setColor(Color.WHITE);
        paintHour.setAntiAlias(true);
        paintHour.setStrokeWidth(11);
        paintHour.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        digitalClock = (TextView) getRootView().findViewById(R.id.digital_clock);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        x = width / 2;
        y = height / 2;
        r = (int) x - 5;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawClock(canvas);
    }

    protected void drawClock(Canvas canvas) {
        canvas.drawCircle(x, y, 15, paintMinute);

        drawLines(canvas);
        drawText(canvas);

        try {
            initCurrentTime(canvas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawLines(Canvas canvas) {
        for (int i = 0; i < 60; i++) {
            if (i % 5 == 0) {
                paint.setStrokeWidth(8);
                canvas.drawLine(x, y - r, x, y - r + 10, paint);
            } else {
                paint.setStrokeWidth(3);
                canvas.drawLine(x, y - r, x, y - r + 10, paint);
            }

            canvas.rotate(6, x, y);
        }
    }

    private void drawText(Canvas canvas) {
        float textSize = (paintNum.getFontMetrics().bottom - paintNum.getFontMetrics().top);
        int distance = r - 5 - TEXT_SIZE;
        float a, b;

        for (int i = 0; i < 12; i++) {
            a = (float) (distance * Math.sin(i * 30 * Math.PI / 180) + x);
            b = (float) (y - distance * Math.cos(i * 30 * Math.PI / 180));
            if (i == 0) {
                canvas.drawText("12", a, b + textSize / 3, paintNum);
            } else {
                canvas.drawText(String.valueOf(i), a, b + textSize / 3, paintNum);
            }
        }
    }

    private void initCurrentTime(Canvas canvas) {
        SimpleDateFormat format = new SimpleDateFormat("HH-mm-ss");
        String time = format.format(new Date(System.currentTimeMillis()));

        String[] split = time.split("-");

        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);
        int second = Integer.parseInt(split[2]);

        int hourAngle = hour * 30 + minute / 2;
        int minuteAngle = minute * 6 + second / 10;
        // jeff_wu@20190712, remove the second hand
        //int secondAngle = second * 6;

        canvas.rotate(hourAngle, x, y);
        canvas.drawLine(x, y, x, y - r + 100, paintHour);
        canvas.save();
        canvas.restore();
        canvas.rotate(-hourAngle, x, y);

        canvas.rotate(minuteAngle, x, y);
        canvas.drawLine(x, y, x, y - r + 60, paintMinute);
        canvas.save();
        canvas.restore();
        canvas.rotate(-minuteAngle, x, y);

        // jeff_wu@20190712, remove the second hand
        /*canvas.rotate(secondAngle, x, y);
        canvas.drawLine(x, y, x, y - r + 20, paintSecond);
        canvas.rotate(-secondAngle, x, y);*/

        // jeff_wu@20190712, remove the number of seconds in analog clock
        //digitalClock.setText(split[0]+":"+split[1]+":"+split[2]);
        digitalClock.setText(split[0] + ":" + split[1]);
    }
}
