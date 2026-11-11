package com.astralarcana.engine.core;

import com.astralarcana.engine.cinematic.CinematicEngine;
import com.astralarcana.engine.cinematic.Sequence;
import com.astralarcana.engine.cinematic.Sequences;
import com.astralarcana.engine.particle.ParticleSystem;
import com.astralarcana.engine.perf.PerformanceEngine;
import com.astralarcana.engine.shader.FramebufferPool;
import com.astralarcana.engine.shader.PostPipeline;
import com.astralarcana.engine.shader.ShaderEngine;
import com.astralarcana.engine.vfx.VFXEngine;
import com.astralarcana.engine.world.WorldVisualEngine;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Comandos de cliente: /astral stats | shaders | reload | quality <nivel> | cinematic <nome>. */
public final class EngineCommands {
    private EngineCommands() {}

    private static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(s));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("astral")
                .then(Commands.literal("stats").executes(c -> {
                    msg("[Astral] " + PerformanceEngine.stats());
                    msg("[Astral] particulas/tick=" + ParticleSystem.usedLastTick() + " vfx=" + VFXEngine.count()
                            + " (culled=" + VFXEngine.culledLastFrame() + ") " + WorldVisualEngine.stats()
                            + " cinematicas=" + CinematicEngine.running());
                    msg("[Astral] framebuffers=" + FramebufferPool.count() + " (" + (FramebufferPool.bytes() / 1024) + " KiB) frames de pos=" + PostPipeline.frames());
                    return 1;
                }))
                .then(Commands.literal("shaders").executes(c -> {
                    msg("[Astral] " + ShaderEngine.status());
                    ShaderEngine.errors().forEach((k, v) -> msg("[Astral] erro em " + k + ": " + v));
                    return 1;
                }))
                .then(Commands.literal("reload").executes(c -> {
                    msg("[Astral] recarregando recursos e shaders...");
                    Minecraft.getInstance().reloadResourcePacks();
                    return 1;
                }))
                .then(Commands.literal("quality").then(Commands.argument("level", StringArgumentType.word()).executes(c -> {
                    String s = StringArgumentType.getString(c, "level").toUpperCase(Locale.ROOT);
                    try {
                        Quality q = Quality.valueOf(s);
                        EngineConfig.QUALITY.set(q);
                        EngineConfig.QUALITY.save();
                        Settings.quality = q;
                        PerformanceEngine.force(q);
                        msg("[Astral] qualidade: " + q);
                    } catch (IllegalArgumentException ex) {
                        msg("[Astral] use: auto, low, medium, high ou ultra");
                    }
                    return 1;
                })))
                .then(Commands.literal("cinematic").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    String n = StringArgumentType.getString(c, "name");
                    Sequence seq = Sequences.get(n);
                    Minecraft mc = Minecraft.getInstance();
                    if (seq == null || mc.player == null) {
                        msg("[Astral] sequencias: " + String.join(", ", Sequences.names()));
                        return 0;
                    }
                    Vec3 o = mc.player.position().add(mc.player.getLookAngle().scale(8)).add(0, 1, 0);
                    CinematicEngine.play(seq, o, 1f);
                    return 1;
                }))));
    }
}
