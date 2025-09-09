package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.capability.PlayerTransformation.MonsterState;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonsterPlayerRenderer implements IPlayerIdentityRenderer<IPlayerIdentity> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<Player, Map<String, LivingEntity>> dummyMonsters = new ConcurrentHashMap<>();

    @Override
    public void render(IPlayerIdentity identity,
                       LivingEntity entity,
                       float entityYaw,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       MonsterState state) {

        if (!(entity instanceof Player player)) return;

        String monsterId = identity.getMonsterId();
        LivingEntity dummy = dummyMonsters
                .computeIfAbsent(player, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(monsterId, id -> identity.createDummy(player.level()));

        dummy.setPos(player.getX(), player.getY(), player.getZ());
        dummy.setYRot(player.getYRot());
        dummy.setXRot(player.getXRot());
        dummy.yHeadRot = player.yHeadRot;
        dummy.yBodyRot = player.yBodyRot;
        dummy.setYBodyRot(player.yBodyRot);
        dummy.setDeltaMovement(player.getDeltaMovement());

        //アニメーションや状態をダミーに反映
        identity.applyAnimation(dummy, state);
        //描画
        poseStack.pushPose();
        try {
            @SuppressWarnings("unchecked")
            EntityRenderer<LivingEntity> renderer =
                    (EntityRenderer<LivingEntity>) Minecraft.getInstance()
                            .getEntityRenderDispatcher()
                            .getRenderer(dummy);
            renderer.render(dummy, entityYaw, partialTicks, poseStack, buffer, packedLight);
        } finally {
            poseStack.popPose();
        }
    }
}
