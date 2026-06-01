package com.mimic.monstermod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;

/**
 * 全カスタムエンティティの基底クラス。
 * OBB同期・部位HPシステム・アニメーション状態管理を提供。
 *
 * 配置: com/mimic/monstermod/entity/base/CustomEntityBase.java
 */
public abstract class CustomEntityBase extends PathfinderMob {

    protected final Map<String, Float> partHp = new HashMap<>();
    protected final Map<String, Float> partMaxHp = new HashMap<>();
    protected final Map<String, OBBData> obbMap = new HashMap<>();

    protected String currentAnimation = "idle";
    protected float animationTick = 0f;
    protected boolean isFlying = false;
    protected boolean isBreathing = false;

    private int syncTimer = 0;
    private static final int SYNC_INTERVAL = 3;

    protected CustomEntityBase(EntityType<? extends CustomEntityBase> type, Level level) {
        super(type, level);
        initPartHP();
    }

    public static AttributeSupplier.Builder createBaseAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    protected abstract void initPartHP();

    protected void registerPart(String partName, float hpMultiplier) {
        float totalHp = (float) this.getAttributeValue(Attributes.MAX_HEALTH);
        float max = totalHp * hpMultiplier;
        partHp.put(partName, max);
        partMaxHp.put(partName, max);
    }

    public void damagePartOf(String partName, float damage, String damageType) {
        if (!partHp.containsKey(partName)) return;
        float modified = applyPartWeakness(partName, damage, damageType);
        float newHp = Math.max(0, partHp.get(partName) - modified);
        partHp.put(partName, newHp);
        onPartDamaged(partName, modified, newHp);
        this.hurt(this.damageSources().generic(), modified);
    }

    protected float applyPartWeakness(String partName, float damage, String damageType) {
        return damage;
    }

    protected void onPartDamaged(String partName, float damage, float remainingHp) {}

    public void updateOBB(String boneName, Matrix4f boneMatrix,
                          Vector3f halfExtents, String partGroup) {
        Vector3f center = new Vector3f();
        Quaternionf orientation = new Quaternionf();
        boneMatrix.getTranslation(center);
        boneMatrix.getUnnormalizedRotation(orientation);
        obbMap.put(boneName, new OBBData(center, halfExtents, orientation, partGroup));
    }

    public Map<String, OBBData> getOBBMap() { return obbMap; }
    public Map<String, Float> getPartHp() { return partHp; }
    public Map<String, Float> getPartMaxHp() { return partMaxHp; }

    @Override
    public void tick() {
        super.tick();
        animationTick++;
        if (!this.level().isClientSide()) {
            if (++syncTimer >= SYNC_INTERVAL) {
                syncTimer = 0;
                syncOBBToClients();
            }
        }
    }

    protected void syncOBBToClients() {}

    public void setAnimation(String name) {
        if (!name.equals(currentAnimation)) {
            currentAnimation = name;
            animationTick = 0;
        }
    }

    public String getCurrentAnimation() { return currentAnimation; }
    public float getAnimationTick() { return animationTick; }

    // ── OBBデータ ─────────────────────────────────────────────────
    public static class OBBData {
        public final Vector3f center;
        public final Vector3f halfExtents;
        public final Quaternionf orientation;
        public final String partGroup;

        public OBBData(Vector3f c, Vector3f h, Quaternionf o, String p) {
            this.center = new Vector3f(c);
            this.halfExtents = new Vector3f(h);
            this.orientation = new Quaternionf(o);
            this.partGroup = p;
        }

        /** SAT: 点がOBB内部にあるか判定 */
        public boolean containsPoint(Vector3f point) {
            Vector3f local = new Quaternionf(orientation).conjugate()
                    .transform(new Vector3f(point).sub(center));
            return Math.abs(local.x) <= halfExtents.x
                    && Math.abs(local.y) <= halfExtents.y
                    && Math.abs(local.z) <= halfExtents.z;
        }
    }
}