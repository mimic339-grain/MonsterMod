package com.mimic.monstermod.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 会話1本ぶん(複数ページ)。IDで識別し、コマンドやエンティティ紐付けから呼び出す。
 * 実体は DialogueStore(ワールドデータ)に保存される。
 */
public class DialogueSet {

    private final String id;
    private final List<DialoguePage> pages = new ArrayList<>();

    public DialogueSet(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public List<DialoguePage> getPages() { return pages; }
    public boolean isEmpty() { return pages.isEmpty(); }

    public void addPage(DialoguePage page) { pages.add(page); }
    public void clearPages() { pages.clear(); }

    // ---- NBT(ワールド保存用) ----
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        ListTag list = new ListTag();
        for (DialoguePage p : pages) list.add(p.save());
        tag.put("pages", list);
        return tag;
    }

    public static DialogueSet load(CompoundTag tag) {
        DialogueSet set = new DialogueSet(tag.getString("id"));
        ListTag list = tag.getList("pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            set.pages.add(DialoguePage.load(list.getCompound(i)));
        }
        return set;
    }

    // ---- ネットワーク(再生時に定義ごとクライアントへ送る) ----
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeVarInt(pages.size());
        for (DialoguePage p : pages) p.write(buf);
    }

    public static DialogueSet read(FriendlyByteBuf buf) {
        DialogueSet set = new DialogueSet(buf.readUtf());
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) set.pages.add(DialoguePage.read(buf));
        return set;
    }
}
