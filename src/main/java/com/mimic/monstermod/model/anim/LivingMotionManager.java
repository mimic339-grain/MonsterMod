package com.mimic.monstermod.model.anim;

import com.mimic.monstermod.capability.MonsterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Minecraftエンティティの状態を監視して LivingMotion を自動切り替えするマネージャー。
 *
 * EFM参考:
 *   - api/animation/Animator.java — updateMotion() で状態監視
 *   - api/animation/LivingMotions.java — 状態対応モーション定義
 *
 * 配置: com/mimic/monstermod/model/anim/LivingMotionManager.java
 */
public class LivingMotionManager {

    private final AnimationPlayer player;
    private LivingMotion currentMotion = LivingMotion.IDLE;
    private LivingMotion prevMotion    = LivingMotion.IDLE;

    /** ブレンド遷移用（前のモーションと新しいモーションの間を補間） */
    private float blendTimer = 0f;
    private static final float BLEND_DURATION = 0.1f; // 100ms ブレンド

    public LivingMotionManager(AnimationPlayer player) {
        this.player = player;
    }

    // ── 毎tick: Minecraft状態 → LivingMotion に変換 ─────────────────
    /**
     * エンティティの状態からモーションを決定して AnimationPlayer に反映する。
     * EFM: Animator.updateMotion() パターン。
     * CustomEntityBase.tick() から毎tick呼ぶ。
     */
    public void updateMotion(LivingEntity entity) {
        LivingMotion newMotion = resolveMotion(entity);

        if (newMotion != currentMotion) {
            prevMotion    = currentMotion;
            currentMotion = newMotion;
            blendTimer    = 0f;
            player.setPlayAnimation(currentMotion.animName, currentMotion.isLoop);
            player.setPlaybackSpeed(currentMotion.defaultSpeed);
        }

        blendTimer += 1f / 20f;
    }

    /**
     * プレイヤーの変身状態から適切なモーションを決定する。
     * EFM: Animator.updateMotion() + LivingEntityPatch.updateMotion() パターン。
     */
    private LivingMotion resolveMotion(LivingEntity entity) {
        if (!entity.isAlive()) return LivingMotion.DEATH;
        if (entity.hurtTime > 0) return LivingMotion.HURT;

        // 【修正ポイント】AnimationPlayer ではなく entity (LivingEntity) から取得する
        if (entity instanceof Player playerEntity) {
            var lazyCap = playerEntity.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION);
            var optionalCap = lazyCap.resolve();

            if (optionalCap.isPresent()) {
                MonsterTransformation transformation = optionalCap.get();
                if (transformation.isTransformed()) {
                    // 【修正ポイント】resolveMonsterMotion の第1引数は Player 型なので、playerEntity を渡す
                    return resolveMonsterMotion(playerEntity, transformation);
                }
            }
        }

        // 通常プレイヤー/エンティティモーション
        if (entity.isFallFlying()) return LivingMotion.FLY;
        if (entity.isSwimming())   return LivingMotion.SWIM;
        if (entity.isCrouching())  return LivingMotion.SNEAK;

        float speed = (float) entity.getDeltaMovement().horizontalDistanceSqr();
        if (speed > 0.04f) return LivingMotion.RUN;  // 0.04 = 走行速度閾値
        if (speed > 0.005f) return LivingMotion.WALK;
        return LivingMotion.IDLE;
    }

    private LivingMotion resolveMonsterMotion(Player player, MonsterTransformation cap) {
        // Monster種別ごとの固有モーション（今後拡張）
        String type = cap.getMonsterType();
        if ("dragon".equals(type) && player.isFallFlying()) return LivingMotion.FLY;

        float speed = (float) player.getDeltaMovement().horizontalDistanceSqr();
        if (speed > 0.04f) return LivingMotion.RUN;
        if (speed > 0.005f) return LivingMotion.WALK;
        return LivingMotion.IDLE;
    }

    /** ブレンド係数 (0=前のモーション, 1=新しいモーション) */
    public float getBlendWeight() {
        return Math.min(1f, blendTimer / BLEND_DURATION);
    }

    public LivingMotion getCurrentMotion() { return currentMotion; }
    public LivingMotion getPrevMotion()    { return prevMotion; }
    public AnimationPlayer getPlayer()     { return player; }
}