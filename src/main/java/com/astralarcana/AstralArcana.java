package com.astralarcana;

import com.astralarcana.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AstralArcana.MODID)
public class AstralArcana {
    public static final String MODID = "astralarcana";

    public AstralArcana() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Reg.ITEMS.register(bus);
        Reg.ENTITIES.register(bus);
        Reg.PARTICLES.register(bus);
        Reg.TABS.register(bus);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, com.astralarcana.engine.core.EngineConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
