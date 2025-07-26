package net.mimic.monstermod.common.capabilities;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.config.ModConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MorphCapabilityAttacher {
    public static final Capability<IMorphCapability> MORPH_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation MORPH_CAP_RL = new ResourceLocation(MonsterMod.MOD_ID, "morph");

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IMorphCapability.class);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(MORPH_CAPABILITY).isPresent()) {
                event.addCapability(MORPH_CAP_RL, new MorphCapabilityProvider());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(MORPH_CAPABILITY).ifPresent(oldCap -> {
                event.getEntity().getCapability(MORPH_CAPABILITY).ifPresent(newCap -> {
                    newCap.deserializeNBT(oldCap.serializeNBT());
                    if (ModConfig.COMMON.revokeIdentityOnDeath.get()) {
                        newCap.unmorph(event.getEntity());
                    }
                });
            });
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().getCapability(MORPH_CAPABILITY).ifPresent(cap -> {
            if (cap.getMorphEntityTypeId() != null) {
                cap.morphInto(cap.getMorphEntityTypeId(), event.getEntity());
            }
        });
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().getCapability(MORPH_CAPABILITY).ifPresent(cap -> {
            if (cap.getMorphEntityTypeId() != null) {
                cap.morphInto(cap.getMorphEntityTypeId(), event.getEntity());
            }
        });
    }
}