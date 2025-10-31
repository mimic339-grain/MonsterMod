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

public class BaseMonsterIdentity {

    protected final String id;
    @Nullable protected BaseMonsterEntity entity;
    public int[] abilityCooldowns;
    @Nullable public AnimationPlayerTemplate.AnimationPlayer animationPlayer;
    protected Map<String, AnimationPlayerTemplate.ModelPartProxy> boneMap = new HashMap<>();
    public String currentState = "idle";
    public boolean loop = true;
    public Map<String, Map<String, Vector3f>> lastBoneTransforms = new HashMap<>();

    private boolean pendingAttack = false;
    private boolean pendingDodge = false;
    private int pendingSkill = -1;
    private boolean pendingMenu = false;

    public BaseMonsterIdentity(ResourceLocation mobId, int abilityCount) {
        this.id = mobId.toString();
        this.abilityCooldowns = new int[abilityCount];
    }

    @Nullable
    public BaseMonsterEntity getEntity() { return entity; }

    public void setEntity(@Nullable BaseMonsterEntity entity) {
        this.entity = entity;
        autoInitBoneMap(entity);
    }

    public Map<String, AnimationPlayerTemplate.ModelPartProxy> getBoneMap() { return boneMap; }

    public void autoInitBoneMap(BaseMonsterEntity entity) {
        if (entity == null || entity.getModelRoot() == null) return;
        boneMap.clear();
        registerPartsRecursive(entity.getModelRoot(), "");
        MonsterMod.LOGGER.info("Initialized boneMap for " + id + " : " + boneMap.keySet());
    }

