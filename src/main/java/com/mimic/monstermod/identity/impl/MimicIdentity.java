package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

public class MimicIdentity extends BaseMonsterIdentity {

    public static final ResourceLocation IDENTITY_ID =
            new ResourceLocation("monstermod", "mimic");

    public MimicIdentity() {
        super(IDENTITY_ID.toString());
    }

    @Override
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f); // Mimic固有
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return 0.45f; // Mimic固有
    }
}
