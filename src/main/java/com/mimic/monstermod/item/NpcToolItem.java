package com.mimic.monstermod.item;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_OpenNpcEditorPacket;
import com.mimic.monstermod.npc.NpcSettings;
import com.mimic.monstermod.npc.NpcTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * NPC作成ツール。
 *
 * 操作:
 *  - Shift + 右クリック          : 設定画面を開く(モデルとなるエンティティ・挙動を決める)
 *  - 右クリック(未設定のとき)    : 設定画面を開く
 *  - 右クリック(設定済み)        : 設定したNPCを召喚する
 *                                  ブロックを見ていればその上、空中なら足元に出す
 *  - エンティティに右クリック    : 既存のエンティティを今の設定でNPC化(他MOD製も可)
 *  - Shift + エンティティ右クリック : NPC化を解除して元の挙動へ戻す
 *
 * これにより「町を歩くNPC」「交渉用に固定された死なない村人」「会話できる魔物」
 * 「動かない無機物」などを、用途に応じて作り分けられる。
 */
public class NpcToolItem extends Item {

    private static final String TAG_TYPE      = "npc_entity_type";
    private static final String TAG_SETTINGS  = "npc_settings";
    private static final String TAG_CONFIGURED = "npc_configured";

    public NpcToolItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // ---- アイテムNBT ----
    public static String getEntityTypeId(ItemStack stack) {
        String s = stack.getOrCreateTag().getString(TAG_TYPE);
        return s.isEmpty() ? "minecraft:villager" : s;
    }

    public static void setEntityTypeId(ItemStack stack, String id) {
        stack.getOrCreateTag().putString(TAG_TYPE, id);
    }

    /** 一度でも設定画面で保存したか。未設定なら右クリックで設定画面を開く */
    public static boolean isConfigured(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_CONFIGURED);
    }

    public static NpcSettings getSettings(ItemStack stack, Entity forEntity) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_SETTINGS)) return NpcSettings.defaults(forEntity);

        CompoundTag t = tag.getCompound(TAG_SETTINGS);
        NpcSettings.Movement mv;
        try { mv = NpcSettings.Movement.valueOf(t.getString("movement")); }
        catch (IllegalArgumentException e) { mv = NpcSettings.Movement.FIXED; }
        NpcSettings.LookMode lm;
        try { lm = NpcSettings.LookMode.valueOf(t.getString("look")); }
        catch (IllegalArgumentException e) { lm = NpcSettings.LookMode.LOOK_PLAYER; }

        return new NpcSettings(mv,
                t.getBoolean("attacks"), t.getBoolean("retaliate"), t.getBoolean("invulnerable"),
                lm, forEntity.getYRot(), forEntity.getX(), forEntity.getY(), forEntity.getZ());
    }

    public static void setSettings(ItemStack stack, NpcSettings s) {
        CompoundTag t = new CompoundTag();
        t.putString("movement", s.movement().name());
        t.putBoolean("attacks", s.attacksPlayers());
        t.putBoolean("retaliate", s.allowRetaliation());
        t.putBoolean("invulnerable", s.invulnerable());
        t.putString("look", s.lookMode().name());
        stack.getOrCreateTag().put(TAG_SETTINGS, t);
        stack.getOrCreateTag().putBoolean(TAG_CONFIGURED, true);
    }

    /** 空中で右クリック: 設定画面 or その場に召喚 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (!sp.hasPermissions(2)) {
            sp.displayClientMessage(Component.literal("NPCの設定には権限が必要です")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        // Shift、または一度も設定していない場合は設定画面を開く
        if (sp.isShiftKeyDown() || !isConfigured(stack)) {
            openEditor(sp, stack);
            return InteractionResultHolder.success(stack);
        }

        // 設定済みなら足元に召喚する
        summon(sp, stack, sp.blockPosition());
        return InteractionResultHolder.success(stack);
    }

    /** ブロックを右クリック: 設定画面 or その上に召喚 */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!sp.hasPermissions(2)) return InteractionResult.FAIL;

        ItemStack stack = ctx.getItemInHand();
        if (sp.isShiftKeyDown() || !isConfigured(stack)) {
            openEditor(sp, stack);
            return InteractionResult.SUCCESS;
        }

        summon(sp, stack, ctx.getClickedPos().above());
        return InteractionResult.SUCCESS;
    }

    private void openEditor(ServerPlayer sp, ItemStack stack) {
        ModMessages.sendToPlayer(new S2C_OpenNpcEditorPacket(
                getEntityTypeId(stack), getSettings(stack, sp)), sp);
    }

    /** 設定したエンティティをNPCとして召喚する */
    private void summon(ServerPlayer sp, ItemStack stack, BlockPos pos) {
        String typeId = getEntityTypeId(stack);
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(typeId));
        if (type == null) {
            sp.displayClientMessage(Component.literal("不明なエンティティ: " + typeId)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        Entity spawned = type.spawn((ServerLevel) sp.level(), pos, MobSpawnType.SPAWN_EGG);
        if (spawned == null) {
            sp.displayClientMessage(Component.literal("召喚に失敗しました")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 呼び出したプレイヤーの方を向いた状態で置き、その場所を固定位置にする
        spawned.setYRot(sp.getYRot() + 180.0F);
        NpcTickEvents.applyNow(spawned, getSettings(stack, spawned).anchoredAt(spawned));

        sp.displayClientMessage(Component.literal(
                spawned.getName().getString() + " を召喚しました")
                .withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("モデル: " + getEntityTypeId(stack)).withStyle(ChatFormatting.GRAY));
        if (!isConfigured(stack)) {
            tooltip.add(Component.literal("未設定 — 右クリックで設定画面").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal("右クリック: 召喚").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Shift+右クリック: 設定画面").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("エンティティに右クリック: NPC化(他MODのMobも可)").withStyle(ChatFormatting.DARK_GRAY));
    }
}
