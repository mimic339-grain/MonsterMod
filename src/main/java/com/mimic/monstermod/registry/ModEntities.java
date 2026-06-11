package com.mimic.monstermod.registry;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.monster.MimicEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 全エンティティタイプを Forge Deferred Register で登録するクラス。
 *
 * 【使い方】
 *   1. MOD_ENTITIES.register(modEventBus) を MonsterMod#<init> で呼ぶ。
 *   2. 新しいエンティティを追加するときは register() を1行追加するだけ。
 *
 * 配置: com/mimic/monstermod/registry/ModEntities.java
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> MOD_ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MonsterMod.MOD_ID);

    public static final RegistryObject<EntityType<MimicEntity>> MIMIC =
            MOD_ENTITIES.register("mimic",
                    () -> EntityType.Builder.<MimicEntity>of(MimicEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 1.0f)          // 幅, 高さ（ブロック単位）
                            .clientTrackingRange(80)      // クライアント追跡距離
                            .updateInterval(3)            // tick間隔でクライアントに送信
                            .build("mimic"));

    // ── 今後追加するエンティティ ─────────────────────────────────
    // public static final RegistryObject<EntityType<DragonEntity>> DRAGON =
    //     MOD_ENTITIES.register("dragon", () -> EntityType.Builder...);
}