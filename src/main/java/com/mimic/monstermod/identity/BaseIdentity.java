package com.mimic.monstermod.identity;

import com.mimic.monstermod.effect.EffectRenderManager;
import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public class BaseIdentity {
    protected final String id;
    @Nullable
    protected final BaseEntity entity;

    // フィールドでの "= new ...[0]" をすべて削除し、宣言のみにする
    protected int[] abilityCooldowns;
    protected int[] lockCooldowns;
    protected int[] comboWindows;
    protected com.mimic.monstermod.skill.SkillId[] skillIds;
    protected int[] defaultCooldowns;

    public BaseIdentity(@Nullable BaseEntity entity, int abilityCount) {
        this(entity, abilityCount, entity != null ? entity.getType().toString() : "unknown");
    }

    /**
     * 実体を持たない Identity(ボマーなど)用。
     * 実体から取れないので、IDを直接渡す。
     */
    public BaseIdentity(@Nullable BaseEntity entity, int abilityCount, String id) {
        this.entity = entity;
        this.id = id;

        // 全ての配列をここで確実に abilityCount の数だけ確保する
        this.abilityCooldowns = new int[abilityCount];
        this.lockCooldowns = new int[abilityCount];
        this.comboWindows = new int[abilityCount];
        this.skillIds = new com.mimic.monstermod.skill.SkillId[abilityCount];
        this.defaultCooldowns = new int[abilityCount];
    }


    public String getId() {
        return id;
    }

    @Nullable
    public BaseEntity getEntity() {
        return entity;
    }

    /**
     * 変身先の体(モデル・当たり判定)を持つ Identity かどうか。
     *
     * false を返すと「見た目はプレイヤーのまま、能力だけが変わる」種類になる。
     * ボマーや人狼のように、固定スキルだけを与えたい役職はこちら。
     *
     * false のとき除外されるもの:
     *   ・モンスター用HPバーの表示({@link com.mimic.monstermod.gui.MonsterHpOverlay})
     *   ・影を消す処理(EntityRenderDispatcherMixin)
     *   ・炎上表示を消す処理(EntityMixin)
     * 当たり判定・目線の高さ・段差の自動上りは元から
     * 「変身先の実体があるか」で判定しているため、実体を持たないこの種類では自動的に除外される。
     */
    public boolean hasOwnBody() {
        return true;
    }

    /**
     * そのスキルが「武装中(押したがまだ発動していない)」か。
     *
     * ボマーの「殴った相手にボムを付ける」のように、
     * 押した瞬間ではなく次の行動で効果が出るスキル用。
     * HUDはこれが true の間、枠を青(使用中)で描く。
     */
    public boolean isArmed(int index) {
        return false;
    }

    public SkillId[] getSkillIds() {
        return this.skillIds;
    }

    /**
     * 指定したスキルの予兆ロック中かチェック
     */
    public boolean isLocking(int index) {
        if (index < 0 || index >= lockCooldowns.length) return false;
        return lockCooldowns[index] > 0;
    }

    public int getLockTime(int index) {
        if (index < 0 || index >= lockCooldowns.length) return 0;
        return lockCooldowns[index];
    }

    //HUD用のeffecttick中の青色枠表示 todo あっていない　スキル発動中のeffecttick期間中のみ青色にみせるようにするもの　この計算ではあっていない　またeffecttickが1だと青色にする処理をなくす1tickだけ青色とかわからなすぎるからいらない
    public boolean isEffectActive(int index) {
        if (index < 0 || index >= this.skillIds.length) return false;

        int lockTime = lockCooldowns[index]; // 現在の残り時間
        if (lockTime <= 0) return false;

        SkillId skillId = this.skillIds[index];
        if (skillId == null) return false;

        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return false;

        // 1tickだけのスキルは青枠にしない
        if (lead.effectTicks <= 1) return false;

        // --- タイムラインの再計算 ---
        // lockTime は total(全時間) から始まって 0 に向かって減っていく。
        // [全時間] = [予兆(totalPreviewTicks)] + [効果(effectTicks)] + [後隙(recoveryTicks)] + 1

        // 青く光らせたいのは「予兆が終わった瞬間」から「後隙が始まるまで」
        int effectEnd = lead.recoveryTicks + 1;              // ここまで減ったら青終わり
        int effectStart = effectEnd + lead.effectTicks;      // ここから青開始

        // 例：予兆60t, 効果20t, 後隙30t の場合
        // 111t(開始) -> 51t(予兆終了・効果開始) -> 31t(効果終了・後隙開始) -> 0
        // この場合、51 >= lockTime > 31 の間だけ true になる
        return lockTime <= effectStart && lockTime > effectEnd;
    }

    public void tickServer(Player player) {
        updateCooldowns(player, "サーバー");
        copyFromPlayerServer(player);
    }

    public void tickClient(Player player) {
        updateCooldowns(player, "クライアント");
        copyFromPlayerClient(player);
    }

    public int getDefaultCooldown(int index) {
        if (index < 0 || index >= defaultCooldowns.length) return 0;
        return defaultCooldowns[index];
    }

    public void updateCooldowns(Player player, String side) {
        for (int i = 0; i < abilityCooldowns.length; i++) {
            // 2. コンボ受付時間の減算 (新規)
            if (comboWindows[i] > 0) {
                comboWindows[i]--;
            }

            // 1. 予兆ロaック減算
            if (lockCooldowns[i] > 0) {
                lockCooldowns[i]--;
                continue; // ロック中はリロードを進めない
            }


            // 2. リロード減算
            if (abilityCooldowns[i] > 0) {
                abilityCooldowns[i]--;
            }
        }
    }


    public boolean isAnySkillActive() {
        for (int i = 0; i < lockCooldowns.length; i++) {
            if (lockCooldowns[i] > 0) return true;
        }
        return false;
    }

    /**
     * いずれかのスキルのコンボ受付タイマーが動いているか（コンボ可能か）を返す
     */
    public boolean isComboWindowActive() {
        for (int window : comboWindows) {
            if (window > 0) return true;
        }
        return false;
    }

    /**
     * 特定のインデックスのコンボタイマーを取得（HUD用）
     */
    public int getComboWindow(int index) {
        if (index < 0 || index >= comboWindows.length) return 0;
        return comboWindows[index];
    }

    /**
     * SkillCategoryに基づいた発動可否判定。
     * (以前はHunterIdentityに同一ロジックが重複実装されていたため、片方だけ修正すると
     *  挙動がずれるリスクがあった。BaseIdentityに一本化し、HunterIdentityは継承する)
     */
    protected boolean canCastByCategory(SkillLead lead) {
        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.CANCEL) return true;

        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.COMBO) {
            return isComboWindowActive();
        }

        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.NORMAL) {
            boolean isDashing = false;
            for (int i = 0; i < skillIds.length; i++) {
                SkillLead l = SkillLeadRegistry.getNullable(skillIds[i]);
                if (l != null && l.category == com.mimic.monstermod.skill.SkillType.Category.DASH && comboWindows[i] > 0) {
                    isDashing = true;
                    break;
                }
            }
            return !isAnySkillActive() || isDashing;
        }

        // DASH, UNIQUEなどは他のスキルが動いていないことが条件
        return !isAnySkillActive();
    }

    public void handleAbility(Player player, int skillIndex) {
        // 1. 現在のID配列が自分のものか再確認
        if (skillIds == null || skillIndex < 0 || skillIndex >= skillIds.length) return;

        SkillId skillId = skillIds[skillIndex];
        SkillLead lead = SkillLeadRegistry.getNullable(skillId);
        if (lead == null) return;

        if (player.level().isClientSide()) {
            // ここで再確認：もしIdentityが切り替わったばかりで同期が遅れている場合、
            // 自分のskillIdsに入っていないIDが来たら弾くなどのガードが必要です
            if (abilityCooldowns[skillIndex] > 0 || lockCooldowns[skillIndex] > 0) return;
            if (!canCastByCategory(lead)) return;

            this.lockCooldowns[skillIndex] = 20;

            com.mimic.monstermod.network.ModMessages.INSTANCE.sendToServer(
                    new com.mimic.monstermod.network.client.C2S_SkillCastRequestPacket(skillId)
            );
        }
    }
    /**
     * サーバー側・クライアント側共通：スキル発動の結果をIdentityの状態（CD・Lock）に反映させる
     * 引数を (SkillLead lead) のみにし、内部で index を探す設計に統一
     */
    public void applyCastResult(SkillLead lead) {
        int index = findSkillIndex(lead.skillId());
        if (index == -1) return;

        // CANCELカテゴリーなら既存の全スキル予兆ロックを即座にリセット
        if (lead.category == com.mimic.monstermod.skill.SkillType.Category.CANCEL) {
            for (int i = 0; i < lockCooldowns.length; i++) lockCooldowns[i] = 0;
        }

        // 正式なタイマーセット
        this.lockCooldowns[index] = lead.skillTicks;
        this.abilityCooldowns[index] = (index < defaultCooldowns.length) ? defaultCooldowns[index] : 60;
        this.comboWindows[index] = lead.comboWindowTicks;
    }

    public int findSkillIndex(com.mimic.monstermod.skill.SkillId skillId) {
        if (skillId == null) return -1;
        for (int i = 0; i < skillIds.length; i++) {
            if (skillIds[i].equals(skillId)) return i;
        }
        return -1;
    }

    public int getCooldown(int index) {
        if (index < 0 || index >= abilityCooldowns.length) return 0;
        return abilityCooldowns[index];
    }

    /**
     * サーバーがスキル発動を拒否した際に、クライアント側で仮設定した予兆ロックを解除する。
     * (handleAbility が応答を待たずに lockCooldowns を仮セットするため、
     *  拒否された場合はこれを呼ばないと連打時に再入力できなくなる)
     */
    public void clearLock(int index) {
        if (index < 0 || index >= lockCooldowns.length) return;
        this.lockCooldowns[index] = 0;
    }

    // --- 同期/描画系（省略せずそのまま保持） ---
    public void copyFromPlayerServer(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
    }

    public void copyFromPlayerClient(Player player) {
        if (entity == null) return;
        copyRotationPoseAndEquip(player);
        entity.setDeltaMovement(player.getDeltaMovement());
        entity.tickCount = player.tickCount;
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        if (moving) entity.walkAnimation.update((float) player.getDeltaMovement().horizontalDistance(), 1.0f);
        entity.setPlayerActiveMove(moving);
        entity.setSprinting(player.isSprinting());
        entity.setShiftKeyDown(player.isCrouching());
    }

    public void copyRotationPoseAndEquip(Player player) {
        if (entity == null) return;
        entity.yBodyRot = player.yBodyRot;
        entity.yBodyRotO = player.yBodyRotO;
        entity.setYRot(player.getYRot());
        entity.setXRot(player.getXRot());
        float relativeHeadYaw = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
        entity.setYHeadRot(player.yBodyRot + relativeHeadYaw);
        entity.yRotO = player.yRotO;
        entity.xRotO = player.xRotO;
        entity.yHeadRotO = player.yBodyRotO + Mth.wrapDegrees(player.yHeadRotO - player.yBodyRotO);
        entity.setPose(player.getPose());
        for (EquipmentSlot slot : EquipmentSlot.values()) entity.setItemSlot(slot, player.getItemBySlot(slot));
    }

    public void render(Player player, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (entity == null) return;
        float bodyYaw = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        poseStack.pushPose();
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).render(entity, bodyYaw, partialTicks, poseStack, buffer, light);
        EffectRenderManager.renderAll(player, poseStack, buffer, light, partialTicks);//ここにidentity変身したlayerの追加　todo hunter用のidentityに必要
        poseStack.popPose();
    }

    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    public void handleMenu(Player player) {
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        // サーバーが覚えている現在のクールダウン状態を保存
        tag.putIntArray("cooldowns", abilityCooldowns);
        tag.putIntArray("lock_cooldowns", lockCooldowns);
        tag.putIntArray("combo_windows", comboWindows);
        tag.putIntArray("default_cooldowns", defaultCooldowns);
        // ★スキルIDも保存する
        ListTag skillList = new ListTag();
        for (SkillId sid : skillIds) {
            if (sid != null) skillList.add(StringTag.valueOf(sid.toString()));
            else skillList.add(StringTag.valueOf("null"));
        }
        tag.put("skill_ids", skillList);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;

        // 1. スキルID文字列リストから SkillId オブジェクトを再生成
        if (tag.contains("skill_ids", Tag.TAG_LIST)) {
            ListTag skillList = tag.getList("skill_ids", Tag.TAG_STRING);
            for (int i = 0; i < this.skillIds.length && i < skillList.size(); i++) {
                String s = skillList.getString(i);
                if (!s.equals("null") && !s.isEmpty()) {
                    // 文字列から ResourceLocation を経由して再構築
                    this.skillIds[i] = new com.mimic.monstermod.skill.SkillId(new net.minecraft.resources.ResourceLocation(s));
                }
            }
        }

        // 2. 各種タイマー配列の復元
        restoreArray(tag, "cooldowns", this.abilityCooldowns);
        restoreArray(tag, "lock_cooldowns", this.lockCooldowns);
        restoreArray(tag, "combo_windows", this.comboWindows);

        if (tag.contains("default_cooldowns")) {
            this.defaultCooldowns = tag.getIntArray("default_cooldowns");
        }
    }

    private void restoreArray(CompoundTag tag, String key, int[] target) {
        if (tag.contains(key)) {
            int[] saved = tag.getIntArray(key);
            // 現在のModバージョンとセーブデータの配列長が異なっても安全にコピー
            int length = Math.min(target.length, saved.length);
            System.arraycopy(saved, 0, target, 0, length);
        }
    }
}