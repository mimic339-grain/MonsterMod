package com.mimic.monster.command;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;


public class TransformCommand {
    //コマンドの登録
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("transform")
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .executes(ctx -> {
                                    //プレイヤーとエンティティタイプの取得
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ResourceLocation rl = ResourceLocationArgument.getId(ctx, "entity");
                                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);

                                    player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
                                        //変身先タイプの設定
                                        cap.setTransformedType(type);
                                        //変身処理
                                        applyTransformation(player, type);
                                    });

                                    return 1;
                                }))
        );
    }

    private static void applyTransformation(ServerPlayer player, EntityType<?> type) {
        if (type != null && type != EntityType.PLAYER) {
            // 当たり判定を変更
            Entity tempEntity = type.create(player.level());
            if (tempEntity instanceof LivingEntity living) {
                player.setBoundingBox(living.getBoundingBox());
                // 能力値を変身先に合わせる
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
                        living.getAttributeBaseValue(Attributes.MAX_HEALTH)
                );
                player.setHealth(player.getMaxHealth());
                player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                        living.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
                );
            }
        }
    }
}
