package com.mimic.monstermod.animation;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 完全版 AnimationPlayerTemplate — YSMMOD準拠
 * - Bone単位で position/rotation/scale を補間
 * - AnimationPlayer で秒単位tick、ループ、前アニメーションブレンド対応
 * - BaseMonsterIdentity の boneMap に直接Pose適用可能
 */
public class AnimationPlayerTemplate {

    /* -------------------------------
       データ構造
    ------------------------------- */
    public static class Keyframe {
        public final float time;
        public final Vector3f value;
        public Keyframe(float time, Vector3f value) { this.time = time; this.value = new Vector3f(value); }
    }

    public static class BoneTrack {
        public final List<Keyframe> rotation = new ArrayList<>();
        public final List<Keyframe> position = new ArrayList<>();
        public final List<Keyframe> scale = new ArrayList<>();
    }

    public static class Animation {
        public final String name;
        public final float length;
        public final Map<String, BoneTrack> bones = new HashMap<>();
        public Animation(String name, float length) { this.name = name; this.length = length; }
    }

    /* -------------------------------
       AnimationPlayer / Clip
    ------------------------------- */
    private final Map<String, Animation> animations = new HashMap<>();
    public Animation getAnimation(String name) { return animations.get(name); }

    public static class AnimationClip {
        public final Animation animation;
        public AnimationClip(Animation anim) { this.animation = anim; }
    }

    public static class AnimationPlayer {
        private final AnimationClip clip;
        private float time = 0f;
        private boolean loop = true;
        private final Map<String, Map<String, Vector3f>> currentPose = new HashMap<>();

        public AnimationPlayer(AnimationClip clip) { this.clip = clip; }

        public String getCurrentAnimationName() {
            return (clip != null && clip.animation != null) ? clip.animation.name : "none";
        }

        public void tick(float delta) {
            if (delta <= 0f || Float.isNaN(delta) || clip == null || clip.animation == null) return;
            time += delta;
            if (time > clip.animation.length) {
                if (loop) time %= clip.animation.length;
                else time = clip.animation.length;
            }
            updatePose();
        }

        private void updatePose() {
            currentPose.clear();
            if (clip == null || clip.animation == null) return;

            for (var entry : clip.animation.bones.entrySet()) {
                String bone = entry.getKey();
                BoneTrack track = entry.getValue();
                Map<String, Vector3f> transforms = new HashMap<>();
                transforms.put("position", interpolateVec(track.position, time));
                transforms.put("rotation", interpolateVec(track.rotation, time));
                transforms.put("scale", interpolateVec(track.scale, time));
                currentPose.put(bone, transforms);
            }
        }

        public Map<String, Map<String, Vector3f>> getCurrentPose() { return currentPose; }
        public void setTime(float t) { this.time = t; updatePose(); }
        public float getTime() { return time; }
        public void setLoop(boolean loop) { this.loop = loop; }

        /** 前アニメーションとblend */
        public void blendFromPrevious(AnimationPlayer prev, float blendFactor) {
            if (prev == null || blendFactor <= 0f) return;
            Map<String, Map<String, Vector3f>> prevPose = prev.getCurrentPose();
            Map<String, Map<String, Vector3f>> blended = new HashMap<>();
            for (var entry : currentPose.entrySet()) {
                String bone = entry.getKey();
                Map<String, Vector3f> cur = entry.getValue();
                Map<String, Vector3f> prv = prevPose.getOrDefault(bone, Collections.emptyMap());
                blended.put(bone, lerpPose(prv, cur, blendFactor));
            }
            currentPose.clear();
            currentPose.putAll(blended);
        }

        /** boneMapにPose適用 */
        public void applyToProxyMap(Map<String, ModelPartProxy> proxyMap) {
            if (proxyMap == null) return;
            for (var entry : currentPose.entrySet()) {
                ModelPartProxy proxy = proxyMap.get(entry.getKey());
                if (proxy == null) continue;
                applyPoseToProxy(proxy, entry.getValue());
            }
        }

        /** ModelPartに直接Pose適用 */
        public void applyToModelParts(Map<String, ModelPart> modelParts) {
            if (modelParts == null) return;
            for (var entry : currentPose.entrySet()) {
                ModelPart part = modelParts.get(entry.getKey());
                if (part == null) continue;
                applyPoseToProxy(new ModelPartAdapter(part), entry.getValue());
            }
        }
    }

    /* -------------------------------
       YSMMOD式 ブレンド
    ------------------------------- */
    public static Map<String, Map<String, Vector3f>> blend(
            Map<String, Map<String, Vector3f>> from,
            Map<String, Map<String, Vector3f>> to,
            float partialTicks
    ) {
        if (to == null) return new HashMap<>();
        if (from == null || from.isEmpty()) return deepCopyPose(to);

        Map<String, Map<String, Vector3f>> out = new HashMap<>();
        for (String bone : to.keySet()) {
            Map<String, Vector3f> fromBone = from.getOrDefault(bone, Collections.emptyMap());
            Map<String, Vector3f> toBone = to.get(bone);
            out.put(bone, lerpPose(fromBone, toBone, partialTicks));
        }
        return out;
    }

    private static Map<String, Map<String, Vector3f>> deepCopyPose(Map<String, Map<String, Vector3f>> pose) {
        Map<String, Map<String, Vector3f>> copy = new HashMap<>();
        for (var entry : pose.entrySet()) {
            Map<String, Vector3f> inner = new HashMap<>();
            for (var t : entry.getValue().entrySet()) inner.put(t.getKey(), new Vector3f(t.getValue()));
            copy.put(entry.getKey(), inner);
        }
        return copy;
    }

