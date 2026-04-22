package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.HunterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mobの属性登録や、その他Mod全体に関わるイベントを処理するクラス。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {

    // エンティティの属性（体力、移動速度など）を登録するイベント
    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(ModEntitieType.MIMIC.get(), MimicEntity.createAttributes().build());
        event.put(ModEntitieType.HUNTER.get(), HunterEntity.createAttributes().build());



    }
}