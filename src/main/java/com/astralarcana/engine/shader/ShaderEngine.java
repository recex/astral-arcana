package com.astralarcana.engine.shader;

import com.astralarcana.AstralArcana;
import com.astralarcana.engine.core.Engine;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registro central de programas GLSL. Falha de compilacao NAO derruba o jogo: o erro fica registrado,
 * o programa fica indisponivel e o pipeline usa um fallback ou desliga o efeito.
 * Recarrega junto com os recursos (F3+T ou /astral reload).
 */
public final class ShaderEngine {
    private ShaderEngine() {}

    private static final Map<String, ShaderProgram> PROGRAMS = new HashMap<>();
    private static final Map<String, String> ERRORS = new TreeMap<>();

    public static void register(RegisterShadersEvent e, String name, VertexFormat format) {
        PROGRAMS.remove(name);
        ERRORS.remove(name);
        try {
            ShaderInstance si = new ShaderInstance(e.getResourceProvider(), AstralArcana.id(name), format);
            e.registerShader(si, inst -> PROGRAMS.put(name, new ShaderProgram(name, inst)));
        } catch (Exception ex) {
            ERRORS.put(name, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            Engine.LOG.error("[AstralEngine] Falha ao compilar o shader '{}': {}", name, ex.toString());
        }
    }

    public static ShaderProgram get(String name) {
        return PROGRAMS.get(name);
    }

    public static boolean anyReady() {
        return !PROGRAMS.isEmpty();
    }

    public static Map<String, String> errors() {
        return Collections.unmodifiableMap(ERRORS);
    }

    public static String status() {
        StringBuilder sb = new StringBuilder();
        for (String n : new String[]{"cinematic", "distort", "bloom_extract", "blur", "bloom_composite"}) {
            sb.append(n).append(PROGRAMS.containsKey(n) ? "=OK " : "=FALHOU ");
        }
        return sb.toString().trim();
    }

    /** Descarta caches de uniforms (os ShaderInstance novos chegam pelo RegisterShadersEvent). */
    public static void invalidate() {
        PROGRAMS.clear();
    }
}
