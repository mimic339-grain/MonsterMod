package com.mimic.monstermod.util;

import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * サーバー側で遅延実行を管理するユーティリティ。
 * PotionEffect（見た目）と組み合わせて、数秒後のダメージなどを確実に実行します。
 */
public class DelayUtil {

    // 遅延タスクを保持するリスト
    private static final List<DelayedTask> DELAYED_TASKS = new ArrayList<>();

    /**
     * 遅延タスクを予約する
     * @param target 対象のエンティティ
     * @param ticks 実行までの時間（1秒 = 20ticks）
     * @param action 実行したい処理 (target) -> { ... }
     */
    public static void setDelay(LivingEntity target, int ticks, Consumer<LivingEntity> action) {
        if (target == null || ticks <= 0) return;
        DELAYED_TASKS.add(new DelayedTask(target, ticks, action));
    }

    /**
     * サーバーのTickイベントから毎ティック呼び出される
     */
    public static void tickDelayedTasks() {
        if (DELAYED_TASKS.isEmpty()) return;

        Iterator<DelayedTask> iterator = DELAYED_TASKS.iterator();
        while (iterator.hasNext()) {
            DelayedTask task = iterator.next();
            task.ticks--;

            // 時間が来た場合
            if (task.ticks <= 0) {
                // エンティティが存在し、かつ生きている場合のみ実行
                if (task.target != null && task.target.isAlive() && !task.target.isRemoved()) {
                    try {
                        task.action.accept(task.target);
                    } catch (Exception e) {
                        System.err.println("DelayUtil: タスク実行中にエラーが発生しました。");
                        e.printStackTrace();
                    }
                }
                iterator.remove(); // 実行後、または対象消失後にリストから削除
            }
        }
    }

    // 遅延タスクを保持するための内部レコード的クラス
    private static class DelayedTask {
        final LivingEntity target;
        int ticks;
        final Consumer<LivingEntity> action;

        DelayedTask(LivingEntity target, int ticks, Consumer<LivingEntity> action) {
            this.target = target;
            this.ticks = ticks;
            this.action = action;
        }
    }
}