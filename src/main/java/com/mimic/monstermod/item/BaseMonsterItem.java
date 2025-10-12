package com.mimic.monstermod.item;

import com.mimic.monstermod.Math.Cooldown;
import com.mimic.monstermod.Math.SkillUtility;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMonsterItem extends Item {

    protected final SkillUtility skillUtility;
    protected final Cooldown cooldown;

    public BaseMonsterItem(Properties properties, long cooldownMillis) {
        super(properties);
        this.skillUtility = new SkillUtility(cooldownMillis);
        this.cooldown = new Cooldown(cooldownMillis);
    }

    public Cooldown getCooldown() {
        return cooldown;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {

            if (!transformation.isTransformed() || transformation.getTransformedMobId() == null) {
                sendClientMessage(player, "変身していません。");
                return;
            }

            ResourceLocation mobId = transformation.getTransformedMobId();
            if (!isTargetMonster(mobId)) {
                sendClientMessage(player, "対象モンスターではありません。");
                return;
            }

            if (cooldown.canUse(player.getUUID().toString())) {
                cooldown.use(player.getUUID().toString());
                // サーバー側でスキル発動
                if (!level.isClientSide()) {
                    activateSkill(player);
                }
            } else {
                showCooldownIndicator(player);
            }
        });

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** スキル発動（個別アイテムで実装） */
    protected abstract void activateSkill(Player player);

    /** 対象モンスター判定 */
    protected abstract boolean isTargetMonster(ResourceLocation mobId);

    /** クライアント向けメッセージ */
    protected void sendClientMessage(Player player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    /** クールダウン表示（HUD描画等で色を変える想定） */
    protected void showCooldownIndicator(Player player) {
        long remaining = cooldown.getRemaining(player.getUUID().toString());
        if (remaining > 0) {
            sendClientMessage(player, "スキルはクールダウン中です: " + remaining + "ms");
        }
    }

    /** 足元範囲攻撃の簡略版 */
    protected void performFootprintAttack(Player player, List<int[]> offsets, int baseDamage) {
        int px = (int) player.getX();
        int py = (int) player.getY();
        int pz = (int) player.getZ();

        List<int[]> targets = new ArrayList<>();
        for (int[] offset : offsets) {
            targets.add(new int[]{px + offset[0], py, pz + offset[1]});
        }

        // プレイヤーの攻撃力を加味
        int attackBonus = (int) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        int totalDamage = baseDamage + attackBonus;

        // 足元パーティクル表示＋ダメージ処理
        if (!player.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            for (int[] pos : targets) {
                double x = pos[0] + 0.5;
                double y = pos[1];
                double z = pos[2] + 0.5;

                // ダメージ処理
                serverLevel.getEntitiesOfClass(Player.class, player.getBoundingBox().move(x - player.getX(), 0, z - player.getZ()))
                        .forEach(e -> {
                            if (!e.equals(player)) e.hurt(serverLevel.damageSources().magic(), totalDamage);
                        });
            }
        }
    }
}
