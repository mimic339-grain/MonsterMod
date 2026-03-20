package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.MathMain;
import net.minecraft.resources.ResourceLocation;

public final class SkillLead {

    public final SkillId id;

    public final MathMain.Shape shape;
    public final MathMain.Transform transform;

    /* ===== サイズ ===== */
    public final float radius;
    public final float height;
    public final float angleDeg;
    public final float xRadius;
    public final float zRadius;
    public final float baseHalf;
    public final float depth;

    /* ===== 挙動 ===== */
    public final boolean followCaster;
    public final boolean yAnchorToGround;

    public final AttackType attackType;
    public final int lifetimeTick;

    /* ===== 描画 ===== */
    public final boolean render2D;
    public final boolean render2DOverlay;
    public final boolean renderBlock2D;
    public final boolean render3DPreview;

    /* ===== 見た目 ===== */
    public final ResourceLocation previewTexture;

    /* ====================== */
    /* Constructor            */
    /* ====================== */

    private SkillLead(Builder b) {
        this.id = b.id;
        this.shape = b.shape;
        this.transform = b.transform;

        this.radius = b.radius;
        this.height = b.height;
        this.angleDeg = b.angleDeg;
        this.xRadius = b.xRadius;
        this.zRadius = b.zRadius;
        this.baseHalf = b.baseHalf;
        this.depth = b.depth;

        this.followCaster = b.followCaster;
        this.yAnchorToGround = b.yAnchorToGround;

        this.attackType = b.attackType;
        this.lifetimeTick = b.lifetimeTick;

        this.render2D = b.render2D;
        this.render2DOverlay = b.render2DOverlay;
        this.renderBlock2D = b.renderBlock2D;
        this.render3DPreview = b.render3DPreview;

        this.previewTexture = b.previewTexture; // ★ ここ重要
    }

    public SkillId skillId() {
        return id;
    }

    /* ====================== */
    /* Builder                */
    /* ====================== */

    public static class Builder {

        private final SkillId id;

        private MathMain.Shape shape;
        private MathMain.Transform transform = MathMain.Transform.identity();

        private float radius = 0f;
        private float height = 0f;
        private float angleDeg = 0f;
        private float xRadius = 0f;
        private float zRadius = 0f;
        private float baseHalf = 0f;
        private float depth = 0f;

        private boolean followCaster = false;
        private boolean yAnchorToGround = false;

        private AttackType attackType = AttackType.NONE;
        private int lifetimeTick = 1;

        private boolean render2D = false;
        private boolean render2DOverlay = false;
        private boolean renderBlock2D = false;
        private boolean render3DPreview = false;

        /* ===== 見た目 ===== */
        private ResourceLocation previewTexture =
                new ResourceLocation("monstermod", "textures/misc/attackpreview.png");

        public Builder(SkillId id) {
            if (id == null) throw new NullPointerException("SkillId null");
            this.id = id;
        }

        /* ===== shape ===== */

        public Builder shape(MathMain.Shape shape) {
            this.shape = shape;
            return this;
        }

        public Builder transform(MathMain.Transform transform) {
            this.transform = transform;
            return this;
        }

        /* ===== サイズ ===== */

        public Builder sphere(float r) {
            this.radius = r;
            return this;
        }

        public Builder cylinder(float r, float h) {
            this.radius = r;
            this.height = h;
            return this;
        }

        public Builder fan(float r, float angle, float h) {
            this.radius = r;
            this.angleDeg = angle;
            this.height = h;
            return this;
        }

        public Builder rect(float xr, float zr, float h) {
            this.xRadius = xr;
            this.zRadius = zr;
            this.height = h;
            return this;
        }

        public Builder triangle(float baseHalf, float depth) {
            this.baseHalf = baseHalf;
            this.depth = depth;
            return this;
        }

        public Builder box(float x, float y, float z) {
            this.xRadius = x / 2f;
            this.height = y;
            this.zRadius = z / 2f;
            return this;
        }

        /* ===== 挙動 ===== */

        public Builder followCaster(boolean v) {
            this.followCaster = v;
            return this;
        }

        public Builder yAnchorToGround(boolean v) {
            this.yAnchorToGround = v;
            return this;
        }

        public Builder attackType(AttackType t) {
            this.attackType = t;
            return this;
        }

        public Builder lifetime(int t) {
            this.lifetimeTick = t;
            return this;
        }

        /* ===== 描画 ===== */

        public Builder render2D() {
            this.render2D = true;
            return this;
        }

        public Builder render2DOverlay() {
            this.render2DOverlay = true;
            return this;
        }

        public Builder renderBlock2D() {
            this.renderBlock2D = true;
            return this;
        }

        public Builder render3DPreview() {
            this.render3DPreview = true;
            return this;
        }

        public Builder texture(ResourceLocation tex) {
            this.previewTexture = tex;
            return this;
        }

        /* ===== build ===== */

        public SkillLead build() {

            if (shape == null) {
                throw new IllegalStateException("shape未設定: " + id);
            }

            switch (shape) {

                case SPHERE -> {
                    if (radius <= 0f) {
                        throw new IllegalStateException("SPHERE radius未設定: " + id);
                    }

                    // ★ 超重要
                    if (height <= 0f) {
                        height = radius * 2f;
                    }
                }

                case CYLINDER -> {
                    if (radius <= 0f) {
                        throw new IllegalStateException("CYLINDER radius未設定: " + id);
                    }

                    if (height <= 0f) {
                        height = 1f; // デフォルト薄さ
                    }
                }

                case FAN -> {
                    if (radius <= 0f || angleDeg <= 0f) {
                        throw new IllegalStateException("FAN size未設定: " + id);
                    }

                    if (height <= 0f) {
                        height = 1f;
                    }
                }

                case RECT_PRISM, BOX -> {
                    if (xRadius <= 0f || zRadius <= 0f) {
                        throw new IllegalStateException("BOX size未設定: " + id);
                    }

                    if (height <= 0f) {
                        height = 1f;
                    }
                }

                case TRI_PRISM -> {
                    if (baseHalf <= 0f || depth <= 0f) {
                        throw new IllegalStateException("TRI size未設定: " + id);
                    }

                    if (height <= 0f) {
                        height = 1f;
                    }
                }

                case CAPSULE -> {
                    if (radius <= 0f || height <= 0f) {
                        throw new IllegalStateException("CAPSULE size未設定: " + id);
                    }
                }
            }

            return new SkillLead(this);
        }
    }
}