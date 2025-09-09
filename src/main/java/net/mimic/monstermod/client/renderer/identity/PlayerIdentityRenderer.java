package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PlayerIdentityRenderer {

    private static final Map<Class<? extends IPlayerIdentity>, IPlayerIdentityRenderer<? extends IPlayerIdentity>> RENDERERS = new HashMap<>();

    @SubscribeEvent
    public static void registerIdentityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // MimicIdentity用のレンダラー登録（汎用Monster対応）
        RENDERERS.put(MimicIdentity.class, new MonsterPlayerRenderer());
        MonsterMod.getLogger().debug("PlayerIdentityRenderer: {}個のIdentityRendererを登録しました。", RENDERERS.size());
    }

    public static void render(IPlayerIdentity identity, LivingEntity entity,
                              float entityYaw, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight) {

        if (identity == null) {
            MonsterMod.getLogger().warn("PlayerIdentityRenderer.renderがnullのidentityで呼び出されました。");
            return;
        }
        //レンダラーを取得
        @SuppressWarnings("unchecked")
        IPlayerIdentityRenderer<IPlayerIdentity> renderer =
                (IPlayerIdentityRenderer<IPlayerIdentity>) RENDERERS.get(identity.getClass());

        if (renderer == null) {
            MonsterMod.getLogger().warn("IdentityRendererが見つかりません: {}", identity.getClass().getName());
            return;
        }

        //変身中ならアニメーション状態を描画
        PlayerTransformation.MonsterState state = new PlayerTransformation.MonsterState();
        if (entity instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed() && transformation.getTransformedMobId() != null) {
                    PlayerTransformation.MonsterState s = transformation.getMonsterState(transformation.getTransformedMobId());
                    if (s != null) state.animationState = s.animationState;
                }
            });
        }
        //描画処理
        renderer.render(identity, entity, entityYaw, partialTicks, poseStack, buffer, packedLight, state);
    }
}
