package com.mimic.monstermod.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

/**
 * 「このエンティティをNPCとして固定する」設定。
 *
 * 【なぜ専用のNPCエンティティを作らないか】
 * AI無効化(Mob#setNoAi)・重力(Entity#setNoGravity)・無敵(Entity#setInvulnerable)
 * ・向き(LivingEntity#setYHeadRot)はすべて Mob / Entity の共通メソッドであり、
 * 外部から呼べる。そのため専用クラスを作らなくても
 * 「バニラの村人」「他MODのMob」を含む任意のMobをそのままNPC化できる。
 * 見た目や既存の挙動をそのまま活かせるのが利点。
 *
 * 設定はエンティティの persistentData に保存する(バニラが永続化してくれる)。
 * 実際の強制は NpcTickEvents が毎tick行う。
 */
public record NpcSettings(
        boolean immobile,     // 動かない(AI無効・移動量ゼロ・位置固定)
        boolean noGravity,    // 落ちない(足場を壊されても留まる)
        boolean invulnerable, // ダメージを受けない
        boolean fireProof,    // 燃えない
        LookMode lookMode,    // 首の向き
        float fixedYaw,       // lookMode=FIXED のときの向き
        double anchorX, double anchorY, double anchorZ // 固定する位置
) {
    public enum LookMode {
        FREE,        // 何もしない(元の挙動のまま)
        LOOK_PLAYER, // 最寄りのプレイヤーを向き続ける
        FIXED        // 決まった方向を向いたまま
    }

    private static final String KEY = "monstermod_npc";

    public static NpcSettings defaults(Entity e) {
        return new NpcSettings(true, true, true, true,
                LookMode.LOOK_PLAYER, e.getYRot(), e.getX(), e.getY(), e.getZ());
    }

    /** 位置だけ現在地に更新したものを返す(設置し直したときなどに使う) */
    public NpcSettings withAnchor(Entity e) {
        return new NpcSettings(immobile, noGravity, invulnerable, fireProof,
                lookMode, fixedYaw, e.getX(), e.getY(), e.getZ());
    }

    // ---- エンティティへの保存/読み出し ----
    public static void save(Entity e, NpcSettings s) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("immobile", s.immobile);
        tag.putBoolean("noGravity", s.noGravity);
        tag.putBoolean("invulnerable", s.invulnerable);
        tag.putBoolean("fireProof", s.fireProof);
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
        LookMode lm;
        try { lm = LookMode.valueOf(tag.getString("look")); }
        catch (IllegalArgumentException ex) { lm = LookMode.FREE; }
        return new NpcSettings(
                tag.getBoolean("immobile"),
                tag.getBoolean("noGravity"),
                tag.getBoolean("invulnerable"),
                tag.getBoolean("fireProof"),
                lm,
                tag.getFloat("yaw"),
                tag.getDouble("ax"), tag.getDouble("ay"), tag.getDouble("az"));
    }

    public static void clear(Entity e) {
        e.getPersistentData().remove(KEY);
    }

    public static boolean isNpc(Entity e) {
        return e.getPersistentData().contains(KEY);
    }

    // ---- ネットワーク(設定画面とのやり取り用) ----
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(immobile);
        buf.writeBoolean(noGravity);
        buf.writeBoolean(invulnerable);
        buf.writeBoolean(fireProof);
        buf.writeEnum(lookMode);
        buf.writeFloat(fixedYaw);
        buf.writeDouble(anchorX); buf.writeDouble(anchorY); buf.writeDouble(anchorZ);
    }

    public static NpcSettings read(FriendlyByteBuf buf) {
        return new NpcSettings(
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readEnum(LookMode.class), buf.readFloat(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
