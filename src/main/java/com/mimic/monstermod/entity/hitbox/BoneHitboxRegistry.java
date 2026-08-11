package com.mimic.monstermod.entity.hitbox;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「どのモンスターがどのボーン構造・部位設定を持つか」を1箇所にまとめたレジストリ。
 * 実体として出したモンスターと、そのモンスターに変身したプレイヤーの両方がここを参照するので、
 * 部位のダメージ倍率などを変えたい場合はここ(と各Profile)だけ直せばよい。
 *
 * 新しいモンスターに部位判定を付ける場合は、
 *   1. geo.jsonに hitbox_* ボーン(Cube 1個)を用意する
 *   2. 部位設定(YatagarasuHitboxProfileと同じ形)を作る
 *   3. ここに register する
 * の3手順で済むようにしてある。
 */
public final class BoneHitboxRegistry {

    private BoneHitboxRegistry() {}

    /** 1モンスター分のボーン構造+部位設定 */
    public record Rig(BoneRigData rigData, List<YatagarasuHitboxProfile.PartConfig> parts) {}

    private static final Map<ResourceLocation, Rig> RIGS = new HashMap<>();

    /**
     * プレイヤーに確保しておく部位パーツの上限数。
     * Forgeはエンティティ追加時にしかパーツを登録しないため、
     * 変身前(=どのモンスターになるか未定)の時点で最大数を確保しておく必要がある。
     * 新しいモンスターの部位数がこれを超える場合はここを増やすこと。
     */
    public static final int MAX_PARTS_PER_PLAYER = 12;

    public static void register(ResourceLocation id, Rig rig) {
        RIGS.put(id, rig);
    }

    @Nullable
    public static Rig get(@Nullable ResourceLocation id) {
        return id == null ? null : RIGS.get(id);
    }
}
