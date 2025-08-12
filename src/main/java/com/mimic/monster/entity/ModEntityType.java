package com.mimic.monster.entity;

import com.mimic.monster.entity.monster.Mimic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(
        bus = Bus.MOD
)
//EntityTypeを登録する
public class ModEntityType {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;
    public static final RegistryObject<EntityType<Mimic>> MIMIC;

    // 登録メソッド
    private static <T extends Entity> RegistryObject<EntityType<T>> register(String key, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(key, () -> builder.build(key));
    }

    // Mod起動時に呼ばれる、登録を行うためのメソッド
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    static {
        ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "monstermod");
        //EntityTypeを登録するためのRegistryObject
        MIMIC = register("mimic", Builder.of(Mimic::new, MobCategory.MISC).sized(0.8F, 1.8F));
    }
}
