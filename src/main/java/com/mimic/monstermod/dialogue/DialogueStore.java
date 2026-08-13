package com.mimic.monstermod.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ゲーム内で作った会話定義を「ワールドデータ」として永続化する保管庫。
 *
 * MOD内のJSONではなくワールドに保存するため、
 * ゲーム内で作った会話がそのままセーブデータに残り、MODの再ビルドが不要になる。
 *
 * 保存先はオーバーワールド固定(ディメンションを跨いで同じ会話を使えるようにするため)。
 * 取得は DialogueStore.get(server) を使う。
 */
public class DialogueStore extends SavedData {

    private static final String DATA_NAME = "monstermod_dialogues";

    private final Map<String, DialogueSet> dialogues = new LinkedHashMap<>();

    public DialogueStore() {}

    /** サーバーからこの保管庫を取得する(無ければ生成) */
    public static DialogueStore get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                DialogueStore::load, DialogueStore::new, DATA_NAME);
    }

    @Nullable
    public DialogueSet getDialogue(String id) {
        return dialogues.get(id);
    }

    public List<String> listIds() {
        return new ArrayList<>(dialogues.keySet());
    }

    /** 会話を登録・上書きする。呼んだら保存が必要なのでsetDirtyする */
    public void put(DialogueSet set) {
        dialogues.put(set.getId(), set);
        setDirty();
    }

    public boolean remove(String id) {
        boolean removed = dialogues.remove(id) != null;
        if (removed) setDirty();
        return removed;
    }

    // ---- SavedData ----
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DialogueSet set : dialogues.values()) list.add(set.save());
        tag.put("dialogues", list);
        return tag;
    }

    public static DialogueStore load(CompoundTag tag) {
        DialogueStore store = new DialogueStore();
        ListTag list = tag.getList("dialogues", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            DialogueSet set = DialogueSet.load(list.getCompound(i));
            store.dialogues.put(set.getId(), set);
        }
        return store;
    }
}
