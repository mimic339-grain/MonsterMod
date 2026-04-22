package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.layer.VanillaChainLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LayerAddClient {

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // 1. プレイヤー
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);
            if (renderer != null) addVanillaLayer(renderer);
        }
/*現在entityに付与するつもりがないからコメントアウト　ちなみにパケットを送る必要がある　todo ただ変身したgeckolibmodelには表示しないといけない
        // 2. 全エンティティに対してより柔軟にチェック
        for (net.minecraft.world.entity.EntityType<?> type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES) {
            try {
                Object renderer = event.getEntityRenderer(type);

                // GeckoLib Mob (ミミック等) への登録
                if (renderer instanceof software.bernie.geckolib.renderer.GeoRenderer<?> geoRenderer) {
                    applyChainLayer(geoRenderer);
                }
                // バニラ形式 Mob への登録
                if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
                    addVanillaLayer(livingRenderer);
                }
            } catch (Exception e) {
                // 特殊エンティティ対策
            }
        }　*/
    }

    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity, M extends EntityModel<T>> void addVanillaLayer(LivingEntityRenderer<?, ?> renderer) {
        LivingEntityRenderer<T, M> vanillaRenderer = (LivingEntityRenderer<T, M>) renderer;
        vanillaRenderer.addLayer(new VanillaChainLayer<>(vanillaRenderer));
    }
}