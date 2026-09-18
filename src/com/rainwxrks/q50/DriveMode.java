package com.rainwxrks.q50;

/**
 * Read-only decoder for a Drive Mode sensor exposed by the Q50 InTouch vehicle-data bridge.
 *
 * This class does not send CAN frames and does not command the factory ASC/BOSE module.
 */
public final class DriveMode {
    public static final int UNKNOWN = -1;
    public static final int STANDARD = 0;
    public static final int SPORT = 1;
    public static final int PERSONAL = 2;
    public static final int ECO = 3;
    public static final int SNOW = 4;
    public static final int SPORT_PLUS = 5;

    private DriveMode() {}

    public static int decode(float raw) {
        if (Float.isNaN(raw) || Float.isInfinite(raw)) return UNKNOWN;
        int n = Math.round(raw);
        if (Math.abs(raw - n) > 0.15f || n < 0 || n > 5) return UNKNOWN;
        return n;
    }

    public static String name(int mode) {
        switch (mode) {
            case STANDARD: return "STANDARD";
            case SPORT: return "SPORT";
            case PERSONAL: return "PERSONAL";
            case ECO: return "ECO";
            case SNOW: return "SNOW";
            case SPORT_PLUS: return "SPORT+";
            default: return "UNKNOWN";
        }
    }

    /** Gain for RainWxrks' optional synthetic note. Factory ASC is not modified. */
    public static float soundGain(int mode) {
        switch (mode) {
            case SPORT: return 1.20f;
            case SPORT_PLUS: return 1.30f;
            case ECO: return 0.70f;
            case SNOW: return 0.78f;
            case PERSONAL: return 1.00f;
            case STANDARD: return 0.88f;
            default: return 1.00f;
        }
    }
}
