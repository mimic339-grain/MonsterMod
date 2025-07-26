package net.mimic.monstermod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.client.render.PlayerMorphRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer; // PlayerRenderer をインポート
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerEntityRendererMixin {

    // PlayerRendererMixin は PlayerRenderer のインスタンスメソッドにアクセスできる
    // ただし、PlayerMorphRenderer は別の GeoEntityRenderer です。
    // ここで直接 PlayerMorphRenderer のインスタンスを生成するのは良くない設計です。
    // 理想的には、ClientSetup で PlayerMorphRenderer のインスタンスを生成し、
    // それをこの Mixin がアクセスできるような方法 (例: static フィールドへの代入) で提供する必要があります。
    //
    // 今回は、最もシンプルな「エラーを消す」ための仮の修正として、
    // PlayerMorphRenderer のインスタンスを一時的に作成する形にします。
    // 本番環境では、ClientSetupなどでPlayerMorphRendererのインスタンスを生成し、
    // それをここで利用するように変更してください。
    private PlayerMorphRenderer mimicRendererInstance; // インスタンスフィールドとして宣言

    @Inject(method = "render(net/minecraft/world/entity/player/Player;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void monsterMod_renderPlayer(Player player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        // 仮の変身チェック (NBTタグを使用)
        if (player.getPersistentData().getBoolean("IsMimicTransformed")) {
            // PlayerMorphRenderer のインスタンスがなければ生成
            // これは効率的ではないため、実際のModではClientSetupなどでインスタンスを管理すべきです。
            if (this.mimicRendererInstance == null) {
                // this を PlayerRenderer にキャストして getEntityRenderDispatcher().getContext() を呼び出す
                // ただし、Mixin の対象が PlayerRenderer なので、this は PlayerRenderer インスタンスです。
                this.mimicRendererInstance = new PlayerMorphRenderer(((PlayerRenderer)(Object)this).entityRenderDispatcher.getContext());
            }

            // 元のプレイヤーレンダリングをキャンセル
            ci.cancel();

            // 変身後のミミックレンダリングを呼び出す
            this.mimicRendererInstance.renderTransformedPlayer(player, partialTicks, poseStack, bufferSource, packedLight);
        }
    }
}