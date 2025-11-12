package com.mimic.monstermod.util;

import com.google.gson.*;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.*;

/**
 * MonsterAnimationUtil — Bedrock形式完全対応版
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
        JsonArray bones = extractBones(rootJson);
        if (bones == null) {
            MonsterMod.LOGGER.warn("[MonsterAnimationUtil] Invalid model JSON: missing bones");
            ModelPart empty = new ModelPart(Collections.emptyList(), Collections.emptyMap());
            empty.visible = true;
            return new ModelBuildResult(empty, new LinkedHashMap<>());
        }

        Map<String, BoneDef> tempMap = new LinkedHashMap<>();

        // 1) BoneDef 作成
        for (JsonElement e : bones) {
            if (!e.isJsonObject()) continue;
            JsonObject bone = e.getAsJsonObject();
            if (!bone.has("name")) continue;
            String name = bone.get("name").getAsString();
            tempMap.put(name, new BoneDef(name));
        }

        // 2) pivot, parent, cubes 設定
        for (JsonElement e : bones) {
            if (!e.isJsonObject()) continue;
            JsonObject bone = e.getAsJsonObject();
            if (!bone.has("name")) continue;
            String name = bone.get("name").getAsString();
            BoneDef def = tempMap.get(name);
            if (def == null) continue;

            // pivot
            if (bone.has("pivot") && bone.get("pivot").isJsonArray()) {
                JsonArray pivot = bone.getAsJsonArray("pivot");
                def.pivot.set(
                        getAsFloat(pivot, 0, 0f),
                        getAsFloat(pivot, 1, 0f),
                        getAsFloat(pivot, 2, 0f)
                );
            }

            // parent
            if (bone.has("parent")) {
                String parentName = bone.get("parent").getAsString();
                if (tempMap.containsKey(parentName)) {
                    def.parent = tempMap.get(parentName);
                    tempMap.get(parentName).children.add(def);
                }
            }

            // cubes
            if (bone.has("cubes")) {
                JsonElement cubesEl = bone.get("cubes");
                if (cubesEl.isJsonArray()) {
                    for (JsonElement ce : cubesEl.getAsJsonArray()) {
                        if (ce.isJsonObject()) processCube(def, ce.getAsJsonObject());
                    }
                } else if (cubesEl.isJsonObject()) {
                    processCube(def, cubesEl.getAsJsonObject());
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
        for (BoneDef r : roots) {
            ModelPart p = partMap.get(r.name);
            if (p == null) {
                p = new ModelPart(Collections.emptyList(), Collections.emptyMap());
                p.visible = true;
                partMap.put(r.name, p);
            }
            childrenMap.put(r.name, p);
        }
        ModelPart topRoot = new ModelPart(Collections.emptyList(), childrenMap);
        topRoot.visible = true;

        MonsterMod.LOGGER.info("[MonsterAnimationUtil] Model build complete. root bones = {}, total parts = {}",
                roots.size(), partMap.size());
        MonsterMod.LOGGER.debug("[MonsterAnimationUtil] Named parts: {}", partMap.keySet());
        return new ModelBuildResult(topRoot, partMap);
    }

    private static void processCube(BoneDef def, JsonObject cube) {
        float ox = getAsFloat(cube, "origin", 0, 0f);
        float oy = getAsFloat(cube, "origin", 1, 0f);
        float oz = getAsFloat(cube, "origin", 2, 0f);
        float sx = getAsFloat(cube, "size", 0, 0f);
        float sy = getAsFloat(cube, "size", 1, 0f);
        float sz = getAsFloat(cube, "size", 2, 0f);

        int uvx = 0, uvy = 0;
        if (cube.has("uv")) {
            JsonElement uvEl = cube.get("uv");
            if (uvEl.isJsonArray()) {
                JsonArray uv = uvEl.getAsJsonArray();
                uvx = Math.round(getAsFloat(uv, 0, 0f));
                uvy = Math.round(getAsFloat(uv, 1, 0f));
            } else if (uvEl.isJsonObject()) {
                JsonObject uvObj = uvEl.getAsJsonObject();
                if (uvObj.has("north") && uvObj.get("north").isJsonObject()) {
                    JsonObject north = uvObj.getAsJsonObject("north");
                    if (north.has("uv") && north.get("uv").isJsonArray()) {
                        JsonArray uv = north.getAsJsonArray("uv");
                        uvx = Math.round(getAsFloat(uv, 0, 0f));
                        uvy = Math.round(getAsFloat(uv, 1, 0f));
                    }
                }
            }
        } else if (cube.has("uvs") && cube.get("uvs").isJsonArray()) {
            JsonArray uv = cube.getAsJsonArray("uvs");
            uvx = Math.round(getAsFloat(uv, 0, 0f));
            uvy = Math.round(getAsFloat(uv, 1, 0f));
        }

        def.cubes.add(new CubeData(ox, oy, oz, sx, sy, sz, uvx, uvy));
    }

    private static JsonArray extractBones(JsonObject rootJson) {
        if (rootJson == null) return null;
        if (rootJson.has("bones") && rootJson.get("bones").isJsonArray()) return rootJson.getAsJsonArray("bones");
        if (rootJson.has("minecraft:geometry") && rootJson.get("minecraft:geometry").isJsonArray()) {
            JsonArray geometries = rootJson.getAsJsonArray("minecraft:geometry");
            if (geometries.size() > 0 && geometries.get(0).isJsonObject()) {
                JsonObject geom = geometries.get(0).getAsJsonObject();
                if (geom.has("bones") && geom.get("bones").isJsonArray()) return geom.getAsJsonArray("bones");
            }
        }
        return null;
    }

    public static Map<String, AnimationPlayerTemplate.ModelPartProxy> loadBedrockModelAsProxies(JsonObject rootJson, int texWidth, int texHeight) {
        ModelBuildResult res = buildModelFromJson(rootJson, texWidth, texHeight);
        Map<String, AnimationPlayerTemplate.ModelPartProxy> proxies = new LinkedHashMap<>();
        for (var e : res.namedParts.entrySet()) {
            proxies.put(e.getKey(), new ModelPartProxyAdapter(e.getValue()));
        }
        return proxies;
    }

    private static ModelPart buildPartRecursive(BoneDef def, Map<String, ModelPart> partMap, int texWidth, int texHeight) {
        List<ModelPart.Cube> cubeList = new ArrayList<>();
        for (CubeData c : def.cubes) {
            float cx = c.ox - def.pivot.x;
            float cy = c.oy - def.pivot.y;
            float cz = -(c.oz - def.pivot.z); // Z反転

            cubeList.add(new ModelPart.Cube(
                    c.uvx, c.uvy,
                    cx, cy, cz,
                    c.sx, c.sy, c.sz,
                    0f, 0f, 0f,
                    false,
                    texWidth, texHeight,
                    Set.of(Direction.values())
            ));
        }

        Map<String, ModelPart> children = new LinkedHashMap<>();
        for (BoneDef child : def.children) {
            children.put(child.name, buildPartRecursive(child, partMap, texWidth, texHeight));
        }

        ModelPart part = new ModelPart(cubeList, children);
        part.setPos(def.pivot.x, def.pivot.y, def.pivot.z);
        part.visible = true;
        partMap.put(def.name, part);
        return part;
    }

    private static float getAsFloat(JsonArray arr, int index, float def) {
        return arr != null && arr.size() > index ? safeGetFloat(arr.get(index), def) : def;
    }

    private static float safeGetFloat(JsonElement el, float def) {
        try { return (el != null && !el.isJsonNull()) ? el.getAsFloat() : def; }
        catch (Exception ex) { return def; }
    }

    private static float getAsFloat(JsonObject obj, String key, int index, float def) {
        if (!obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el.isJsonArray()) return getAsFloat(el.getAsJsonArray(), index, def);
        try { return obj.get(key).getAsFloat(); }
        catch (Exception ignored) { return def; }
    }

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
        final int uvx, uvy;
        CubeData(float ox, float oy, float oz, float sx, float sy, float sz, int uvx, int uvy) {
            this.ox = ox; this.oy = oy; this.oz = oz;
            this.sx = sx; this.sy = sy;
            this.sz = sz;
            this.uvx = uvx;
            this.uvy = uvy;
        }
    }

    private static class ModelPartProxyAdapter implements AnimationPlayerTemplate.ModelPartProxy {
        private final ModelPart part;
        ModelPartProxyAdapter(ModelPart part) { this.part = part; }
        @Override public void setRotation(Vector3f rot) { if (rot != null) { part.xRot = rot.x(); part.yRot = rot.y(); part.zRot = rot.z(); } }
        @Override public void setPosition(Vector3f pos) { if (pos != null) part.setPos(pos.x(), pos.y(), pos.z()); }
        @Override public void setScale(Vector3f scale) { }
    }
}
