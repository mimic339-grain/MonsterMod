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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BaseMonsterIdentity — YSMMOD スタイル対応 完全版
 * - namedParts から boneMap を作成
 * - pivot/origin/position の二重加算を避ける
 * - 描画時に角度は度->ラジアンへ変換して適用
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

    // 再入防止フラグ（ensureModelInitialized と autoInitBoneMap のループ防止）
    private boolean boneMapInitializing = false;

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
        // bone map は autoInitBoneMap で初期化（再入防止あり）
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
    // BoneMap initialization
    // ---------------------------
    public void autoInitBoneMap(@Nullable BaseMonsterEntity entity) {
        if (entity == null || !entity.level().isClientSide) return;
        if (!boneMap.isEmpty() || boneMapInitializing) return; // 初期化済み or 進行中は抜ける

        boneMapInitializing = true;
        try {
            // モデルを確実に用意する（entity 側で深い再帰が起きる設計ならここで guard）
            try { entity.ensureModelInitialized(); } catch (Exception e) { MonsterMod.LOGGER.warn("[BaseMonsterIdentity] ensureModelInitialized threw: {}", e.toString()); }

            ModelPart root = entity.getModelRoot();
            if (root == null) return;

            // モデル JSON があるなら json→ModelPart と namedParts を取得する（失敗時は既存 root を使う）
            JsonObject modelJson = loadModelJson(id);
            ModelBuildResult buildResult = null;
            if (modelJson != null) {
                try {
                    int texWidth = modelJson.has("texture_width") ? modelJson.get("texture_width").getAsInt() : 64;
                    int texHeight = modelJson.has("texture_height") ? modelJson.get("texture_height").getAsInt() : 64;
                    buildResult = MonsterAnimationUtil.buildModelFromJson(modelJson, texWidth, texHeight);
                } catch (Exception ex) {
                    MonsterMod.LOGGER.warn("[BaseMonsterIdentity] buildModelFromJson failed for {}: {}", id, ex.toString());
                }
            }

            if (buildResult != null && buildResult.namedParts != null && !buildResult.namedParts.isEmpty()) {
                // namedParts をそのまま使用して boneMap を構築する
                // 注意: Entity の modelRoot を buildResult.root に差し替えない（意図しない副作用を避ける）
                setNamedPartsFromModelMap(buildResult.namedParts);
            } else {
                // namedParts が無い場合は既存 ModelPart 階層を再帰的に登録する
                registerPartsRecursive(root, "root");
            }
        } finally {
            boneMapInitializing = false;
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
                    // アニメーション JSON が度 (degrees) で指定されていることが多いので rad に変換して入れる
                    part.xRot = (float) Math.toRadians(rot.x());
                    part.yRot = (float) Math.toRadians(rot.y());
                    part.zRot = (float) Math.toRadians(rot.z());
                }
                @Override public void setPosition(Vector3f pos) {
                    if (pos == null) return;
                    part.setPos(pos.x(), pos.y(), pos.z());
                }
                @Override public void setScale(Vector3f scale) { /* unused */ }
            });
        }
    }

    // 再帰的に ModelPart を登録する（reflection回避版）
    private void registerPartsRecursive(ModelPart part, String name) {
        if (part == null || name == null) return;
        boneMap.put(name, new AnimationPlayerTemplate.ModelPartProxy() {
            @Override public void setRotation(Vector3f rot) {
                if (rot == null) return;
                part.xRot = (float) Math.toRadians(rot.x());
                part.yRot = (float) Math.toRadians(rot.y());
                part.zRot = (float) Math.toRadians(rot.z());
            }
            @Override public void setPosition(Vector3f pos) {
                if (pos == null) return;
                part.setPos(pos.x(), pos.y(), pos.z());
            }
            @Override public void setScale(Vector3f scale) {}
        });

        // children フィールドへの reflection は避けられない場合があるが、
        // まず ModelPart#getAllParts() を試す（API による差異があるため安全に）
        try {
            // getAllParts() は Stream<ModelPart> 返す可能性があるため互換性を考慮して配列化する
            var stream = part.getAllParts();
            stream.forEach(child -> {
                if (child != null) {
                    String childName = name + "." + Integer.toHexString(System.identityHashCode(child));
                    registerPartsRecursive(child, childName);
                }
            });
        } catch (Throwable t) {
            // fallback: reflection で children フィールドを読む（例外が出たら無視）
            try {
                var childrenField = ModelPart.class.getDeclaredField("children");
                childrenField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var children = (Map<String, ModelPart>) childrenField.get(part);
                if (children != null) for (var e : children.entrySet()) registerPartsRecursive(e.getValue(), name + "." + e.getKey());
            } catch (Exception ignored) {}
        }
    }

    @Nullable
    public static JsonObject loadModelJson(String id) {
        try {
            String[] parts = id.split(":");
            String modid = parts.length > 1 ? parts[0] : MonsterMod.MOD_ID;
            String path  = parts.length > 1 ? parts[1] : parts[0];
            var res = new ResourceLocation(modid, "models/" + path + ".geo.json");
            var optRes = Minecraft.getInstance().getResourceManager().getResource(res);
            if (optRes.isEmpty()) return null;
            try (var reader = new java.io.InputStreamReader(optRes.get().open())) {
                return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) { return null; }
    }

    // ---------------------------
    // Rendering + Animation
    // ---------------------------
    public void renderInterpolated(BaseMonsterEntity entity, float partialTicks,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity == null) return;
        ModelPart root = entity.getModelRoot();
        if (root == null) return;

        poseStack.pushPose();
        try {
            if (animationPlayer != null) {
                Map<String, Map<String, Vector3f>> interpPose =
                        AnimationPlayerTemplate.blend(lastBoneTransforms, animationPlayer.getCurrentPose(), partialTicks);
                for (var e : boneMap.entrySet()) {
                    var transforms = interpPose.get(e.getKey());
                    if (transforms != null) AnimationPlayerTemplate.applyPoseToProxy(e.getValue(), transforms);
                }
            }

            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(getTexture()));
            root.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        } finally {
            poseStack.popPose();
        }
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
        if (clip == null) return;
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
            var res = new ResourceLocation(modid, "animations/" + path + "/" + name + ".json");
            var player = AnimationPlayerTemplate.load(res);
            if (player == null) return null;
            var anim = player.getAnimation(name);
            if (anim == null) return null;
            return new AnimationPlayerTemplate.AnimationClip(anim);
        } catch (Exception e) { return null; }
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

    // input/skill placeholders (そのまま)
    public void handleClientInput(@Nullable Player player, int skillIndex) { }
    private boolean pendingDodge = false;
    public void setPendingDodge(boolean dodge) { this.pendingDodge = dodge; }
    public void handleMenuInput(Player player) { }
    public int consumeSkill() { return -1; }
    protected void updateAnimationStateServer(Player player) { }
}
