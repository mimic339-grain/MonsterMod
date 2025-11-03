package com.mimic.monstermod.identity;

import com.google.gson.*;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CIdentityAnimSyncPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BaseMonsterIdentity {

    protected final String id;
    @Nullable protected BaseMonsterEntity entity;
    @Nullable public AnimationPlayerTemplate.AnimationPlayer animationPlayer;
    protected final Map<String, AnimationPlayerTemplate.ModelPartProxy> boneMap = new HashMap<>();
    protected Map<String, Map<String, Vector3f>> lastBoneTransforms = new HashMap<>();
    public String currentState = "idle";
    public boolean loop = true;
    public int[] abilityCooldowns;

    private boolean pendingDodge = false;
    private int pendingSkill = -1;
    private boolean pendingMenu = false;

    public BaseMonsterIdentity(ResourceLocation mobId, int abilityCount) {
        this.id = mobId.toString();
        this.abilityCooldowns = new int[Math.max(abilityCount, 1)];
    }

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

    /** GeoJSON → ModelPart 生成 */
    private static ModelPart parseBoneRecursive(JsonObject obj, Map<String, ModelPart> boneMap) {
        String name = obj.has("name") ? obj.get("name").getAsString() : "bone";
        CubeListBuilder cubes = CubeListBuilder.create();
        if (obj.has("cubes")) {
            for (JsonElement e : obj.getAsJsonArray("cubes")) {
                JsonObject cube = e.getAsJsonObject();
                float[] origin = {0f, 0f, 0f};
                float[] size = {0f, 0f, 0f};
                if (cube.has("origin")) { JsonArray arr = cube.getAsJsonArray("origin"); for (int i = 0; i < 3; i++) origin[i] = arr.get(i).getAsFloat(); }
                if (cube.has("size")) { JsonArray arr = cube.getAsJsonArray("size"); for (int i = 0; i < 3; i++) size[i] = arr.get(i).getAsFloat(); }
                if (cube.has("from") && cube.has("to")) { JsonArray f = cube.getAsJsonArray("from"); JsonArray t = cube.getAsJsonArray("to"); for (int i = 0; i < 3; i++) origin[i] = f.get(i).getAsFloat(); for (int i = 0; i < 3; i++) size[i] = t.get(i).getAsFloat() - origin[i]; }
                cubes.addBox(origin[0], origin[1], origin[2], size[0], size[1], size[2]);
            }
        }
        float px = 0f, py = 0f, pz = 0f;
        if (obj.has("pivot")) { JsonArray arr = obj.getAsJsonArray("pivot"); px = arr.get(0).getAsFloat(); py = arr.get(1).getAsFloat(); pz = arr.get(2).getAsFloat(); }
        float rx = 0f, ry = 0f, rz = 0f;
        if (obj.has("rotation")) { JsonArray arr = obj.getAsJsonArray("rotation"); rx = arr.get(0).getAsFloat(); ry = arr.get(1).getAsFloat(); rz = arr.get(2).getAsFloat(); }

        ModelPart part = new ModelPart(Collections.emptyList(), new HashMap<>());
        part.x = px; part.y = py; part.z = pz;
        part.xRot = rx; part.yRot = ry; part.zRot = rz;

        if (obj.has("children")) {
            JsonObject children = obj.getAsJsonObject("children");
            for (Map.Entry<String, JsonElement> entry : children.entrySet()) {
                ModelPart child = parseBoneRecursive(entry.getValue().getAsJsonObject(), boneMap);
                try { var field = ModelPart.class.getDeclaredField("children"); field.setAccessible(true); Map<String, ModelPart> map = (Map<String, ModelPart>) field.get(part); map.put(entry.getKey(), child); }
                catch (Exception ex) { MonsterMod.LOGGER.error("[MonsterMod] Failed to add child bone", ex); }
            }
        }

        boneMap.put(name, part);
        return part;
    }

    public static ModelPart generateModelFromGeoJSON(ResourceLocation geoLoc) {
        try {
            var optRes = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(geoLoc);
            if (optRes.isEmpty()) { MonsterMod.LOGGER.warn("[MonsterMod] GeoJSON not found: " + geoLoc); return null; }
            try (InputStreamReader reader = new InputStreamReader(optRes.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray bones = root.has("minecraft:geometry") ?
                        root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")
                        : root.getAsJsonArray("bones");
                if (bones == null || bones.size() == 0) return null;
                ModelPart rootPart = new ModelPart(Collections.emptyList(), new HashMap<>());
                Map<String, ModelPart> boneMap = new HashMap<>();
                for (JsonElement e : bones) parseBoneRecursive(e.getAsJsonObject(), boneMap);
                try {
                    var field = ModelPart.class.getDeclaredField("children");
                    field.setAccessible(true);
                    Map<String, ModelPart> map = (Map<String, ModelPart>) field.get(rootPart);
                    for (var entry : boneMap.entrySet()) map.put(entry.getKey(), entry.getValue());
                } catch (Exception ex) { MonsterMod.LOGGER.error("[MonsterMod] Failed to attach bones to root", ex); }
                return rootPart;
            }
        } catch (Exception ex) { MonsterMod.LOGGER.error("[MonsterMod] Failed to parse GeoJSON: " + geoLoc, ex); return null; }
    }

    /** BoneMap初期化 */
    public void autoInitBoneMap(@Nullable BaseMonsterEntity entity) {
        boneMap.clear();
        if (entity == null) return;
        entity.ensureModelInitialized();
        ModelPart root = entity.getModelRoot();
        if (root == null) return;
        registerPartsRecursive(root, "root");
        MonsterMod.LOGGER.info("[MonsterMod] Initialized boneMap for " + id + ": " + boneMap.keySet());
    }

    private void registerPartsRecursive(ModelPart part, String name) {
        boneMap.put(name, new AnimationPlayerTemplate.ModelPartProxy() {
            @Override public void setRotation(Vector3f rot) { part.xRot = rot.x(); part.yRot = rot.y(); part.zRot = rot.z(); }
            @Override public void setPosition(Vector3f pos) { part.x = pos.x(); part.y = pos.y(); part.z = pos.z(); }
            @Override public void setScale(Vector3f scale) { }
        });
        try {
            var field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            Map<String, ModelPart> children = (Map<String, ModelPart>) field.get(part);
            for (var entry : children.entrySet()) registerPartsRecursive(entry.getValue(), entry.getKey());
        } catch (Exception e) { MonsterMod.LOGGER.error("[MonsterMod] Bone registration failed", e); }
    }

    /** Tick / サーバー同期 / クライアント補間 */
    public void tick(Player player, float deltaSeconds) {
        if (player.level().isClientSide) tickClient(deltaSeconds);
        else tickServer(player, deltaSeconds);
    }

    protected void tickServer(Player player, float deltaSeconds) {
        for (int i = 0; i < abilityCooldowns.length; i++) if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;
        updateAnimationStateServer(player);
        if (animationPlayer != null) { animationPlayer.tick(deltaSeconds); lastBoneTransforms = animationPlayer.getCurrentPose(); }
        if (player instanceof ServerPlayer sp && animationPlayer != null) {
            var packet = new S2CIdentityAnimSyncPacket(sp.getUUID(), getPoseArrayForSync());
            ModMessages.sendToAllClientsExcept(packet, sp);
        }
    }

    public void tickClient(float deltaSeconds) { if (animationPlayer != null) { animationPlayer.tick(deltaSeconds); lastBoneTransforms = animationPlayer.getCurrentPose(); } }

    /** 描画補間 */
    public void renderInterpolated(BaseMonsterEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity == null || animationPlayer == null) return;
        entity.ensureModelInitialized();
        ModelPart root = entity.getModelRoot();
        if (root == null) return;

        poseStack.pushPose();
        Map<String, Map<String, Vector3f>> interpolatedPose = AnimationPlayerTemplate.blend(lastBoneTransforms, animationPlayer.getCurrentPose(), partialTicks);
        for (var entry : boneMap.entrySet()) {
            var transforms = interpolatedPose.get(entry.getKey());
            if (transforms != null) AnimationPlayerTemplate.applyPoseToProxy(entry.getValue(), transforms);
        }

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(getTexture()));
        root.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        poseStack.popPose();
    }

    @Nullable
    public ResourceLocation getTexture() { return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png"); }

    /** Animation管理 */
    protected void updateAnimationStateServer(Player player) {
        boolean isMoving = player.getDeltaMovement().lengthSqr() > 0.01;
        boolean dodgePressed = consumeDodge();
        int skillPressed = consumeSkill();
        String next = "idle"; boolean nextLoop = true;
        if (dodgePressed) { next = "dodge"; nextLoop = false; }
        else if (skillPressed >= 0) { next = "skill_" + (skillPressed + 1); nextLoop = false; }
        else if (isMoving) next = "walk";
        if (!next.equals(currentState) || loop != nextLoop) playAnimation(next, nextLoop, getAnimationTime(), 0.15f);
    }

    public void playAnimation(String state, boolean loop, float startTime, float blendTime) {
        AnimationPlayerTemplate.AnimationClip clip = loadClip(state);
        if (clip == null) { MonsterMod.LOGGER.warn("[MonsterMod] Missing animation: " + state + " for " + id); return; }
        var prev = animationPlayer;
        var newPlayer = new AnimationPlayerTemplate.AnimationPlayer(clip);
        if (prev != null && blendTime > 0f) newPlayer.blendFromPrevious(prev, blendTime);
        newPlayer.setLoop(loop); newPlayer.setTime(startTime);
        animationPlayer = newPlayer; currentState = state; this.loop = loop;
    }

    public float getAnimationTime() { return animationPlayer != null ? animationPlayer.getTime() : 0f; }

    protected AnimationPlayerTemplate.AnimationClip loadClip(String name) {
        try {
            String[] parts = id.split(":"); String modid = parts.length > 1 ? parts[0] : MonsterMod.MOD_ID; String path = parts.length > 1 ? parts[1] : parts[0];
            ResourceLocation res = new ResourceLocation(modid, "animations/" + path + "/" + name + ".json");
            AnimationPlayerTemplate player = AnimationPlayerTemplate.load(res);
            if (player == null) return null;
            var anim = player.getAnimation(name); if (anim == null) return null;
            return new AnimationPlayerTemplate.AnimationClip(anim);
        } catch (Exception e) { MonsterMod.LOGGER.error("[MonsterMod] Exception loading animation " + name + " for " + id, e); return null; }
    }

    /** Pose同期 */
    public Map<String, float[]> getPoseArrayForSync() {
        Map<String, float[]> map = new HashMap<>();
        if (animationPlayer == null) return map;
        for (String bone : boneMap.keySet()) {
            var transforms = lastBoneTransforms.get(bone);
            if (transforms == null) continue;
            Vector3f pos = transforms.getOrDefault("position", new Vector3f());
            Vector3f rot = transforms.getOrDefault("rotation", new Vector3f());
            map.put(bone, new float[]{pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z()});
        }
        return map;
    }

    public void applyServerTransforms(Map<String, float[]> serverPose) {
        if (serverPose == null || serverPose.isEmpty()) return;
        for (var entry : serverPose.entrySet()) {
            float[] arr = entry.getValue(); if (arr == null || arr.length < 6) continue;
            Map<String, Vector3f> target = Map.of("position", new Vector3f(arr[0], arr[1], arr[2]), "rotation", new Vector3f(arr[3], arr[4], arr[5]));
            Map<String, Vector3f> prev = lastBoneTransforms.getOrDefault(entry.getKey(), target);
            lastBoneTransforms.put(entry.getKey(), AnimationPlayerTemplate.lerpPose(prev, target, 0.25f));
        }
    }

    /** 入力 / NBT */
    public void setPendingDodge(boolean dodge){ this.pendingDodge=dodge; }
    public void setPendingSkill(int skillIndex){ this.pendingSkill=skillIndex; }
    public void setPendingMenu(boolean menu){ this.pendingMenu=menu; }
    public boolean consumeDodge(){ boolean v=pendingDodge; pendingDodge=false; return v; }
    public int consumeSkill(){ int v=pendingSkill; pendingSkill=-1; return v; }
    public boolean consumeMenu(){ boolean v=pendingMenu; pendingMenu=false; return v; }
    public void handleClientInput(Player player,int skillIndex){ if(skillIndex>=0)setPendingSkill(skillIndex); }
    public void handleMenuInput(Player player){ setPendingMenu(true); }

    public CompoundTag serializeNBT(){
        CompoundTag tag=new CompoundTag();
        tag.putString("id",id); tag.putInt("cooldownCount",abilityCooldowns.length);
        for(int i=0;i<abilityCooldowns.length;i++) tag.putInt("cd_"+i,abilityCooldowns[i]);
        tag.putString("state",currentState); tag.putBoolean("loop",loop);
        if(animationPlayer!=null) tag.putFloat("anim_time",animationPlayer.getTime());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag){
        int count=tag.getInt("cooldownCount"); abilityCooldowns=new int[Math.max(count,1)];
        for(int i=0;i<count;i++) abilityCooldowns[i]=tag.getInt("cd_"+i);
        String state=tag.getString("state"); boolean loopSaved=tag.getBoolean("loop"); float savedTime=tag.getFloat("anim_time");
        playAnimation(state.isEmpty()?"idle":state,loopSaved,savedTime,0f);
    }

}

