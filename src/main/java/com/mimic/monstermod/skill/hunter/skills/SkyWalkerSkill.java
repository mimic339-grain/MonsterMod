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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

public class SkyWalkerSkill extends HunterSkill {
    private static final String STATE_KEY = "SKY_WALKER";

    @Override public SkillId getId() { return SkillId.of("monstermod", "hunter_sky_walker"); }
    @Override public String getName() { return "天歩"; }
    @Override public String getDescription() { return "5秒間、重力を無視して空を駆ける。ただし攻撃や採掘は通常通り。"; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation("minecraft", "textures/item/elytra.png"); }

    @Override public SheathState getAllowedState() { return SheathState.BOTH; }
    @Override public SkillType.Category getCategory() { return SkillType.Category.DASH; }
    @Override public EnumSet<HunterSkillSlot> getAllowedSlots() { return EnumSet.of(HunterSkillSlot.DODGE, HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3); }
    @Override public WeaponCategory getRequiredWeapon() { return null; }

    @Override public int getCooldownTicks() { return 800; } // 40秒
    @Override public int getActiveTicks() { return 100; } // 5秒

    @Override
    public com.mimic.monstermod.skill.SkillLead toLead() {
        return new com.mimic.monstermod.skill.SkillLead.Builder(getId())
                .category(getCategory())
                .attackType(com.mimic.monstermod.skill.SkillType.MOVEMENT)
                .totalPreviewTicks(getPreTicks())
                .beforeRecoverTicks(0)
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

        // 即座に飛行許可を与える
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    @Override
    public void onEffectEnd(ServerPlayer player) {
        // フラグを折る
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
            cap.setState(STATE_KEY, false);
        });

        // クリエイティブやスペクテイターでなければ飛行を禁止する
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
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
                    // クライアント側で飛行状態を維持する
                    if (!player.getAbilities().flying) {
                        player.getAbilities().mayfly = true;
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                    }

                    // 演出：足元に雲のようなパーティクル
                    if (player.tickCount % 2 == 0) {
                        player.level().addParticle(ParticleTypes.CLOUD,
                                player.getX() + (Math.random() - 0.5) * 0.3,
                                player.getY() + 0.1,
                                player.getZ() + (Math.random() - 0.5) * 0.3,
                                0, -0.05, 0);
                    }
                }
            });
        }
    }
}