package com.mimic.monstermod.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 交渉1件ぶん。
 *
 * バニラの村人取引と違い、入力・出力ともに複数指定できる。
 * (例: エメラルド20 + ダイヤ20 → 木64 + 石64 + 鉄64 + 金64)
 *
 * 回数制限を持ち、使い切ると成立しなくなる。
 * 残り回数をプレイヤーに見せるかどうかも個別に選べる。
 */
public class NpcTrade {

    private final List<ItemStack> inputs = new ArrayList<>();
    private final List<ItemStack> outputs = new ArrayList<>();
    private int maxUses;      // 総交換可能回数。0以下なら無制限
    private int uses;         // 使用済み回数
    private boolean showUses; // 残り回数をプレイヤーに表示するか

    public NpcTrade() {
        this(0, true);
    }

    public NpcTrade(int maxUses, boolean showUses) {
        this.maxUses = maxUses;
        this.showUses = showUses;
    }

    public List<ItemStack> getInputs() { return inputs; }
    public List<ItemStack> getOutputs() { return outputs; }
    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int v) { this.maxUses = v; }
    public int getUses() { return uses; }
    public boolean isShowUses() { return showUses; }
    public void setShowUses(boolean v) { this.showUses = v; }

    public boolean isUnlimited() { return maxUses <= 0; }

    /** 残り回数。無制限なら Integer.MAX_VALUE */
    public int remaining() {
        return isUnlimited() ? Integer.MAX_VALUE : Math.max(0, maxUses - uses);
    }

    /** 回数を使い切っているか(バニラの赤い×と同じ状態) */
    public boolean isSoldOut() {
        return !isUnlimited() && uses >= maxUses;
    }

    public void recordUse() {
        if (!isUnlimited()) uses++;
    }

    /** プレイヤーが必要な入力アイテムを全て持っているか */
    public boolean canAfford(Player player) {
        for (ItemStack need : inputs) {
            if (need.isEmpty()) continue;
            if (countInInventory(player, need) < need.getCount()) return false;
        }
        return true;
    }

    /** 入力を消費し、出力を渡す。成立しなければ false */
    public boolean execute(Player player) {
        if (isSoldOut() || !canAfford(player)) return false;

        for (ItemStack need : inputs) {
            if (need.isEmpty()) continue;
            removeFromInventory(player, need, need.getCount());
        }
        for (ItemStack give : outputs) {
            if (give.isEmpty()) continue;
            ItemStack copy = give.copy();
            // 入りきらない分は足元に落とす(消えてしまわないように)
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
        recordUse();
        return true;
    }

    /** 同じ種類のアイテムの所持数を数える(NBTは見ず、アイテム種別で比較する) */
    private static int countInInventory(Player player, ItemStack need) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(need.getItem())) total += stack.getCount();
        }
        return total;
    }

    private static void removeFromInventory(Player player, ItemStack need, int amount) {
        int left = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (left <= 0) break;
            if (stack.isEmpty() || !stack.is(need.getItem())) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
    }

    // ---- NBT ----
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("inputs", saveStacks(inputs));
        tag.put("outputs", saveStacks(outputs));
        tag.putInt("maxUses", maxUses);
        tag.putInt("uses", uses);
        tag.putBoolean("showUses", showUses);
        return tag;
    }

    public static NpcTrade load(CompoundTag tag) {
        NpcTrade t = new NpcTrade(tag.getInt("maxUses"), tag.getBoolean("showUses"));
        t.uses = tag.getInt("uses");
        loadStacks(tag.getList("inputs", Tag.TAG_COMPOUND), t.inputs);
        loadStacks(tag.getList("outputs", Tag.TAG_COMPOUND), t.outputs);
        return t;
    }

    private static ListTag saveStacks(List<ItemStack> list) {
        ListTag out = new ListTag();
        for (ItemStack s : list) {
            if (s.isEmpty()) continue;
            out.add(s.save(new CompoundTag()));
        }
        return out;
    }

    private static void loadStacks(ListTag list, List<ItemStack> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.of(list.getCompound(i));
            if (!s.isEmpty()) into.add(s);
        }
    }

    // ---- ネットワーク ----
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(inputs.size());
        for (ItemStack s : inputs) buf.writeItem(s);
        buf.writeVarInt(outputs.size());
        for (ItemStack s : outputs) buf.writeItem(s);
        buf.writeVarInt(maxUses);
        buf.writeVarInt(uses);
        buf.writeBoolean(showUses);
    }

    public static NpcTrade read(FriendlyByteBuf buf) {
        NpcTrade t = new NpcTrade();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) t.inputs.add(buf.readItem());
        n = buf.readVarInt();
        for (int i = 0; i < n; i++) t.outputs.add(buf.readItem());
        t.maxUses = buf.readVarInt();
        t.uses = buf.readVarInt();
        t.showUses = buf.readBoolean();
        return t;
    }
}
