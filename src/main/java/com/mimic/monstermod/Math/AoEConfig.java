package com.mimic.monstermod.Math;

import net.minecraft.world.phys.Vec3;

public class AoEConfig {
    public enum Shape { BOX, TRI_PRISM, SPHERE, CYLINDER, FAN }
    public enum RenderType { R2D, R2DOverlay, R2DBlock, R3D }

    public Shape shape;
    public RenderType renderType;
    public double xRadius, yRadius, zRadius; // BOX
    public double radius; // 球/円/円柱
    public double height;
    public double angleDeg; // FAN
    public Vec3 position;
    public boolean followOwner;
    public boolean isDamage;
    public float yaw, pitch, roll;
    public int lifetimeTicks = 20; // 表示寿命
}
