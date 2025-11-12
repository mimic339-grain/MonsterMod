package com.mimic.monstermod.identity;

import com.google.gson.JsonObject;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.util.MonsterAnimationUtil;
import com.mimic.monstermod.util.MonsterAnimationUtil.ModelBuildResult;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 完全版 BaseMonsterIdentity — MonsterAnimationUtil 対応
 * - GeoJSON → ModelPart 階層 + namedParts → boneMap
 * - cubeなしボーンも ModelPart 作成
 * - pivot/origin 補正 + Z反転対応
 * - reflection は modelRoot 注入のみに限定
 */
public class BaseMonsterIdentity {

    protected final String id;
    @Nullable protected BaseMonsterEntity entity;
    @Nullable public AnimationPlayerTemplate.AnimationPlayer animationPlayer;
    public final Map<String, AnimationPlayerTemplate.ModelPartProxy> boneMap = new LinkedHashMap<>();
    protected Map<String, Map<String, Vector3f>> lastBoneTransforms = new LinkedHashMap<>();
    public String currentState = "idle";
    public boolean loop = true;
    public int[] abilityCooldowns;

    public BaseMonsterIdentity(ResourceLocation mobId, int abilityCount) {
        this.id = mobId.toString();
        this.abilityCooldowns = new int[Math.max(abilityCount, 1)];
    }

    // ---------------------------
    // Entity access
    // ---------------------------
    @Nullable
    public BaseMonsterEntity getEntity() { return entity; }

    public void setEntity(@Nullable BaseMonsterEntity entity) {
        this.entity = entity;
        autoInitBoneMap(entity);
    }

    @Nullable
    protected BaseMonsterEntity createClientEntity(Player player) { return null; }

    public void ensureClientEntity(Player player) {
        if (entity != null) return;
        BaseMonsterEntity e = createClientEntity(player);
        if (e != null) {
            e.setPos(player.getX(), player.getY(), player.getZ());
            setEntity(e);
        } else {
            MonsterMod.LOGGER.error("[BaseMonsterIdentity] createClientEntity returned null for " + id);
        }
    }

    public String getId() { return id; }

    // ---------------------------
    // Input handling / skills
    // ---------------------------
    public void handleClientInput(@Nullable Player player, int skillIndex) {
        MonsterMod.LOGGER.debug("[BaseMonsterIdentity] handleClientInput called: skillIndex={}", skillIndex);
    }

    private boolean pendingDodge = false;
    public void setPendingDodge(boolean dodge) {
        this.pendingDodge = dodge;
        MonsterMod.LOGGER.debug("[BaseMonsterIdentity] setPendingDodge called: {}", dodge);
    }

    public void handleMenuInput(Player player) {
        if (player == null) return;
        MonsterMod.LOGGER.debug("[BaseMonsterIdentity] handleMenuInput called for player {}", player.getName().getString());
    }

    public int consumeSkill() { return -1; }

    protected void updateAnimationStateServer(Player player) { }

    // ---------------------------
    // BoneMap initialization
    // ---------------------------
    public void autoInitBoneMap(@Nullable BaseMonsterEntity entity) {
        boneMap.clear();
        if (entity == null) return;

        entity.ensureModelInitialized();
        ModelPart existingRoot = entity.getModelRoot();
        JsonObject modelJson = BaseMonsterIdentity.loadModelJson(id);

        if (modelJson == null) {
            MonsterMod.LOGGER.warn("[BaseMonsterIdentity] No geo.json for {}, falling back.", id);
            if (existingRoot != null) registerPartsRecursive(existingRoot, "root");
            return;
        }

        int texWidth = modelJson.has("texture_width") ? modelJson.get("texture_width").getAsInt() : 64;
        int texHeight = modelJson.has("texture_height") ? modelJson.get("texture_height").getAsInt() : 64;

        ModelBuildResult buildResult;
        try {
            buildResult = MonsterAnimationUtil.buildModelFromJson(modelJson, texWidth, texHeight);
        } catch (Exception ex) {
            MonsterMod.LOGGER.error("[BaseMonsterIdentity] buildModelFromJson failed for " + id, ex);
            if (existingRoot != null) registerPartsRecursive(existingRoot, "root");
            return;
        }

        boolean injected = tryInjectModelRootIntoEntity(entity, buildResult.root);
        if (!injected) {
            if (existingRoot != null) registerPartsRecursive(existingRoot, "root");
            else setNamedPartsFromModelMap(buildResult.namedParts);
            return;
        }

        setNamedPartsFromModelMap(buildResult.namedParts);
        MonsterMod.LOGGER.info("[BaseMonsterIdentity] autoInitBoneMap complete for {}. bones = {}", id, boneMap.size());
    }

