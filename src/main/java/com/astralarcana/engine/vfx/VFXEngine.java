package com.astralarcana.engine.vfx;

import com.astralarcana.engine.perf.PerformanceEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Gerencia efeitos geometricos (fitas, portais, campos, rachaduras...). Culling por frustum e limite por qualidade. */
public final class VFXEngine {
    private VFXEngine() {}

    private static final List<VfxEffect> EFFECTS = new ArrayList<>();
    private static final VfxEffect.Ctx CTX = new VfxEffect.Ctx();
    private static int culled;

    public static void add(VfxEffect e) {
        int cap = 24 + PerformanceEngine.quality().ordinal() * 16;
        if (EFFECTS.size() >= cap) EFFECTS.remove(0);
        EFFECTS.add(e);
    }

    public static boolean active() {
        return !EFFECTS.isEmpty();
    }

    public static int count() {
        return EFFECTS.size();
    }

    public static int culledLastFrame() {
        return culled;
    }

    public static void tick() {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null) {
            EFFECTS.clear();
            return;
        }
        for (VfxEffect e : EFFECTS) {
            e.age++;
            e.tick(l);
        }
        EFFECTS.removeIf(VfxEffect::isDone);
    }

    public static void clear() {
        EFFECTS.clear();
    }

    public static void render(PoseStack ps, VertexConsumer vc, Vec3 cam, float pt, Frustum frustum) {
        culled = 0;
        CTX.ps = ps;
        CTX.vc = vc;
        CTX.cam = cam;
        CTX.pt = pt;
        for (VfxEffect e : EFFECTS) {
            if (!PerformanceEngine.visible(frustum, e.bounds())) {
                culled++;
                continue;
            }
            e.render(CTX);
        }
    }
}
