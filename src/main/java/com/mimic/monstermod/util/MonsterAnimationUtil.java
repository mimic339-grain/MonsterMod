package com.mimic.monstermod.util;

import com.google.gson.*;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.*;

/**
 * MonsterAnimationUtil — 完全版 YSMMOD互換
 * - GeoJSON(GeckoLib/Bedrock-style) → ModelPart階層 + BoneMap生成
 * - pivot/origin の補正 (origin を pivot 基準に変換)、Z反転対応
 * - cubeなしボーンも ModelPart 作成
 * - ModelPart と namedParts マップを返す ModelBuildResult 構造
 * - loadBedrockModelAsProxies() で ModelPartProxy マップを生成
 */
public class MonsterAnimationUtil {

    public static class ModelBuildResult {
        public final ModelPart root;
        public final Map<String, ModelPart> namedParts;
        public ModelBuildResult(ModelPart root, Map<String, ModelPart> namedParts) {
            this.root = root;
            this.namedParts = Collections.unmodifiableMap(new LinkedHashMap<>(namedParts));
        }
    }

    public static ModelBuildResult buildModelFromJson(JsonObject rootJson, int texWidth, int texHeight) {
        if (rootJson == null || !rootJson.has("bones")) {
            MonsterMod.LOGGER.warn("[MonsterAnimationUtil] Invalid model JSON: missing bones");
            return new ModelBuildResult(new ModelPart(Collections.emptyList(), new HashMap<>()), new HashMap<>());
        }

        JsonArray bones = rootJson.getAsJsonArray("bones");
        Map<String, BoneDef> tempMap = new LinkedHashMap<>();

        // 1) BoneDef 作成
        for (JsonElement e : bones) {
            JsonObject bone = e.getAsJsonObject();
            String name = bone.get("name").getAsString();
            tempMap.put(name, new BoneDef(name));
        }

        // 2) pivot, parent, cubes 設定
        for (JsonElement e : bones) {
            JsonObject bone = e.getAsJsonObject();
            String name = bone.get("name").getAsString();
            BoneDef def = tempMap.get(name);

            if (bone.has("pivot")) {
                JsonArray pivot = bone.getAsJsonArray("pivot");
                def.pivot.set(
                        getAsFloat(pivot, 0, 0f),
                        getAsFloat(pivot, 1, 0f),
                        getAsFloat(pivot, 2, 0f)
                );
            }

            if (bone.has("parent")) {
                String parentName = bone.get("parent").getAsString();
                if (tempMap.containsKey(parentName)) {
                    def.parent = tempMap.get(parentName);
                    tempMap.get(parentName).children.add(def);
                }
            }

            if (bone.has("cubes")) {
                JsonArray cubes = bone.getAsJsonArray("cubes");
                for (JsonElement ce : cubes) {
                    JsonObject cube = ce.getAsJsonObject();
                    float ox = getAsFloat(cube, "origin", 0, 0f);
                    float oy = getAsFloat(cube, "origin", 1, 0f);
                    float oz = getAsFloat(cube, "origin", 2, 0f);
                    float sx = getAsFloat(cube, "size", 0, 0f);
                    float sy = getAsFloat(cube, "size", 1, 0f);
                    float sz = getAsFloat(cube, "size", 2, 0f);
                    def.cubes.add(new CubeData(ox, oy, oz, sx, sy, sz));
                }
            }
        }

        // 3) root ボーン収集
        List<BoneDef> roots = new ArrayList<>();
        for (BoneDef def : tempMap.values()) if (def.parent == null) roots.add(def);

        // 4) ModelPart 再帰生成
        Map<String, ModelPart> partMap = new LinkedHashMap<>();
        for (BoneDef rootDef : roots) {
            buildPartRecursive(rootDef, partMap, texWidth, texHeight);
        }

        // 5) トップレベル ModelPart（複数 root 対応）
        Map<String, ModelPart> childrenMap = new LinkedHashMap<>();
        for (BoneDef r : roots) childrenMap.put(r.name, partMap.get(r.name));
        ModelPart topRoot = new ModelPart(Collections.emptyList(), childrenMap);

        MonsterMod.LOGGER.info("[MonsterAnimationUtil] Model build complete. root bones = {}, total parts = {}",
                roots.size(), partMap.size());
        return new ModelBuildResult(topRoot, partMap);
    }

