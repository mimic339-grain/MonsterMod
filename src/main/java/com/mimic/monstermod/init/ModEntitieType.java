package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.entity.HunterEntity;
import com.mimic.monstermod.entity.monster.*;
import com.mimic.monstermod.entity.obj.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
public class ModEntitieType {

    private static final Map<ResourceLocation, EntityType<? extends BaseEntity>> ENTITY_MAP = new HashMap<>();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MonsterMod.MOD_ID);

    public static final RegistryObject<EntityType<MimicEntity>> MIMIC =
            ENTITY_TYPES.register("mimic",
                    () -> EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 0.5f) // MimicEntity側の寸法設定に合わせるのが吉
                            .build(MonsterMod.MOD_ID + ":mimic")
            );

    public static final RegistryObject<EntityType<YatagarasuEntity>> YATAGARASU =
            ENTITY_TYPES.register("yatagarasu",
                    () -> EntityType.Builder.of(YatagarasuEntity::new, MobCategory.MONSTER)
                            .sized(2.1f, 2.8f) // 八咫烏のモデル(33x45x33px)に合わせたサイズ
                            .build(MonsterMod.MOD_ID + ":yatagarasu")
            );

    public static final RegistryObject<EntityType<HunterEntity>> HUNTER =
            ENTITY_TYPES.register("hunter",
                    () -> EntityType.Builder.of(HunterEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .build(new ResourceLocation(MonsterMod.MOD_ID, "hunter").toString()));

    public static final RegistryObject<EntityType<OnibiEntity>> ONIBI =
            ENTITY_TYPES.register("onibi",
                    () -> EntityType.Builder.of(OnibiEntity::new, MobCategory.MISC)
                            .sized(0.7f, 0.7f) // 当たり判定のサイズ(AABB)
                            .setUpdateInterval(1) // 弾幕なので更新頻度を高く
                            .build(new ResourceLocation(MonsterMod.MOD_ID, "onibi").toString())
            );
    public static final RegistryObject<EntityType<SpiralOnibiEntity>> SPIRALONIBI =
            ENTITY_TYPES.register("spiralonibi",
                    () -> EntityType.Builder.of(SpiralOnibiEntity::new, MobCategory.MISC) // エラーが消えます
                            .sized(0.7f, 0.7f)
                            .setUpdateInterval(1)
                            .build(new ResourceLocation(MonsterMod.MOD_ID, "spiralonibi").toString())
            );
    public static final RegistryObject<EntityType<TornadoEntity>> TORNADO = ENTITY_TYPES.register("tornado", () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC).sized(1.0f, 1.0f).clientTrackingRange(128).updateInterval(3).build("tornado"));
    public static final RegistryObject<EntityType<BigTornadoEntity>> BIGTORNADO = ENTITY_TYPES.register("bigtornado", () -> EntityType.Builder.<BigTornadoEntity>of(BigTornadoEntity::new, MobCategory.MISC).sized(1.0f, 1.0f).clientTrackingRange(128).updateInterval(3).build("bigtornado"));
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    @SuppressWarnings("unchecked")
    public static EntityType<? extends BaseEntity> getEntityType(ResourceLocation id) {
        if (ENTITY_MAP.isEmpty()) {
            ENTITY_MAP.put(MIMIC.getId(), MIMIC.get());
            ENTITY_MAP.put(YATAGARASU.getId(), YATAGARASU.get()); // ★八咫烏を追加
            ENTITY_MAP.put(HUNTER.getId(), HUNTER.get());
            ENTITY_MAP.put(TORNADO.getId(), (EntityType<? extends BaseEntity>) (Object) TORNADO.get());
        }
        return ENTITY_MAP.get(id);
    }
}