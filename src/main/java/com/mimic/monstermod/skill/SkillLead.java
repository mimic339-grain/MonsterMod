package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.Math.ProjectilePattern;
import net.minecraft.resources.ResourceLocation;

public final class SkillLead {

    public final SkillId id;
    public final MathMain.Shape shape;
    public final MathMain.Transform transform;

    // ★ 判定・描画の「唯一の真実」となるパラメータ
    public final MathMain params;

    /* ===== 弾幕パターン(Radial/Spiral系スキル用、無ければnull) ===== */
    public final ProjectilePattern projectilePattern;

    /* ===== 旧コード互換用のフィールド (外部参照用) ===== */
    public final float radius;
    public final float height;
    public final float angleDeg;
    public final float xRadius;
    public final float zRadius;
    public final float baseHalf;
    public final float depth;

    /* ===== 挙動制御 (新規追加: 上書き・コンボ・キャンセル) ===== */
    public final SkillType.Category category;
    public final boolean canBeCanceled;

    /* ===== 挙動 ===== */
    public final boolean followCaster;//プレビューがついてくるかどうか
    public final boolean yAnchorToGround;
    public final boolean autoRoot;
    public final int beforeRecoverTicks;
    public final int totalPreviewTicks;
    public final SkillType skillType;
    public final int effectTicks;
    public final int skillTicks; // 合計時間
    public final int recoveryTicks;      // 攻撃後の硬直時間
    public final int comboWindowTicks;   // DASH/NORMAL後にコンボを受け付ける時間 (デフォルト60t = 3s)
    public final boolean canMoveDuringEffect; // 攻撃発生中に動けるか
    /* ===== 描画フラグ (これらが無いとエラーになるため維持) ===== */
    public final boolean render2D;
    public final boolean render2DOverlay;
    public final boolean renderBlock2D;
    public final boolean render3DPreview;
    public final ResourceLocation previewTexture;

    private SkillLead(Builder b, int total) {
        this.id = b.id;
        this.shape = b.shape;
        this.transform = b.transform;

        // パラメータを確定
        this.params = b.mathBuilder.build();
        this.projectilePattern = b.projectilePattern;

        // 互換性のためにフィールドにもコピー
        this.radius = b.radius;
        this.height = b.height;
        this.angleDeg = b.angleDeg;
        this.xRadius = b.xRadius;
        this.zRadius = b.zRadius;
        this.baseHalf = b.baseHalf;
        this.depth = b.depth;

        // 挙動制御の反映
        this.category = b.category;
        this.canBeCanceled = b.canBeCanceled;

        this.followCaster = b.followCaster;
        this.yAnchorToGround = b.yAnchorToGround;
        this.autoRoot = b.autoRoot;
        this.beforeRecoverTicks = b.beforeRecoverTicks;
        this.totalPreviewTicks = b.totalPreviewTicks;
        this.skillType = b.skillType;
        this.effectTicks = b.effectTicks;
        this.skillTicks = total;
        this.recoveryTicks = b.recoveryTicks;
        this.comboWindowTicks = b.comboWindowTicks;
        this.canMoveDuringEffect = b.canMoveDuringEffect;

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

        private ProjectilePattern projectilePattern = null;

        private float radius = 0f;
        private float height = 0f;
        private float angleDeg = 0f;
        private float xRadius = 0f;
        private float zRadius = 0f;
        private float baseHalf = 0f;
        private float depth = 0f;

        // 挙動制御の初期値
        private SkillType.Category category = SkillType.Category.NORMAL;
        private boolean canBeCanceled = false;//緊急回避用

        private boolean autoRoot = true;//硬直あるかどうか
        private int beforeRecoverTicks = 10;//硬直時間
        private int totalPreviewTicks = 60;//デフォルト３秒間プレビュー　プレビューそう時間
        private boolean followCaster = false;//
        private boolean yAnchorToGround = false;
        private SkillType skillType = SkillType.NONE;
        private int effectTicks = 1;
        private int recoveryTicks =30; // デフォルト1.5秒
        private int comboWindowTicks = 60;//コンボ猶予時間 3秒
        private boolean canMoveDuringEffect = false; // デフォルトは動けない

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

        /* ===== 挙動制御設定 ===== */
        public Builder category(SkillType.Category category) {
            this.category = category;
            return this;
        }

        public Builder canBeCanceled(boolean canBeCanceled) {
            this.canBeCanceled = canBeCanceled;
            return this;
        }

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
            mathBuilder.shape(MathMain.Shape.RECT_PRISM).rect(xr, zr).height(h);
            return this;
        }
        public Builder triangle(float baseHalf, float depth) {
            this.baseHalf = baseHalf; this.depth = depth;
            mathBuilder.triangle(baseHalf, depth);
            return this;
        }
        public Builder box(float x, float y, float z) {
            this.xRadius = x/2f; this.zRadius = z/2f; this.height = y;
            mathBuilder.shape(MathMain.Shape.BOX).rect(x/2f, z/2f).height(y);
            return this;
        }
        public Builder radial(float r, float h) {
            this.shape(MathMain.Shape.RADIAL); // 形状を確定
            this.radius = r;
            this.height = h;
            this.mathBuilder.radius(r).height(h); // MathMain側にも半径(射程)と高さを伝える
            return this;
        }
        public Builder spiral(float r, float h) {
            this.shape(MathMain.Shape.SPIRAL); // 形状を SPIRAL に設定
            this.radius = r;
            this.height = h;
            this.mathBuilder.radius(r).height(h);
            return this;
        }

