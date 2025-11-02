package com.mimic.monstermod.identity;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.animation.AnimationPlayerTemplate;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CIdentityAnimSyncPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * 完全版 BaseMonsterIdentity
 * AnimationPlayerTemplate互換 / Pose補間 / NBT保存 / 同期対応
 * GeckoLib非依存 / YSM方式
 */
public class BaseMonsterIdentity {

    protected final String id;
    @Nullable protected BaseMonsterEntity entity;
    @Nullable public AnimationPlayerTemplate.AnimationPlayer animationPlayer;
    protected final Map<String, AnimationPlayerTemplate.ModelPartProxy> boneMap = new HashMap<>();
    public Map<String, Map<String, Vector3f>> lastBoneTransforms = new HashMap<>();

    public String currentState = "idle";
    public boolean loop = true;
    public int[] abilityCooldowns;

    private boolean pendingAttack = false;
    private boolean pendingDodge = false;
    private int pendingSkill = -1;
    private boolean pendingMenu = false;
// BaseMonsterIdentity に追加
    /**
     * この Identity に対応する描画用 Entity クラスを返す
     * 必要に応じて各 Identity サブクラスでオーバーライド
     */
    public Class<? extends BaseMonsterEntity> getEntityClass() {
        // 現状は MimicEntity を返すデフォルト実装
        // 将来的に Identity Registry などでモブごとに切り替える
        return com.mimic.monstermod.entity.MimicEntity.class;
    }

    /** Identity の ID を返す */
    public String getId() {
        return id;
    }
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

