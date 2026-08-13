package com.mimic.monstermod.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

/**
 * NPCの挙動設定。
 *
 * 【考え方】
 * 「NPC = 動かないもの」ではない。町を歩き回るNPC、交渉用に固定された死なない村人、
 * 会話できる魔物、動かない無機物など、用途ごとに挙動を組み合わせて作れるようにする。
 *
 * 【専用のNPCエンティティを作らない理由】
 * AI無効化(Mob#setNoAi)・重力(setNoGravity)・無敵(setInvulnerable)・
 * 向き(setYHeadRot)・ターゲット制御(LivingChangeTargetEvent)はいずれも
 * 特定クラスに依存しない共通の仕組みで行える。そのため
 * バニラのMobも他MODのMobも自作のMobも、そのままNPCとして使える。
 *
 * 設定はエンティティの persistentData に保存する(バニラが永続化してくれる)。
 * 実際の強制は NpcTickEvents が担当する。
 */
public record NpcSettings(
        Movement movement,        // 移動: 固定 / 自由に動き回る
        boolean attacksPlayers,   // 自分からプレイヤーを襲うか
        boolean allowRetaliation, // 攻撃されたら反撃するか
        boolean invulnerable,     // ダメージを受けないか
        LookMode lookMode,        // 視点: 固定 / プレイヤーを見続ける / 元のまま
        float fixedYaw,           // lookMode=FIXED のときの向き
        double anchorX, double anchorY, double anchorZ // movement=FIXED のときの固定位置
) {
    /** 移動の扱い */
    public enum Movement {
        FIXED, // その場に固定。水で流されず、足場が消えても落ちず、押されない
        FREE   // 元のAIのまま歩き回る
    }

    /** 視点の扱い */
    public enum LookMode {
        FREE,        // 元の挙動のまま
        LOOK_PLAYER, // 最寄りのプレイヤーを見続ける
        FIXED        // 設置したときの向きで固定
    }

    private static final String KEY = "monstermod_npc";

    /** 会話用NPCとして最も使いやすい既定値(固定・無害・死なない・プレイヤーを見る) */
    public static NpcSettings defaults(Entity e) {
        return new NpcSettings(Movement.FIXED, false, false, true,
                LookMode.LOOK_PLAYER, e.getYRot(), e.getX(), e.getY(), e.getZ());
    }

    public boolean isFixed() { return movement == Movement.FIXED; }

    /** 位置と向きだけを対象エンティティの現在値に合わせたものを返す(設置時に使う) */
    public NpcSettings anchoredAt(Entity e) {
        return new NpcSettings(movement, attacksPlayers, allowRetaliation, invulnerable,
                lookMode, e.getYRot(), e.getX(), e.getY(), e.getZ());
    }

    // ---- エンティティへの保存/読み出し ----
    public static void save(Entity e, NpcSettings s) {
        CompoundTag tag = new CompoundTag();
        tag.putString("movement", s.movement.name());
        tag.putBoolean("attacks", s.attacksPlayers);
        tag.putBoolean("retaliate", s.allowRetaliation);
        tag.putBoolean("invulnerable", s.invulnerable);
        tag.putString("look", s.lookMode.name());
        tag.putFloat("yaw", s.fixedYaw);
        tag.putDouble("ax", s.anchorX);
        tag.putDouble("ay", s.anchorY);
        tag.putDouble("az", s.anchorZ);
        e.getPersistentData().put(KEY, tag);
    }

    /** NPC設定が無ければ null */
    public static NpcSettings load(Entity e) {
        CompoundTag root = e.getPersistentData();
        if (!root.contains(KEY)) return null;
        CompoundTag tag = root.getCompound(KEY);

        Movement mv;
        try { mv = Movement.valueOf(tag.getString("movement")); }
        catch (IllegalArgumentException ex) { mv = Movement.FIXED; }

        LookMode lm;
        try { lm = LookMode.valueOf(tag.getString("look")); }
        catch (IllegalArgumentException ex) { lm = LookMode.FREE; }

        return new NpcSettings(mv,
                tag.getBoolean("attacks"), tag.getBoolean("retaliate"),
                tag.getBoolean("invulnerable"), lm, tag.getFloat("yaw"),
                tag.getDouble("ax"), tag.getDouble("ay"), tag.getDouble("az"));
    }

    public static void clear(Entity e) { e.getPersistentData().remove(KEY); }

    public static boolean isNpc(Entity e) { return e.getPersistentData().contains(KEY); }

    // ---- ネットワーク ----
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(movement);
        buf.writeBoolean(attacksPlayers);
        buf.writeBoolean(allowRetaliation);
        buf.writeBoolean(invulnerable);
        buf.writeEnum(lookMode);
        buf.writeFloat(fixedYaw);
        buf.writeDouble(anchorX); buf.writeDouble(anchorY); buf.writeDouble(anchorZ);
    }

    public static NpcSettings read(FriendlyByteBuf buf) {
        return new NpcSettings(
                buf.readEnum(Movement.class),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readEnum(LookMode.class), buf.readFloat(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
