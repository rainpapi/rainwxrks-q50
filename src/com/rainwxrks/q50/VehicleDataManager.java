package com.rainwxrks.q50;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RAINWX RKS vehicle-data acquisition layer.
 *
 * The Gen-1 InTouch environment can expose vehicle values as Android-style
 * sensor updates, but it does not give an app a generic "poll arbitrary CAN"
 * API. This class therefore discovers every exposed sensor, subscribes to it,
 * normalizes known vehicle signals, and never invents a value when a signal is
 * unavailable.
 */
public final class VehicleDataManager {
    public static final int COOLANT_TYPE = 14;
    public static final int OIL_TEMP_TYPE = 15;
    public static final int OIL_PRESSURE_TYPE = 16;
    public static final int SPEED_TYPE = 17;
    public static final int THROTTLE_TYPE = 23;

    private static final String TAG = "RainVehicleData";

    private final VehicleData data = new VehicleData();
    private final Map<Integer, Sensor> sensors = new HashMap<>();
    private final Map<Integer, Float> raw = new HashMap<>();
    private final Map<Integer, String> names = new HashMap<>();

    private int boostType = -1;
    private int iatType = -1;
    private int voltageType = -1;
    private int rpmType = -1;
    private int driveModeType = -1;
    private int mapType = -1;

    private int registeredCount;
    private int vehicleSignalCount;
    private boolean discovered;

    public VehicleDataManager() {
    }

    public void discover(SensorManager sm) {
        sensors.clear();
        names.clear();
        boostType = iatType = voltageType = rpmType = driveModeType = mapType = -1;
        registeredCount = 0;
        vehicleSignalCount = 0;
        discovered = false;

        if (sm == null) return;

        try {
            List<Sensor> all = sm.getSensorList(Sensor.TYPE_ALL);
            if (all == null) return;

            for (Sensor s : all) {
                if (s == null) continue;

                final int type = s.getType();
                final String name = safe(s.getName()).toLowerCase(Locale.US);

                sensors.put(type, s);
                names.put(type, name);

                if (isVehicleSignal(type, name)) vehicleSignalCount++;

                if (boostType < 0 && containsAny(name,
                        "boost", "turbo boost", "charge pressure"))
                    boostType = type;

                if (iatType < 0 && containsAny(name,
                        "iat", "intake air", "intake_air", "charge air temperature"))
                    iatType = type;

                if (voltageType < 0 && containsAny(name,
                        "battery voltage", "system voltage", "charging voltage", "vehicle voltage"))
                    voltageType = type;

                if (rpmType < 0 && containsAny(name,
                        "rpm", "engine speed", "engine rpm"))
                    rpmType = type;

                if (driveModeType < 0 && containsAny(name,
                        "drive mode", "driving mode", "infiniti mode", "driver mode"))
                    driveModeType = type;

                if (mapType < 0 && containsAny(name,
                        "map", "manifold absolute pressure", "manifold pressure"))
                    mapType = type;

                Log.i(TAG, "SENSOR type=" + type
                        + " name=\"" + safe(s.getName()) + "\""
                        + " vendor=\"" + safe(s.getVendor()) + "\""
                        + " range=" + s.getMaximumRange()
                        + " resolution=" + s.getResolution()
                        + " delay=" + s.getMinDelay());
            }

            discovered = true;

            Log.i(TAG, "DISCOVERY complete sensors=" + sensors.size()
                    + " vehicleSignals=" + vehicleSignalCount
                    + " boostType=" + boostType
                    + " iatType=" + iatType
                    + " voltageType=" + voltageType
                    + " rpmType=" + rpmType
                    + " driveModeType=" + driveModeType
                    + " mapType=" + mapType);
        } catch (Throwable t) {
            Log.e(TAG, "Sensor discovery failed", t);
        }
    }

