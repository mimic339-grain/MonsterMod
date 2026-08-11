package com.mimic.monstermod.identity.monster.yatagarasu;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.entity.obj.TornadoEntity;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.skill.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TornadoSkill extends SkillEffectSpec {

    public TornadoSkill() {
        super(0, DamageType.MAGIC, SkillType.STRIKE, List.of());
    }

    public static SkillLead createLead(SkillId id) {
        return new SkillLead.Builder(id)
                .category(SkillType.Category.CANCEL)//todoキャンセルの場合プレビュー出さないようにskilllead出なってる
                .shape(MathMain.Shape.RADIAL)
                .radial(15.0f, 1.0f) // 吸引範囲を15マス程度に設定
                .attackType(SkillType.STRIKE)
                .followCaster(true)
                .totalPreviewTicks(100) // 5秒間の溜め（調整可）
                .effectTicks(100)      // 竜巻が持続する時間（5秒）
                .recoveryTicks(40)
                .render2D()
                .build();
    }

    /**
     * プレビュー（予兆）期間中に毎Tick呼ばれる処理
     */
    @Override
    public void onPreviewTick(LivingEntity attacker, int remainingTicks) {
        // プレビュー中の引き寄せ
        Level level = attacker.level();
        if (level.isClientSide) return;

        double pullRadius = 15.0;
        List<Player> targets = level.getEntitiesOfClass(Player.class, attacker.getBoundingBox().inflate(pullRadius),
                p -> p != attacker);

        for (Player target : targets) {
            Vec3 vec = attacker.position().subtract(target.position());
            if (vec.length() > 1.5) {
                // 強制的に座標を更新するのではなく、DeltaMovement で滑らかに引き寄せる
                Vec3 pull = vec.normalize().scale(0.25);
                target.setDeltaMovement(pull.x, target.getDeltaMovement().y, pull.z);
                target.hurtMarked = true;
            }
        }
    }

    
    @Override
    protected void applyToCaster(LivingEntity attacker) {
        if (attacker.level().isClientSide) return;

        // 中心（大）
        spawnTornadoEntity(attacker, attacker.position(), 7.5f, 15.0f, true);

        // 四隅（小・追尾・打ち上げ）
        Vec3[] pos = { new Vec3(5,0,5), new Vec3(-5,0,5), new Vec3(5,0,-5), new Vec3(-5,0,-5) };
        for (Vec3 offset : pos) {
            spawnTornadoEntity(attacker, attacker.position().add(offset), 5.0f, 10.0f, false);
        }
    }

    private void spawnTornadoEntity(LivingEntity owner, Vec3 pos, float size, float damage, boolean isCenter) {
        if (!owner.level().isClientSide) {
            TornadoEntity tornado = new TornadoEntity(ModEntitieType.TORNADO.get(), owner.level());
            tornado.setPos(pos.x, pos.y, pos.z);
            tornado.setProperties(owner, 100, size, damage, isCenter);

            owner.level().addFreshEntity(tornado);
        }
    }
}