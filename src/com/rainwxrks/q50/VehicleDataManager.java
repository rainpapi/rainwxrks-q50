package com.rainwxrks.q50;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RAINWX RKS vehicle-data acquisition layer.
 *
 * The head-unit Android sensor framework does not provide a generic
 * "read arbitrary CAN PID" API. We therefore discover every exposed sensor
 * at runtime and bind vehicle values by explicit sensor names/aliases.
 *
 * Important: this class never treats an ordinary Android sensor such as the
 * phone/head-unit temperature sensor as engine oil or coolant temperature.
 */
public final class VehicleDataManager {
    private static final String TAG = "RainVehicleData";

    /*
     * These are legacy/custom automotive sensor types used by earlier builds.
     * They are only accepted as fallbacks when the sensor is clearly exposed
     * by an automotive/vehicle source. Standard Android sensor IDs are not
     * assumed to be Q50 CAN signals.
     */
    private static final int LEGACY_COOLANT_TYPE = 14;
    private static final int LEGACY_OIL_TEMP_TYPE = 15;
    private static final int LEGACY_OIL_PRESSURE_TYPE = 16;
    private static final int LEGACY_SPEED_TYPE = 17;
    private static final int LEGACY_THROTTLE_TYPE = 23;

    private final VehicleData data = new VehicleData();
    private final Map<Integer, Sensor> sensors = new HashMap<>();
    private final Map<Integer, String> names = new HashMap<>();
    private final Map<Integer, Boolean> automotiveSource = new HashMap<>();
    private final Map<Integer, Float> raw = new HashMap<>();

    private int speedType = -1;
    private int coolantType = -1;
    private int oilTempType = -1;
    private int oilPressureType = -1;
    private int throttleType = -1;
    private int rpmType = -1;
    private int boostType = -1;
    private int iatType = -1;
    private int voltageType = -1;
    private int transmissionTempType = -1;
    private int fuelType = -1;
    private int driveModeType = -1;
    private int mapType = -1;

    private int registeredCount;
    private int vehicleSignalCount;
    private boolean discovered;

