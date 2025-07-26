package net.mimic.monstermod.mixin;

import net.mimic.monstermod.common.capabilities.IMorphCapability;
import net.mimic.monstermod.common.capabilities.MorphCapabilityAttacher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> p_21021_, Level p_21022_) {
        super(p_21021_, p_21022_);
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void monstermod_getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        this.getCapability(MorphCapabilityAttacher.MORPH_CAPABILITY).ifPresent(cap -> {
            LivingEntity morphEntity = cap.getMorphEntity(this.level());
            if (morphEntity != null) {
                cir.setReturnValue(morphEntity.getDimensions(pose));
            }
        });
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void monstermod_readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        this.getCapability(MorphCapabilityAttacher.MORPH_CAPABILITY).ifPresent(cap -> {
            if (tag.contains(MorphCapabilityAttacher.MORPH_CAP_RL.toString())) {
                cap.deserializeNBT(tag.getCompound(MorphCapabilityAttacher.MORPH_CAP_RL.toString()));
            }
        });
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void monstermod_addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        this.getCapability(MorphCapabilityAttacher.MORPH_CAPABILITY).ifPresent(cap -> {
            tag.put(MorphCapabilityAttacher.MORPH_CAP_RL.toString(), cap.serializeNBT());
        });
    }
}