    /* -------------------------------
       補間 / lerp
    ------------------------------- */
    public static Vector3f interpolateVec(List<Keyframe> frames, float t) {
        if (frames == null || frames.isEmpty()) return new Vector3f(0, 0, 0);
        if (frames.size() == 1) return new Vector3f(frames.get(0).value);
        if (t <= frames.get(0).time) return new Vector3f(frames.get(0).value);
        if (t >= frames.get(frames.size() - 1).time) return new Vector3f(frames.get(frames.size() - 1).value);

        for (int i = 0; i < frames.size() - 1; i++) {
            Keyframe a = frames.get(i), b = frames.get(i + 1);
            if (t >= a.time && t <= b.time) {
                float f = (t - a.time) / (b.time - a.time);
                return new Vector3f(
                        Mth.lerp(f, a.value.x(), b.value.x()),
                        Mth.lerp(f, a.value.y(), b.value.y()),
                        Mth.lerp(f, a.value.z(), b.value.z())
                );
            }
        }
        return new Vector3f(frames.get(frames.size() - 1).value);
    }

    public static Map<String, Vector3f> lerpPose(Map<String, Vector3f> from, Map<String, Vector3f> to, float f) {
        Map<String, Vector3f> out = new HashMap<>();
        if (to == null) return out;
        for (String key : to.keySet()) {
            Vector3f vFrom = from != null ? from.getOrDefault(key, new Vector3f()) : new Vector3f();
            Vector3f vTo = to.get(key);
            out.put(key, new Vector3f(
                    Mth.lerp(f, vFrom.x(), vTo.x()),
                    Mth.lerp(f, vFrom.y(), vTo.y()),
                    Mth.lerp(f, vFrom.z(), vTo.z())
            ));
        }
        return out;
    }

    public static void applyPoseToProxy(ModelPartProxy proxy, Map<String, Vector3f> transforms) {
        if (proxy == null || transforms == null) return;
        Vector3f rot = transforms.getOrDefault("rotation", new Vector3f());
        Vector3f pos = transforms.getOrDefault("position", new Vector3f());
        Vector3f scale = transforms.getOrDefault("scale", new Vector3f(1, 1, 1));
        proxy.setRotation(rot);
        proxy.setPosition(pos);
        proxy.setScale(scale);
    }

    /* -------------------------------
       ModelPart proxy interface
    ------------------------------- */
    public interface ModelPartProxy {
        void setRotation(Vector3f rot);
        void setPosition(Vector3f pos);
        void setScale(Vector3f scale);
    }

    public static class ModelPartAdapter implements ModelPartProxy {
        private final ModelPart part;
        public ModelPartAdapter(ModelPart part) { this.part = part; }
        @Override public void setRotation(Vector3f rot) {
            if (rot == null || part == null) return;
            part.xRot = (float) rot.x();
            part.yRot = (float) rot.y();
            part.zRot = (float) rot.z();
        }
        @Override public void setPosition(Vector3f pos) { if (pos != null && part != null) part.setPos(pos.x(), pos.y(), pos.z()); }
        @Override public void setScale(Vector3f scale) { /* optional */ }
    }

    /* -------------------------------
       JSON 読み込み
    ------------------------------- */
    public static void readKeyframes(JsonArray arr, List<Keyframe> list) {
        if (arr == null || list == null) return;
        for (JsonElement e : arr) {
            try {
                JsonObject obj = e.getAsJsonObject();
                float t = obj.get("time").getAsFloat();
                JsonArray v = obj.getAsJsonArray("value");
                list.add(new Keyframe(t, new Vector3f(v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat())));
            } catch (Exception ex) { System.err.println("[MonsterMod] Failed to parse keyframe: " + ex); }
        }
        list.sort(Comparator.comparingDouble(k -> k.time));
    }

    public static AnimationPlayerTemplate load(ResourceLocation loc) {
        AnimationPlayerTemplate player = new AnimationPlayerTemplate();
        try {
            var optional = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (optional.isEmpty()) { System.err.println("[MonsterMod] Animation resource not found: " + loc); return player; }
            try (InputStreamReader reader = new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject anims = root.has("animations") ? root.getAsJsonObject("animations") : root;
                for (var entry : anims.entrySet()) {
                    String name = entry.getKey();
                    try {
                        JsonObject obj = entry.getValue().getAsJsonObject();
                        float len = obj.has("length") ? obj.get("length").getAsFloat() : 1f;
                        Animation anim = new Animation(name, len);
                        if (obj.has("bones")) {
                            JsonObject bones = obj.getAsJsonObject("bones");
                            for (var boneEntry : bones.entrySet()) {
                                String boneName = boneEntry.getKey();
                                try {
                                    JsonObject boneObj = boneEntry.getValue().getAsJsonObject();
                                    BoneTrack track = new BoneTrack();
                                    if (boneObj.has("rotation")) readKeyframes(boneObj.getAsJsonArray("rotation"), track.rotation);
                                    if (boneObj.has("position")) readKeyframes(boneObj.getAsJsonArray("position"), track.position);
                                    if (boneObj.has("scale")) readKeyframes(boneObj.getAsJsonArray("scale"), track.scale);
                                    anim.bones.put(boneName, track);
                                } catch (Exception bex) { System.err.println("[MonsterMod] Failed to parse bone: " + boneName + " -> " + bex); }
                            }
                        }
                        player.animations.put(name, anim);
                    } catch (Exception e) { System.err.println("[MonsterMod] Failed to parse animation: " + name + " -> " + e); }
                }
            }
        } catch (Exception e) { System.err.println("[MonsterMod] Failed to load animation file: " + loc + " -> " + e); }
        return player;
    }
}
