package com.rainwxrks.q50;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * RainWxrks Q50 controller.
 *
 * Vehicle values now flow through VehicleDataManager instead of being
 * discovered and interpreted directly inside the UI controller.
 */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sm;
    private GaugeView home;
    private RainCheckView rainCheck;
    private PerformanceView performance;
    private SettingsView settings;
    private CameraView camera;
    private DiagnosticManager diagnostics;

    private VehicleDataManager vehicleData;
    private boolean registered = false;

    private boolean exhaustEnabled = false;
    private ExhaustSound exhaustSound;

    private int driveMode = DriveMode.UNKNOWN;
    private float driveModeRaw = Float.NaN;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        diagnostics = new DiagnosticManager(sm);
        exhaustSound = new ExhaustSound();

        vehicleData = new VehicleDataManager();

        showHome();
        registerVehicleData();
    }

    private final RainUI.Listener nav = new RainUI.Listener() {
        public void home() { showHome(); }
        public void rain() { showRainCheck(); }
        public void performance() { showPerformance(); }
        public void camera() { showCamera(); }
        public void settings() { showSettings(); }

        public void setExhaustEnabled(boolean enabled) {
            exhaustEnabled = enabled;
            if (exhaustSound != null) exhaustSound.setEnabled(enabled);
        }

        public void setDriveMode(String mode, float raw) {
            if (settings != null) settings.setDriveMode(mode, raw);
        }
    };

    private void showHome() {
        if (home == null) home = new GaugeView(this, nav);
        setContentView(home);

        if (performance != null && vehicleData != null) {
            VehicleData d = vehicleData.getData();
            performance.setSpeed(
                    d.hasSpeed ? d.speedRaw * GaugeView.SPEED_RAW_TO_MPH : 0f,
                    d.hasSpeed
            );
        }

        pushVehicleDataToUi();
    }

    private void showRainCheck() {
        if (rainCheck == null) {
            rainCheck = new RainCheckView(this, nav, new RainCheckView.Listener() {
                public void onScan() {
                    rainCheck.setResult(diagnostics.scan(), 0);
                }

                public void onClear() {
                    rainCheck.setResult(diagnostics.clearCodes(), 0);
                }
            });
        }
        setContentView(rainCheck);
    }

    private void showPerformance() {
        if (performance == null) performance = new PerformanceView(this, nav);
        setContentView(performance);
        pushVehicleDataToUi();
    }

    private void showCamera() {
        if (camera == null) camera = new CameraView(this, nav);
        setContentView(camera);
    }

    private void showSettings() {
        if (settings == null) settings = new SettingsView(this, nav);
        settings.setDriveMode(DriveMode.name(driveMode), driveModeRaw);
        setContentView(settings);
    }

    private void registerVehicleData() {
        if (sm == null || vehicleData == null) return;

        try {
            vehicleData.discover(sm);
            vehicleData.register(sm, this);
            registered = true;

            if (home != null) {
                home.setStatus(vehicleData.getStatus());
            }

            pushVehicleDataToUi();
        } catch (Throwable ignored) {
            registered = false;
            if (home != null) home.setStatus("VEHICLE DATA UNAVAILABLE");
        }
    }

    private void pushVehicleDataToUi() {
        if (vehicleData == null) return;

        VehicleData d = vehicleData.getData();

        if (home != null) {
            if (d.hasSpeed) home.setValue(GaugeView.SPEED, d.speedRaw);
            if (d.hasCoolant) home.setValue(GaugeView.COOLANT, d.coolantC);
            if (d.hasOilTemp) home.setValue(GaugeView.OILT, d.oilTempC);
            if (d.hasOilPressure) home.setValue(GaugeView.OILP, d.oilPressureRaw);
            if (d.hasThrottle) home.setValue(GaugeView.THROTTLE, d.throttleRaw);

            home.setExtraData(
                    d.hasBoost ? d.boostPsi : 0f, d.hasBoost,
                    d.hasIat ? d.iatC : 0f, d.hasIat,
                    d.hasVoltage ? d.voltage : 0f, d.hasVoltage
            );

            if (d.hasDriveMode) {
                driveModeRaw = d.driveModeRaw;
                driveMode = DriveMode.decode(driveModeRaw);
                home.setDriveMode(DriveMode.name(driveMode));
                if (settings != null) {
                    settings.setDriveMode(DriveMode.name(driveMode), driveModeRaw);
                }
                if (exhaustSound != null) {
                    exhaustSound.setModeGain(DriveMode.soundGain(driveMode));
                }
            }

            home.invalidate();
        }

        if (performance != null) {
            performance.setSpeed(
                    d.hasSpeed ? d.speedRaw * GaugeView.SPEED_RAW_TO_MPH : 0f,
                    d.hasSpeed
            );
        }

        if (exhaustSound != null) {
            if (d.hasRpm) exhaustSound.setRpm(d.rpm);
            if (d.hasThrottle) exhaustSound.setThrottle(d.throttleRaw);
        }
    }

    @Override protected void onResume() {
        super.onResume();

        if (sm != null && !registered) {
            registerVehicleData();
        }

        if (exhaustSound != null) {
            exhaustSound.setEnabled(exhaustEnabled);
        }
    }

    @Override protected void onPause() {
        super.onPause();

        if (exhaustSound != null) exhaustSound.setEnabled(false);

        if (vehicleData != null && sm != null) {
            vehicleData.unregister(sm, this);
        }

        registered = false;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        try {
            if (vehicleData != null) {
                vehicleData.onSensorChanged(e);
            }

            // Keep the accelerometer path for the performance G-force display.
            if (performance != null
                    && e != null
                    && e.sensor != null
                    && e.sensor.getType() == Sensor.TYPE_ACCELEROMETER
                    && e.values != null
                    && e.values.length >= 2) {

                float gx = e.values[0] / 9.80665f;
                float gy = e.values[1] / 9.80665f;
                performance.setGForce(gx, gy);
            }

            pushVehicleDataToUi();
        } catch (Throwable ignored) {
        }
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {
    }

    @Override protected void onDestroy() {
        if (exhaustSound != null) exhaustSound.release();
        super.onDestroy();
    }
}
