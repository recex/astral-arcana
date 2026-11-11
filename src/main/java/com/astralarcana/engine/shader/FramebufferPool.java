package com.astralarcana.engine.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/** Cache de render targets nomeados. Redimensiona quando preciso e libera tudo ao sair do mundo/recarregar. */
public final class FramebufferPool {
    private FramebufferPool() {}

    private static final Map<String, RenderTarget> TARGETS = new HashMap<>();

    public static RenderTarget acquire(String key, int w, int h, boolean depth) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        RenderTarget t = TARGETS.get(key);
        if (t == null || t.useDepth != depth) {
            if (t != null) t.destroyBuffers();
            t = new TextureTarget(w, h, depth, Minecraft.ON_OSX);
            TARGETS.put(key, t);
        } else if (t.width != w || t.height != h) {
            t.resize(w, h, Minecraft.ON_OSX);
        }
        t.setFilterMode(9729);
        t.setClearColor(0f, 0f, 0f, 0f);
        return t;
    }

    /** Libera todos os buffers de GPU (deve rodar na thread de render). */
    public static void clear() {
        for (RenderTarget t : TARGETS.values()) t.destroyBuffers();
        TARGETS.clear();
    }

    public static long bytes() {
        long total = 0;
        for (RenderTarget t : TARGETS.values()) total += (long) t.width * t.height * (4L + (t.useDepth ? 4L : 0L));
        return total;
    }

    public static int count() {
        return TARGETS.size();
    }
}
