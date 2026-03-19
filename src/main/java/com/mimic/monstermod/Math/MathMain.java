package com.mimic.monstermod.Math;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * MathMain
 * 【役割】
 * ・AoE の「純粋な 3D 数学判定」の唯一の基礎
 * ・Preview / Attack / 2D / Block を一切知らない
 * 【設計原則】
 * ・全 Shape は完全 3D
 * ・回転可能（yaw / pitch / roll）
 * ・contains の数式は用途により一切変化しない
 */
public class MathMain {

    /* =========================
     * Shape
     * ========================= */
    public enum Shape {
        CYLINDER,      // 円柱
        FAN,           // 扇柱
        RECT_PRISM,    // 矩形柱
        TRI_PRISM,     // 三角柱
        BOX,           // 直方体
        SPHERE,        // 球
        CAPSULE        // 回転可能カプセル
    }

    /* =========================
     * Transform
     * ========================= */
    public final Vec3 origin;
    public final float yaw;
    public final float pitch;
    public final float roll;

    /* =========================
     * Shape
     * ========================= */
    public final Shape shape;

    /* =========================
     * Common
     * ========================= */
    public final float radius;
    public final float height;

    /* =========================
     * Shape specific
     * ========================= */
    public final float angleDeg;      // FAN
    public final float xRadius;       // RECT / BOX
    public final float zRadius;

    public final float baseHalf;      // TRI
    public final float depth;

    /* ======================
     * Transform
     * AoE の位置・回転・追従定義
     * ====================== */
    public static final class Transform {

        public final float yaw;
        public final float pitch;
        public final float roll;

        public Transform(float yaw, float pitch, float roll) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        public static Transform identity() {
            return new Transform(0f, 0f, 0f);
        }

        public static Transform of(float yaw, float pitch, float roll) {
            return new Transform(yaw, pitch, roll);
        }
    }

    /* =========================
     * Builder
     * ========================= */
    public static class Builder {
        private Vec3 origin;
        private float yaw, pitch, roll;
        private Shape shape;

        private float radius;
        private float height;

        private float angleDeg;
        private float xRadius, zRadius;
        private float baseHalf, depth;

        public Builder origin(Vec3 o){ this.origin = o; return this; }
        public Builder rotation(float yaw, float pitch, float roll){
            this.yaw = yaw; this.pitch = pitch; this.roll = roll;
            return this;
        }
        public Builder shape(Shape s){ this.shape = s; return this; }

        public Builder radius(float r){ this.radius = r; return this; }
        public Builder height(float h){ this.height = h; return this; }

        public Builder fan(float radius, float angleDeg){
            this.radius = radius;
            this.angleDeg = angleDeg;
            return this;
        }

        public Builder rect(float xr, float zr){
            this.xRadius = xr;
            this.zRadius = zr;
            return this;
        }

        public Builder triangle(float baseHalf, float depth){
            this.baseHalf = baseHalf;
            this.depth = depth;
            return this;
        }

        public MathMain build(){
            if (origin == null) throw new IllegalStateException("origin 未指定");
            if (shape == null) throw new IllegalStateException("shape 未指定");
            return new MathMain(this);
        }
    }

    public MathMain(Builder b){
        this.origin = b.origin;
        this.yaw = b.yaw;
        this.pitch = b.pitch;
        this.roll = b.roll;
        this.shape = b.shape;

        this.radius = b.radius;
        this.height = b.height;

        this.angleDeg = b.angleDeg;
        this.xRadius = b.xRadius;
        this.zRadius = b.zRadius;
        this.baseHalf = b.baseHalf;
        this.depth = b.depth;
    }
    /* =========================
     * Serialize (Server → Client)
     * ========================= */
    public void write(FriendlyByteBuf buf){
        // Transform
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);

        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(roll);

        // Shape
        buf.writeEnum(shape);

        // Common
        buf.writeFloat(radius);
        buf.writeFloat(height);

