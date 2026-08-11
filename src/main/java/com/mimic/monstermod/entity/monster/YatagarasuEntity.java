package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.entity.hitbox.BonePoseResolver;
import com.mimic.monstermod.entity.hitbox.BoneRigData;
import com.mimic.monstermod.entity.hitbox.YatagarasuBodyPart;
import com.mimic.monstermod.entity.hitbox.YatagarasuHitboxProfile;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class YatagarasuEntity extends BaseEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private String currentSkillAnim = null;

    // --- ボーン追従ヒットボックス(弱点部位) ---
    private static final List<String> HITBOX_BONE_NAMES =
            YatagarasuHitboxProfile.PARTS.stream().map(YatagarasuHitboxProfile.PartConfig::boneName).toList();
    private static final BoneRigData BONE_RIG = BoneRigData.load(
            "assets/monstermod/geo/yatagarasu.geo.json",
            "assets/monstermod/animations/yatagarasu.animation.json",
            HITBOX_BONE_NAMES
    );

    private final YatagarasuBodyPart[] bodyParts;

    public YatagarasuEntity(EntityType<? extends BaseEntity> type, Level level) {
        super(type, level);
        // これを呼ぶことで、getDimensions() と getEyeHeight() が即座に再計算されます
        this.refreshDimensions();

        this.bodyParts = new YatagarasuBodyPart[YatagarasuHitboxProfile.PARTS.size()];
        for (int i = 0; i < bodyParts.length; i++) {
            bodyParts[i] = new YatagarasuBodyPart(this, YatagarasuHitboxProfile.PARTS.get(i));
        }
    }

    public static BoneRigData getBoneRig() {
        return BONE_RIG;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return bodyParts;
    }

    // 現在再生中のアニメーション名と、その再生が始まったtick。
    // SynchedEntityDataではなくローカルに持つ(下のresolveAnimationName()が
    // 同期済みのgetCurrentSkill()と移動状態から決定的に導出するため、
    // クライアント・サーバーそれぞれが独立に同じ値を計算できる)。
    private String activeAnim = "";
    private int activeAnimAnchorTick = 0;

    /**
     * 今再生されるべきアニメーション名を返す。
     * 描画(mainPredicate)と当たり判定(updateBodyPartHitboxes)の両方がこれを使うため、
     * 見た目と当たり判定のアニメーションが食い違わない。
     */
    public String resolveAnimationName() {
        String currentSkill = getCurrentSkill();
        if (currentSkill != null && !currentSkill.isEmpty()) {
            String skillAnim = switch (currentSkill) {
                case "spiral_dash" -> "animation.yatagarasu.spin";
                case "air_slash", "tornado" -> "animation.yatagarasu.tatsumaki";
                case "roar" -> "animation.yatagarasu.roar";
                case "charge" -> "animation.yatagarasu.charge";
                default -> null;
            };
            if (skillAnim != null) return skillAnim;
        }
        // 移動判定はサーバー権威の速度から導出する(クライアントでも同じ値が得られる)
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D;
        return moving ? "animation.yatagarasu.run" : "animation.yatagarasu.idle";
    }

    @Override
    public void tick() {
        super.tick();

        // アニメーションが切り替わったら経過時間の基準をリセットする
        String resolved = resolveAnimationName();
        if (!resolved.equals(activeAnim)) {
            activeAnim = resolved;
            activeAnimAnchorTick = this.tickCount;
        }

        if (!level().isClientSide && BONE_RIG.isLoaded()) {
            updateBodyPartHitboxes();
        }
    }

    public String getActiveAnimation() {
        return activeAnim;
    }

    /** 現在のアニメーションが再生され始めてから何秒経過したか(ループなら周回込み) */
    public double getCurrentAnimationElapsedSeconds() {
        if (activeAnim.isEmpty()) return 0.0;

        double elapsedSeconds = Math.max(0, this.tickCount - activeAnimAnchorTick) / 20.0;
        double length = BONE_RIG.getAnimationLength(activeAnim);
        if (length > 0 && BONE_RIG.isLooping(activeAnim)) {
            elapsedSeconds = elapsedSeconds % length;
        }
        return elapsedSeconds;
    }

    /** ボーン追従ヒットボックスを現在のアニメーションに合わせて毎tick更新する */
    private void updateBodyPartHitboxes() {
        if (activeAnim.isEmpty()) return;
        double elapsedSeconds = getCurrentAnimationElapsedSeconds();

        for (YatagarasuBodyPart part : bodyParts) {
            String boneName = part.getConfig().boneName();
            Vector3f[] corners = BonePoseResolver.resolveWorldCorners(
                    BONE_RIG, boneName, activeAnim, elapsedSeconds, this.position(), this.getYRot());
            if (corners == null) continue;
            part.updateFromWorldAABB(BonePoseResolver.enclosingAABB(corners));
        }
    }
    @Override
    protected EntityDimensions getCustomDimensions() {
        return EntityDimensions.fixed(1.2f, 3.0f);
    }

    @Override
    protected float getCustomEyeHeight(EntityDimensions dimensions) {
        return 3.0f; // ここで固定値を返す
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
    }
    // --- 1. スキルアニメーション再生 ---
    /**
     *todo スキル1の螺旋突進はプレビュ＋animation.yatagarasu.spin_idleー→スキル発動でanimation.yatagarasu.spinを行う　スキル効果時間が終わったらanimation.yatagarasu.spin_afterをする
     *スキル２　エアスラッシュ　プレビュー→スキル発動　animation.yatagarasu.tatsumakiを行う
     *スキル３　プレビュー＋animation.yatagarasu.chargeでぐるぐる回って　→スキル発動でanimation.yatagarasu.tatsumakiを行う
     *スキル４　プレビュー＋animation.yatagarasu.spin_idleを自分ではなくentityを自分の横に出して自分は        // --- 2. 通常時のアニメーション（待機・移動） ---
     *         if (this.onGround()) {
     *             if (event.isMoving()) {
     *                 controller.setAnimation(RawAnimation.begin().then("animation.yatagarasu.run", Animation.LoopType.LOOP));
     *             } else {
     *                 controller.setAnimation(RawAnimation.begin().then("animation.yatagarasu.idle", Animation.LoopType.LOOP));
     *             }
     *         }これのidleでいいんだけど自分の横に出す八咫烏はanimation.yatagarasu.spin_idleでスキル発動まで待機させる→スキル発動で幻影の八咫烏のみにanimation.yatagarasu.spinを行わせる そのまま消える
     *スキル５ 6 9 10 11 この鏡を出すのはこのアニメーしょんはない作るべき？？
     *スキル7　プレビュー→スキル発動でanimation.yataarasu.roar
     *スキル８　プレビュー＋animation.yatagarasu.charge+上に上昇する当たり判定はそのままモデルと自分の目線だけ上に上がるコードで上空に上がったように見せる？→その場でスキル発動でanimation.yataarasu.roar
     *
     *
     *
     *
     */
    private PlayState mainPredicate(AnimationState<YatagarasuEntity> event) {
        AnimationController<YatagarasuEntity> controller = event.getController();
        String currentSkill = getCurrentSkill();

        // 【重要】再生するアニメーション名は resolveAnimationName() に一本化する。
        // ここで独自に名前を決めてしまうと、ボーン追従ヒットボックス側が参照している
        // アニメーションと食い違い、当たり判定と見た目がズレる。
        String anim = resolveAnimationName();
        boolean isSkillAnim = currentSkill != null && !currentSkill.isEmpty()
                && !anim.equals("animation.yatagarasu.run")
                && !anim.equals("animation.yatagarasu.idle");

        controller.setAnimation(RawAnimation.begin().then(anim,
                isSkillAnim ? Animation.LoopType.PLAY_ONCE : Animation.LoopType.LOOP));

        // スキルアニメーションが終了したらスキル状態をリセットする（サーバー同期用）
        if (isSkillAnim && controller.getAnimationState() == AnimationController.State.STOPPED
                && !level().isClientSide) {
            setCurrentSkill(null);
        }
        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseEntity.createDefaultAttributes(
                300.0D, // HP（ボス級）
                0.35D,  // 移動速度（少し速め）
                15.0D,  // 攻撃力
                1.0D,   // ノックバック耐性
                0D   // アーマー
        );
    }
}
//No,技名（仮）,攻撃内容・特徴
//1,螺旋突進,体を細くし、水平姿勢でスピンしながら高速突進。→あたったらダメージ　
//2,エアスラッシュ,羽を飛ばす前方範囲攻撃。→はんいにいたらだめーじ　形は扇形
//3,八咫の旋風,回転予備動作後、四隅に追尾竜巻を発生。四隅の竜巻は追尾型で範囲は普通　もう一個自分を中心に大竜巻の発生中心は吸引＋大ダメージ＋大範囲。
//4　鏡像の乱舞　強ワザ　自分の横方向に２体挟むようにmodelを配置してこのmodelには当たり判定があり偽物と分けるためこのモデルの後ろには鏡がある　んで、そのモデルが螺旋突進,体を細くし、水平姿勢でスピンしながら高速突進。→あたったらダメージを行う感じ　まっすぐにしか行けないけど３回連続でやったりする
//５鏡の発生　自分から8方向に設置　entityとして動かない鏡を発生させる　この鏡に変身しているplayerが当たれば他の鏡にワープするため螺旋突進して突進してきて自分の方向ではないとわかってもかmonsterが鏡に当たればランダムに自分の近くの鏡に来る可能性があり
//また、この鏡に弾幕が設置された鏡に鬼火が触れると、**逆方向へ加速(1.5倍)**して反射。
//6,炎柱,設置型で鬼火柱を立たせる。　playerの存在する場所を特定してそこに設置型でお区感じ　範囲は4かける4とか四角か円型かは未定　おすすめあれば
//7,神威の咆哮,咆哮によるバインド（移動不可）状態付与。　広範囲のbind　
//8	昇天爆砕	回転上昇後に咆哮し、範囲を100かける100とかで決めて一定時間30秒ぐらい地面からy+30上の場所に鬼火を設置してふわふわと落下させる落下は傘のようにらんだむにwasd咆哮に動く　地面についたら消えるがあたったらDamage　
//9,渦巻く鬼火,蚊取り線香のように、自身を中心に回転しながら外側へ広がる弾幕。
//10,鬼火弾幕（通常）,低速移動する鬼火を東方風の八方放射,自身から16方向へ直線的に鬼火を高速射出。
//11鏡の風向　　鏡像の乱舞を当てやすくするため自分の周囲にいすぎるとまっすぐ方向しか無理だから当たらない方向も多く発生する　そのため、一定数カ所に集める必要があるので大きなentityとして鏡が吸引する感じのを一個だけ発生　これを破壊することが必須
//
//s