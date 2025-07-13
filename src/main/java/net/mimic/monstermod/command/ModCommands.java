package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.item.ModItems;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("monstermod")
                .requires((commandSource) -> commandSource.hasPermission(2))
                .then(Commands.literal("transform")
                        .then(Commands.argument("mob_id", StringArgumentType.string())
                                .executes(ModCommands::transformPlayer)))
                .then(Commands.literal("untransform")
                        .executes(ModCommands::untransformPlayer)));
    }

    private static int transformPlayer(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("プレイヤーが見つかりません。"));
            return 0;
        }

        String mobIdString = StringArgumentType.getString(context, "mob_id");
        ResourceLocation targetMobId = new ResourceLocation(mobIdString);

        final boolean[] commandSuccessfullyProcessed = {false}; // コマンドが正常に処理されたかを示すフラグ

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            // すでに変身しているかチェックし、同じモブIDなら処理を中断
            if (transformation.getTransformedMobId() != null && transformation.getTransformedMobId().equals(targetMobId)) {
                context.getSource().sendFailure(Component.literal("すでに" + mobIdString + "に変身しています。"));
                return; // ラムダ式の処理をここで終了
            }

            // インベントリをクリア（変身に成功する場合のみ）
            player.getInventory().clearContent();

            transformation.setTransformed(true);
            transformation.setTransformedMobId(targetMobId);

            if (targetMobId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {
                transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                transformation.setBiting(false);
                player.sendSystemMessage(Component.literal("Mimicに変身しました！"));

                // Mimicに変身した際にアイテムを配布
                player.getInventory().add(new ItemStack(ModItems.MIMIC_SWITCH_ITEM.get()));
                player.getInventory().add(new ItemStack(ModItems.MIMIC_BITE_ITEM.get()));
                // アイテムを配布した後、ホットバーの最初のスロットにMimicSwitchItemを選択
                player.getInventory().selected = 0;

            } else {
                transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                transformation.setBiting(false);
                player.sendSystemMessage(Component.literal(mobIdString + "に変身しました！"));
            }

            ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.getMimicState().name(), transformation.isBiting()), player);

            context.getSource().sendSuccess(() -> Component.literal("変身しました！"), false);
            commandSuccessfullyProcessed[0] = true; // 正常処理されたことをマーク
        });

        // フラグに基づいて戻り値を返す
        return commandSuccessfullyProcessed[0] ? 1 : 0;
    }

    private static int untransformPlayer(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("プレイヤーが見つかりません。"));
            return 0;
        }

        final boolean[] commandSuccessfullyProcessed = {false}; // コマンドが正常に処理されたかを示すフラグ

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            // 変身しているかチェック
            if (!transformation.isTransformed()) {
                context.getSource().sendFailure(Component.literal("変身していません。"));
                return; // ラムダ式の処理をここで終了
            }

            transformation.setTransformed(false);
            transformation.setTransformedMobId(null);
            transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
            transformation.setBiting(false);

            // 変身解除時にMimic関連アイテムをインベントリから削除
            player.getInventory().clearContent();
            // 必要であれば、デフォルトのアイテム（剣、ツルハシなど）を再配布するロジックをここに追加
            // 例: player.addItem(new ItemStack(Items.WOODEN_SWORD));

            ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.getMimicState().name(), transformation.isBiting()), player);

            context.getSource().sendSuccess(() -> Component.literal("変身を解除しました！"), false);
            commandSuccessfullyProcessed[0] = true; // 正常処理されたことをマーク
        });

        // フラグに基づいて戻り値を返す
        return commandSuccessfullyProcessed[0] ? 1 : 0;
    }
}