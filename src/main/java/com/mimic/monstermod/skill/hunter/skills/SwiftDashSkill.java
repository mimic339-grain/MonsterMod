package com.mimic.monstermod.skill.hunter.skills;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

public class SwiftDashSkill extends HunterSkill {
    private static final String STATE_KEY = "SWIFT_DASHING";

    @Override public SkillId getId() { return SkillId.of("monstermod", "hunter_swift_dash"); }
    @Override public String getName() { return "瞬塵"; }
    @Override public String getDescription() { return "向いている方向に鋭く平行移動する。間合いを一瞬で支配する。"; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation("minecraft", "textures/item/feather.png"); }

    @Override public SkillType.Category getCategory() { return SkillType.Category.DASH; }
    @Override public EnumSet<HunterSkillSlot> getAllowedSlots() { return EnumSet.of(HunterSkillSlot.DODGE, HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3); }
    @Override public WeaponCategory getRequiredWeapon() { return null; }
    @Override public SheathState getAllowedState() { return SheathState.BOTH; }

    @Override public int getCooldownTicks() { return 240; } // 12秒
    @Override
    public com.mimic.monstermod.skill.SkillLead toLead() {
        return new com.mimic.monstermod.skill.SkillLead.Builder(getId())
                .category(getCategory())
                .attackType(com.mimic.monstermod.skill.SkillType.MOVEMENT)
                .totalPreviewTicks(getPreTicks())
                .effectTicks(getActiveTicks())
                .recoveryTicks(getPostTicks())
                .autoRoot(false)
                .canMoveDuringEffect(true)
                .build();
    }

    @Override
    public void applyEffect(ServerPlayer player) {
        // サーバー側でフラグを立てる
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
            cap.setState(STATE_KEY, true);
        });

        // 初速を与える (8マス程度飛ばすための強めのベクトル)
        Vec3 look = player.getLookAngle();
        Vec3 movement = new Vec3(look.x, 0, look.z).normalize().scale(1.5);
        player.setDeltaMovement(movement.x, 0, movement.z);
        player.hurtMarked = true;
    }

    @Override
    public void onEffectEnd(ServerPlayer player) {
        // 効果終了時にフラグを折る
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
            cap.setState(STATE_KEY, false);
        });

        // 急ブレーキをかけて位置を安定させる
        player.setDeltaMovement(player.getDeltaMovement().scale(0.2));
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
                    // 1. 高速移動を維持（毎ティック速度を上書きして減速を防ぐ）
                    Vec3 look = player.getLookAngle();
                    Vec3 dashVel = new Vec3(look.x, 0, look.z).normalize().scale(1.6);
                    player.setDeltaMovement(dashVel.x, 0, dashVel.z);

                    // 2. 演出：残像のような煙パーティクル
                    for (int i = 0; i < 2; i++) {
                        player.level().addParticle(ParticleTypes.POOF,
                                player.getX() + (Math.random() - 0.5) * 0.5,
                                player.getY() + 0.5,
                                player.getZ() + (Math.random() - 0.5) * 0.5,
                                0, 0, 0);
                    }
                }
            });
        }
    }
}