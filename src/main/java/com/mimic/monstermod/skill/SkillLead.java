package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.MathMain;
import net.minecraft.resources.ResourceLocation;

public final class SkillLead {

    public final SkillId id;
    public final MathMain.Shape shape;
    public final MathMain.Transform transform;

    // ★ 判定・描画の「唯一の真実」となるパラメータ
    public final MathMain params;

    /* ===== 旧コード互換用のフィールド (外部参照用) ===== */
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
    public final boolean autoRoot;
    public final int rootTickBeforeDamage;
    public final int totalPreviewTicks;
    public final AttackType attackType;

    /* ===== 描画フラグ (これらが無いとエラーになるため維持) ===== */
    public final boolean render2D;
    public final boolean render2DOverlay;
    public final boolean renderBlock2D;
    public final boolean render3DPreview;
    public final ResourceLocation previewTexture;

    private SkillLead(Builder b) {
        this.id = b.id;
        this.shape = b.shape;
        this.transform = b.transform;

        // パラメータを確定
        this.params = b.mathBuilder.build();

        // 互換性のためにフィールドにもコピー
        this.radius = b.radius;
        this.height = b.height;
        this.angleDeg = b.angleDeg;
        this.xRadius = b.xRadius;
        this.zRadius = b.zRadius;
        this.baseHalf = b.baseHalf;
        this.depth = b.depth;

        this.followCaster = b.followCaster;
        this.yAnchorToGround = b.yAnchorToGround;
        this.autoRoot = b.autoRoot;
        this.rootTickBeforeDamage = b.rootTickBeforeDamage;
        this.totalPreviewTicks = b.totalPreviewTicks;
        this.attackType = b.attackType;

        this.render2D = b.render2D;
        this.render2DOverlay = b.render2DOverlay;
        this.renderBlock2D = b.renderBlock2D;
        this.render3DPreview = b.render3DPreview;
        this.previewTexture = b.previewTexture;
    }

    public SkillId skillId() { return id; }

    public static class Builder {
        private final SkillId id;
        private MathMain.Shape shape;
        private MathMain.Transform transform = MathMain.Transform.identity();

        // ★ 内部で判定用Builderを並行して動かす
        private final MathMain.Builder mathBuilder = new MathMain.Builder();

        private float radius = 0f;
        private float height = 0f;
        private float angleDeg = 0f;
        private float xRadius = 0f;
        private float zRadius = 0f;
        private float baseHalf = 0f;
        private float depth = 0f;

        private boolean autoRoot = true;
        private int rootTickBeforeDamage = 10;
        private int totalPreviewTicks = 60;
        private boolean followCaster = false;
        private boolean yAnchorToGround = false;
        private AttackType attackType = AttackType.NONE;

        private boolean render2D = false;
        private boolean render2DOverlay = false;
        private boolean renderBlock2D = false;
        private boolean render3DPreview = false;
        private ResourceLocation previewTexture = new ResourceLocation("monstermod", "textures/misc/attackpreview.png");

        public Builder(SkillId id) { this.id = id; }

        public Builder shape(MathMain.Shape shape) {
            this.shape = shape;
            this.mathBuilder.shape(shape);
            return this;
        }

        public Builder transform(MathMain.Transform t) { this.transform = t; return this; }

        /* ===== サイズ設定 (MathMain.Builderと同期させる) ===== */
        public Builder sphere(float r) {
            this.radius = r; this.height = r * 2;
            mathBuilder.radius(r).height(r * 2);
            return this;
        }
        public Builder cylinder(float r, float h) {
            this.radius = r; this.height = h;
            mathBuilder.radius(r).height(h);
            return this;
        }
        public Builder fan(float r, float angle, float h) {
            this.radius = r; this.angleDeg = angle; this.height = h;
            mathBuilder.fan(r, angle).height(h);
            return this;
        }
        public Builder rect(float xr, float zr, float h) {
            this.xRadius = xr; this.zRadius = zr; this.height = h;
            mathBuilder.rect(xr, zr).height(h);
            return this;
        }
        public Builder triangle(float baseHalf, float depth) {
            this.baseHalf = baseHalf; this.depth = depth;
            mathBuilder.triangle(baseHalf, depth);
            return this;
        }
        public Builder box(float x, float y, float z) {
            this.xRadius = x/2f; this.zRadius = z/2f; this.height = y;
            mathBuilder.rect(x/2f, z/2f).height(y);
            return this;
        }

        /* ===== 挙動設定 ===== */
        public Builder followCaster(boolean v) { this.followCaster = v; return this; }
        public Builder yAnchorToGround(boolean v) { this.yAnchorToGround = v; return this; }
        public Builder attackType(AttackType t) { this.attackType = t; return this; }
        public Builder autoRoot(boolean v) { this.autoRoot = v; return this; }
        public Builder rootTickBeforeDamage(int t) { this.rootTickBeforeDamage = t; return this; }
        public Builder totalPreviewTicks(int t) { this.totalPreviewTicks = t; return this; }

        /* ===== 描画フラグ (エラー回避のために復活) ===== */
        public Builder render2D() { this.render2D = true; return this; }
        public Builder render2DOverlay() { this.render2DOverlay = true; return this; }
        public Builder renderBlock2D() { this.renderBlock2D = true; return this; }
        public Builder render3DPreview() { this.render3DPreview = true; return this; }
        public Builder texture(ResourceLocation tex) { this.previewTexture = tex; return this; }

        public SkillLead build() {
            if (shape == null) throw new IllegalStateException("shape未設定: " + id);

            // NONEのままなら、デフォルトでENTITY_AOEにする安全策
            if (this.attackType == AttackType.NONE) {
                this.attackType = AttackType.ENTITY_AOE;
            }

            // MathMain.Builder に原点を仮置き
            mathBuilder.origin(net.minecraft.world.phys.Vec3.ZERO);

            return new SkillLead(this);
        }
    }
}