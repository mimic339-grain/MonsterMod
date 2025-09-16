package net.mimic.monstermod.entity;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MonsterMod.MOD_ID);

    // MimicEntity の登録
    public static final RegistryObject<EntityType<MimicEntity>> MIMIC =
            ENTITY_TYPES.register("mimic",
                    () -> EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER)
                            .sized(0.7f, 0.7f)
                            .build(MonsterMod.MOD_ID + ":mimic")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
