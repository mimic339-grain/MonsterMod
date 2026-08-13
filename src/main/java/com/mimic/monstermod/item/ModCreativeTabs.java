package com.mimic.monstermod.item;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabs {

    public static CreativeModeTab WEAPON_TAB;

    @SubscribeEvent
    public static void registerTabs(RegisterEvent event) {

        // CreativeModeTab を登録するタイミングか？
        if (!event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) return;

    }
}