        /**
         * ProjectilePatternから放射状スキルを構築する。
         * 射程(radius)はパターンの速度×寿命から自動算出されるため、
         * 実際に飛ぶ弾の射程とプレビューの長さが必ず一致する。
         */
        public Builder radialPattern(ProjectilePattern pattern) {
            this.projectilePattern = pattern;
            return this.radial((float) pattern.maxReach(), 1.0f);
        }

        /**
         * ProjectilePatternから螺旋状スキルを構築する。
         * 射程(radius)はパターンの速度×寿命から自動算出される。
         */
        public Builder spiralPattern(ProjectilePattern pattern) {
            this.projectilePattern = pattern;
            return this.spiral((float) pattern.maxReach(), 1.0f);
        }
        /* ===== 挙動設定 ===== */
        public Builder followCaster(boolean v) { this.followCaster = v; return this; }
        public Builder yAnchorToGround(boolean v) { this.yAnchorToGround = v; return this; }
        public Builder attackType(SkillType t) { this.skillType = t; return this; }
        public Builder autoRoot(boolean v) { this.autoRoot = v; return this; }
        public Builder beforeRecoverTicks(int t) { this.beforeRecoverTicks = t; return this; }
        public Builder totalPreviewTicks(int t) { this.totalPreviewTicks = t; return this; }
        public Builder effectTicks(int t) { this.effectTicks = t; return this; }
        public Builder recoveryTicks(int t) { this.recoveryTicks = t; return this; }
        public Builder comboWindowTicks(int t) { this.comboWindowTicks = t; return this; }
        public Builder canMoveDuringEffect(boolean v) {this.canMoveDuringEffect = v;return this;}

        /* ===== 描画フラグ (エラー回避のために復活) ===== */
        public Builder render2D() { this.render2D = true; return this; }
        public Builder renderBlock2D() { this.renderBlock2D = true; return this; }
        public Builder render3DPreview() { this.render3DPreview = true; return this; }
        public Builder texture(ResourceLocation tex) { this.previewTexture = tex; return this; }

        // SkillLead.java の Builder クラス内
        public SkillLead build() {
            // 1. 形状が必要なタイプなのに Shape が無い場合だけエラーにする
            if (shape == null) {
                if (this.skillType == SkillType.STRIKE || this.skillType == SkillType.TOUCH) {
                    throw new IllegalStateException("攻撃・接触スキルには shape 設定が必要です: " + id);
                }

                // --- 追加: Shapeが無いスキル用の安全策 ---
                // 内部の mathBuilder が build() で落ちないように最小のダミー形状を入れる
                this.shape = MathMain.Shape.CYLINDER;
                this.mathBuilder.shape(MathMain.Shape.CYLINDER).radius(0).height(0);

                // 描画フラグを強制OFF
                this.render2D = false;
                this.renderBlock2D = false;
                this.render3DPreview = false;
            }
            // 自動設定ロジック
            if (this.effectTicks <= 1) {
                this.canMoveDuringEffect = false; // 1tickなら動く暇がないので強制false
            }
            if (this.totalPreviewTicks <= 0) {
                this.render2D = false;
                this.render2DOverlay = false;
                this.renderBlock2D = false;
                this.render3DPreview = false;
            }

            //dashは仕様として移動専用なのでプレビュはない
            if (this.category == SkillType.Category.DASH) {
                this.totalPreviewTicks = 0;
            }
            //comboは前硬直はない
            if (this.category == SkillType.Category.COMBO) {
                this.beforeRecoverTicks = 0; // 攻撃前硬直なし（即座に発動）
            }
            //uniqueはcombowindowticksはない
            if (this.category == SkillType.Category.UNIQUE) {
                this.comboWindowTicks= 0;
            }
            //cancelはコンボを繋げないし
            if (this.category == SkillType.Category.CANCEL) {
                this.comboWindowTicks= 0;
                this.canBeCanceled = false; // キャンセルスキルをさらにキャンセルはできない
                this.render2D = false;
                this.renderBlock2D = false;
                this.render3DPreview = false;
            }
            // 2. 合計時間を計算
            int total = this.totalPreviewTicks + this.effectTicks + this.recoveryTicks+ 1;
            // 3. 合計時間に基づいた設定 (NORMALのコンボ猶予など)
            if (this.category == SkillType.Category.NORMAL) {
                this.comboWindowTicks = total;
            }
            if (this.category == SkillType.Category.COMBO) {
                this.comboWindowTicks = total;
            }
            mathBuilder.origin(net.minecraft.world.phys.Vec3.ZERO);
            return new SkillLead(this, total);
        }
    }
}