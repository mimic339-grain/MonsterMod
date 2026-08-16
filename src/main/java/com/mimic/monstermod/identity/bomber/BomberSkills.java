package com.mimic.monstermod.identity.bomber;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.bomb.BombAttachment;
import com.mimic.monstermod.bomb.BombInstance;
import com.mimic.monstermod.bomb.BombKind;
import com.mimic.monstermod.bomb.BombTiming;
import com.mimic.monstermod.init.ModItems;
import com.mimic.monstermod.skill.DamageType;
import com.mimic.monstermod.skill.SkillEffectSpec;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * ボマーの6スキル。
 *
 * 【押した瞬間に効くもの / 次の行動で効くもの】
 * 2(アイテム)・4(透明化)・5(設置ボム入手)は押した瞬間に完結する。
 * 1(殴打)・3(ブロック)・6(受け渡し)は押しただけでは何も起きず、
 * 次に殴る/右クリックするまで待つ「武装」状態になる。
 * 武装は {@link BomberIdentity#setArmed} が持ち、実際の付与は {@link BomberEvents} が行う。
 * こう分けているのは、狙った相手・狙った場所に確実に仕掛けたいから。
 *
 * どのスキルも予兆(プレビュー)を出さない。
 * 仕掛ける瞬間が相手に見えてしまうとボマーが成立しないため。
 */
public final class BomberSkills {

    private BomberSkills() {}

    private static final String MODID = "monstermod";

    public static final SkillId TOUCH  = new SkillId(new ResourceLocation(MODID, "bomb_touch"));
    public static final SkillId ITEM   = new SkillId(new ResourceLocation(MODID, "bomb_item"));
    public static final SkillId BLOCK  = new SkillId(new ResourceLocation(MODID, "bomb_block"));
    public static final SkillId VANISH = new SkillId(new ResourceLocation(MODID, "bomb_vanish"));
    public static final SkillId PLACE  = new SkillId(new ResourceLocation(MODID, "bomb_place"));
    public static final SkillId RELAY  = new SkillId(new ResourceLocation(MODID, "bomb_relay"));
    public static final SkillId CHAIN  = new SkillId(new ResourceLocation(MODID, "bomb_chain"));
    public static final SkillId DUMMY  = new SkillId(new ResourceLocation(MODID, "bomb_dummy"));

    /** 透明化の長さ */
    public static final int VANISH_TICKS = 30 * BombTiming.TICKS_PER_SECOND;

    /** 殴って付けるボムの爆発半径 */
    public static final float TOUCH_RADIUS = 15.0F;
    /** アイテム・ブロックに仕込むボムの爆発半径 */
    public static final float TRAP_RADIUS = 13.5F;

    /**
     * ボマーのスキルは全て予兆なしの即時発動で、効果は自分に付く。
     *
     * 【MOVEMENT にしている理由】
     * STRIKE にすると、SkillUtil は範囲内に対象がいるときしか効果を実行しない
     * (対象ごとに apply を呼ぶ作りのため)。
     * ボマーのスキルは相手を必要としない自分への付与なので、
     * 対象がいなくても必ず実行される MOVEMENT を使う。
     */
    private static SkillLead.Builder base(SkillId id) {
        return new SkillLead.Builder(id)
                .category(SkillType.Category.UNIQUE)
                .shape(MathMain.Shape.SPHERE)
                .sphere(0.5f)
                .attackType(SkillType.MOVEMENT)
                .followCaster(true)
                .totalPreviewTicks(0)  // 予兆を出すと仕掛けが相手にバレる
                .effectTicks(1)
                .recoveryTicks(0);
    }

    public static void registerLeads() {
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(TOUCH).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(ITEM).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(BLOCK).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(VANISH).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(PLACE).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(RELAY).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(CHAIN).build());
        com.mimic.monstermod.skill.SkillLeadRegistry.register(base(DUMMY).build());
    }

    // ---------------- 効果の中身 ----------------

    /** 武装するだけのスキル(1・3・6)の共通処理 */
    private static class ArmSkill extends SkillEffectSpec {
        private final int slot;
        private final String message;
        private final boolean melee;

        ArmSkill(int slot, String message, boolean melee) {
            super(0, DamageType.MAGIC, SkillType.STRIKE, List.of());
            this.slot = slot;
            this.message = message;
            this.melee = melee;
        }

        @Override
        protected void applyToCaster(LivingEntity attacker) {
            if (attacker.level().isClientSide) return;
            if (!(attacker instanceof Player player)) return;

            BomberIdentity bomber = BomberIdentity.of(player);
            if (bomber == null) return;

            // 殴る系(1と6)は同時に武装できない。どちらが出るか分からないと事故るため
            if (melee) bomber.armExclusiveMelee(slot);
            else bomber.setArmed(slot, true);

            // 武装しただけでは何も起きていないので、クールダウンはまだ始めない。
            // 実際に仕掛けた瞬間に BomberEvents 側で改めて設定する
            bomber.setCooldown(slot, 0);
            BomberIdentity.sync(player); // 送らないとHUDの枠が青に変わらない

            player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.AQUA), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BIT.get(), SoundSource.PLAYERS, 0.7F, 1.6F);
        }
    }

    /** 1: 殴った相手にボムを付ける(武装) */
    public static SkillEffectSpec touch() {
        return new ArmSkill(BomberIdentity.SLOT_TOUCH, "起爆装置: 次に殴った相手に仕掛ける", true);
    }

    /**
     * 3: ブロックにボムを仕掛ける(武装)。
     * 殴る操作は「壊す」と紛らわしく、右クリックは箱を開くなどと衝突するため、
     * 見ているブロックへスニークし続けて仕掛ける形にしている(BomberEvents が進行を見る)。
     */
    public static SkillEffectSpec blockTrap() {
        return new ArmSkill(BomberIdentity.SLOT_BLOCK,
                "仕掛け: 設置したいブロックを見てスニークし続ける", false);
    }

    /** 仕掛けられなかったときにクールダウンを返す。押し損を作らないため */
    static void refund(BomberIdentity bomber, Player player, int slot) {
        if (bomber == null) return;
        bomber.setArmed(slot, false);
        bomber.setCooldown(slot, 0);
        BomberIdentity.sync(player);
    }

    /** 6: 受け渡しボム(武装) */
    public static SkillEffectSpec relay() {
        return new ArmSkill(BomberIdentity.SLOT_RELAY, "受け渡し: 次に殴った相手へ移す", true);
    }

    /**
     * 2: 手に持っているアイテムにボムを仕込む。
     * 見た目も名前も変わらないが、NBTが付くのでスタックできなくなる。
     * そこだけが唯一の手掛かりになる(剣のように元からスタックしない物は見分けが付きにくい)。
     */
    public static SkillEffectSpec itemTrap() {
        return new SkillEffectSpec(0, DamageType.MAGIC, SkillType.STRIKE, List.of()) {
            @Override
            protected void applyToCaster(LivingEntity attacker) {
                if (!(attacker.level() instanceof ServerLevel level)) return;
                if (!(attacker instanceof Player player)) return;

                BomberIdentity bomber = BomberIdentity.of(player);

                ItemStack target = player.getOffhandItem();
                if (!BomberIdentity.canBombItem(target)) {
                    // 仕掛けられなかったので、押した分のクールダウンは返す
                    refund(bomber, player, BomberIdentity.SLOT_ITEM);
                    player.displayClientMessage(Component.literal(
                                    "左手に、まだ仕掛けていないアイテムを持ってください")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }

                // まとめて仕掛けられないようにする。
                // 複数個に一度で付けられると、配って回るだけで無差別に撒けてしまう
                if (target.getCount() > 1) {
                    refund(bomber, player, BomberIdentity.SLOT_ITEM);
                    player.displayClientMessage(Component.literal(
                                    "1個ずつしか仕掛けられない（今 " + target.getCount() + " 個）")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }

                // 起動は「右クリックされたとき」なので、この時点ではまだ動かさない(armed=false)
                BombAttachment.add(target, new BombInstance(
                        BombKind.ITEM, player.getUUID(),
                        BombTiming.rollTimedFuse(level), TRAP_RADIUS, false));

                player.displayClientMessage(Component.literal("アイテムに仕込んだ")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        };
    }

    /** 4: 自分を30秒だけ透明にする。仕掛ける瞬間を見られないための保険 */
    public static SkillEffectSpec vanish() {
        return new SkillEffectSpec(0, DamageType.MAGIC, SkillType.STRIKE, List.of()) {
            @Override
            protected void applyToCaster(LivingEntity attacker) {
                if (attacker.level().isClientSide) return;

                attacker.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, VANISH_TICKS, 0, false, false, true));

                attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                        SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7F, 1.2F);

                if (attacker instanceof Player player) {
                    player.displayClientMessage(Component.literal("30秒間 姿を消した")
                            .withStyle(ChatFormatting.AQUA), true);
                }
            }
        };
    }

    /** 5: 設置用の大型ボムを手に入れる。置いてから時間を選ぶ形になる */
    public static SkillEffectSpec place() {
        return giveBomb(ModItems.PLACED_BOMB, "設置ボムを取り出した");
    }

    /** 7: 連鎖ボムを手に入れる。仕掛けを繋げておくと芋づる式に誘爆する */
    public static SkillEffectSpec chain() {
        return giveBomb(ModItems.CHAIN_BOMB, "連鎖ボムを取り出した");
    }

    /** 8: 偽ボムを手に入れる。解除させて相手のキットを無駄遣いさせる */
    public static SkillEffectSpec dummy() {
        return giveBomb(ModItems.DUMMY_BOMB, "偽ボムを取り出した");
    }

    /** 設置系のボムを1個渡すだけの共通処理 */
    private static SkillEffectSpec giveBomb(
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item,
            String message) {
        return new SkillEffectSpec(0, DamageType.MAGIC, SkillType.STRIKE, List.of()) {
            @Override
            protected void applyToCaster(LivingEntity attacker) {
                if (attacker.level().isClientSide) return;
                if (!(attacker instanceof Player player)) return;

                ItemStack bomb = new ItemStack(item.get());
                if (!player.getInventory().add(bomb)) player.drop(bomb, false);

                player.displayClientMessage(Component.literal(message)
                        .withStyle(ChatFormatting.AQUA), true);
            }
        };
    }
}
