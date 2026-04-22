package com.mimic.monstermod.skill.hunter;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.hunter.skills.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class HunterSkillRegistry {
    // スキルIDをキーにして、新しいHunterSkill(抽象クラス)を保持する
    private static final Map<SkillId, HunterSkill> REGISTRY = new HashMap<>();

    static {
        // ★ ここで各スキルのクラスをインスタンス化して登録する
        register(new SwiftDashSkill()); // 名前: 瞬塵 / CD: 12秒
        register(new LeapJumpSkill());  // 名前: 飛燕 / CD: 20秒
        register(new HardeningSkill());
        register(new SkyWalkerSkill());
        register(new DolphinDashSkill());

    }

    /**
     * スキルをレジストリに登録します
     */
    public static void register(HunterSkill skill) {
        REGISTRY.put(skill.getId(), skill);

        // 1. SkillLeadRegistry への登録
        com.mimic.monstermod.skill.SkillLeadRegistry.register(skill.toLead());

        // 2. SkillEffectRegistry への登録
        // SkillEffectSpec を匿名クラスで作成し、apply をオーバーライドして中身を繋ぐ
        com.mimic.monstermod.skill.SkillEffectRegistry.register(skill.getId(), new com.mimic.monstermod.skill.SkillEffectSpec(
                0.0f, // ダメージ0
                com.mimic.monstermod.skill.DamageType.PHYSICAL,
                com.mimic.monstermod.skill.SkillType.MOVEMENT,
                java.util.List.of()
        ) {
            @Override
            public void apply(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.entity.LivingEntity target) {
                // caster が ServerPlayer の時だけ、HunterSkill の applyEffect を実行する
                if (caster instanceof net.minecraft.server.level.ServerPlayer sp) {
                    skill.applyEffect(sp);
                }
            }
        });
    }
    /**
     * IDからスキルを取得します
     */
    public static HunterSkill get(SkillId id) {
        return REGISTRY.get(id);
    }

    /**
     * 登録されている全スキルを取得します
     */
    public static Collection<HunterSkill> getAll() {
        return REGISTRY.values();
    }

    // ★ 追加：外部から呼び出すための初期化トリガー
    public static void init() {
        // 何もしなくても、ここが呼ばれることで上の static ブロックが実行される
    }
}