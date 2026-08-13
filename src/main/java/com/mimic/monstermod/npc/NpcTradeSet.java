package com.mimic.monstermod.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 1体のNPCが持つ交渉一覧。
 *
 * エンティティの persistentData に保存するため、そのNPC固有の在庫として
 * ワールドに永続化される(交換するたびに残り回数が減り、次回も引き継がれる)。
 */
public class NpcTradeSet {

    private static final String KEY = "monstermod_npc_trades";

    private final List<NpcTrade> trades = new ArrayList<>();

    public List<NpcTrade> getTrades() { return trades; }
    public boolean isEmpty() { return trades.isEmpty(); }

    /** NBT化。エンティティにもNPCツールのアイテムにも同じ形で保存する */
    public CompoundTag toTag() {
        ListTag list = new ListTag();
        for (NpcTrade t : trades) list.add(t.save());
        CompoundTag tag = new CompoundTag();
        tag.put("trades", list);
        return tag;
    }

    public static NpcTradeSet fromTag(CompoundTag tag) {
        NpcTradeSet set = new NpcTradeSet();
        ListTag list = tag.getList("trades", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) set.trades.add(NpcTrade.load(list.getCompound(i)));
        return set;
    }

    public static void save(Entity e, NpcTradeSet set) {
        e.getPersistentData().put(KEY, set.toTag());
    }

    /** 交渉が設定されていなければ空のセットを返す */
    public static NpcTradeSet load(Entity e) {
        CompoundTag root = e.getPersistentData();
        if (!root.contains(KEY)) return new NpcTradeSet();
        return fromTag(root.getCompound(KEY));
    }

    /** 中身が1件も無い(入力も出力も空)なら実質未設定として扱う */
    public boolean hasAnyUsable() {
        for (NpcTrade t : trades) {
            if (!t.getInputs().isEmpty() || !t.getOutputs().isEmpty()) return true;
        }
        return false;
    }

    public static boolean has(Entity e) {
        return e.getPersistentData().contains(KEY);
    }

    public static void clear(Entity e) {
        e.getPersistentData().remove(KEY);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(trades.size());
        for (NpcTrade t : trades) t.write(buf);
    }

    public static NpcTradeSet read(FriendlyByteBuf buf) {
        NpcTradeSet set = new NpcTradeSet();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) set.trades.add(NpcTrade.read(buf));
        return set;
    }
}
