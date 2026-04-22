package com.mimic.monstermod.skill.hunter.skills;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

public class LeapJumpSkill extends HunterSkill {
    public static final String STATE_KEY = "FALL_PROTECT"; // 他からも参照できるようにpublicに

    @Override public SkillId getId() { return SkillId.of("monstermod", "hunter_leap_jump"); }
    @Override public String getName() { return "飛燕"; }
    @Override public String getDescription() { return "前方上空へ力強く跳躍し、着地まで落下ダメージを無効化する。"; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation("minecraft", "textures/item/rabbit_foot.png"); }

    @Override public SkillType.Category getCategory() { return SkillType.Category.DASH; }
    @Override public EnumSet<HunterSkillSlot> getAllowedSlots() { return EnumSet.of(HunterSkillSlot.DODGE, HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3); }
    @Override public WeaponCategory getRequiredWeapon() { return null; }
    @Override public SheathState getAllowedState() { return SheathState.BOTH; }
    @Override public int getCooldownTicks() { return 400; }

    @Override
    public void applyEffect(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize().scale(1.6);
        player.setDeltaMovement(horizontal.x, 1.2, horizontal.z);

        // サーバー側でフラグを立てる
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
            cap.setState(STATE_KEY, true);
        });

        player.fallDistance = 0;
        player.hurtMarked = true;
    }

    @Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
    public static class ClientHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY)) {
                    // クライアント側では「着地判定」が出るまで落下距離を0に保ち、
                    // 着地モーション（画面の揺れ）が発生しないようにする
                    if (!player.onGround()) {
                        player.fallDistance = 0;
                    }

                    // 演出
                    if (player.tickCount % 2 == 0) {
                        player.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                                player.getX(), player.getY(), player.getZ(), 0, 0.05, 0);
                    }
                }
            });
        }
    }
}