        // Shape specific
        buf.writeFloat(angleDeg);
        buf.writeFloat(xRadius);
        buf.writeFloat(zRadius);
        buf.writeFloat(baseHalf);
        buf.writeFloat(depth);
    }

    /* =========================
     * Deserialize (Client mirror)
     * ========================= */
    public static MathMain read(FriendlyByteBuf buf){
        Builder b = new Builder();

        b.origin(new Vec3(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        ));

        b.rotation(
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );

        b.shape(buf.readEnum(Shape.class));

        b.radius(buf.readFloat());
        b.height(buf.readFloat());

        b.angleDeg = buf.readFloat();
        b.xRadius = buf.readFloat();
        b.zRadius = buf.readFloat();
        b.baseHalf = buf.readFloat();
        b.depth = buf.readFloat();

        return b.build();
    }
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof MathMain m)) return false;

        return Float.compare(m.yaw, yaw) == 0 &&
                Float.compare(m.pitch, pitch) == 0 &&
                Float.compare(m.roll, roll) == 0 &&
                Float.compare(m.radius, radius) == 0 &&
                Float.compare(m.height, height) == 0 &&
                Float.compare(m.angleDeg, angleDeg) == 0 &&
                Float.compare(m.xRadius, xRadius) == 0 &&
                Float.compare(m.zRadius, zRadius) == 0 &&
                Float.compare(m.baseHalf, baseHalf) == 0 &&
                Float.compare(m.depth, depth) == 0 &&
                shape == m.shape &&
                origin.equals(m.origin);
    }

    @Override
    public int hashCode(){
        int result = origin.hashCode();
        result = 31 * result + shape.hashCode();
        result = 31 * result + Float.hashCode(yaw);
        result = 31 * result + Float.hashCode(pitch);
        result = 31 * result + Float.hashCode(roll);
        result = 31 * result + Float.hashCode(radius);
        result = 31 * result + Float.hashCode(height);
        result = 31 * result + Float.hashCode(angleDeg);
        result = 31 * result + Float.hashCode(xRadius);
        result = 31 * result + Float.hashCode(zRadius);
        result = 31 * result + Float.hashCode(baseHalf);
        result = 31 * result + Float.hashCode(depth);
        return result;
    }

    /* =========================
     * 判定
     * ========================= */
    public boolean contains(Vec3 worldPoint){
        Vec3 p = toLocal(worldPoint);

        return switch (shape) {
            case CYLINDER -> p.x*p.x + p.z*p.z <= radius*radius
                    && Math.abs(p.y) <= height/2f;

            case FAN -> containsFan(p);

            case RECT_PRISM ->
                    Math.abs(p.x)<=xRadius &&
                            Math.abs(p.z)<=zRadius &&
                            Math.abs(p.y)<=height/2f;

            case TRI_PRISM -> containsTriangle(p);

            case BOX ->
                    Math.abs(p.x)<=xRadius &&
                            Math.abs(p.y)<=height/2f &&
                            Math.abs(p.z)<=zRadius;

            case SPHERE -> p.lengthSqr() <= radius*radius;

            case CAPSULE -> containsCapsule(p);
        };
    }

    /* =========================
     * Internal
     * ========================= */
    private Vec3 toLocal(Vec3 p){
        Vec3 v = p.subtract(origin);

        v = rotateX(v, -pitch);
        v = rotateY(v, -yaw);
        v = rotateZ(v, -roll);

        return v;
    }

    private boolean containsFan(Vec3 p){
        if (Math.abs(p.y) > height/2f) return false;
        double d2 = p.x*p.x + p.z*p.z;
        if (d2 == 0 || d2 > radius*radius) return false;
        return (p.z / Math.sqrt(d2))
                >= Math.cos(Math.toRadians(angleDeg/2f));
    }

    private boolean containsTriangle(Vec3 p){
        if (Math.abs(p.y) > height/2f) return false;
        if (p.z < 0 || p.z > depth) return false;
        float w = baseHalf * (1f - (float)(p.z/depth));
        return Math.abs(p.x) <= w;
    }

    private boolean containsCapsule(Vec3 p){
        float half = height/2f;
        float y = Math.max(-half, Math.min(half, (float)p.y));
        return p.subtract(new Vec3(0, y, 0)).lengthSqr()
                <= radius*radius;
    }

    /* =========================
     * Rotation helpers
     * ========================= */
    private static Vec3 rotateX(Vec3 v, float deg){
        double r = Math.toRadians(deg);
        double c = Math.cos(r), s = Math.sin(r);
        return new Vec3(v.x, v.y*c - v.z*s, v.y*s + v.z*c);
    }

    private static Vec3 rotateY(Vec3 v, float deg){
        double r = Math.toRadians(deg);
        double c = Math.cos(r), s = Math.sin(r);
        return new Vec3(v.x*c + v.z*s, v.y, -v.x*s + v.z*c);
    }

    private static Vec3 rotateZ(Vec3 v, float deg){
        double r = Math.toRadians(deg);
        double c = Math.cos(r), s = Math.sin(r);
        return new Vec3(v.x*c - v.y*s, v.x*s + v.y*c, v.z);
    }
}
