package com.mimic.monstermod.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * 「このエンティティを右クリックしたらこの会話」という紐付け。
 *
 * Capabilityを増やすと同期・永続化の面倒が増えるため、
 * バニラが元から永続化してくれる Entity#getPersistentData() に保存する。
 * (村人・プレイヤーどちらにも同じ方法で付けられる)
 *
 * 書き込みは DialogueEditorItem、読み出しは DialogueInteractEvents / VillagerMixin。
 */
public final class DialogueBinding {

    private DialogueBinding() {}

    private static final String KEY = "monstermod_dialogue_id";

    /** このエンティティに会話を紐付ける。idがnull/空なら紐付けを解除 */
    public static void set(Entity entity, String dialogueId) {
        CompoundTag data = entity.getPersistentData();
        if (dialogueId == null || dialogueId.isEmpty()) {
            data.remove(KEY);
        } else {
            data.putString(KEY, dialogueId);
        }
    }

    /** 紐付けられた会話ID。無ければnull */
    public static String get(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(KEY)) return null;
        String id = data.getString(KEY);
        return id.isEmpty() ? null : id;
    }

    public static boolean has(Entity entity) {
        return get(entity) != null;
    }
}
