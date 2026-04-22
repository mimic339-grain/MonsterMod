package com.mimic.monstermod.skill.hunter.skills;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.weapon.WeaponCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

public class HardeningSkill extends HunterSkill {
    private static final String STATE_KEY = "HARDENING_ACTIVE";

    @Override public SkillId getId() { return SkillId.of("monstermod", "hunter_hardening"); }
    @Override public String getName() { return "硬化"; }
    @Override public String getDescription() { return "5秒間、あらゆる衝撃を無効化する代わりに、一切の移動ができなくなる。"; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation("minecraft", "textures/item/iron_ingot.png"); }
    @Override public SkillType.Category getCategory() { return SkillType.Category.CANCEL; }
    @Override public EnumSet<HunterSkillSlot> getAllowedSlots() { return EnumSet.of(HunterSkillSlot.DODGE, HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3); }
    @Override public SheathState getAllowedState() { return SheathState.BOTH; }
    @Override public WeaponCategory getRequiredWeapon() { return null; }
    @Override public int getCooldownTicks() { return 1200; }
    @Override public int getActiveTicks() { return 100; }

    @Override
    public void applyEffect(ServerPlayer player) {
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
            cap.setState(STATE_KEY, true);
        });
        player.setInvulnerable(true);
    }

    @Override
    public void onEffectEnd(ServerPlayer player) {
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
            cap.setState(STATE_KEY, false);
        });
        player.setInvulnerable(false);
    }

    @Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
    public static class ClientHandler {
        @SubscribeEvent
        public static void onInputUpdate(MovementInputUpdateEvent event) {
            if (event.getEntity() instanceof LocalPlayer player) {
                // ここも ifPresent で安全に取り出す
                CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                    if (cap.hasState(STATE_KEY)) {
                        var input = event.getInput();
                        input.leftImpulse = 0;
                        input.forwardImpulse = 0;
                        input.up = input.down = input.left = input.right = false;
                        input.jumping = input.shiftKeyDown = false;
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onComputeFov(ViewportEvent.ComputeFov event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            // ifPresent を使う形に変更
            CapabilityRegistry.getPlayerData(mc.player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY)) {
                    event.setFOV(event.getFOV() * 0.7f);
                }
            });
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY)) {
                    // 慣性を殺す
                    player.setDeltaMovement(player.getDeltaMovement().x * 0.5, player.getDeltaMovement().y, player.getDeltaMovement().z * 0.5);

                    if (player.tickCount % 5 == 0) {
                        player.level().addParticle(net.minecraft.core.particles.ParticleTypes.CRIT,
                                player.getX() + (Math.random() - 0.5), player.getY() + (Math.random() * 2), player.getZ() + (Math.random() - 0.5), 0, 0, 0);
                    }
                }
            });
        }
    }
}