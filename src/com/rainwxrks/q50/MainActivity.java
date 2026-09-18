package com.rainwxrks.q50;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int SENSOR_COOLANT = 14;
    private static final int SENSOR_OIL_TEMP = 15;
    private static final int SENSOR_SPEED = 17;

    private SensorManager sm;
    private TextView mphView;
    private TextView oilValue;
    private TextView waterValue;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0A0A0A"));
        root.setGravity(Gravity.CENTER);
        root.setPadding(30, 20, 30, 20);

        TextView header = new TextView(this);
        header.setText("\u2602 RainWxrks");
        header.setTextColor(Color.parseColor("#A855F7"));
        header.setTextSize(24);
        header.setGravity(Gravity.CENTER);
        root.addView(header);

        mphView = new TextView(this);
        mphView.setText("0 MPH");
        mphView.setTextColor(Color.WHITE);
        mphView.setTextSize(72);
        mphView.setGravity(Gravity.CENTER);
        mphView.setPadding(0, 30, 0, 10);
        root.addView(mphView);

        TextView sub = new TextView(this);
        sub.setText("V37 CAN | dongle-free");
        sub.setTextColor(Color.parseColor("#666666"));
        sub.setTextSize(12);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 40, 0, 0);

        LinearLayout oilCard = createCard(row, "OIL TEMP");
        LinearLayout waterCard = createCard(row, "WATER TEMP");

        oilValue = (TextView) oilCard.getChildAt(1);
        waterValue = (TextView) waterCard.getChildAt(1);

        row.addView(oilCard);
        row.addView(waterCard);
        root.addView(row);

        setContentView(root);
    }

    private LinearLayout createCard(LinearLayout parent, String label) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1A1A1A"));
        card.setPadding(30, 18, 30, 18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(12, 0, 12, 0);
        card.setLayoutParams(lp);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.parseColor("#A855F7"));
        l.setTextSize(12);
        l.setGravity(Gravity.CENTER);

        TextView v = new TextView(this);
        v.setText("-- \u00B0C");
        v.setTextColor(Color.WHITE);
        v.setTextSize(26);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, 8, 0, 0);

        card.addView(l);
        card.addView(v);
        return card;
    }

    @Override
    protected void onResume() {
        super.onResume();
        int[] ids = {SENSOR_SPEED, SENSOR_OIL_TEMP, SENSOR_COOLANT};
        for (int id : ids) {
            Sensor s = sm.getDefaultSensor(id);
            if (s!= null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float raw = e.values[0];
        int type = e.sensor.getType();
        if (type == SENSOR_SPEED) {
            final float mph = raw * 0.621f;
            runOnUiThread(new Runnable() {
                public void run() { mphView.setText(String.format("%.0f MPH", mph)); }
            });
        } else if (type == SENSOR_OIL_TEMP) {
            final float c = raw;
            runOnUiThread(new Runnable() {
                public void run() { oilValue.setText(String.format("%.0f\u00B0C / %.0f\u00B0F", c, c*9/5+32)); }
            });
        } else if (type == SENSOR_COOLANT) {
            final float c = raw;
            runOnUiThread(new Runnable() {
                public void run() { waterValue.setText(String.format("%.0f\u00B0C / %.0f\u00B0F", c, c*9/5+32)); }
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc) {}
}
