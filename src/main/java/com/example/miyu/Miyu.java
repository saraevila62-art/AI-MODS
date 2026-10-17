package com.example.miyu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Miyu.ID)
public class Miyu {
    public static final String ID = "miyu";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredHolder<EntityType<?>, EntityType<MiyuEntity>> MIYU = ENTITIES.register("miyu",
            () -> EntityType.Builder.of(MiyuEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f).build("miyu"));
    public static final DeferredItem<DeferredSpawnEggItem> EGG = ITEMS.register("miyu_spawn_egg",
            () -> new DeferredSpawnEggItem(MIYU, 0x7a2147, 0xff7aa2, new Item.Properties()));

    public Miyu(IEventBus bus, ModContainer container) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::tab);
        bus.addListener(this::payloads);
        container.registerConfig(ModConfig.Type.CLIENT, Cfg.SPEC);
    }
    void attributes(EntityAttributeCreationEvent e) { e.put(MIYU.get(), MiyuEntity.attrs().build()); }
    void tab(BuildCreativeModeTabContentsEvent e) { if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(EGG); }
    void payloads(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(CmdPayload.TYPE, CmdPayload.CODEC, CmdPayload::handle);
    }
}
