package com.mimic.monstermod.bomb;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ボムの「付与」の読み書き。
 *
 * 付ける先はエンティティとアイテムの2種類。
 * (ブロックに仕掛けたぶんは座標で管理する必要があるので {@link BombStore} が持つ)
 *
 * 【複数持てるようにしている理由】
 * 受け渡しボムを持ったまま別のボムも掛けられる、という要望があるため、
 * 対象は常にボムの一覧を持つ形にしてある。
 * 受け渡しでは該当する1個だけを移し、解除では一覧ごと消す。
 */
public final class BombAttachment {

    private BombAttachment() {}

    private static final String KEY = "monstermod_bombs";

    // ---------------- エンティティ ----------------

    public static List<BombInstance> get(Entity entity) {
        return read(entity.getPersistentData());
    }

    public static void set(Entity entity, List<BombInstance> bombs) {
        write(entity.getPersistentData(), bombs);
    }

    public static boolean has(Entity entity) {
        return !get(entity).isEmpty();
    }

    /** その種類のボムを背負っているか。受け渡しを持っている人かどうかの判定に使う */
    public static boolean hasKind(Entity entity, BombKind kind) {
        for (BombInstance b : get(entity)) {
            if (b.getKind() == kind) return true;
        }
        return false;
    }

    public static void add(Entity entity, BombInstance bomb) {
        List<BombInstance> list = get(entity);
        list.add(bomb);
        set(entity, list);
    }

    /** 解除。付いていたボムを全て消し、消した数を返す(2重掛けでも一度で全部消える) */
    public static int clear(Entity entity) {
        int n = get(entity).size();
        entity.getPersistentData().remove(KEY);
        return n;
    }

    /**
     * 受け渡し用に、その種類のボムを1個だけ取り出す(元からは消える)。
     * タイマーは引き継がれるので、押し付けても猶予は増えない。
     */
    public static BombInstance takeOne(Entity entity, BombKind kind) {
        List<BombInstance> list = get(entity);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKind() == kind) {
                BombInstance taken = list.remove(i);
                set(entity, list);
                return taken.copyForTransfer();
            }
        }
        return null;
    }

    // ---------------- アイテム ----------------
    // アイテムに付けたボムは、見た目や名前では見分けが付かない。
    // ただしNBTが付くのでスタックできなくなり、そこだけが手掛かりになる。

    public static List<BombInstance> get(ItemStack stack) {
        return stack.hasTag() ? read(stack.getTag()) : new ArrayList<>();
    }

    public static boolean has(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(KEY);
    }

    public static void add(ItemStack stack, BombInstance bomb) {
        List<BombInstance> list = get(stack);
        list.add(bomb);
        write(stack.getOrCreateTag(), list);
    }

    public static void set(ItemStack stack, List<BombInstance> bombs) {
        write(stack.getOrCreateTag(), bombs);
    }

    public static int clear(ItemStack stack) {
        int n = get(stack).size();
        if (stack.hasTag()) stack.getTag().remove(KEY);
        return n;
    }

    // ---------------- 共通 ----------------

    private static List<BombInstance> read(CompoundTag root) {
        List<BombInstance> list = new ArrayList<>();
        if (root == null || !root.contains(KEY)) return list;
        ListTag tags = root.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < tags.size(); i++) list.add(BombInstance.load(tags.getCompound(i)));
        return list;
    }

    private static void write(CompoundTag root, List<BombInstance> bombs) {
        if (bombs.isEmpty()) {
            root.remove(KEY);
            return;
        }
        ListTag tags = new ListTag();
        for (BombInstance b : bombs) tags.add(b.save());
        root.put(KEY, tags);
    }
}
