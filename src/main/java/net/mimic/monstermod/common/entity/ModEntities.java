package net.mimic.monstermod.common.entity; // この行を確認/修正

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.common.entity.MimicEntity; // この行を確認/修正

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MonsterMod.MOD_ID);

    public static final RegistryObject<EntityType<MimicEntity>> MIMIC = ENTITIES.register("mimic",
            () -> EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.7F)
                    .build("mimic"));
}