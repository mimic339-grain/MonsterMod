package com.mimic.monstermod.entity.hitbox;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mimic.monstermod.MonsterMod;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * .geo.json / .animation.json を、MinecraftのResourceManagerを一切経由せずに
 * JVMのクラスパスリソースとして直接読み込むローダー。
 *
 * 【なぜこうするか】
 * 専用サーバー(dedicated server)は通常 assets/ 配下のクライアント用アセットパックを
 * ロードしない。GeckoLibのモデル/アニメーションキャッシュはクライアント向けの仕組みの
 * ため、サーバー側の当たり判定計算がそれに依存すると専用サーバーで動かない恐れがある。
 * ここではmodのjarに実ファイルとして同梱されている同じ .json を、Minecraftのアセット
 * 管理を介さず直接読むことで、クライアント・専用サーバーどちらでも同一に動作させる。
 *
 * Blockbench/GeckoLibの座標単位(1 = 1/16ブロック)は、ここでブロック単位に変換して保持する。
 */
public final class BoneRigData {

    private static final float UNITS_TO_BLOCKS = 1.0f / 16.0f;

    /** rotation(度)のキーフレーム。time は秒。 */
    public record Keyframe(double time, Vector3f rotationDeg) {}

    /** hitbox_*ボーンが持つ、ボーンローカル空間でのCube(直方体)のサイズ・原点(ブロック単位) */
    public record BoneCube(Vector3f origin, Vector3f size) {}

    private final Map<String, String> parentOf = new HashMap<>();
    private final Map<String, Vector3f> pivotOf = new HashMap<>();
    private final Map<String, BoneCube> hitboxCubeOf = new HashMap<>();
    // animationName -> boneName -> 時系列ソート済みのキーフレーム
    private final Map<String, Map<String, List<Keyframe>>> rotationByAnimation = new HashMap<>();
    private final Map<String, Double> animationLength = new HashMap<>();
    private final Map<String, Boolean> animationLoop = new HashMap<>();

    private boolean loaded = false;

    public boolean isLoaded() {
        return loaded;
    }

    public String getParent(String bone) {
        return parentOf.get(bone);
    }

    public Vector3f getPivot(String bone) {
        return pivotOf.getOrDefault(bone, new Vector3f());
    }

    public BoneCube getHitboxCube(String bone) {
        return hitboxCubeOf.get(bone);
    }

    public double getAnimationLength(String animation) {
        return animationLength.getOrDefault(animation, 0.0);
    }

    public boolean isLooping(String animation) {
        return animationLoop.getOrDefault(animation, false);
    }

    /** ボーン単体の、あるアニメーション内・ある時刻(秒)における回転(度)を線形補間で求める */
    public Vector3f resolveRotationDeg(String animation, String bone, double timeSeconds) {
        Map<String, List<Keyframe>> boneMap = rotationByAnimation.get(animation);
        if (boneMap == null) return new Vector3f();
        List<Keyframe> frames = boneMap.get(bone);
        if (frames == null || frames.isEmpty()) return new Vector3f();
        if (frames.size() == 1) return new Vector3f(frames.get(0).rotationDeg());

        if (timeSeconds <= frames.get(0).time()) return new Vector3f(frames.get(0).rotationDeg());
        Keyframe last = frames.get(frames.size() - 1);
        if (timeSeconds >= last.time()) return new Vector3f(last.rotationDeg());

        for (int i = 0; i < frames.size() - 1; i++) {
            Keyframe a = frames.get(i);
            Keyframe b = frames.get(i + 1);
            if (timeSeconds >= a.time() && timeSeconds <= b.time()) {
                double span = b.time() - a.time();
                float t = span <= 0 ? 0f : (float) ((timeSeconds - a.time()) / span);
                return new Vector3f(a.rotationDeg()).lerp(b.rotationDeg(), t);
            }
        }
        return new Vector3f(last.rotationDeg());
    }

    /** 指定ボーンから根までの親チェーン(根が先頭)を返す */
    public List<String> resolveParentChain(String boneName) {
        List<String> chain = new ArrayList<>();
        String current = boneName;
        while (current != null) {
            chain.add(0, current);
            current = parentOf.get(current);
        }
        return chain;
    }

