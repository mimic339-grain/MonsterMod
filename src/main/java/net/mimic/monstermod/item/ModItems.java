package net.mimic.monstermod.item;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.item.custom.MimicSwitchItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MonsterMod.MOD_ID);

    public static final RegistryObject<Item> MIMIC_SWITCH = ITEMS.register("mimic_switch",
            () -> new MimicSwitchItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}