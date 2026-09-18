package com.rainwxrks.q50;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Lightweight synthesized VR30-style exhaust note for the head-unit speakers.
 * It follows live RPM and throttle when those signals are exposed to the app.
 * This does not send commands to the vehicle or change the physical exhaust.
 */
public final class ExhaustSound {
    private static final int SAMPLE_RATE = 22050;
    private static final int BUFFER_SAMPLES = 1102; // ~50 ms
    private final short[] buffer = new short[BUFFER_SAMPLES];
    private final Object lock = new Object();
    private AudioTrack track;
    private Thread thread;
    private volatile boolean running;
    private volatile boolean enabled;
    private volatile float rpm;
    private volatile float throttle;
    private volatile float modeGain = 1.0f;
    private double phase;
    private double noiseState;

    public ExhaustSound() {
        try {
            int min = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int size = Math.max(min, BUFFER_SAMPLES * 2);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, size,
                    AudioTrack.MODE_STREAM);
        } catch (Throwable ignored) {
            track = null;
        }
    }

    public void setEnabled(boolean on) {
        enabled = on;
        if (on) {
            start();
        } else {
            stopThread();
        }
    }

    public void setRpm(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return;
        rpm = clamp(value, 0f, 8000f);
    }

    public void setModeGain(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return;
        modeGain = clamp(value, 0.5f, 1.5f);
    }

    public void setThrottle(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return;
        float t = value <= 1.5f ? value * 100f : value;
        throttle = clamp(t, 0f, 100f);
    }

    private void start() {
        if (track == null || running) return;
        running = true;
        thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    track.play();
                    while (running) {
                        render();
                        track.write(buffer, 0, buffer.length);
                    }
                } catch (Throwable ignored) {
                } finally {
                    try { track.pause(); } catch (Throwable ignored) {}
                }
            }
        }, "RainExhaustSound");
        thread.setDaemon(true);
        thread.start();
    }

    private void render() {
        float r = rpm;
        float t = throttle / 100f;
        // V6 four-stroke firing fundamental: RPM/60 * 6/2 = RPM/20.
        double fundamental = Math.max(38.0, r / 20.0);
        // Keep idle/off-road silence natural while retaining a small idle rumble.
        float drive = r < 450f ? 0.18f : (0.24f + 0.76f * t);
        float volume = enabled ? drive * modeGain : 0f;
        if (r < 250f) volume = 0f;

        for (int i = 0; i < buffer.length; i++) {
            double x = phase;
            double s = 0.0;
            s += Math.sin(x) * 0.48;
            s += Math.sin(x * 2.0 + 0.25) * 0.22;
            s += Math.sin(x * 3.0 + 0.55) * 0.13;
            s += Math.sin(x * 5.0 + 1.10) * 0.08;
            s += Math.sin(x * 7.0 + 1.70) * 0.04;

            // Light deterministic exhaust texture, stronger under throttle.
            noiseState = noiseState * 0.985 + ((Math.sin((phase + i) * 0.17) * 2.0) - 1.0) * 0.015;
            s += noiseState * (0.35 * t);

            float sample = (float)(s * volume * 0.42);
            sample = (float)Math.tanh(sample * 1.8);
            buffer[i] = (short)(sample * 32767f);

            phase += 2.0 * Math.PI * fundamental / SAMPLE_RATE;
            if (phase > Math.PI * 2.0) phase -= Math.PI * 2.0;
        }
    }

    private void stopThread() {
        running = false;
        Thread t = thread;
        if (t != null && t != Thread.currentThread()) {
            try { t.join(200); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        thread = null;
    }

    public void release() {
        stopThread();
        if (track != null) {
            try { track.stop(); } catch (Throwable ignored) {}
            try { track.release(); } catch (Throwable ignored) {}
            track = null;
        }
    }

    private static float clamp(float x, float lo, float hi) {
        return Math.max(lo, Math.min(hi, x));
    }
}