    private boolean tryInjectModelRootIntoEntity(BaseMonsterEntity entity, ModelPart rootToInject) {
        try {
            Field f = null;
            Class<?> cls = entity.getClass();
            while (cls != null && cls != Object.class) {
                try { f = cls.getDeclaredField("modelRoot"); break; } catch (NoSuchFieldException ignored) {}
                try { f = cls.getDeclaredField("root"); break; } catch (NoSuchFieldException ignored) {}
                try { f = cls.getDeclaredField("model"); break; } catch (NoSuchFieldException ignored) {}
                cls = cls.getSuperclass();
            }
            if (f == null) return false;
            f.setAccessible(true);
            f.set(entity, rootToInject);
            return true;
        } catch (Throwable t) {
            MonsterMod.LOGGER.warn("[BaseMonsterIdentity] Reflection inject modelRoot failed: {}", t.toString());
            return false;
        }
    }

    public void setNamedPartsFromModelMap(Map<String, ModelPart> namedParts) {
        boneMap.clear();
        if (namedParts == null) return;
        for (var e : namedParts.entrySet()) {
            String name = e.getKey();
            ModelPart part = e.getValue();
            boneMap.put(name, new AnimationPlayerTemplate.ModelPartProxy() {
                @Override public void setRotation(Vector3f rot) {
                    if (rot == null) return;
                    part.xRot = rot.x(); part.yRot = rot.y(); part.zRot = rot.z();
                }
                @Override public void setPosition(Vector3f pos) {
                    if (pos == null) return;
                    part.setPos(pos.x(), pos.y(), pos.z());
                }
                @Override public void setScale(Vector3f scale) {}
            });
        }
        MonsterMod.LOGGER.debug("[BaseMonsterIdentity] setNamedPartsFromModelMap registered {} bones.", boneMap.size());
    }

    private void registerPartsRecursive(ModelPart part, String name) {
        boneMap.put(name, new AnimationPlayerTemplate.ModelPartProxy() {
            @Override public void setRotation(Vector3f rot) { part.xRot = rot.x(); part.yRot = rot.y(); part.zRot = rot.z(); }
            @Override public void setPosition(Vector3f pos) { part.x = pos.x(); part.y = pos.y(); part.z = pos.z(); }
            @Override public void setScale(Vector3f scale) {}
        });

        try {
            var childrenField = ModelPart.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var children = (Map<String, ModelPart>) childrenField.get(part);
            if (children != null) for (var e : children.entrySet()) registerPartsRecursive(e.getValue(), e.getKey());
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[MonsterMod] registerPartsRecursive failed", e);
        }
    }