    public void discover(SensorManager sm) {
        sensors.clear();
        names.clear();
        automotiveSource.clear();
        raw.clear();

        speedType = coolantType = oilTempType = oilPressureType = -1;
        throttleType = rpmType = boostType = iatType = -1;
        voltageType = transmissionTempType = fuelType = -1;
        driveModeType = mapType = -1;

        registeredCount = 0;
        vehicleSignalCount = 0;
        discovered = false;
        data.clear();

        if (sm == null) return;

        try {
            List<Sensor> all = sm.getSensorList(Sensor.TYPE_ALL);
            if (all == null) return;

            for (Sensor s : all) {
                if (s == null) continue;

                int type = s.getType();
                String name = safe(s.getName()).toLowerCase(Locale.US);
                String vendor = safe(s.getVendor()).toLowerCase(Locale.US);
                String sourceText = (name + " " + vendor).trim();

                boolean automotive = containsAny(sourceText,
                        "vehicle", "automotive", "infiniti", "nissan",
                        "intouch", "can", "obd", "ecu", "engine", "powertrain");

                sensors.put(type, s);
                names.put(type, name);
                automotiveSource.put(type, automotive);

                if (isVehicleSignal(type, name, automotive)) vehicleSignalCount++;

                if (speedType < 0 && matches(name,
                        "vehicle speed", "road speed", "wheel speed", "speed vehicle",
                        "speed_kph", "speed mph", "speed kmh"))
                    speedType = type;

                if (coolantType < 0 && matches(name,
                        "coolant temperature", "engine coolant", "engine coolant temp",
                        "coolant temp", "ect", "water temperature", "water temp"))
                    coolantType = type;

                if (oilTempType < 0 && matches(name,
                        "oil temperature", "engine oil temperature", "oil temp",
                        "engine oil temp", "eot"))
                    oilTempType = type;

                if (oilPressureType < 0 && matches(name,
                        "oil pressure", "engine oil pressure", "oil press", "eop"))
                    oilPressureType = type;

                if (throttleType < 0 && matches(name,
                        "throttle position", "throttle angle", "throttle",
                        "accelerator position", "accel position", "pedal position"))
                    throttleType = type;

                if (rpmType < 0 && matches(name,
                        "rpm", "engine rpm", "engine speed", "engine rev"))
                    rpmType = type;

                if (boostType < 0 && matches(name,
                        "boost", "turbo boost", "boost pressure", "charge pressure"))
                    boostType = type;

                if (iatType < 0 && matches(name,
                        "iat", "intake air temperature", "intake air temp",
                        "charge air temperature", "charge air temp"))
                    iatType = type;

                if (voltageType < 0 && matches(name,
                        "battery voltage", "system voltage", "charging voltage",
                        "vehicle voltage", "alternator voltage"))
                    voltageType = type;

                if (transmissionTempType < 0 && matches(name,
                        "transmission temperature", "transmission temp",
                        "trans temp", "atf temperature", "atf temp",
                        "transmission oil temperature"))
                    transmissionTempType = type;

                if (fuelType < 0 && matches(name,
                        "fuel level", "fuel percent", "fuel percentage",
                        "fuel tank level", "fuel remaining"))
                    fuelType = type;

                if (driveModeType < 0 && matches(name,
                        "drive mode", "driving mode", "driver mode",
                        "infiniti mode", "drive selector"))
                    driveModeType = type;

                if (mapType < 0 && matches(name,
                        "map", "manifold absolute pressure", "manifold pressure"))
                    mapType = type;

                /*
                 * Only use the legacy type IDs when the source itself looks
                 * automotive. This avoids confusing standard Android sensor
                 * IDs with Q50 engine data.
                 */
                if (automotive) {
                    if (coolantType < 0 && type == LEGACY_COOLANT_TYPE)
                        coolantType = type;
                    if (oilTempType < 0 && type == LEGACY_OIL_TEMP_TYPE)
                        oilTempType = type;
                    if (oilPressureType < 0 && type == LEGACY_OIL_PRESSURE_TYPE)
                        oilPressureType = type;
                    if (speedType < 0 && type == LEGACY_SPEED_TYPE)
                        speedType = type;
                    if (throttleType < 0 && type == LEGACY_THROTTLE_TYPE)
                        throttleType = type;
                }

                Log.i(TAG, "SENSOR type=" + type
                        + " name=\"" + safe(s.getName()) + "\""
                        + " vendor=\"" + safe(s.getVendor()) + "\""
                        + " stringType=\"" + safe(s.getStringType()) + "\""
                        + " automotive=" + automotive
                        + " range=" + s.getMaximumRange()
                        + " resolution=" + s.getResolution()
                        + " delay=" + s.getMinDelay());
            }

            discovered = true;

            Log.i(TAG, "DISCOVERY complete sensors=" + sensors.size()
                    + " vehicleSignals=" + vehicleSignalCount
                    + " speedType=" + speedType
                    + " coolantType=" + coolantType
                    + " oilTempType=" + oilTempType
                    + " oilPressureType=" + oilPressureType
                    + " rpmType=" + rpmType
                    + " throttleType=" + throttleType
                    + " boostType=" + boostType
                    + " iatType=" + iatType
                    + " voltageType=" + voltageType
                    + " transmissionTempType=" + transmissionTempType
                    + " fuelType=" + fuelType
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
                if (sm.registerListener(listener, s, 100000)) {
                    registeredCount++;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Unable to register " + safe(s.getName()), t);
            }
        }

        data.live = registeredCount > 0 && vehicleSignalCount > 0;

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
        if (e == null || e.sensor == null || e.values == null || e.values.length == 0) {
            return;
        }

        int type = e.sensor.getType();
        float value = e.values[0];

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

    public String getStatus() {
        if (registeredCount == 0) return "NO LIVE SENSOR SOURCES";
        if (vehicleSignalCount == 0) return "NO VEHICLE SIGNALS EXPOSED";
        return "LIVE VEHICLE SOURCES " + vehicleSignalCount
                + " • REGISTERED " + registeredCount + " • 10 HZ";
    }

    public String getInventorySummary() {
        StringBuilder out = new StringBuilder();
        out.append("RAINWX VEHICLE DATA\n");
        out.append("Sensors: ").append(sensors.size()).append('\n');
        out.append("Vehicle signals: ").append(vehicleSignalCount).append('\n');
        out.append("Registered: ").append(registeredCount).append('\n');
        out.append("Speed: ").append(typeName(speedType)).append('\n');
        out.append("Coolant: ").append(typeName(coolantType)).append('\n');
        out.append("Oil Temp: ").append(typeName(oilTempType)).append('\n');
        out.append("Oil Pressure: ").append(typeName(oilPressureType)).append('\n');
        out.append("RPM: ").append(typeName(rpmType)).append('\n');
        out.append("Throttle: ").append(typeName(throttleType)).append('\n');
        out.append("Boost: ").append(typeName(boostType)).append('\n');
        out.append("IAT: ").append(typeName(iatType)).append('\n');
        out.append("Voltage: ").append(typeName(voltageType)).append('\n');
        out.append("Transmission Temp: ").append(typeName(transmissionTempType)).append('\n');
        out.append("Fuel: ").append(typeName(fuelType)).append('\n');
        out.append("Drive Mode: ").append(typeName(driveModeType)).append('\n');
        out.append("MAP: ").append(typeName(mapType)).append('\n');
        return out.toString();
    }

    private void normalize(int type, float value, String sensorName) {
        String name = safe(sensorName).toLowerCase(Locale.US);

        if (type == speedType || matches(name,
                "vehicle speed", "road speed", "wheel speed", "speed vehicle")) {
            data.speedRaw = normalizeSpeed(value, name);
            data.hasSpeed = finite(data.speedRaw);
        }

        if (type == coolantType || matches(name,
                "coolant temperature", "engine coolant", "coolant temp",
                "water temperature", "water temp", "ect")) {
            data.coolantC = normalizeTemperatureC(value, name);
            data.hasCoolant = finite(data.coolantC);
        }

        if (type == oilTempType || matches(name,
                "oil temperature", "engine oil temperature", "oil temp", "eot")) {
            data.oilTempC = normalizeTemperatureC(value, name);
            data.hasOilTemp = finite(data.oilTempC);
        }

        if (type == oilPressureType || matches(name,
                "oil pressure", "engine oil pressure", "oil press", "eop")) {
            data.oilPressurePsi = normalizePressurePsi(value, name);
            data.hasOilPressure = finite(data.oilPressurePsi);
        }

        if (type == throttleType || matches(name,
                "throttle position", "throttle angle", "throttle",
                "accelerator position", "accel position", "pedal position")) {
            data.throttleRaw = normalizePercent(value, name);
            data.hasThrottle = finite(data.throttleRaw);
        }

        if (type == rpmType || matches(name, "rpm", "engine rpm", "engine speed", "engine rev")) {
            data.rpm = value;
            data.hasRpm = finite(value);
        }

        if (type == boostType || matches(name,
                "boost", "turbo boost", "boost pressure", "charge pressure")) {
            data.boostPsi = normalizePressurePsi(value, name);
            data.hasBoost = finite(data.boostPsi);
        }

        if (type == iatType || matches(name,
                "iat", "intake air temperature", "intake air temp",
                "charge air temperature", "charge air temp")) {
            data.iatC = normalizeTemperatureC(value, name);
            data.hasIat = finite(data.iatC);
        }

        if (type == voltageType || matches(name,
                "battery voltage", "system voltage", "charging voltage",
                "vehicle voltage", "alternator voltage")) {
            data.voltage = normalizeVoltage(value, name);
            data.hasVoltage = finite(data.voltage);
        }

        if (type == transmissionTempType || matches(name,
                "transmission temperature", "transmission temp",
                "trans temp", "atf temperature", "atf temp",
                "transmission oil temperature")) {
            data.transmissionTempC = normalizeTemperatureC(value, name);
            data.hasTransmissionTemp = finite(data.transmissionTempC);
        }

        if (type == fuelType || matches(name,
                "fuel level", "fuel percent", "fuel percentage",
                "fuel tank level", "fuel remaining")) {
            data.fuelPercent = normalizePercent(value, name);
            data.hasFuel = finite(data.fuelPercent);
        }

        if (type == driveModeType || matches(name,
                "drive mode", "driving mode", "driver mode",
                "infiniti mode", "drive selector")) {
            data.driveModeRaw = value;
            data.hasDriveMode = finite(value);
        }

        if (type == mapType || matches(name,
                "map", "manifold absolute pressure", "manifold pressure")) {
            data.mapKpa = normalizePressureKpa(value, name);
            data.hasMap = finite(data.mapKpa);
        }

        /*
         * Boost fallback is deliberately conservative: only an explicitly
         * identified MAP/manifold signal may be converted into boost.
         */
        if (!data.hasBoost && data.hasMap) {
            float boostPsi = (data.mapKpa - 101.325f) * 0.1450377f;
            if (finite(boostPsi)) {
                data.boostPsi = boostPsi;
                data.hasBoost = true;
            }
        }
    }

    private boolean isVehicleSignal(int type, String name, boolean automotive) {
        if (automotive) return true;

        return matches(name,
                "vehicle speed", "coolant", "oil", "throttle", "rpm",
                "boost", "turbo", "intake air", "iat", "battery voltage",
                "system voltage", "transmission", "fuel level", "drive mode",
                "manifold", "map");
    }

    private float normalizeSpeed(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "mph", "mi/h")) return value / 0.621371f;
        if (containsAny(name, "kph", "km/h", "kmh")) return value;
        return value;
    }

    private float normalizeTemperatureC(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "fahrenheit", "°f", "deg f")) {
            return (value - 32f) * 5f / 9f;
        }
        return value;
    }

    private float normalizePressurePsi(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "kpa")) return value * 0.1450377f;
        if (containsAny(name, "bar")) return value * 14.5038f;
        return value;
    }

    private float normalizePressureKpa(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "psi")) return value * 6.894757f;
        if (containsAny(name, "bar")) return value * 100f;
        return value;
    }

    private float normalizePercent(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "fraction", "ratio")) return clamp(value * 100f, 0f, 100f);
        if (value >= 0f && value <= 1.01f) return value * 100f;
        return clamp(value, 0f, 100f);
    }

    private float normalizeVoltage(float value, String name) {
        if (!finite(value)) return Float.NaN;
        if (containsAny(name, "millivolt", "mv")) return value / 1000f;
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

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
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

    private static boolean matches(String s, String... terms) {
        return containsAny(s, terms);
    }
}
