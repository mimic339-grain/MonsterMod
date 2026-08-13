package com.mimic.monstermod.item;

import com.mimic.monstermod.dialogue.DialogueBinding;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.DialogueStore;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_OpenDialogueEditorPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 会話設定アイテム。ゲーム内で会話を作り、エンティティに紐付けるための道具。
 *
 * 使い方:
 *  - 空中で右クリック        : 編集画面を開く。画面の一番上でIDを直接入力する
 *                             (金床でのリネームは不要)
 *  - エンティティに右クリック : このアイテムに記録された会話IDをそのエンティティに紐付ける
 *  - Shift + エンティティ右クリック : 紐付けを解除
 *
 * 編集画面での保存は C2S_SaveDialoguePacket 経由で
 * DialogueStore(ワールドデータ)へ書き込まれる。
 */
public class DialogueEditorItem extends Item {

    private static final String TAG_ID = "dialogue_id";

    public DialogueEditorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** このアイテムが「最後に編集/紐付けに使った会話ID」。エンティティ紐付けで使う */
    public static String getDialogueId(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_ID);
    }

    public static void setDialogueId(ItemStack stack, String id) {
        stack.getOrCreateTag().putString(TAG_ID, id == null ? "" : id);
    }

    /**
     * 空中で右クリック: 編集画面を開く。
     * 既にIDが記録されていればその内容を読み込んで編集できるようにする。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }

        if (!sp.hasPermissions(2)) {
            sp.displayClientMessage(Component.literal("会話の編集には権限が必要です")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        String id = getDialogueId(stack);
        DialogueSet existing = null;
        if (!id.isEmpty() && sp.getServer() != null) {
            existing = DialogueStore.get(sp.getServer()).getDialogue(id);
        }

        ModMessages.sendToPlayer(new S2C_OpenDialogueEditorPacket(id, existing), sp);
        return InteractionResultHolder.success(stack);
    }

    /**
     * エンティティを右クリック: そのエンティティに会話を紐付ける。
     * 以後、その村人/プレイヤーを右クリックすると会話が始まる
     * (村人の場合は Shift+右クリックが交渉になる)。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!player.hasPermissions(2)) return InteractionResult.FAIL;

        if (player.isShiftKeyDown()) {
            DialogueBinding.set(target, null);
            player.displayClientMessage(Component.literal("会話の紐付けを解除しました")
                    .withStyle(ChatFormatting.YELLOW), false);
            return InteractionResult.SUCCESS;
        }

        String id = getDialogueId(stack);
        if (id.isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "先に空中で右クリックして会話を作成・保存してください")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.FAIL;
        }

        DialogueBinding.set(target, id);
        player.displayClientMessage(Component.literal(
                target.getName().getString() + " に会話 '" + id + "' を設定しました")
                .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String id = getDialogueId(stack);
        tooltip.add(Component.literal("会話ID: " + (id.isEmpty() ? "(未設定)" : id))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("右クリック: 編集画面を開く").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("エンティティに右クリック: 会話を紐付け").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: 紐付け解除").withStyle(ChatFormatting.DARK_GRAY));
    }
}
