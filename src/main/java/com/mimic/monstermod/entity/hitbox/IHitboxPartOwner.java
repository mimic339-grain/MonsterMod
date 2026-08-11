package com.mimic.monstermod.entity.hitbox;

/**
 * 部位当たり判定パーツを保持しているエンティティ(Mixinで付与されるPlayer)へアクセスするための窓口。
 * Mixinで生やしたメンバへ外部から安全に触るために用意している。
 */
public interface IHitboxPartOwner {
    BoneHitboxPart[] monstermod$getHitboxParts();
}
