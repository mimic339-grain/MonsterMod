package com.mimic.monstermod.entity;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.monster.MimicEntity;
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

    private static final Map<ResourceLocation, EntityType<? extends BaseMonsterEntity>> ENTITY_MAP = new HashMap<>();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MonsterMod.MOD_ID);

    public static final RegistryObject<EntityType<MimicEntity>> MIMIC =
            ENTITY_TYPES.register("mimic",
                    () -> EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 0.7f)
                            .build(MonsterMod.MOD_ID + ":mimic")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    /**
     * ResourceLocation から EntityType を返す汎用メソッド
     */
    @SuppressWarnings("unchecked")
    public static EntityType<? extends BaseMonsterEntity> getEntityType(ResourceLocation id) {
        // 最初の呼び出し時に RegistryObject を Map に登録
        if (ENTITY_MAP.isEmpty()) {
            ENTITY_MAP.put(MIMIC.getId(), MIMIC.get());
            // 他のモンスターも同様に追加
            // ENTITY_MAP.put(PRO_HERO.getId(), PRO_HERO.get());
        }

        return ENTITY_MAP.get(id);
    }
}
