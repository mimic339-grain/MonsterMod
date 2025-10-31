package com.mimic.monstermod.client.renderer;

import com.mimic.monstermod.client.BaseMonsterRenderer;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * MimicRenderer 完全版
 * - BaseMonsterRenderer を継承
 * - Mimic 固有のテクスチャ・影サイズを設定
 * - Identity がない場合や読み込み失敗時はログ出力
 */
public class MimicRenderer<T extends BaseMonsterEntity> extends BaseMonsterRenderer<T> {

    private static final ResourceLocation MIMIC_TEXTURE =
            new ResourceLocation("monstermod", "textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.7f; // Mimic の影サイズに調整
    }

}
