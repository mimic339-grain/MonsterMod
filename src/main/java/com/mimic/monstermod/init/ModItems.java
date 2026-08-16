package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.item.BeamWandItem;
import com.mimic.monstermod.item.DialogueEditorItem;
import com.mimic.monstermod.item.NpcToolItem;
import com.mimic.monstermod.item.VortexWandItem;
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
    // 会話設定アイテム(ゲーム内で会話を作り、エンティティに紐付ける道具)
    public static final RegistryObject<Item> DIALOGUE_EDITOR =
            ITEMS.register("dialogue_editor", DialogueEditorItem::new);

    // NPC作成ツール(任意のMobをNPC化・設置する)
    public static final RegistryObject<Item> NPC_TOOL =
            ITEMS.register("npc_tool", NpcToolItem::new);

    // ビーム確認用の杖(見た目と当たり判定のテスト用)
    public static final RegistryObject<Item> BEAM_WAND =
            ITEMS.register("beam_wand", BeamWandItem::new);

    // 竜巻確認用の杖
    public static final RegistryObject<Item> VORTEX_WAND =
            ITEMS.register("vortex_wand", VortexWandItem::new);


    // 登録処理
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
