package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PlayerRenderer Mixin 完全版（IdentityMod方式）
 *
 * - 変身中のプレイヤーは BaseMonsterIdentity に描画を完全委譲
 * - Player の回転・装備・姿勢は Tick 内で同期済み
 * - partialTicks 補間に対応
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    private static final Map<UUID, BaseMonsterIdentity> IDENTITY_CACHE = new HashMap<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderIdentity(AbstractClientPlayer player,
                                float entityYaw,
                                float partialTicks,
                                PoseStack poseStack,
                                MultiBufferSource buffer,
                                int packedLight,
                                CallbackInfo ci) {

        Level level = player.level(); // Level参照を取得

        // キャッシュからIdentityを取得または新規作成
        BaseMonsterIdentity identity = IDENTITY_CACHE.computeIfAbsent(
                player.getUUID(),
                uuid -> {
                    MimicEntity entity = new MimicEntity(ModEntitieType.MIMIC.get(), level);
                    return new BaseMonsterIdentity(entity, 0);
                }
        );

        // Mimicの状態を毎フレーム同期
        identity.copyFromPlayerClient(player);

        // レンダリング処理（プレイヤーの代わりにMimicを描画）
        identity.render(player, partialTicks, poseStack, buffer, packedLight);

        // 通常のPlayer描画をキャンセル
        ci.cancel();
    }
}
