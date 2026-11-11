package com.astralarcana.engine.anim;

import java.util.ArrayList;
import java.util.List;

/** Curva por keyframes: cada chave tem tempo, valor e o easing usado para CHEGAR nela. */
public final class Track {
    private final List<float[]> keys = new ArrayList<>();
    private final List<Easing> eases = new ArrayList<>();

    public Track key(float time, float value, Easing ease) {
        keys.add(new float[]{time, value});
        eases.add(ease);
        return this;
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    /** Antes da primeira chave vale 0; depois da ultima, mantem o ultimo valor. */
    public float eval(float time) {
        int n = keys.size();
        if (n == 0) return 0f;
        float[] first = keys.get(0);
        if (time < first[0]) return 0f;
        if (time <= first[0]) return first[1];
        for (int i = 1; i < n; i++) {
            float[] a = keys.get(i - 1), b = keys.get(i);
            if (time <= b[0]) {
                float span = b[0] - a[0];
                float t = span <= 1.0E-6f ? 1f : (time - a[0]) / span;
                float k = eases.get(i).apply(t);
                return a[1] + (b[1] - a[1]) * k;
            }
        }
        return keys.get(n - 1)[1];
    }

    public float endTime() {
        return keys.isEmpty() ? 0f : keys.get(keys.size() - 1)[0];
    }
}
