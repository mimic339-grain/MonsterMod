package net.mimic.monstermod.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity; // Entity クラスをインポート
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // `event.getSource().getEntity()` が Entity 型を返すため、LivingEntity にキャストする前に instanceof で確認
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity) {
            LivingEntity killer = (LivingEntity) sourceEntity;
            // ここにキラー (`killer`) を使ったロジックを記述します。
        }
    }
}