package com.astralarcana.engine.anim;

/** Curva de Bezier cubica 1D no estilo CSS cubic-bezier(x1, y1, x2, y2): mapeia t (tempo) para valor. */
public final class Bezier {
    public static final Bezier EASE = new Bezier(0.25f, 0.1f, 0.25f, 1f);
    public static final Bezier EASE_IN_OUT = new Bezier(0.42f, 0f, 0.58f, 1f);
    public static final Bezier SNAP = new Bezier(0.2f, 0.9f, 0.1f, 1f);
    public static final Bezier ANTICIPATE = new Bezier(0.6f, -0.3f, 0.7f, 1f);

    private final float x1, y1, x2, y2;

    public Bezier(float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    private static float coord(float a, float b, float s) {
        float u = 1f - s;
        return 3f * u * u * s * a + 3f * u * s * s * b + s * s * s;
    }

    private static float deriv(float a, float b, float s) {
        float u = 1f - s;
        return 3f * u * u * a + 6f * u * s * (b - a) + 3f * s * s * (1f - b);
    }

    /** Avalia a curva: encontra s tal que x(s) = t (Newton + bissecao) e devolve y(s). */
    public float apply(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float s = t;
        for (int i = 0; i < 8; i++) {
            float err = coord(x1, x2, s) - t;
            if (Math.abs(err) < 1.0E-5f) return coord(y1, y2, s);
            float d = deriv(x1, x2, s);
            if (Math.abs(d) < 1.0E-6f) break;
            s -= err / d;
        }
        float lo = 0f, hi = 1f;
        s = t;
        for (int i = 0; i < 20; i++) {
            float x = coord(x1, x2, s);
            if (Math.abs(x - t) < 1.0E-5f) break;
            if (x < t) lo = s; else hi = s;
            s = (lo + hi) * 0.5f;
        }
        return coord(y1, y2, s);
    }

    /** Bezier cubica escalar entre p0 e p3 com controles p1, p2 (util para trajetorias). */
    public static float cubic(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }
}
