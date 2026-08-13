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

import java.util.List;

/**
 * NPC作成ツール。/summon を使わずにNPCを設置・設定するための道具。
 *
 * 操作:
 *  - 空中で右クリック          : 設定画面を開く(湧かせるMobの種類とNPCの挙動を決める)
 *  - ブロックを右クリック      : そのブロックの上に、設定したMobをNPCとして召喚
 *  - 既存のエンティティを右クリック : そのエンティティを今の設定でNPC化(他MODのMobも可)
 *  - Shift + エンティティ右クリック : NPC化を解除して元の挙動へ戻す
 *
 * バニラ・他MODを問わず任意のMobをNPC化できるのは、AI無効化などが
 * Mob / Entity の共通メソッドで行えるため(詳細は NpcTickEvents を参照)。
 */
public class NpcToolItem extends Item {

    private static final String TAG_TYPE     = "npc_entity_type";
    private static final String TAG_SETTINGS = "npc_settings";

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

    /** アイテムに記録された挙動設定。未設定なら既定値 */
    public static NpcSettings getSettings(ItemStack stack, Entity forEntity) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_SETTINGS)) return NpcSettings.defaults(forEntity);

        CompoundTag t = tag.getCompound(TAG_SETTINGS);
        NpcSettings.LookMode lm;
        try { lm = NpcSettings.LookMode.valueOf(t.getString("look")); }
        catch (IllegalArgumentException e) { lm = NpcSettings.LookMode.LOOK_PLAYER; }
        return new NpcSettings(
                t.getBoolean("immobile"), t.getBoolean("noGravity"),
                t.getBoolean("invulnerable"), t.getBoolean("fireProof"),
                lm, forEntity.getYRot(),
                forEntity.getX(), forEntity.getY(), forEntity.getZ());
    }

    public static void setSettings(ItemStack stack, NpcSettings s) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("immobile", s.immobile());
        t.putBoolean("noGravity", s.noGravity());
        t.putBoolean("invulnerable", s.invulnerable());
        t.putBoolean("fireProof", s.fireProof());
        t.putString("look", s.lookMode().name());
        stack.getOrCreateTag().put(TAG_SETTINGS, t);
    }

    /** 空中で右クリック: 設定画面を開く */
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
        ModMessages.sendToPlayer(new S2C_OpenNpcEditorPacket(
                getEntityTypeId(stack), getSettings(stack, sp)), sp);
        return InteractionResultHolder.success(stack);
    }

    /** ブロックを右クリック: その上にNPCを召喚する(/summon の代わり) */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.SUCCESS;
        if (!player.hasPermissions(2)) return InteractionResult.FAIL;

        ItemStack stack = ctx.getItemInHand();
        String typeId = getEntityTypeId(stack);
        EntityType<?> type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation(typeId));
        if (type == null) {
            player.displayClientMessage(Component.literal("不明なエンティティ: " + typeId)
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        BlockPos pos = ctx.getClickedPos().above();
        Entity spawned = type.spawn((ServerLevel) level, pos, MobSpawnType.SPAWN_EGG);
        if (spawned == null) {
            player.displayClientMessage(Component.literal("召喚に失敗しました")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // プレイヤーの方を向いた状態で置く
        spawned.setYRot(player.getYRot() + 180.0F);
        NpcTickEvents.applyNow(spawned, getSettings(stack, spawned));

        player.displayClientMessage(Component.literal(
                spawned.getName().getString() + " をNPCとして設置しました")
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("召喚するMob: " + getEntityTypeId(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("右クリック: 設定画面").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: NPCを設置").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("エンティティに右クリック: NPC化(他MODのMobも可)").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: NPC化を解除").withStyle(ChatFormatting.DARK_GRAY));
    }
}
