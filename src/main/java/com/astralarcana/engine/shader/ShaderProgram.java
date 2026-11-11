package com.astralarcana.engine.shader;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Envolve um ShaderInstance com cache de uniforms. Uniforms inexistentes viram no-ops (nunca crasham). */
public final class ShaderProgram {
    public final String name;
    public final ShaderInstance inst;
    private final Map<String, AbstractUniform> cache = new HashMap<>();

    ShaderProgram(String name, ShaderInstance inst) {
        this.name = name;
        this.inst = inst;
    }

    private AbstractUniform u(String n) {
        AbstractUniform u = cache.get(n);
        if (u == null) {
            u = inst.safeGetUniform(n);
            cache.put(n, u);
        }
        return u;
    }

    public void bind() {
        RenderSystem.setShader(() -> inst);
    }

    public void set1(String n, float a) { u(n).set(a); }
    public void set2(String n, float a, float b) { u(n).set(a, b); }
    public void set3(String n, float a, float b, float c) { u(n).set(a, b, c); }
    public void set4(String n, float a, float b, float c, float d) { u(n).set(a, b, c, d); }
    public void setMat(String n, Matrix4f m) { u(n).set(m); }
    /** Matriz 4x4 em ordem de coluna (16 floats); usada para empacotar ate 4 vec4. */
    public void setMat(String n, float[] m16) { u(n).set(m16); }
}
