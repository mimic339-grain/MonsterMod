package net.mimic.monstermod.variable.entity;

import java.util.List;
import java.util.Map;

public interface IPlayerData extends IEntityData  {
    // 選択可能なスキル枠（例：3）
    List<String> getSkillSlots();
    void setSkillSlot(int index, String skillId);

    // UI操作・キーバインド管理
    Map<Integer, String> getKeybindMap();  // key → skillId

    // Tick処理（クールダウン減算など）
    void tick();
}
