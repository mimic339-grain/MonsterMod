package com.mimic.monstermod.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * ブロックに仕掛けたボムの置き場。
 *
 * エンティティやアイテムと違い、ブロックはNBTを持てない(タイルエンティティが要る)ので、
 * ワールド単位の保存データとして「座標 → ボム」で持つ。
 * ワールドを再読み込みしてもタイマーが残るようにしてある。
 *
 * 使う側: {@link BombTicker}(カウントダウン)、感圧板や踏みつけの検知、解除アイテム
 */
public class BombStore extends SavedData {

    private static final String NAME = "monstermod_block_bombs";

    private final Map<BlockPos, BombInstance> bombs = new HashMap<>();

    /**
     * コマンドで固定したタイマーの長さ(tick)。0以下ならランダム。
     * 「今日は30秒固定でやろう」のように、遊ぶ側でテンポを決められるようにするためのもの。
     */
    private int fixedFuse = 0;

    public int getFixedFuse() { return fixedFuse; }

    public void setFixedFuse(int ticks) {
        this.fixedFuse = Math.max(0, ticks);
        setDirty();
    }

    public static BombStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BombStore::load, BombStore::new, NAME);
    }

    public Map<BlockPos, BombInstance> all() {
        return bombs;
    }

    public BombInstance at(BlockPos pos) {
        return bombs.get(pos.immutable());
    }

    public void put(BlockPos pos, BombInstance bomb) {
        bombs.put(pos.immutable(), bomb);
        setDirty();
    }

    public BombInstance remove(BlockPos pos) {
        BombInstance removed = bombs.remove(pos.immutable());
        if (removed != null) setDirty();
        return removed;
    }

    public boolean has(BlockPos pos) {
        return bombs.containsKey(pos.immutable());
    }

    // ---- 保存 ----
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        bombs.forEach((pos, bomb) -> {
            CompoundTag entry = bomb.save();
            entry.putLong("pos", pos.asLong());
            list.add(entry);
        });
        tag.put("bombs", list);
        tag.putInt("fixedFuse", fixedFuse);
        return tag;
    }

    public static BombStore load(CompoundTag tag) {
        BombStore store = new BombStore();
        store.fixedFuse = tag.getInt("fixedFuse");
        ListTag list = tag.getList("bombs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            store.bombs.put(BlockPos.of(entry.getLong("pos")), BombInstance.load(entry));
        }
        return store;
    }
}
