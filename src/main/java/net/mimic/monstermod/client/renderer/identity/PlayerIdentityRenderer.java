package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod; // ★追加: ロガーのため
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 登録されたすべてのIPlayerIdentityRendererを管理し、適切なレンダラーを呼び出すためのハブ。
 * クライアントサイドでのみ動作します。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PlayerIdentityRenderer {
    private static final Map<Class<? extends IPlayerIdentity>, IPlayerIdentityRenderer<? extends IPlayerIdentity>> RENDERERS = new HashMap<>();

    /**
     * EntityRenderersEvent.RegisterRenderersイベントでレンダラーを登録します。
     * このメソッドはForgeのModイベントバスによって自動的に呼び出されます。
     */
    @SubscribeEvent
    public static void registerIdentityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // MimicIdentityのレンダラーを登録
        // EntityRendererProvider.ContextはregisterRenderersイベントから取得
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(
                Minecraft.getInstance().getEntityRenderDispatcher(),
                Minecraft.getInstance().getItemRenderer(),
                Minecraft.getInstance().getResourceManager(),
                Minecraft.getInstance().getEntityModels(),
                Minecraft.getInstance().font);

        RENDERERS.put(MimicIdentity.class, new MimicPlayerRenderer(context));

        MonsterMod.getLogger().debug("PlayerIdentityRenderer: {}個のIdentityRendererを登録しました。", RENDERERS.size());
    }

    /**
     * プレイヤーの現在の変身状態に基づいて、適切なIdentityRendererを呼び出し描画します。
     * @param identity 描画するIdentityインスタンス
     * @param entity 描画対象のLivingEntity（通常はプレイヤー）
     * @param entityYaw エンティティのY軸の回転
     * @param partialTicks 部分ティック
     * @param poseStack ポーズスタック
     * @param buffer バッファソース
     * @param packedLight パックされた光のデータ
     */
    public static void render(IPlayerIdentity identity, LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (identity == null) {
            MonsterMod.getLogger().warn("PlayerIdentityRenderer.renderがnullのidentityで呼び出されました。");
            return;
        }

        @SuppressWarnings("unchecked")
        IPlayerIdentityRenderer<IPlayerIdentity> renderer = (IPlayerIdentityRenderer<IPlayerIdentity>) RENDERERS.get(identity.getClass());

        if (renderer != null) {
            renderer.render(identity, entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        } else {
            MonsterMod.getLogger().warn("IdentityRendererが見つかりません: {}", identity.getClass().getName());
            // フォールバックとして、元のプレイヤーレンダリングを許可するか、エラーモデルを描画するなどを検討
        }
    }
}