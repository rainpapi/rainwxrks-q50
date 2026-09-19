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
 * RAINWX RKS Q50 controller.
 *
 * Camera functionality is intentionally removed from the controller.
 * Vehicle values are supplied only when a real exposed vehicle source exists.
 * No demo/fake vehicle values are generated.
 */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sm;
    private GaugeView home;
    private RainCheckView rainCheck;
    private PerformanceView performance;
    private SettingsView settings;
    private DiagnosticManager diagnostics;
    private VehicleDataManager vehicleData;

    private boolean registered;
    private int driveMode = DriveMode.UNKNOWN;
    private float driveModeRaw = Float.NaN;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        diagnostics = new DiagnosticManager(sm);
        vehicleData = new VehicleDataManager();

        showHome();
        registerVehicleData();
    }

    private final RainUI.Listener nav = new RainUI.Listener() {
        @Override public void home() { showHome(); }
        @Override public void rain() { showRainCheck(); }
        @Override public void performance() { showPerformance(); }

        /*
         * Kept only for source compatibility with an older RainUI.Listener.
         * Camera navigation is deliberately disabled and no CameraView is
         * created or referenced by this app.
         */
        @Override public void camera() { }

        @Override public void settings() { showSettings(); }

        @Override public void setExhaustEnabled(boolean enabled) {
            // Exhaust Note was removed because it was synthetic, not vehicle data.
        }

        @Override public void setDriveMode(String mode, float raw) {
            if (settings != null) settings.setDriveMode(mode, raw);
        }
    };

    private void showHome() {
        if (home == null) home = new GaugeView(this, nav);
        setContentView(home);
        pushVehicleDataToUi();
    }

    private void showRainCheck() {
        if (rainCheck == null) {
            rainCheck = new RainCheckView(this, nav, new RainCheckView.Listener() {
                @Override public void onScan() {
                    rainCheck.setResult(diagnostics.scan(), 0);
                }

                @Override public void onClear() {
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

            if (home != null) home.setStatus(vehicleData.getStatus());
            pushVehicleDataToUi();
        } catch (Throwable t) {
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
            if (d.hasOilPressure) home.setValue(GaugeView.OILP, d.oilPressurePsi);
            if (d.hasRpm) home.setValue(GaugeView.RPM, d.rpm);
            if (d.hasThrottle) home.setValue(GaugeView.THROTTLE, d.throttleRaw);
            if (d.hasTransmissionTemp) home.setValue(GaugeView.TRANS_TEMP, d.transmissionTempC);
            if (d.hasFuel) home.setValue(GaugeView.FUEL, d.fuelPercent);

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
            }

            home.invalidate();
        }

        if (performance != null) {
            performance.setSpeed(
                    d.hasSpeed ? d.speedRaw * GaugeView.SPEED_RAW_TO_MPH : 0f,
                    d.hasSpeed
            );
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (sm != null && !registered) registerVehicleData();
    }

    @Override protected void onPause() {
        super.onPause();

        if (vehicleData != null && sm != null) {
            vehicleData.unregister(sm, this);
        }

        registered = false;
    }

    @Override public void onSensorChanged(SensorEvent e) {
        try {
            if (vehicleData != null) vehicleData.onSensorChanged(e);

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

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }
}