    public static Map<String, AnimationPlayerTemplate.ModelPartProxy> loadBedrockModelAsProxies(JsonObject rootJson, int texWidth, int texHeight) {
        ModelBuildResult res = buildModelFromJson(rootJson, texWidth, texHeight);
        Map<String, AnimationPlayerTemplate.ModelPartProxy> proxies = new LinkedHashMap<>();
        for (var e : res.namedParts.entrySet()) {
            proxies.put(e.getKey(), new ModelPartProxyAdapter(e.getValue()));
        }
        return proxies;
    }

    // ---------------------------
    // 再帰生成
    // ---------------------------
    private static ModelPart buildPartRecursive(BoneDef def, Map<String, ModelPart> partMap, int texWidth, int texHeight) {
        List<ModelPart.Cube> cubeList = new ArrayList<>();
        for (CubeData c : def.cubes) {
            float cx = c.ox - def.pivot.x;
            float cy = c.oy - def.pivot.y;
            float cz = -(c.oz - def.pivot.z); // Z反転
            cubeList.add(new ModelPart.Cube(
                    0, 0,
                    cx, cy, cz,
                    c.sx, c.sy, c.sz,
                    0f, 0f, 0f,
                    false,
                    texWidth, texHeight,
                    Set.of(Direction.values())
            ));
        }

        // 空のボーンでも ModelPart を作る
        Map<String, ModelPart> children = new LinkedHashMap<>();
        for (BoneDef child : def.children) {
            children.put(child.name, buildPartRecursive(child, partMap, texWidth, texHeight));
        }

        ModelPart part = new ModelPart(cubeList, children);
        part.setPos(def.pivot.x, def.pivot.y, def.pivot.z);
        partMap.put(def.name, part);
        return part;
    }

    private static float getAsFloat(JsonArray arr, int index, float def) {
        return arr != null && arr.size() > index ? arr.get(index).getAsFloat() : def;
    }

    private static float getAsFloat(JsonObject obj, String key, int index, float def) {
        if (!obj.has(key)) return def;
        return getAsFloat(obj.getAsJsonArray(key), index, def);
    }

    // ---------------------------
    // 内部クラス
    // ---------------------------
    private static class BoneDef {
        final String name;
        final Vector3f pivot = new Vector3f();
        BoneDef parent;
        final List<BoneDef> children = new ArrayList<>();
        final List<CubeData> cubes = new ArrayList<>();
        BoneDef(String name) { this.name = name; }
    }

    private static class CubeData {
        final float ox, oy, oz, sx, sy, sz;
        CubeData(float ox, float oy, float oz, float sx, float sy, float sz) {
            this.ox = ox; this.oy = oy; this.oz = oz;
            this.sx = sx; this.sy = sy; this.sz = sz;
        }
    }

    private static class ModelPartProxyAdapter implements AnimationPlayerTemplate.ModelPartProxy {
        private final ModelPart part;
        ModelPartProxyAdapter(ModelPart part) { this.part = part; }

        @Override public void setRotation(Vector3f rot) {
            if (rot == null) return;
            part.xRot = rot.x(); part.yRot = rot.y(); part.zRot = rot.z();
        }

        @Override public void setPosition(Vector3f pos) {
            if (pos == null) return;
            part.setPos(pos.x(), pos.y(), pos.z());
        }

        @Override public void setScale(Vector3f scale) {
            // ModelPart標準にscaleはなし
        }
    }
}
