package com.mimic.monstermod.util;

import com.google.gson.*;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.*;

/**
 * MonsterAnimationUtil — YSMMOD 互換 Bedrock形式対応（pivot 親子相対化済み）
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

        Map<String, BoneDef> defs = new LinkedHashMap<>();

        // Step1: ボーン定義登録
        for (JsonElement e : bones) {
            if (!e.isJsonObject()) continue;
            JsonObject bone = e.getAsJsonObject();
            if (!bone.has("name")) continue;
            defs.put(bone.get("name").getAsString(), new BoneDef(bone.get("name").getAsString()));
        }

        // Step2: pivot, parent, cubes を設定
        for (JsonElement e : bones) {
            if (!e.isJsonObject()) continue;
            JsonObject bone = e.getAsJsonObject();
            String name = bone.get("name").getAsString();
            BoneDef def = defs.get(name);
            if (def == null) continue;

            // pivot
            if (bone.has("pivot") && bone.get("pivot").isJsonArray()) {
                JsonArray pivot = bone.getAsJsonArray("pivot");
                def.pivot.set(getAsFloat(pivot,0,0f), getAsFloat(pivot,1,0f), getAsFloat(pivot,2,0f));
            }

            // parent
            if (bone.has("parent")) {
                String parentName = bone.get("parent").getAsString();
                if (defs.containsKey(parentName)) {
                    def.parent = defs.get(parentName);
                    defs.get(parentName).children.add(def);
                }
            }

            // cubes
            if (bone.has("cubes")) {
                JsonElement cubesEl = bone.get("cubes");
                if (cubesEl.isJsonArray()) for (JsonElement ce : cubesEl.getAsJsonArray()) if (ce.isJsonObject()) processCube(def, ce.getAsJsonObject());
                else if (cubesEl.isJsonObject()) processCube(def, cubesEl.getAsJsonObject());
            }
        }

        // Step3: root bones
        List<BoneDef> roots = new ArrayList<>();
        for (BoneDef def : defs.values()) if (def.parent == null) roots.add(def);

        // Step4: pivot を親子相対化
        for (BoneDef def : defs.values()) if (def.parent != null) def.pivot.sub(def.parent.pivot, def.pivot);

        // Step5: ModelPart 再帰構築
        Map<String, ModelPart> namedParts = new LinkedHashMap<>();
        ModelPart rootPart;
        if (roots.size() == 1) rootPart = buildPartRecursive(roots.get(0), namedParts, texWidth, texHeight);
        else {
            Map<String, ModelPart> children = new LinkedHashMap<>();
            for (BoneDef r : roots) children.put(r.name, buildPartRecursive(r, namedParts, texWidth, texHeight));
            rootPart = new ModelPart(Collections.emptyList(), children);
        }

        MonsterMod.LOGGER.info("[MonsterAnimationUtil] Build OK - roots: {}, parts: {}", roots.size(), namedParts.size());
        return new ModelBuildResult(rootPart, namedParts);
    }

    private static void processCube(BoneDef def, JsonObject cube) {
        float ox = getAsFloat(cube, "origin", 0, 0f);
        float oy = getAsFloat(cube, "origin", 1, 0f);
        float oz = getAsFloat(cube, "origin", 2, 0f);
        float sx = getAsFloat(cube, "size", 0, 0f);
        float sy = getAsFloat(cube, "size", 1, 0f);
        float sz = getAsFloat(cube, "size", 2, 0f);

        int uvx = 0, uvy = 0;
        if (cube.has("uv") && cube.get("uv").isJsonArray()) {
            JsonArray uv = cube.getAsJsonArray("uv");
            uvx = Math.round(getAsFloat(uv, 0, 0f));
            uvy = Math.round(getAsFloat(uv, 1, 0f));
        }

        // pivot 差分を引いて cube 相対座標化
        def.cubes.add(new CubeData(ox - def.pivot.x(), oy - def.pivot.y(), oz - def.pivot.z(), sx, sy, sz, uvx, uvy));
    }

    private static ModelPart buildPartRecursive(BoneDef def, Map<String, ModelPart> namedParts, int texWidth, int texHeight) {
        List<ModelPart.Cube> cubeList = new ArrayList<>();
        for (CubeData c : def.cubes) cubeList.add(new ModelPart.Cube(
                c.uvx, c.uvy, c.ox, c.oy, c.oz, c.sx, c.sy, c.sz,
                0f,0f,0f, false, texWidth, texHeight, Set.of(Direction.values())
        ));

        Map<String, ModelPart> children = new LinkedHashMap<>();
        for (BoneDef child : def.children) children.put(child.name, buildPartRecursive(child, namedParts, texWidth, texHeight));

        ModelPart part = new ModelPart(cubeList, children);
        part.setPos(def.pivot.x(), def.pivot.y(), def.pivot.z());
        part.visible = true;
        namedParts.put(def.name, part);
        return part;
    }

    private static JsonArray extractBones(JsonObject rootJson) {
        if (rootJson == null) return null;
        if (rootJson.has("bones") && rootJson.get("bones").isJsonArray()) return rootJson.getAsJsonArray("bones");
        if (rootJson.has("minecraft:geometry") && rootJson.get("minecraft:geometry").isJsonArray()) {
            JsonArray geom = rootJson.getAsJsonArray("minecraft:geometry");
            if (!geom.isEmpty() && geom.get(0).isJsonObject()) {
                JsonObject g = geom.get(0).getAsJsonObject();
                if (g.has("bones") && g.get("bones").isJsonArray()) return g.getAsJsonArray("bones");
            }
        }
        return null;
    }

    private static float getAsFloat(JsonArray arr, int idx, float def) { return arr != null && arr.size() > idx ? safeGetFloat(arr.get(idx), def) : def; }
    private static float safeGetFloat(JsonElement el, float def) { try { return el != null && !el.isJsonNull() ? el.getAsFloat() : def; } catch(Exception e){ return def; } }
    private static float getAsFloat(JsonObject obj, String key, int idx, float def) {
        if (!obj.has(key)) return def;
        JsonElement el = obj.get(key);
        if (el.isJsonArray()) return getAsFloat(el.getAsJsonArray(), idx, def);
        return safeGetFloat(el, def);
    }

    private static class BoneDef {
        final String name;
        final Vector3f pivot = new Vector3f();
        BoneDef parent;
        final List<BoneDef> children = new ArrayList<>();
        final List<CubeData> cubes = new ArrayList<>();
        BoneDef(String n){ name = n; }
    }

    private static class CubeData {
        final float ox, oy, oz, sx, sy, sz;
        final int uvx, uvy;
        CubeData(float ox,float oy,float oz,float sx,float sy,float sz,int uvx,int uvy){
            this.ox=ox;this.oy=oy;this.oz=oz;this.sx=sx;this.sy=sy;this.sz=sz;this.uvx=uvx;this.uvy=uvy;
        }
    }

    public static Map<String, AnimationPlayerTemplate.ModelPartProxy> loadBedrockModelAsProxies(JsonObject rootJson,int texWidth,int texHeight){
        ModelBuildResult res = buildModelFromJson(rootJson, texWidth, texHeight);
        Map<String, AnimationPlayerTemplate.ModelPartProxy> proxies = new LinkedHashMap<>();
        for(var e: res.namedParts.entrySet()){
            ModelPart p = e.getValue();
            proxies.put(e.getKey(), new AnimationPlayerTemplate.ModelPartProxy(){
                @Override public void setRotation(Vector3f rot){ if(rot!=null){ p.xRot = (float)Math.toRadians(rot.x()); p.yRot = (float)Math.toRadians(rot.y()); p.zRot = (float)Math.toRadians(rot.z()); } }
                @Override public void setPosition(Vector3f pos){ if(pos!=null) p.setPos(pos.x(), pos.y(), pos.z()); }
                @Override public void setScale(Vector3f scale){ if(scale!=null){ p.xScale=scale.x(); p.yScale=scale.y(); p.zScale=scale.z(); } }
            });
        }
        return proxies;
    }
}
