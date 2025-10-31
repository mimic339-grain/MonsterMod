package com.mimic.monstermod.animation;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 完全版 AnimationPlayerTemplate (YSM方式 / BaseMonsterIdentity互換)
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
        public Keyframe(float time, Vector3f value) { this.time = time; this.value = value; }
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
        private Map<String, Map<String, Vector3f>> currentPose = new HashMap<>();

        public AnimationPlayer(AnimationClip clip) { this.clip = clip; }

        /** 秒単位でtick */
        public void tick(float delta) {
            if (delta <= 0f || Float.isNaN(delta)) return;
            time += delta;
            if (time > clip.animation.length) {
                if (loop) time %= clip.animation.length;
                else time = clip.animation.length;
            }
            updatePose();
        }

        private void updatePose() {
            currentPose.clear();
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

        /** 前のAnimationPlayerからブレンド */
        public void blendFromPrevious(AnimationPlayer prev, float blendTime) {
            Map<String, Map<String, Vector3f>> prevPose = prev.getCurrentPose();
            for (var entry : currentPose.entrySet()) {
                String bone = entry.getKey();
                Map<String, Vector3f> c = entry.getValue();
                Map<String, Vector3f> p = prevPose.getOrDefault(bone, c);
                currentPose.put(bone, lerpPose(p, c, blendTime));
            }
        }
    }

    /* -------------------------------
       ヘルパー
       ------------------------------- */
    public static Vector3f interpolateVec(List<Keyframe> frames, float t) {
        if (frames.isEmpty()) return new Vector3f();
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
        for (String key : to.keySet()) {
            Vector3f vFrom = from.getOrDefault(key, new Vector3f());
            Vector3f vTo = to.get(key);
            out.put(key, new Vector3f(
                    Mth.lerp(f, vFrom.x(), vTo.x()),
                    Mth.lerp(f, vFrom.y(), vTo.y()),
                    Mth.lerp(f, vFrom.z(), vTo.z())
            ));
        }
        return out;
    }

    public static Map<String, Map<String, Vector3f>> blend(Map<String, Map<String, Vector3f>> from,
                                                           Map<String, Map<String, Vector3f>> to, float f) {
        Map<String, Map<String, Vector3f>> result = new HashMap<>();
        for (String bone : to.keySet()) {
            Map<String, Vector3f> fromBone = from.getOrDefault(bone, new HashMap<>());
            Map<String, Vector3f> toBone = to.get(bone);
            result.put(bone, lerpPose(fromBone, toBone, f));
        }
        return result;
    }

    public static void applyPoseToProxy(ModelPartProxy proxy, Map<String, Vector3f> transforms) {
        if (transforms.containsKey("rotation")) proxy.setRotation(transforms.get("rotation"));
        if (transforms.containsKey("position")) proxy.setPosition(transforms.get("position"));
        if (transforms.containsKey("scale")) proxy.setScale(transforms.get("scale"));
    }

    public static void readKeyframes(JsonArray arr, List<Keyframe> list) {
        for (JsonElement e : arr) {
            JsonObject obj = e.getAsJsonObject();
            float t = obj.get("time").getAsFloat();
            JsonArray v = obj.getAsJsonArray("value");
            Vector3f vec = new Vector3f(v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat());
            list.add(new Keyframe(t, vec));
        }
        list.sort(Comparator.comparingDouble(k -> k.time));
    }

    /** JSONからAnimationPlayerTemplateをロード */
    public static AnimationPlayerTemplate load(ResourceLocation loc) {
        AnimationPlayerTemplate player = new AnimationPlayerTemplate();
        try {
            String path = "assets/" + loc.getNamespace() + "/animations/" + loc.getPath() + ".json";
            InputStreamReader reader = new InputStreamReader(
                    Objects.requireNonNull(AnimationPlayerTemplate.class.getClassLoader().getResourceAsStream(path)),
                    StandardCharsets.UTF_8
            );
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject anims = root.has("animations") ? root.getAsJsonObject("animations") : root;

            for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
                String name = entry.getKey();
                try {
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    float len = obj.has("length") ? obj.get("length").getAsFloat() : 1f;
                    Animation anim = new Animation(name, len);

                    JsonObject bones = obj.getAsJsonObject("bones");
                    for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
                        String boneName = boneEntry.getKey();
                        JsonObject boneObj = boneEntry.getValue().getAsJsonObject();
                        BoneTrack track = new BoneTrack();

                        if (boneObj.has("rotation")) readKeyframes(boneObj.getAsJsonArray("rotation"), track.rotation);
                        if (boneObj.has("position")) readKeyframes(boneObj.getAsJsonArray("position"), track.position);
                        if (boneObj.has("scale")) readKeyframes(boneObj.getAsJsonArray("scale"), track.scale);

                        anim.bones.put(boneName, track);
                    }
                    player.animations.put(name, anim);
                } catch (Exception e) {
                    System.err.println("[MonsterMod] Failed to parse animation: " + name + " -> " + e);
                }
            }
        } catch (Exception e) {
            System.err.println("[MonsterMod] Failed to load animation file: " + loc + " -> " + e);
        }
        return player;
    }

    /* -------------------------------
       ModelPart proxy interface
       ------------------------------- */
    public interface ModelPartProxy {
        void setRotation(Vector3f rot);
        void setPosition(Vector3f pos);
        void setScale(Vector3f scale);
    }
}
