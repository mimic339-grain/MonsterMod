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



    @SubscribeEvent
    public static void registerTabs(RegisterEvent event) {

        // CreativeModeTab を登録するタイミングか？
        if (!event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) return;

        // MonsterMod用のタブ。会話設定アイテムなどの道具を入れる
        event.register(Registries.CREATIVE_MODE_TAB,
                helper -> helper.register(
                        new ResourceLocation(MonsterMod.MOD_ID, "tools"),
                        CreativeModeTab.builder()
                                .title(Component.literal("MonsterMod"))
                                .icon(() -> new ItemStack(ModItems.DIALOGUE_EDITOR.get()))
                                .displayItems((params, output) -> output.accept(ModItems.DIALOGUE_EDITOR.get()))
                                .build()
                )
        );
    }
}
