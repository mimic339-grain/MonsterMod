package com.mimic.monstermod.command; // パッケージ名は任意

import com.mimic.monstermod.MonsterMod;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

// ★重要: Bus.MOD を指定しないと setup イベントを拾えません
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> RULE_DEATH_KEEP_TRANSFORM;
    public static GameRules.Key<GameRules.BooleanValue> RULE_DEATH_RESET_HP;

    @SubscribeEvent
    public static void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // ゲームルールを登録（これだけで /gamerule に追加される）
            RULE_DEATH_KEEP_TRANSFORM = GameRules.register("deathKeepTransform", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
            RULE_DEATH_RESET_HP = GameRules.register("deathResetHp", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
        });
    }
}