    /* ---------------------------
       BoneMap 自動初期化
       --------------------------- */
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
            @Override public void setScale(Vector3f scale) { /* optional */ }
        });

        try {
            var field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            Map<String, ModelPart> children = (Map<String, ModelPart>) field.get(part);
            for (var entry : children.entrySet()) {
                registerPartsRecursive(entry.getValue(), entry.getKey());
            }
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[MonsterMod] Bone registration failed", e);
        }
    }

    /* ---------------------------
       Tick処理
       --------------------------- */
    public void tick(Player player, float deltaSeconds) {
        if (player.level().isClientSide) tickClient(deltaSeconds);
        else tickServer(player, deltaSeconds);
    }

    protected void tickServer(Player player, float deltaSeconds) {
        // クールタイム進行
        for (int i = 0; i < abilityCooldowns.length; i++)
            if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;

        updateAnimationStateServer(player);

        if (animationPlayer != null) {
            animationPlayer.tick(deltaSeconds);
            lastBoneTransforms = animationPlayer.getCurrentPose();
        }

        // サーバー→クライアント同期
        if (player instanceof ServerPlayer sp && animationPlayer != null) {
            var packet = new S2CIdentityAnimSyncPacket(sp.getUUID(), getPoseArrayForSync());
            ModMessages.sendToAllClientsExcept(packet, sp);
        }
    }

    public void tickClient(float deltaSeconds) {
        if (animationPlayer != null) {
            animationPlayer.tick(deltaSeconds);
            lastBoneTransforms = animationPlayer.getCurrentPose();
        }
    }

    /* ---------------------------
       レンダリング補間
       --------------------------- */
    public void renderInterpolated(BaseMonsterEntity entity, float partialTicks,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity == null || animationPlayer == null) return;
        entity.ensureModelInitialized();
        ModelPart root = entity.getModelRoot();
        if (root == null) return;

        poseStack.pushPose();
        Map<String, Map<String, Vector3f>> interpolatedPose = AnimationPlayerTemplate.blend(
                lastBoneTransforms, animationPlayer.getCurrentPose(), partialTicks
        );

        for (var entry : boneMap.entrySet()) {
            var transforms = interpolatedPose.get(entry.getKey());
            if (transforms != null)
                AnimationPlayerTemplate.applyPoseToProxy(entry.getValue(), transforms);
        }

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(getTexture()));
        root.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        poseStack.popPose();
    }

    @Nullable
    public ResourceLocation getTexture() {
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }

    /* ---------------------------
       アニメーション管理
       --------------------------- */
    protected void updateAnimationStateServer(Player player) {
        boolean isMoving = player.getDeltaMovement().lengthSqr() > 0.01;
        boolean attackPressed = consumeAttack();
        boolean dodgePressed = consumeDodge();
        int skillPressed = consumeSkill();

        String next = "idle";
        boolean nextLoop = true;

        if (dodgePressed) { next = "dodge"; nextLoop = false; }
        else if (attackPressed) { next = "attack"; nextLoop = false; }
        else if (skillPressed >= 0) { next = "skill_" + (skillPressed+1); nextLoop = false; }
        else if (isMoving) { next = "walk"; }

        if (!next.equals(currentState) || loop != nextLoop)
            playAnimation(next, nextLoop, getAnimationTime(), 0.15f);
    }

    public void playAnimation(String state, boolean loop, float startTime, float blendTime) {
        AnimationPlayerTemplate.AnimationClip clip = loadClip(state);
        if (clip == null) {
            MonsterMod.LOGGER.warn("[MonsterMod] Missing animation: " + state + " for " + id);
            return;
        }

        var prev = animationPlayer;
        var newPlayer = new AnimationPlayerTemplate.AnimationPlayer(clip);
        if (prev != null && blendTime > 0f)
            newPlayer.blendFromPrevious(prev, blendTime);

        newPlayer.setLoop(loop);
        newPlayer.setTime(startTime);
        animationPlayer = newPlayer;
        currentState = state;
        this.loop = loop;
    }

    public float getAnimationTime() {
        return animationPlayer != null ? animationPlayer.getTime() : 0f;
    }

    @Nullable
    protected AnimationPlayerTemplate.AnimationClip loadClip(String name) {
        try {
            String[] parts = id.split(":");
            String modid = parts.length > 1 ? parts[0] : MonsterMod.MOD_ID;
            String path = parts.length > 1 ? parts[1] : parts[0];
            ResourceLocation res = new ResourceLocation(modid, "animations/" + path + "/" + name + ".json");

            AnimationPlayerTemplate player = AnimationPlayerTemplate.load(res);
            if (player == null) {
                MonsterMod.LOGGER.warn("[MonsterMod] Failed to load animation file: " + res);
                return null;
            }
            var anim = player.getAnimation(name);
            if (anim == null) {
                MonsterMod.LOGGER.warn("[MonsterMod] Animation node not found: " + name + " in " + res);
                return null;
            }
            return new AnimationPlayerTemplate.AnimationClip(anim);
        } catch (Exception e) {
            MonsterMod.LOGGER.error("[MonsterMod] Exception loading animation " + name + " for " + id, e);
            return null;
        }
    }

    /* ---------------------------
       同期処理
       --------------------------- */
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
            float[] arr = entry.getValue();
            if (arr == null || arr.length < 6) continue;
            Map<String, Vector3f> target = Map.of(
                    "position", new Vector3f(arr[0], arr[1], arr[2]),
                    "rotation", new Vector3f(arr[3], arr[4], arr[5])
            );
            Map<String, Vector3f> prev = lastBoneTransforms.getOrDefault(entry.getKey(), target);
            lastBoneTransforms.put(entry.getKey(), AnimationPlayerTemplate.lerpPose(prev, target, 0.25f));
        }
    }

    /* ---------------------------
       入力処理 / NBT保存
       --------------------------- */
    public void setPendingAttack(boolean attack) { this.pendingAttack = attack; }
    public void setPendingDodge(boolean dodge) { this.pendingDodge = dodge; }
    public void setPendingSkill(int skillIndex) { this.pendingSkill = skillIndex; }
    public void setPendingMenu(boolean menu) { this.pendingMenu = menu; }

    public boolean consumeAttack() { boolean v = pendingAttack; pendingAttack = false; return v; }
    public boolean consumeDodge() { boolean v = pendingDodge; pendingDodge = false; return v; }
    public int consumeSkill() { int v = pendingSkill; pendingSkill = -1; return v; }
    public boolean consumeMenu() { boolean v = pendingMenu; pendingMenu = false; return v; }

    public void handleClientInput(Player player, int skillIndex) { if (skillIndex >= 0) setPendingSkill(skillIndex); }
    public void handleMenuInput(Player player) { setPendingMenu(true); }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("cooldownCount", abilityCooldowns.length);
        for (int i = 0; i < abilityCooldowns.length; i++)
            tag.putInt("cd_" + i, abilityCooldowns[i]);

        tag.putString("state", currentState);
        tag.putBoolean("loop", loop);
        if (animationPlayer != null)
            tag.putFloat("anim_time", animationPlayer.getTime());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        int count = tag.getInt("cooldownCount");
        abilityCooldowns = new int[Math.max(count, 1)];
        for (int i = 0; i < count; i++) abilityCooldowns[i] = tag.getInt("cd_" + i);

        String state = tag.getString("state");
        boolean loopSaved = tag.getBoolean("loop");
        float savedTime = tag.getFloat("anim_time");

        playAnimation(state.isEmpty() ? "idle" : state, loopSaved, savedTime, 0f);
    }
}
