package com.mimic.monstermod.skill;

/**
 * AttackType
 *
 * 【役割】
 * ・SkillLead が「このスキルは何を攻撃するか」を宣言するための型
 *
 * 【設計原則】
 * ・宣言専用（declarative）
 * ・ダメージ計算・実行・条件分岐ロジックは一切持たせない
 * ・Server 側 Executor がこの値を switch して処理を分岐する
 *
 * 【重要】
 * ・Client は描画可否の判断以外で使用してはならない
 */
public enum AttackType {

    /**
     * 攻撃なし
     *
     * ・Preview 専用
     * ・演出のみ（罠予告 / 範囲表示 / チャージ表現など）
     */
    NONE,

    /**
     * Entity AoE 攻撃
     *
     * ・MathMain.contains(Vec3) を唯一の判定真理として使用
     * ・AoEExecutor → Entity 判定
     */
    ENTITY_AOE,

    /**
     * Block AoE 攻撃
     *
     * ・SamplerBlock2D による BlockPos 量子化
     * ・BlockAttackExecutor による Block 処理
     */
    BLOCK_AOE
}
