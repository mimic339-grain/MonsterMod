package net.mimic.monstermod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.item.ModItems;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("monstermod")
                        .then(Commands.literal("transform")
                                .then(Commands.argument("mob_id", StringArgumentType.string())
                                        .executes(ModCommands::transformPlayer)))
                        .then(Commands.literal("untransform")
                                .executes(ModCommands::untransformPlayer))
        );
    }

    private static int transformPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("プレイヤーが見つかりません。"));
            return 0;
        }

        String mobIdString = StringArgumentType.getString(ctx, "mob_id");
        // ★ Crucial Fix: Automatically add your MOD_ID if no namespace is provided
        if (!mobIdString.contains(":")) {
            mobIdString = MonsterMod.MOD_ID + ":" + mobIdString;
        }

        ResourceLocation targetMobId;
        try {
            targetMobId = new ResourceLocation(mobIdString);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("無効なモブIDです: " + mobIdString));
            return 0;
        }

        boolean[] success = {false};
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transformation.isTransformed() && targetMobId.equals(transformation.getTransformedMobId())) {
                ctx.getSource().sendFailure(Component.literal("すでにそのモブに変身済みです。"));
                success[0] = false;
                return;
            }

            boolean isMimic = "mimic".equals(targetMobId.getPath()) &&
                    MonsterMod.MOD_ID.equals(targetMobId.getNamespace());

            transformation.setTransformed(true);
            transformation.setTransformedMobId(targetMobId);

            if (isMimic) {
                transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                transformation.setBiting(false);

                // Inventory management for Mimic transformation
                // Be aware: This clears the player's inventory. For actual gameplay, you might want a different approach.
                player.getInventory().clearContent();
                player.getInventory().add(new ItemStack(ModItems.MIMIC_SWITCH_ITEM.get()));
                player.getInventory().add(new ItemStack(ModItems.MIMIC_BITE_ITEM.get()));
                // Optional: set selected slot
                // player.getInventory().selected = 0;
                ctx.getSource().sendSuccess(() -> Component.literal("Mimicに変身しました！"), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal(targetMobId.getPath() + "に変身しました！"), false);
            }

            // Sync the capability state to the client
            transformation.syncToClient(player);
            success[0] = true;
        });

        return success[0] ? 1 : 0;
    }

    private static int untransformPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("プレイヤーが見つかりません。"));
            return 0;
        }

        boolean[] success = {false};
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) {
                ctx.getSource().sendFailure(Component.literal("変身していません。"));
                return;
            }

            transformation.setTransformed(false);
            transformation.setTransformedMobId(null);
            transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
            transformation.setBiting(false);

            // Inventory clear on untransform (consider alternative for real game)
            player.getInventory().clearContent();

            transformation.syncToClient(player);
            ctx.getSource().sendSuccess(() -> Component.literal("変身を解除しました。"), false);
            success[0] = true;
        });

        return success[0] ? 1 : 0;
    }
}