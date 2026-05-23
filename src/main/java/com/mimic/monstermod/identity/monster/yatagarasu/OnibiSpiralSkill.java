package com.mimic.monstermod.identity.monster.yatagarasu;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.entity.obj.SpiralOnibiEntity;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.skill.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
public class OnibiSpiralSkill extends SkillEffectSpec {

    public OnibiSpiralSkill() {
        super(0, DamageType.MAGIC, SkillType.STRIKE, List.of());
    }

    public static SkillLead createLead(SkillId id) {
        return new SkillLead.Builder(id)
                .category(SkillType.Category.NORMAL)
                .shape(MathMain.Shape.SPIRAL) // ★ RADIALからSPIRALに変更
                .spiral(20.0f, 1.0f)          // ★ spiralメソッドを使用
                .attackType(SkillType.MOVEMENT)
                .followCaster(true)
                .totalPreviewTicks(30)
                .effectTicks(1)
                .recoveryTicks(40)
                .render2D()
                .build();
    }
    @Override
    protected void applyToCaster(LivingEntity attacker, int tick) {
        Level level = attacker.level();
        if (!level.isClientSide) {
            // ★ 最初(tick 0)の瞬間だけ弾を生成する
            // 弾自体が勝手に回るので、毎tick出す必要はありません
            if (tick == 0) {
                double spawnY = attacker.getY() + 1.25;
                Vec3 center = new Vec3(attacker.getX(), spawnY, attacker.getZ());

                for (int j = 0; j < 16; j++) {
                    // 16方向に均等に発射
                    double startAngle = j * (Math.PI * 2 / 16);

                    // 新しいEntityを生成
                    SpiralOnibiEntity spiral = new SpiralOnibiEntity(ModEntitieType.SPIRALONIBI.get(), level);

                    // 位置を初期化
                    spiral.setPos(center.x, center.y, center.z);
                    // ★ 螺旋の挙動をセット
                    // speed: 外に広がる速さ, rotation: 回転する速さ(1tickあたり)
                    spiral.setSpiralProperties(
                            center,
                            startAngle,
                            0.1,               // 外側への広がり速度
                            Math.toRadians(4),  // 1tickあたりの回転角（ここを大きくするとキツく巻く）
                            600                 // 寿命
                    );

                    level.addFreshEntity(spiral);
                }
            }
        }
    }
}