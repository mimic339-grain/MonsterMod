package com.mimic.monstermod.skill;

public enum SkillType {
    NONE,           // 何もしない（予兆のみ等）
    STRIKE,         // 攻撃スキル（これまでのENTITY_AOEに近い）
    MOVEMENT,          // 回避　移動スキル（★TODO: 無敵時間の追加）
    TOUCH,          // 接触スキル（★TODO: 自分を赤く発光させ、触れた相手にダメージ）
    SPECIAL;        // その他

    public enum Category {
        NORMAL,
        COMBO,
        EMERGENCY
    }
}