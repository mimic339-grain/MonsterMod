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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * .geo.json / .animation.json を、MinecraftのResourceManagerを一切経由せずに
 * JVMのクラスパスリソースとして直接読み込むローダー。
 *
 * 【なぜResourceManagerを使わないか】
 * 専用サーバー(dedicated server)は assets/ 配下のクライアント用アセットパックを
 * ロードしない。当たり判定はサーバー権威で計算する必要があるため、クライアント向けの
 * GeckoLibモデルキャッシュに依存できない。modのjarに実ファイルとして同梱されている
 * 同じ .json を直接読むことで、クライアント・専用サーバーどちらでも同一に動作させる。
 *
 * 【重要】値の変換規則は GeckoLib の BakedModelFactory / BakedAnimationsAdapter に
 * 完全に一致させてある。ここがズレると描画と当たり判定がズレるため、勝手に単純化しないこと。
 *   - ボーン pivot     : X を反転 (BakedModelFactory#constructBone)
 *   - ボーン 基本回転  : toRadians(-x, -y, +z)  (同上)
 *   - Cube origin      : (-(origin.x + size.x)/16, origin.y/16, origin.z/16)
 *   - Cube pivot       : X を反転
 *   - Cube 回転        : toRadians(-x, -y, +z)
 *   - アニメの回転     : toRadians(-x, -y, +z)  (BakedAnimationsAdapter)
 *   - アニメの位置     : 変換なし(生のピクセル値。使用時に X を反転して 1/16 する)
 * pivot / position はピクセル単位のまま保持し、行列適用時に 1/16 する
 * (GeckoLibの RenderUtils と同じ扱い)。origin / size のみブロック単位で保持する。
 */
public final class BoneRigData {

    /** アニメーションのキーフレーム。time は秒。value の単位はチャンネルにより異なる。 */
    public record Keyframe(double time, Vector3f value) {}

    /** hitbox_*ボーンが持つCube(直方体)。origin/sizeはブロック単位、pivotはピクセル単位、rotationはラジアン。 */
    public record BoneCube(Vector3f origin, Vector3f size, Vector3f pivot, Vector3f rotationRad) {}

    /** ボーンの静的データ。pivotはピクセル単位(X反転済み)、baseRotationはラジアン。 */
    public record BoneDef(String parent, Vector3f pivot, Vector3f baseRotationRad) {}

    private final Map<String, BoneDef> bones = new HashMap<>();
    private final Map<String, BoneCube> hitboxCubeOf = new HashMap<>();

    // animationName -> boneName -> キーフレーム(時系列ソート済み)
    private final Map<String, Map<String, List<Keyframe>>> rotationChannels = new HashMap<>();
    private final Map<String, Map<String, List<Keyframe>>> positionChannels = new HashMap<>();
    private final Map<String, Double> animationLength = new HashMap<>();
    private final Map<String, Boolean> animationLoop = new HashMap<>();

    private boolean loaded = false;

    public boolean isLoaded() {
        return loaded;
    }

    public BoneDef getBone(String name) {
        return bones.get(name);
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

    /** ボーンの、あるアニメーション・ある時刻(秒)における回転(ラジアン)。無ければゼロ。 */
    public Vector3f sampleRotationRad(String animation, String bone, double timeSeconds) {
        return sample(rotationChannels, animation, bone, timeSeconds);
    }

    /** ボーンの、あるアニメーション・ある時刻(秒)における位置(ピクセル)。無ければゼロ。 */
    public Vector3f samplePositionPixels(String animation, String bone, double timeSeconds) {
        return sample(positionChannels, animation, bone, timeSeconds);
    }

    private Vector3f sample(Map<String, Map<String, List<Keyframe>>> channels,
                            String animation, String bone, double timeSeconds) {
        Map<String, List<Keyframe>> boneMap = channels.get(animation);
        if (boneMap == null) return new Vector3f();
        List<Keyframe> frames = boneMap.get(bone);
        if (frames == null || frames.isEmpty()) return new Vector3f();
        if (frames.size() == 1) return new Vector3f(frames.get(0).value());

        Keyframe first = frames.get(0);
        Keyframe last = frames.get(frames.size() - 1);
        if (timeSeconds <= first.time()) return new Vector3f(first.value());
        if (timeSeconds >= last.time()) return new Vector3f(last.value());

        for (int i = 0; i < frames.size() - 1; i++) {
            Keyframe a = frames.get(i);
            Keyframe b = frames.get(i + 1);
            if (timeSeconds >= a.time() && timeSeconds <= b.time()) {
                double span = b.time() - a.time();
                float t = span <= 0 ? 0f : (float) ((timeSeconds - a.time()) / span);
                return new Vector3f(a.value()).lerp(b.value(), t);
            }
        }
        return new Vector3f(last.value());
    }

    /** 指定ボーンから根までの親チェーン(根が先頭)を返す */
    public List<String> resolveParentChain(String boneName) {
        List<String> chain = new ArrayList<>();
        String current = boneName;
        while (current != null && bones.containsKey(current)) {
            chain.add(current);
            current = bones.get(current).parent();
        }
        Collections.reverse(chain);
        return chain;
    }

    public static BoneRigData load(String geoResourcePath, String animationResourcePath, List<String> hitboxBoneNames) {
        BoneRigData data = new BoneRigData();
        try {
            data.loadGeometry(geoResourcePath, hitboxBoneNames);
            data.loadAnimations(animationResourcePath);
            data.loaded = true;
            MonsterMod.LOGGER.info("[BoneRigData] {} を読み込みました (ボーン{}個 / ヒットボックス{}個 / アニメーション{}個)",
                    geoResourcePath, data.bones.size(), data.hitboxCubeOf.size(), data.animationLength.size());
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
        for (JsonElement geoEl : root.getAsJsonArray("minecraft:geometry")) {
            for (JsonElement boneEl : geoEl.getAsJsonObject().getAsJsonArray("bones")) {
                JsonObject bone = boneEl.getAsJsonObject();
                String name = bone.get("name").getAsString();
                String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;

                // GeckoLib BakedModelFactory#constructBone と同じ変換
                Vector3f rawPivot = bone.has("pivot") ? readVec3(bone.getAsJsonArray("pivot")) : new Vector3f();
                Vector3f pivot = new Vector3f(-rawPivot.x, rawPivot.y, rawPivot.z);

                Vector3f rawRot = bone.has("rotation") ? readVec3(bone.getAsJsonArray("rotation")) : new Vector3f();
                Vector3f baseRot = new Vector3f(
                        (float) Math.toRadians(-rawRot.x),
                        (float) Math.toRadians(-rawRot.y),
                        (float) Math.toRadians(rawRot.z));

                bones.put(name, new BoneDef(parent, pivot, baseRot));

                if (hitboxBoneNames.contains(name) && bone.has("cubes")) {
                    JsonArray cubes = bone.getAsJsonArray("cubes");
                    if (cubes.size() > 0) {
                        hitboxCubeOf.put(name, parseCube(cubes.get(0).getAsJsonObject()));
                    }
                }
            }
        }
    }

    /** GeckoLib BakedModelFactory#constructCube と同じ変換 */
    private BoneCube parseCube(JsonObject cube) {
        Vector3f size = readVec3(cube.getAsJsonArray("size"));
        Vector3f rawOrigin = readVec3(cube.getAsJsonArray("origin"));

        // origin = (-(origin.x + size.x)/16, origin.y/16, origin.z/16)
        Vector3f origin = new Vector3f(
                -(rawOrigin.x + size.x) / 16f,
                rawOrigin.y / 16f,
                rawOrigin.z / 16f);
        Vector3f vertexSize = new Vector3f(size).mul(1f / 16f);

        Vector3f rawCubePivot = cube.has("pivot") ? readVec3(cube.getAsJsonArray("pivot")) : new Vector3f();
        Vector3f cubePivot = new Vector3f(-rawCubePivot.x, rawCubePivot.y, rawCubePivot.z);

        Vector3f rawCubeRot = cube.has("rotation") ? readVec3(cube.getAsJsonArray("rotation")) : new Vector3f();
        Vector3f cubeRot = new Vector3f(
                (float) Math.toRadians(-rawCubeRot.x),
                (float) Math.toRadians(-rawCubeRot.y),
                (float) Math.toRadians(rawCubeRot.z));

        return new BoneCube(origin, vertexSize, cubePivot, cubeRot);
    }

    private void loadAnimations(String path) throws IOException {
        JsonObject root = readClasspathJson(path);
        JsonObject animations = root.getAsJsonObject("animations");
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            String animName = entry.getKey();
            JsonObject anim = entry.getValue().getAsJsonObject();

            animationLength.put(animName, anim.has("animation_length") ? anim.get("animation_length").getAsDouble() : 0.0);
            animationLoop.put(animName, anim.has("loop") && anim.get("loop").isJsonPrimitive()
                    && anim.get("loop").getAsJsonPrimitive().isBoolean() && anim.get("loop").getAsBoolean());

            Map<String, List<Keyframe>> rotMap = new HashMap<>();
            Map<String, List<Keyframe>> posMap = new HashMap<>();

            if (anim.has("bones")) {
                for (Map.Entry<String, JsonElement> boneEntry : anim.getAsJsonObject("bones").entrySet()) {
                    String boneName = boneEntry.getKey();
                    JsonObject boneAnim = boneEntry.getValue().getAsJsonObject();

                    if (boneAnim.has("rotation")) {
                        rotMap.put(boneName, parseChannel(boneAnim.get("rotation"), true));
                    }
                    if (boneAnim.has("position")) {
                        posMap.put(boneName, parseChannel(boneAnim.get("position"), false));
                    }
                }
            }
            rotationChannels.put(animName, rotMap);
            positionChannels.put(animName, posMap);
        }
    }

    /**
     * @param isRotation trueなら BakedAnimationsAdapter と同じ toRadians(-x,-y,+z) 変換を行う。
     *                   位置チャンネルは変換せず生のピクセル値のまま保持する。
     */
    private List<Keyframe> parseChannel(JsonElement channelEl, boolean isRotation) {
        List<Keyframe> frames = new ArrayList<>();

        if (channelEl.isJsonArray()) {
            frames.add(new Keyframe(0.0, convert(readVec3(channelEl.getAsJsonArray()), isRotation)));
            return frames;
        }

        JsonObject channel = channelEl.getAsJsonObject();
        if (channel.has("vector")) {
            frames.add(new Keyframe(0.0, convert(readVec3(channel.getAsJsonArray("vector")), isRotation)));
            return frames;
        }

        for (Map.Entry<String, JsonElement> keyEntry : channel.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(keyEntry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            JsonElement valueEl = keyEntry.getValue();
            JsonArray vecArr = null;
            if (valueEl.isJsonArray()) {
                vecArr = valueEl.getAsJsonArray();
            } else if (valueEl.isJsonObject()) {
                JsonObject valueObj = valueEl.getAsJsonObject();
                // {"vector":[..]} / {"pre":{"vector":[..]}} / {"post":{"vector":[..]}}
                if (valueObj.has("vector")) {
                    vecArr = valueObj.getAsJsonArray("vector");
                } else if (valueObj.has("post")) {
                    JsonElement post = valueObj.get("post");
                    vecArr = post.isJsonArray() ? post.getAsJsonArray()
                            : post.getAsJsonObject().getAsJsonArray("vector");
                }
            }
            if (vecArr == null) continue;
            frames.add(new Keyframe(time, convert(readVec3(vecArr), isRotation)));
        }
        frames.sort((a, b) -> Double.compare(a.time(), b.time()));
        return frames;
    }

    private Vector3f convert(Vector3f raw, boolean isRotation) {
        if (!isRotation) return raw;
        return new Vector3f(
                (float) Math.toRadians(-raw.x),
                (float) Math.toRadians(-raw.y),
                (float) Math.toRadians(raw.z));
    }
}
