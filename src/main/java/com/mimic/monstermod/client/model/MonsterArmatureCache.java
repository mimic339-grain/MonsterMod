package com.mimic.monstermod.client.model;

import com.mimic.monstermod.model.parser.EntityModelLoader;
import com.mimic.monstermod.model.parser.ParsedModel;
import com.mimic.monstermod.model.anim.AnimationPlayer;
import com.mimic.monstermod.model.anim.LivingMotionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Monster種別ごとのArmature（モデル+AnimationPlayer）をキャッシュ。
 *
 * EFM参考:
 *   - api/asset/AnimationManager — アニメーションリソースのキャッシュ
 *   - gameasset/Animations — アニメーションの静的登録
 *
 * 設計:
 *   - MonsterArmature: ParsedModel + AnimationPlayer + LivingMotionManager を1セットで管理
 *   - プレイヤーUUIDごとにインスタンスを分けて複数プレイヤーの変身を独立して管理
 *
 * 配置: com/mimic/monstermod/client/model/MonsterArmatureCache.java
 */
@OnlyIn(Dist.CLIENT)
public class MonsterArmatureCache {

    /** モデル種別 → ParsedModel（共有・読み取り専用） */
    private static final Map<String, ParsedModel> MODEL_CACHE = new HashMap<>();

    /** プレイヤーUUID → Armatureインスタンス（AnimationPlayerを含む） */
    private static final Map<UUID, MonsterArmature> PLAYER_ARMATURES = new HashMap<>();

    // ── モデルのロード ────────────────────────────────────────────────

    /**
     * ResourceManagerからモデルをロードしてキャッシュ。
     * EFM: AnimationManager.onReloadResources() パターン。
     * ClientEvents.onClientSetup() / ResourceReloadListener から呼ぶ。
     */
    public static void loadModel(String monsterType) {
        if (MODEL_CACHE.containsKey(monsterType)) return;
        try {
            ResourceLocation loc = new ResourceLocation("monstermod", "models/entity/" + monsterType + ".json");
            ParsedModel model = EntityModelLoader.load(
                    Minecraft.getInstance().getResourceManager(), loc);
            MODEL_CACHE.put(monsterType, model);
        } catch (Exception e) {
            // モデルが見つからない場合はnullを入れてnull checkで対応
            MODEL_CACHE.put(monsterType, null);
        }
    }

    /** モデルデータ取得（nullチェック必須） */
    public static ParsedModel get(String monsterType) {
        return MODEL_CACHE.get(monsterType);
    }

    // ── プレイヤーごとのArmatureインスタンス管理 ─────────────────────

    /**
     * プレイヤーが変身したときにArmatureインスタンスを作成。
     * MonsterTransformation.transform() の後にクライアントから呼ぶ。
     */
    public static MonsterArmature getOrCreate(UUID playerUUID, String monsterType) {
        MonsterArmature existing = PLAYER_ARMATURES.get(playerUUID);
        if (existing != null && existing.monsterType.equals(monsterType)) {
            return existing;
        }
        // 新しいインスタンスを作成
        loadModel(monsterType);
        ParsedModel model = MODEL_CACHE.get(monsterType);
        MonsterArmature armature = new MonsterArmature(monsterType, model);
        PLAYER_ARMATURES.put(playerUUID, armature);
        return armature;
    }

    /** 変身解除時にインスタンスを解放 */
    public static void remove(UUID playerUUID) {
        PLAYER_ARMATURES.remove(playerUUID);
    }

    /** キャッシュ全クリア（リソースリロード時） */
    public static void clearAll() {
        MODEL_CACHE.clear();
        PLAYER_ARMATURES.clear();
    }

    // ── MonsterArmature内部クラス ──────────────────────────────────────

    /**
     * 1プレイヤーの変身状態を管理するArmatureインスタンス。
     * EFM: api/animation/Animator.java の役割に近い。
     */
    public static class MonsterArmature {
        public final String monsterType;
        public final ParsedModel model;        // 読み取り専用（共有不可）
        public final AnimationPlayer animPlayer;
        public final LivingMotionManager motionManager;

        MonsterArmature(String monsterType, ParsedModel model) {
            this.monsterType   = monsterType;
            this.model         = model;
            this.animPlayer    = new AnimationPlayer();
            this.motionManager = new LivingMotionManager(animPlayer);
        }

        /** モデルがロード済みか確認 */
        public boolean isReady() { return model != null; }
    }
}