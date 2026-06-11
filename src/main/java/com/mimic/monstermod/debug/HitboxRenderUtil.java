package com.mimic.monstermod.debug;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * デバッグ描画フラグの一元管理。
 *
 * DragonDebugCommandの静的フラグを全てここに集約。
 * VisualConfigScreen / HitboxDebugCommand の両方からここを参照する。
 *
 * 管理するフラグ:
 *   "obb"     — OBBワイヤーフレーム (旧: OBBWireframeRenderer.debugEnabled)
 *   "bones"   — ボーンライン
 *   "partHp"  — 部位HPオーバーレイ
 *   "preview" — MMO攻撃プレビュー判定範囲
 *
 * 配置: com/mimic/monstermod/client/debug/HitboxRenderUtil.java
 */
@OnlyIn(Dist.CLIENT)
public class HitboxRenderUtil {

    private static final Map<String, Boolean> FLAGS = new HashMap<>();

    static {
        FLAGS.put("obb",     false);
        FLAGS.put("bones",   false);
        FLAGS.put("partHp",  false);
        FLAGS.put("preview", false);
    }

    public static boolean isEnabled(String key) {
        return FLAGS.getOrDefault(key, false);
    }

    public static boolean toggle(String key) {
        boolean newVal = !FLAGS.getOrDefault(key, false);
        FLAGS.put(key, newVal);
        return newVal;
    }

    public static void setAll(boolean enabled) {
        FLAGS.replaceAll((k, v) -> enabled);
    }

    public static boolean isAnyEnabled() {
        return FLAGS.values().stream().anyMatch(v -> v);
    }

    public static Map<String, Boolean> getAllFlags() {
        return new HashMap<>(FLAGS); // 防御的コピー
    }
}