    private void registerPartsRecursive(ModelPart part, String prefix) {
        String name = prefix.isEmpty() ? part.toString() : prefix;
        boneMap.put(name, new AnimationPlayerTemplate.ModelPartProxy() {
            @Override public void setRotation(Vector3f rot) { part.xRot = rot.x(); part.yRot = rot.y(); part.zRot = rot.z(); }
            @Override public void setPosition(Vector3f pos) { part.x = pos.x(); part.y = pos.y(); part.z = pos.z(); }
            @Override public void setScale(Vector3f scale) { }
        });

        try {
            var field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            Map<String, ModelPart> children = (Map<String, ModelPart>) field.get(part);
            for (var entry : children.entrySet())
                registerPartsRecursive(entry.getValue(), entry.getKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**------------------------
     * Tick 系
     ------------------------*/
    public void tick(Player player, float deltaSeconds) {
        if (player.level().isClientSide) tickClient(deltaSeconds);
        else tickServer(player, deltaSeconds);
    }

    protected void tickServer(Player player, float deltaSeconds) {
        // クールダウン減少
        for (int i = 0; i < abilityCooldowns.length; i++)
            if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;

        updateAnimationStateServer(player);

        if (animationPlayer != null) animationPlayer.tick(deltaSeconds);

        // サーバー → クライアント同期
        if (player instanceof ServerPlayer sp && animationPlayer != null) {
            S2CIdentityAnimSyncPacket packet = new S2CIdentityAnimSyncPacket(sp.getUUID(), getPoseArrayForSync());
            ModMessages.sendToAllClientsExcept(packet, sp);
        }

        if (animationPlayer != null) lastBoneTransforms = animationPlayer.getCurrentPose();
    }

    public void tickClient(float deltaSeconds) {
        if (animationPlayer == null) return;
        animationPlayer.tick(deltaSeconds);
        lastBoneTransforms = animationPlayer.getCurrentPose();
    }

    /**------------------------
     * Render (render 側では tickClient を呼ばず補間のみ)
     ------------------------*/
    /**------------------------
     * Render (Entity用)
     ------------------------*/
    public void renderInterpolated(BaseMonsterEntity entity, float partialTicks,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (animationPlayer == null || entity == null || entity.getModelRoot() == null) return;

        poseStack.pushPose();

        Map<String, Map<String, Vector3f>> interpolatedPose = AnimationPlayerTemplate.blend(
                lastBoneTransforms, animationPlayer.getCurrentPose(), partialTicks
        );

        for (var entry : boneMap.entrySet()) {
            var proxy = entry.getValue();
            var transforms = interpolatedPose.get(entry.getKey());
            if (transforms != null) AnimationPlayerTemplate.applyPoseToProxy(proxy, transforms);
        }

        // ModelPart 描画
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(getTexture()));
        entity.getModelRoot().render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        poseStack.popPose();
    }

    /**------------------------
     *  getTexture 操作
     ------------------------*/
    @Nullable
    public ResourceLocation getTexture() {
        // Identityごとに適切なテクスチャを返す
        return new ResourceLocation(MonsterMod.MOD_ID, "textures/entity/mimic.png");
    }

    /**------------------------
     * Animation 操作
     ------------------------*/
    protected void updateAnimationStateServer(Player player) {
        boolean isMoving = player.getDeltaMovement().lengthSqr() > 0.01;
        boolean attackPressed = consumeAttack();
        boolean dodgePressed = consumeDodge();
        int skillPressed = consumeSkill();

        String next = "idle";
        boolean nextLoop = true;
        if (dodgePressed) { next = "dodge"; nextLoop = false; }
        else if (attackPressed) { next = "attack"; nextLoop = false; }
        else if (skillPressed >= 0) { next = "skill_" + (skillPressed + 1); nextLoop = false; }
        else if (isMoving) { next = "walk"; nextLoop = true; }

        if (!next.equals(currentState) || loop != nextLoop)
            playAnimation(next, nextLoop, getAnimationTime(), 0.1f);
    }

    public void playAnimation(String state, boolean loop, float startTime, float blendTime) {
        AnimationPlayerTemplate.AnimationClip clip = loadClip(state);
        if (clip == null) return;

        AnimationPlayerTemplate.AnimationPlayer prev = animationPlayer;
        AnimationPlayerTemplate.AnimationPlayer newPlayer = new AnimationPlayerTemplate.AnimationPlayer(clip);
        if (prev != null && blendTime > 0f) newPlayer.blendFromPrevious(prev, blendTime);

        newPlayer.setLoop(loop);
        newPlayer.setTime(startTime);
        animationPlayer = newPlayer;
        currentState = state;
        this.loop = loop;
    }

    public float getAnimationTime() { return animationPlayer != null ? animationPlayer.getTime() : 0f; }

    protected AnimationPlayerTemplate.AnimationClip loadClip(String name) {
        try {
            String[] parts = id.split(":");
            String modid = parts.length > 1 ? parts[0] : MonsterMod.MOD_ID;
            String path = parts.length > 1 ? parts[1] : parts[0];

            AnimationPlayerTemplate player = AnimationPlayerTemplate.load(
                    new ResourceLocation(modid, "animations/" + path + "/" + name)
            );
            if (player == null) return null;

            AnimationPlayerTemplate.Animation anim = player.getAnimation(name);
            if (anim == null) return null;

            return new AnimationPlayerTemplate.AnimationClip(anim);
        } catch (Exception e) {
            MonsterMod.LOGGER.warn("Failed to load animation: " + name + " for " + id, e);
            return null;
        }
    }

    /**------------------------
     * サーバー同期用 Pose
     ------------------------*/
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
            Map<String, Vector3f> t = Map.of(
                    "position", new Vector3f(arr[0], arr[1], arr[2]),
                    "rotation", new Vector3f(arr[3], arr[4], arr[5])
            );
            Map<String, Vector3f> prev = lastBoneTransforms.getOrDefault(entry.getKey(), t);
            lastBoneTransforms.put(entry.getKey(), AnimationPlayerTemplate.lerpPose(prev, t, 0.3f));
        }
    }

    /**------------------------
     * 入力ペンディング管理
     ------------------------*/
    public void setPendingAttack(boolean attack) { this.pendingAttack = attack; }
    public void setPendingDodge(boolean dodge) { this.pendingDodge = dodge; }
    public void setPendingSkill(int skillIndex) { this.pendingSkill = skillIndex; }
    public void setPendingMenu(boolean menu) { this.pendingMenu = menu; }
    public boolean consumeAttack() { boolean val = pendingAttack; pendingAttack = false; return val; }
    public boolean consumeDodge() { boolean val = pendingDodge; pendingDodge = false; return val; }
    public int consumeSkill() { int val = pendingSkill; pendingSkill = -1; return val; }
    public boolean consumeMenu() { boolean val = pendingMenu; pendingMenu = false; return val; }

    public void handleClientInput(Player player, int skillIndex) {
        if (skillIndex >= 0) setPendingSkill(skillIndex);
    }

    public void handleMenuInput(Player player) {
        setPendingMenu(true);
        MonsterMod.LOGGER.info("Menu input handled for player: " + player.getName().getString());
    }

    public void onOpenMenu(Player player) { }//将来GUIを開く処理を実装可能

    /**------------------------
     * NBT 保存
     ------------------------*/
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

        String state = tag.getString("state");
        boolean loopSaved = tag.getBoolean("loop");
        float savedTime = tag.getFloat("anim_time");
        playAnimation(state.isEmpty() ? "idle" : state, loopSaved, savedTime, 0f);
    }
}
