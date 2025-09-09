package net.mimic.monstermod.identity;

import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.Supplier;

/**
 * すべてのモンスターIdentityの共通基底クラス
 * アニメーションのマッピングやサイズなどを設定可能
 */
public abstract class PlayerIdentityType <T extends Enum<T>> implements IPlayerIdentity {

    private final ResourceLocation id;
    //Identityに対応するエンティティを遅延取得
    private final Supplier<EntityType<?>> entityTypeSupplier;

    private final Class<T> animationEnumClass;
    private final Map<String, T> animationMap;
    private T defaultAnimationState;

    public PlayerIdentityType(ResourceLocation id,
                              Supplier<EntityType<?>> entityTypeSupplier,
                              Class<T> animationEnumClass,
                              Map<String, T> animationMap,
                              T defaultAnimationState) {
        this.id = id;
        this.entityTypeSupplier = entityTypeSupplier;
        this.animationEnumClass = animationEnumClass;
        this.animationMap = animationMap;
        this.defaultAnimationState = defaultAnimationState;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }
    //変身時に能力参照などのために作成
    @Override
    public LivingEntity createDummy(net.minecraft.world.level.Level level) {
        try {
            return (LivingEntity) entityTypeSupplier.get().create(level);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void applyAnimation(LivingEntity dummy, PlayerTransformation.MonsterState state) {
        if (dummy == null) return;

        String anim = state.animationState;
        T mapped = animationMap.getOrDefault(anim, defaultAnimationState);

        if (dummy instanceof net.mimic.monstermod.entity.BaseMonsterEntity<?> monster) {
            ((net.mimic.monstermod.entity.BaseMonsterEntity<T>) monster).setAnimationState(mapped);
        }
    }

    @Override
    public String getMonsterId() {
        return id.getPath();
    }

    @Override
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        EntityType<?> type = entityTypeSupplier.get();
        if (type != null) {
            EntityDimensions dimensions = type.getDimensions();
            return new Vec3(dimensions.width, dimensions.height, dimensions.width); // 幅をX/Zに適用
        }
        return new Vec3(0.6f, 1.8f, 0.6f);
    }

    @Override
    public float getEyeHeight(Pose pose) {
        EntityType<?> type = entityTypeSupplier.get();
        if (type != null) {
            EntityDimensions dimensions = type.getDimensions();
            return dimensions.height * 0.85f;
        }
        return 1.62f;
    }

    @Override
    public float getStepHeight() { return 0.6f; }

    @Override
    public void applySpecificAbilities(LivingEntity player) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.hasSavedOriginalStats()) {
                transformation.setOriginalHealth(player.getHealth());
                transformation.setOriginalMaxHealth(player.getMaxHealth());
                transformation.setOriginalAttackDamage(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
                transformation.setOriginalArmor(player.getAttributeValue(Attributes.ARMOR));
                transformation.setOriginalMoveSpeed(player.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }

            // EntityType から属性を取得
            LivingEntity dummy = createDummy(player.level());
            if (dummy != null) {
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(dummy.getMaxHealth());
                player.setHealth((float) dummy.getMaxHealth());
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dummy.getAttributeValue(Attributes.ATTACK_DAMAGE));
                player.getAttribute(Attributes.ARMOR).setBaseValue(dummy.getAttributeValue(Attributes.ARMOR));
                player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(dummy.getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
            // ここでノックバック無効フラグをセット
            transformation.setNoKnockback(true);
        });
    }

    /** 元のステータスに戻すデフォルト実装 */
    @Override
    public void removeSpecificAbilities(LivingEntity player) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transformation.hasSavedOriginalStats()) {
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(transformation.getOriginalMaxHealth());
                player.setHealth((float) Math.min(player.getHealth(), transformation.getOriginalMaxHealth()));
                player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(transformation.getOriginalAttackDamage());
                player.getAttribute(Attributes.ARMOR).setBaseValue(transformation.getOriginalArmor());
                player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(transformation.getOriginalMoveSpeed());
                transformation.clearOriginalStats();
            }
            // ノックバック無効フラグを解除
            transformation.setNoKnockback(false);

            transformation.clearOriginalStats();
        });
    }
}
