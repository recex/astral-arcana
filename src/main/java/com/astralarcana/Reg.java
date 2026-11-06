package com.astralarcana;

import com.astralarcana.entity.BlackHoleEntity;
import com.astralarcana.entity.CosmicSerpentEntity;
import com.astralarcana.entity.MeteorEntity;
import com.astralarcana.item.CosmicStaffItem;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class Reg {
    private Reg() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AstralArcana.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AstralArcana.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AstralArcana.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralArcana.MODID);

    public static final RegistryObject<Item> STAFF = ITEMS.register("cosmic_staff",
            () -> new CosmicStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<EntityType<MeteorEntity>> METEOR = ENTITIES.register("meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(2.0f, 2.0f).clientTrackingRange(16).updateInterval(1).fireImmune().build("meteor"));

    public static final RegistryObject<EntityType<BlackHoleEntity>> BLACK_HOLE = ENTITIES.register("black_hole",
            () -> EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(16).updateInterval(2).fireImmune().build("black_hole"));

    public static final RegistryObject<EntityType<CosmicSerpentEntity>> SERPENT = ENTITIES.register("cosmic_serpent",
            () -> EntityType.Builder.<CosmicSerpentEntity>of(CosmicSerpentEntity::new, MobCategory.MONSTER)
                    .sized(3.0f, 3.0f).clientTrackingRange(20).updateInterval(1).fireImmune().build("cosmic_serpent"));

    public static final RegistryObject<Item> SERPENT_EGG = ITEMS.register("cosmic_serpent_spawn_egg",
            () -> new ForgeSpawnEggItem(SERPENT, 0x14104a, 0x66e0ff, new Item.Properties()));

    public static final RegistryObject<SimpleParticleType> GLOW = PARTICLES.register("glow", () -> new SimpleParticleType(true));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.astralarcana"))
                    .icon(() -> new ItemStack(STAFF.get()))
                    .displayItems((params, out) -> {
                        out.accept(STAFF.get());
                        out.accept(SERPENT_EGG.get());
                    })
                    .build());
}
