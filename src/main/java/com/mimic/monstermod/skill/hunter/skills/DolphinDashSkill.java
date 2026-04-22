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
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;

public class DolphinDashSkill extends HunterSkill {
    public static final String STATE_KEY = "DOLPHIN_DASH";

    @Override public SkillId getId() { return SkillId.of("monstermod", "hunter_dolphin_dash"); }
    @Override public String getName() { return "海神の加護"; }
    @Override public String getDescription() { return "水中で驚異的な推進力を得て、視界も完全に鮮明になる。"; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation("minecraft", "textures/item/heart_of_the_sea.png"); }

    @Override public SkillType.Category getCategory() { return SkillType.Category.DASH; }
    @Override public EnumSet<HunterSkillSlot> getAllowedSlots() { return EnumSet.of(HunterSkillSlot.DODGE, HunterSkillSlot.SKILL_1, HunterSkillSlot.SKILL_2, HunterSkillSlot.SKILL_3); }
    @Override public WeaponCategory getRequiredWeapon() { return null; }
    @Override public SheathState getAllowedState() { return SheathState.BOTH; }
    @Override public int getCooldownTicks() { return 600; }
    @Override public int getActiveTicks() { return 300; }

    @Override
    public void applyEffect(ServerPlayer player) {
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> cap.setState(STATE_KEY, true));
    }

    @Override
    public void onEffectEnd(ServerPlayer player) {
        CapabilityRegistry.getPlayerData(player).ifPresent(cap -> cap.setState(STATE_KEY, false));
    }

    @Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT)
    public static class ClientHandler {

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY) && player.isInWater()) {

                    if (player.zza > 0) {
                        Vec3 look = player.getLookAngle();
                        double speedBoost = 0.4;
                        Vec3 motion = player.getDeltaMovement();
                        if (motion.length() < 2.5) {
                            player.setDeltaMovement(motion.add(look.x * speedBoost, look.y * speedBoost, look.z * speedBoost));
                        }
                    }

                    // 泡パーティクルを激しくする
                    if (player.tickCount % 1 == 0) {
                        player.level().addParticle(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0, player.getZ(), 0, 0, 0);
                    }
                }
            });
        }

        @SubscribeEvent
        public static void onRenderFog(ViewportEvent.RenderFog event) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY) && player.isInWater()) {
                    event.setFarPlaneDistance(2000.0F); // 霧をさらに遠くへ
                    event.setNearPlaneDistance(1900.0F);
                    event.setCanceled(true);
                }
            });
        }

        // 【暗視の再現】ライトマップの明るさを最大化する
        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            CapabilityRegistry.getPlayerData(mc.player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY) && mc.player.isInWater()) {
                    // バニラの暗視がやっている「明るさテクスチャの更新」を強制
                    // これでポーションなしでも画面がパッと明るくなります
                    mc.gameRenderer.lightTexture().tick();
                }
            });
        }

        // 速度感が出るように視野角(FOV)を少し広げる
        @SubscribeEvent
        public static void onFOVUpdate(ComputeFovModifierEvent event) {
            Player player = event.getPlayer();
            CapabilityRegistry.getPlayerData(player).ifPresent(cap -> {
                if (cap.hasState(STATE_KEY) && player.isInWater() && player.zza > 0) {
                    event.setNewFovModifier(event.getFovModifier() * 1.2F);
                }
            });
        }
    }
}