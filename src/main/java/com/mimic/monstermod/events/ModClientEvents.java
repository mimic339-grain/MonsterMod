package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.init.ModItems;
import com.mimic.monstermod.item.BloodStoneItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** パーティクルの見た目(Provider)や、アイテムの見た目の切り替え条件を登録する場所 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        // 【停止中】パーティクル版の竜巻。VortexEntity に置き換えたため登録を外している。
        // 復活させる場合は TornadoParticle / ModParticles のコメントアウトも戻すこと。
        // event.registerSpriteSet(ModParticles.TORNADO.get(), TornadoParticle.Provider::new);
    }

    /**
     * 血石の見た目を「空 / 血入り」で切り替えるための判定を登録する。
     *
     * アイテムのモデル(blood_stone.json)側に
     * 「monstermod:filled が 1 なら別のモデルを使う」と書いてあり、
     * その 0/1 をここで決めている。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.BLOOD_STONE.get(),
                new ResourceLocation(MonsterMod.MOD_ID, "filled"),
                (stack, level, entity, seed) -> BloodStoneItem.isFilled(stack) ? 1.0F : 0.0F));
    }
}
