package com.mimic.monstermod.item;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.item.weapon.SimpleSwordItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    // DeferredRegister 作成
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MonsterMod.MOD_ID);

    // アイテム登録
    public static final RegistryObject<Item> SIMPLE_SWORD =
            ITEMS.register("simple_sword", SimpleSwordItem::new);

    // 登録処理
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
