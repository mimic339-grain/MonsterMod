package com.mimic.monstermod.npc;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;

/**
 * NPCのAIをこちら側の物に差し替える。
 *
 * 【なぜ元のAIを使わないか】
 * ゾンビのような敵Mobは元のAIに「プレイヤーを追う・殴る」が含まれるため、
 * ターゲットを消すだけでは追いかけ回す挙動が残ってしまう。
 * 「攻撃しないゾンビがそのへんを歩いているだけ」を作るには、
 * 元の目標を全部消してこちらの徘徊AIを入れ直すのが確実。
 *
 * 特定のクラスに依存しない Mob#goalSelector / targetSelector しか触らないため、
 * 他MODのMobにもそのまま適用できる。
 */
public final class NpcAiUtil {

    private NpcAiUtil() {}

    private static final String TAG_AI_APPLIED = "monstermod_npc_ai";

    /**
     * 元のAIを全て取り除き、「そのへんを歩くだけ」のAIに差し替える。
     * 何度呼ばれても二重に入らないよう、適用済みかを記録しておく。
     */
    public static void applyWanderAi(Mob mob) {
        if (mob.getPersistentData().getBoolean(TAG_AI_APPLIED)) return;

        // 元の目標を全て破棄する(攻撃・追跡・繁殖などを含む)
        mob.goalSelector.removeAllGoals(g -> true);
        mob.targetSelector.removeAllGoals(g -> true);

        // 溺れないように泳ぐ
        mob.goalSelector.addGoal(0, new FloatGoal(mob));

        // 歩き回るのは経路探索できるMobのみ(できないMobはその場に留まる)
        if (mob instanceof PathfinderMob pathfinder) {
            mob.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(pathfinder, 0.6D));
        }

        // 近くのプレイヤーを見る/たまに周囲を見回す(生きている感じを出す)
        mob.goalSelector.addGoal(2, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(3, new RandomLookAroundGoal(mob));

        mob.getPersistentData().putBoolean(TAG_AI_APPLIED, true);
    }

    /**
     * AI差し替えの記録を消す。
     * 元のAIは復元できない(構成はMobごとに違うため)ので、
     * 元に戻したい場合はそのMobを出し直す必要がある。
     */
    public static void clearMark(Mob mob) {
        mob.getPersistentData().remove(TAG_AI_APPLIED);
    }
}
