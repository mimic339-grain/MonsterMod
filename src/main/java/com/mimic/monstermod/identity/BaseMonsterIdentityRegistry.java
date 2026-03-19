package com.mimic.monstermod.identity;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * BaseMonsterIdentityRegistry
 *
 * 【責務】
 * ・IdentityType を参照して Identity を生成する唯一窓口
 *
 * 【重要設計】
 * ・Identity をキャッシュしない
 * ・null Entity Identity を作らない
 * ・登録作業を増やさない
 */
public final class BaseMonsterIdentityRegistry {

    private BaseMonsterIdentityRegistry() {}

    /**
     * Entity 用 Identity を生成
     */
    public static BaseMonsterIdentity create(ResourceLocation id, BaseMonsterEntity entity) {
        if (entity == null) {
            throw new IllegalStateException("Identity は entity なしで生成してはいけません: " + id);
        }

        IdentityType type = IdentityType.fromId(id);
        if (type == null) {
            MonsterMod.getLogger().error("未登録の IdentityType: {}", id);
            return null;
        }

        return type.createIdentity(entity);
    }

    /**
     * ID 存在確認（Skill / Packet 用）
     */
    public static boolean exists(ResourceLocation id) {
        return IdentityType.fromId(id) != null;
    }
}
