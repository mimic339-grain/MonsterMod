package com.mimic.monstermod.identity.monster.yatagarasu;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.entity.obj.OnibiEntity;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.skill.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class OnibiSkill extends SkillEffectSpec {
    public OnibiSkill() {
        super(0, DamageType.MAGIC, SkillType.STRIKE, List.of());
    }

    public static SkillLead createLead(SkillId id) {
        return new SkillLead.Builder(id)
                .category(SkillType.Category.NORMAL)
                .shape(MathMain.Shape. RADIAL) // 円柱を指定
                .radial(40.0f, 1.0f)
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
            for (int i = 0; i < 16; i++) {
                double angle = i * (Math.PI * 2 / 16);
                OnibiEntity onibi = new OnibiEntity(ModEntitieType.ONIBI.get(), level);
                // 正しい引数の数（3つ）で位置を設定
                onibi.setPos(attacker.getX(), spawnY, attacker.getZ());
                // 等速（歩き速度 0.21）で射出
                Vec3 velocity = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(0.21);
                // 寿命を100tick(5秒)に変更
                onibi.setProperties(400, true, true, velocity);
                level.addFreshEntity(onibi);
            }
        }
    }
}