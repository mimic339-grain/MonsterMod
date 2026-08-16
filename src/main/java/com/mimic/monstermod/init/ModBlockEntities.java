package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.block.PlacedBombBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** ブロックが中身(状態)を持つ場合の登録場所 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MonsterMod.MOD_ID);

    // 設置ボム。残り時間と爆発半径を持ち、その内容がクライアントにも同期される
    public static final RegistryObject<BlockEntityType<PlacedBombBlockEntity>> PLACED_BOMB =
            BLOCK_ENTITIES.register("placed_bomb",
                    () -> BlockEntityType.Builder
                            .of(PlacedBombBlockEntity::new, ModBlocks.PLACED_BOMB.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
