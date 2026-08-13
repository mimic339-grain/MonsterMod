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

        // 設定アイテムを持っているときは紐付け操作を優先する(会話は開かない)
        if (player.getMainHandItem().getItem() instanceof DialogueEditorItem) return;

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
}
