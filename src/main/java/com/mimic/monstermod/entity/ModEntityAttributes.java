package com.mimic.monstermod.entity;

import com.mimic.monstermod.MonsterMod;
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




    }

    // ★追加: プレイヤーの変身中にMimicの攻撃力などを適用する例
    // このイベントハンドラは、LivingEntityMixinやPlayerMixinで属性を動的に変更する代わりに、
    // 攻撃イベントが発生した際にのみMimicの攻撃力を適用する例です。
    /*
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();
                    if (currentIdentity != null && currentIdentity.equals(PlayerIdentityRegistry.MIMIC_IDENTITY.get())) {
                        // Mimicの攻撃力を適用
                        float mimicAttackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE); // Mimicの属性値を取得
                        event.setAmount(mimicAttackDamage); // イベントのダメージ量をMimicのものに設定
                    }
                }
            });
        }
    }
    */
}