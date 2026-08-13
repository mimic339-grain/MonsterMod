package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.dialogue.DialogueBinding;
import com.mimic.monstermod.dialogue.DialogueSet;
import com.mimic.monstermod.dialogue.DialogueStore;
import com.mimic.monstermod.item.DialogueEditorItem;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_StartDialoguePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * エンティティを右クリックしたときに会話を開始する処理。
 *
 * 【Mixinを使わない理由】
 * Forgeの PlayerInteractEvent.EntityInteract はバニラの Villager#mobInteract より
 * 先に発火し、キャンセルすればバニラの処理(交渉GUI)を止められる。
 * わざわざ Villager にMixinを当てる必要がないため、こちらを使う方が安全で壊れにくい。
 *
 * 【操作仕様】会話が紐付いたエンティティのみ操作を入れ替える:
 *   通常の右クリック    → 会話を開始(交渉GUIは開かない)
 *   Shift + 右クリック  → バニラの処理(村人なら交渉)
 * 会話が紐付いていないエンティティは一切触らないため、普通の村人はバニラのまま。
 * 他MODの村人GUIとも衝突しない。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class DialogueInteractEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // メインハンドのみ(オフハンドで二重に発火するのを防ぐ)
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // --- 設定アイテムを持っている場合は「紐付け操作」を行う ---
        // 【重要】この処理をアイテム側(Item#interactLivingEntity)に置いてはいけない。
        // Player#interactOn の呼び出し順序は
        //   1. ForgeHooks.onInteractEntity (このイベント)
        //   2. Entity#interact            ← 村人はここで交渉GUIを開き consumesAction で終了
        //   3. ItemStack#interactLivingEntity
        // となっており、村人相手では 3 に到達しないため紐付けが一切できなかった。
        // 最初に発火するこのイベントで処理してキャンセルする必要がある。
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof DialogueEditorItem) {
            handleBinding(player, held, event);
            return;
        }
        // NPCツールも同じ理由(村人の交渉が先に消費する)でここで処理する
        if (held.getItem() instanceof com.mimic.monstermod.item.NpcToolItem) {
            handleNpcTool(player, held, event);
            return;
        }

        String dialogueId = DialogueBinding.get(event.getTarget());
        if (dialogueId == null) return; // 会話が無いエンティティはバニラのまま

        // Shift押下時はバニラへ通す(村人なら交渉が開く)
        if (player.isShiftKeyDown()) return;

        DialogueStore store = DialogueStore.get(player.server);
        DialogueSet set = store.getDialogue(dialogueId);
        if (set == null || set.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("会話 '" + dialogueId + "' が見つかりません"), true);
            return;
        }

        ModMessages.sendToPlayer(new S2C_StartDialoguePacket(set), player);

        // バニラの処理(交渉GUIなど)を止める
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * 設定アイテムでエンティティに会話を紐付ける/解除する。
     * 村人の交渉GUIより先に処理してキャンセルする必要があるため、ここで行う。
     */
    private static void handleBinding(ServerPlayer player, ItemStack held,
                                      PlayerInteractEvent.EntityInteract event) {
        // バニラの処理(村人の交渉など)を必ず止める
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("会話の設定には権限が必要です")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (player.isShiftKeyDown()) {
            DialogueBinding.set(event.getTarget(), null);
            player.displayClientMessage(Component.literal("会話の紐付けを解除しました")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        String id = DialogueEditorItem.getDialogueId(held);
        if (id.isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "先に空中で右クリックして会話を作成・保存してください")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        DialogueBinding.set(event.getTarget(), id);
        player.displayClientMessage(Component.literal(
                event.getTarget().getName().getString() + " に会話 '" + id + "' を設定しました")
                .withStyle(ChatFormatting.GREEN), true);
    }

    /**
     * NPCツールでエンティティをNPC化する/解除する。
     * バニラ・他MODを問わず任意のMobに適用できる(AI無効化などが共通メソッドのため)。
     */
    private static void handleNpcTool(ServerPlayer player, ItemStack held,
                                      PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("NPCの設定には権限が必要です")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        var target = event.getTarget();
        if (player.isShiftKeyDown()) {
            com.mimic.monstermod.npc.NpcTickEvents.release(target);
            player.displayClientMessage(Component.literal("NPC化を解除しました")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        var settings = com.mimic.monstermod.item.NpcToolItem.getSettings(held, target);
        com.mimic.monstermod.npc.NpcTickEvents.applyNow(target, settings);
        player.displayClientMessage(Component.literal(
                target.getName().getString() + " をNPC化しました")
                .withStyle(ChatFormatting.GREEN), true);
    }
}
