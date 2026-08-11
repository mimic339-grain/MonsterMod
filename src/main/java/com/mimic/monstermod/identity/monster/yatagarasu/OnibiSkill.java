package com.mimic.monstermod.identity.monster.yatagarasu;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.Math.ProjectilePattern;
import com.mimic.monstermod.entity.obj.OnibiEntity;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.skill.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class OnibiSkill extends SkillEffectSpec {

    /**
     * 弾幕の唯一の定義。実際に飛ぶ弾(applyToCaster)とプレビュー(AoeMeshBuilder2D)の
     * 両方がこれを参照するため、射程・弾数・速度は常に一致する。
     */
    public static final ProjectilePattern PATTERN = new ProjectilePattern(16, 0.21, 0.0, 400);

    public OnibiSkill() {
        super(0, DamageType.MAGIC, SkillType.STRIKE, List.of());
    }

    public static SkillLead createLead(SkillId id) {
        return new SkillLead.Builder(id)
                .category(SkillType.Category.NORMAL)
                .shape(MathMain.Shape.RADIAL)
                .radialPattern(PATTERN)
                .attackType(SkillType.MOVEMENT)
                .followCaster(true)
                .totalPreviewTicks(30)
                .effectTicks(1)
                .recoveryTicks(40)
                .render2D()                     // 2D描画フラグをオン
                .build();
    }
    @Override
    protected void applyToCaster(LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide) {
            // 八咫烏の足元(getY)を基準に、プレイヤーの胸の高さ(約1.25m)を指定
            double spawnY = attacker.getY() + 1.25;
            for (int i = 0; i < PATTERN.count(); i++) {
                OnibiEntity onibi = new OnibiEntity(ModEntitieType.ONIBI.get(), level);
                // 正しい引数の数（3つ）で位置を設定
                onibi.setPos(attacker.getX(), spawnY, attacker.getZ());
                Vec3 velocity = PATTERN.velocityAt(i);
                onibi.setProperties(PATTERN.lifeTicks(), true, true, velocity);
                level.addFreshEntity(onibi);
            }
        }
    }
}