    public static BoneRigData load(String geoResourcePath, String animationResourcePath, List<String> hitboxBoneNames) {
        BoneRigData data = new BoneRigData();
        try {
            data.loadGeometry(geoResourcePath, hitboxBoneNames);
            data.loadAnimations(animationResourcePath);
            data.loaded = true;
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[BoneRigData] {} / {} の読み込みに失敗しました。ボーン追従ヒットボックスは無効化されます。",
                    geoResourcePath, animationResourcePath, e);
        }
        return data;
    }

    private static JsonObject readClasspathJson(String path) throws IOException {
        try (InputStream in = BoneRigData.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IOException("classpathリソースが見つかりません: " + path);
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static Vector3f readVec3(JsonArray arr) {
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private void loadGeometry(String path, List<String> hitboxBoneNames) throws IOException {
        JsonObject root = readClasspathJson(path);
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        for (JsonElement geoEl : geometries) {
            JsonArray bones = geoEl.getAsJsonObject().getAsJsonArray("bones");
            for (JsonElement boneEl : bones) {
                JsonObject bone = boneEl.getAsJsonObject();
                String name = bone.get("name").getAsString();
                String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
                Vector3f pivot = bone.has("pivot") ? readVec3(bone.getAsJsonArray("pivot")).mul(UNITS_TO_BLOCKS) : new Vector3f();

                parentOf.put(name, parent);
                pivotOf.put(name, pivot);

                if (hitboxBoneNames.contains(name) && bone.has("cubes")) {
                    JsonArray cubes = bone.getAsJsonArray("cubes");
                    if (cubes.size() > 0) {
                        JsonObject cube = cubes.get(0).getAsJsonObject();
                        Vector3f origin = readVec3(cube.getAsJsonArray("origin")).mul(UNITS_TO_BLOCKS);
                        Vector3f size = readVec3(cube.getAsJsonArray("size")).mul(UNITS_TO_BLOCKS);
                        hitboxCubeOf.put(name, new BoneCube(origin, size));
                    }
                }
            }
        }
    }

    private void loadAnimations(String path) throws IOException {
        JsonObject root = readClasspathJson(path);
        JsonObject animations = root.getAsJsonObject("animations");
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            String animName = entry.getKey();
            JsonObject anim = entry.getValue().getAsJsonObject();

            animationLength.put(animName, anim.has("animation_length") ? anim.get("animation_length").getAsDouble() : 0.0);
            animationLoop.put(animName, anim.has("loop") && anim.get("loop").getAsBoolean());

            Map<String, List<Keyframe>> boneMap = new HashMap<>();
            if (anim.has("bones")) {
                JsonObject bones = anim.getAsJsonObject("bones");
                for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                    String boneName = boneEntry.getKey();
                    JsonObject boneAnim = boneEntry.getValue().getAsJsonObject();
                    if (!boneAnim.has("rotation")) continue;

                    List<Keyframe> frames = parseRotationChannel(boneAnim.get("rotation"));
                    frames.sort((a, b) -> Double.compare(a.time(), b.time()));
                    boneMap.put(boneName, frames);
                }
            }
            rotationByAnimation.put(animName, boneMap);
        }
    }

    private List<Keyframe> parseRotationChannel(JsonElement rotationEl) {
        List<Keyframe> frames = new ArrayList<>();
        if (rotationEl.isJsonArray()) {
            // "rotation": [x, y, z] という短縮形式(定数)
            frames.add(new Keyframe(0.0, readVec3(rotationEl.getAsJsonArray())));
            return frames;
        }

        JsonObject rotation = rotationEl.getAsJsonObject();
        if (rotation.has("vector")) {
            // "rotation": {"vector": [x,y,z]} という定数形式
            frames.add(new Keyframe(0.0, readVec3(rotation.getAsJsonArray("vector"))));
            return frames;
        }

        // "rotation": {"0.0": {"vector":[..]}, "0.5": {"vector":[..]}, ...} というキーフレーム形式
        for (Map.Entry<String, JsonElement> keyEntry : rotation.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(keyEntry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            JsonElement valueEl = keyEntry.getValue();
            JsonArray vecArr = valueEl.isJsonObject() && valueEl.getAsJsonObject().has("vector")
                    ? valueEl.getAsJsonObject().getAsJsonArray("vector")
                    : (valueEl.isJsonArray() ? valueEl.getAsJsonArray() : null);
            if (vecArr == null) continue;
            frames.add(new Keyframe(time, readVec3(vecArr)));
        }
        return frames;
    }
}
