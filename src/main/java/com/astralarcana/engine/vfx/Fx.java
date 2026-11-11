package com.astralarcana.engine.vfx;

import com.astralarcana.client.ClientFx;
import net.minecraft.world.phys.Vec3;

/** Fachada publica usada pelas sequencias para acionar efeitos de tela (flash, distorcao, grade de cor...). */
public final class Fx {
    private Fx() {}

    public static final float[] WHITE = {1f, 1f, 1f};
    public static final float[] ORANGE = {1f, .55f, .15f};
    public static final float[] YELLOW = {1f, .9f, .5f};
    public static final float[] VIOLET = {.65f, .3f, 1f};
    public static final float[] CYAN = {.3f, .85f, 1f};
    public static final float[] SILVER = {.8f, .9f, 1f};
    public static final float[] PINK = {1f, .3f, .55f};

    public static void flash(float[] c, float a, float decay) { ClientFx.apiFlash(c, a, decay); }
    public static void tint(float[] c, float a) { ClientFx.apiTint(c, a); }
    public static void ripple(Vec3 p, float strength, int life) { ClientFx.apiRipple(p, strength, life); }
    public static void warp(float a) { ClientFx.apiWarp(a); }
    public static void glitch(float g) { ClientFx.apiGlitch(g); }
    public static void vignette(float v) { ClientFx.apiVignette(v); }
    public static void desat(float d) { ClientFx.apiDesat(d); }
    public static void fovPunch(float f) { ClientFx.apiFov(f); }
}
