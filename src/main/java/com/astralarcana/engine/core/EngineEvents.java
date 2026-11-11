package com.astralarcana.engine.core;

import com.astralarcana.AstralArcana;
import com.astralarcana.engine.perf.PerformanceEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AstralArcana.MODID, value = Dist.CLIENT)
public final class EngineEvents {
    private EngineEvents() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) PerformanceEngine.frame();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        Engine.onLevelUnload();
    }

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent e) {
        EngineCommands.register(e.getDispatcher());
    }
}