    public void register(SensorManager sm, SensorEventListener listener) {
        if (sm == null || listener == null) return;

        registeredCount = 0;
        for (Sensor s : sensors.values()) {
            try {
                // 100,000 us = 10 Hz. This matches the current InTouch
                // environment without asking the platform for an impossible
                // CAN polling rate.
                if (sm.registerListener(listener, s, 100000)) {
                    registeredCount++;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Unable to register " + safe(s.getName()), t);
            }
        }

        data.live = vehicleSignalCount > 0;
        Log.i(TAG, "REGISTER complete registered=" + registeredCount
                + " discovered=" + discovered);
    }

    public void unregister(SensorManager sm, SensorEventListener listener) {
        if (sm == null || listener == null) return;
        try {
            sm.unregisterListener(listener);
        } catch (Throwable ignored) {
        }
    }

    public void onSensorChanged(SensorEvent e) {
        if (e == null || e.sensor == null || e.values == null || e.values.length == 0) return;

        final int type = e.sensor.getType();
        final float value = e.values[0];

        raw.put(type, value);
        normalize(type, value, names.get(type));
    }

    public VehicleData getData() {
        return data;
    }

    public float getRaw(int type) {
        Float v = raw.get(type);
        return v == null ? Float.NaN : v;
    }

    public boolean hasSensorType(int type) {
        return sensors.containsKey(type);
    }

    public String getStatus() {
        return "LIVE SOURCES " + vehicleSignalCount
                + " • REGISTERED " + registeredCount
                + " • 10 HZ";
    }

    public String getInventorySummary() {
        StringBuilder out = new StringBuilder();
        out.append("RAINWX VEHICLE DATA\n");
        out.append("Sensors: ").append(sensors.size()).append('\n');
        out.append("Vehicle signals: ").append(vehicleSignalCount).append('\n');
        out.append("Registered: ").append(registeredCount).append('\n');
        out.append("Boost: ").append(typeName(boostType)).append('\n');
        out.append("IAT: ").append(typeName(iatType)).append('\n');
        out.append("Voltage: ").append(typeName(voltageType)).append('\n');
        out.append("RPM: ").append(typeName(rpmType)).append('\n');
        out.append("MAP: ").append(typeName(mapType)).append('\n');
        out.append("Drive Mode: ").append(typeName(driveModeType)).append('\n');
        return out.toString();
    }

    private void normalize(int type, float value, String sensorName) {
        final String name = safe(sensorName).toLowerCase(Locale.US);

        if (type == SPEED_TYPE) {
            data.speedRaw = value;
            data.hasSpeed = finite(value);
        }

        if (type == COOLANT_TYPE || containsAny(name, "coolant", "engine coolant")) {
            data.coolantC = normalizeTemperatureC(value, name);
            data.hasCoolant = finite(data.coolantC);
        }

        if (type == OIL_TEMP_TYPE || containsAny(name, "oil temp", "engine oil temperature")) {
            data.oilTempC = normalizeTemperatureC(value, name);
            data.hasOilTemp = finite(data.oilTempC);
        }

        if (type == OIL_PRESSURE_TYPE || containsAny(name, "oil pressure", "eop sensor")) {
            data.oilPressureRaw = value;
            data.hasOilPressure = finite(value);
        }

        if (type == THROTTLE_TYPE || containsAny(name,
                "throttle", "throttle angle", "accelerator position")) {
            data.throttleRaw = value;
            data.hasThrottle = finite(value);
        }

        if (type == boostType) {
            data.boostPsi = normalizePressurePsi(value, name);
            data.hasBoost = finite(data.boostPsi);
        }

        if (type == iatType || containsAny(name,
                "iat", "intake air", "charge air temperature")) {
            data.iatC = normalizeTemperatureC(value, name);
            data.hasIat = finite(data.iatC);
        }

        if (type == voltageType) {
            data.voltage = value;
            data.hasVoltage = finite(value);
        }

        if (type == rpmType) {
            data.rpm = value;
            data.hasRpm = finite(value);
        }

        if (type == driveModeType) {
            data.driveModeRaw = value;
            data.hasDriveMode = finite(value);
        }

        if (type == mapType) {
            data.mapKpa = normalizePressureKpa(value, name);
            data.hasMap = finite(data.mapKpa);
        }

        // If the platform exposes absolute MAP but not a dedicated boost
        // signal, calculate boost only when the sensor explicitly identifies
        // itself as MAP/manifold pressure. No guessing from arbitrary values.
        if (!data.hasBoost && data.hasMap) {
            final float baroKpa = 101.325f;
            final float boostKpa = data.mapKpa - baroKpa;
            final float boostPsi = boostKpa * 0.1450377f;
            if (finite(boostPsi)) {
                data.boostPsi = boostPsi;
                data.hasBoost = true;
            }
        }
    }

    private boolean isVehicleSignal(int type, String name) {
        if (type == COOLANT_TYPE || type == OIL_TEMP_TYPE || type == OIL_PRESSURE_TYPE
                || type == SPEED_TYPE || type == THROTTLE_TYPE) return true;

        return containsAny(name,
                "vehicle", "engine", "coolant", "oil", "boost", "turbo",
                "intake", "iat", "throttle", "rpm", "voltage", "drive mode",
                "driving mode", "manifold", "map", "transmission", "fuel");
    }

    private float normalizeTemperatureC(float value, String name) {
        if (!finite(value)) return Float.NaN;

        if (containsAny(name, "fahrenheit", "°f", "deg f")) {
            return (value - 32f) * 5f / 9f;
        }

        // The current Q50 signal set is already Celsius for coolant/IAT-style
        // values. Do not blindly infer units from magnitude.
        return value;
    }

    private float normalizePressurePsi(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "kpa")) return value * 0.1450377f;
        if (containsAny(name, "bar")) return value * 14.5038f;
        if (containsAny(name, "psi")) return value;
        return value;
    }

    private float normalizePressureKpa(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "psi")) return value * 6.894757f;
        if (containsAny(name, "bar")) return value * 100f;
        return value;
    }

    private String typeName(int type) {
        if (type < 0) return "NOT EXPOSED";
        Sensor s = sensors.get(type);
        return s == null ? String.valueOf(type) : type + " " + safe(s.getName());
    }

    private static boolean finite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean containsAny(String s, String... terms) {
        if (s == null) return false;
        for (String term : terms) {
            if (term != null && s.contains(term)) return true;
        }
        return false;
    }
}