    // ---------------------------
    // Model JSON loading
    // ---------------------------
    @Nullable
    public static JsonObject loadModelJson(String id) {
        try {
            String[] parts = id.split(":");
            String modid = parts.length > 1 ? parts[0] : MonsterMod.MOD_ID;
            String path  = parts.length > 1 ? parts[1] : parts[0];
            ResourceLocation res = new ResourceLocation(modid, "models/" + path + ".geo.json");
            var optRes = Minecraft.getInstance().getResourceManager().getResource(res);
            if (optRes.isEmpty()) return null;
            try (var reader = new java.io.InputStreamReader(optRes.get().open())) {
                return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[BaseMonsterIdentity] Failed to load model JSON for " + id, e);
            return null;
        }
    }

    // ---------------------------
    // Rendering + Animation
    // ---------------------------
    public void renderInterpolated(BaseMonsterEntity entity, float partialTicks,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity == null) return;
        entity.ensureModelInitialized();
        ModelPart root = entity.getModelRoot();
        if (root == null) return;

        poseStack.pushPose();

        if (animationPlayer != null) {
            Map<String, Map<String, Vector3f>> interpPose =
                    AnimationPlayerTemplate.blend(lastBoneTransforms, animationPlayer.getCurrentPose(), partialTicks);
            for (var entry : boneMap.entrySet()) {
                var transforms = interpPose.get(entry.getKey());
                if (transforms != null)
                    AnimationPlayerTemplate.applyPoseToProxy(entry.getValue(), transforms);
            }
        }

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(getTexture()));
        root.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        poseStack.popPose();
    }

    @Nullable
    public ResourceLocation getTexture() {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }

    // ---------------------------
    // Tick
    // ---------------------------
    public void tick(Player player, float deltaSeconds) {
        if (player.level().isClientSide) tickClient(deltaSeconds);
        else tickServer(player, deltaSeconds);
    }

    public void tickClient(float deltaSeconds) {
        if (animationPlayer != null) {
            animationPlayer.tick(deltaSeconds);
            lastBoneTransforms = animationPlayer.getCurrentPose();
        }
    }

    public void tickServer(Player player, float deltaSeconds) {
        for (int i = 0; i < abilityCooldowns.length; i++) if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;
        if (animationPlayer != null) {
            animationPlayer.tick(deltaSeconds);
            lastBoneTransforms = animationPlayer.getCurrentPose();
        }
    }

    // ---------------------------
    // NBT
    // ---------------------------
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("cooldownCount", abilityCooldowns.length);
        for (int i = 0; i < abilityCooldowns.length; i++) tag.putInt("cd_" + i, abilityCooldowns[i]);
        tag.putString("state", currentState);
        tag.putBoolean("loop", loop);
        if (animationPlayer != null) tag.putFloat("anim_time", animationPlayer.getTime());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        int count = tag.getInt("cooldownCount");
        abilityCooldowns = new int[Math.max(count, 1)];
        for (int i = 0; i < count; i++) abilityCooldowns[i] = tag.getInt("cd_" + i);
        String s = tag.getString("state");
        boolean l = tag.getBoolean("loop");
        float t = tag.getFloat("anim_time");
        playAnimation(s.isEmpty() ? "idle" : s, l, t, 0f);
    }

    // ---------------------------
    // Animation control
    // ---------------------------
    public void playAnimation(String state, boolean loop, float startTime, float blendTime) {
        var clip = loadClip(state);
        if (clip == null) {
            MonsterMod.LOGGER.warn("[BaseMonsterIdentity] Missing anim: " + state + " for " + id);
            return;
        }
        var prev = animationPlayer;
        var newPlayer = new AnimationPlayerTemplate.AnimationPlayer(clip);
        if (prev != null && blendTime > 0) newPlayer.blendFromPrevious(prev, blendTime);
        newPlayer.setLoop(loop); newPlayer.setTime(startTime);
        animationPlayer = newPlayer; currentState = state; this.loop = loop;
    }

    @Nullable
    protected AnimationPlayerTemplate.AnimationClip loadClip(String name) {
        try {
            String[] parts = id.split(":");
            String modid = parts.length > 1 ? parts[0] : MonsterMod.MOD_ID;
            String path = parts.length > 1 ? parts[1] : parts[0];
            ResourceLocation res = new ResourceLocation(modid, "animations/" + path + "/" + name + ".json");
            var player = AnimationPlayerTemplate.load(res);
            if (player == null) return null;
            var anim = player.getAnimation(name);
            if (anim == null) return null;
            return new AnimationPlayerTemplate.AnimationClip(anim);
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[BaseMonsterIdentity] Load clip failed: " + name + " for " + id, e);
            return null;
        }
    }

    public float getAnimationTime() { return animationPlayer != null ? animationPlayer.getTime() : 0f; }

    // ---------------------------
    // Server sync helpers
    // ---------------------------
    public Map<String, float[]> getPoseArrayForSync() {
        Map<String, float[]> map = new LinkedHashMap<>();
        for (var entry : lastBoneTransforms.entrySet()) {
            String boneName = entry.getKey();
            Map<String, Vector3f> transforms = entry.getValue();
            Vector3f pos = transforms.getOrDefault("position", new Vector3f(0,0,0));
            Vector3f rot = transforms.getOrDefault("rotation", new Vector3f(0,0,0));
            map.put(boneName, new float[] { pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z() });
        }
        return map;
    }

    public void applyServerTransforms(Map<String, float[]> syncedTransforms) {
        if (syncedTransforms == null) return;
        for (var entry : syncedTransforms.entrySet()) {
            String boneName = entry.getKey();
            float[] arr = entry.getValue();
            if (arr == null || arr.length < 6) continue;
            Map<String, Vector3f> transforms = lastBoneTransforms.computeIfAbsent(boneName, k -> new LinkedHashMap<>());
            transforms.put("position", new Vector3f(arr[0], arr[1], arr[2]));
            transforms.put("rotation", new Vector3f(arr[3], arr[4], arr[5]));
        }
    }
}
