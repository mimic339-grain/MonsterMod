package com.mimic.monstermod.init;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.item.BeamWandItem;
import com.mimic.monstermod.item.BombDefuserItem;
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

    // メギド確認用の杖
    public static final RegistryObject<Item> MEGIDDO_WAND =
            ITEMS.register("megiddo_wand", com.mimic.monstermod.item.MegiddoWandItem::new);

    // 電撃確認用の杖
    public static final RegistryObject<Item> LIGHTNING_WAND =
            ITEMS.register("lightning_wand", com.mimic.monstermod.item.LightningWandItem::new);

    // ボムの解除キット(付けられたボムを外す)
    public static final RegistryObject<Item> BOMB_DEFUSER =
            ITEMS.register("bomb_defuser", BombDefuserItem::new);

    // 解除したボムの残骸。今は素材として持たせるだけで、用途は後で決める
    public static final RegistryObject<Item> BOMB_REMNANT =
            ITEMS.register("bomb_remnant", () -> new Item(new Item.Properties()));

    // 設置する大型ボム(ボマーのスキル5で手に入る)
    public static final RegistryObject<Item> PLACED_BOMB =
            ITEMS.register("placed_bomb",
                    () -> new com.mimic.monstermod.item.PlacedBombItem(
                            com.mimic.monstermod.bomb.BombKind.PLACED));

    // 連鎖ボム。爆発すると範囲内の他のボムを誘爆させる
    public static final RegistryObject<Item> CHAIN_BOMB =
            ITEMS.register("chain_bomb",
                    () -> new com.mimic.monstermod.item.PlacedBombItem(
                            com.mimic.monstermod.bomb.BombKind.CHAIN));

    // 偽物のボム。見た目も音も本物だが、地形は壊れず解除しても残骸が出ない
    public static final RegistryObject<Item> DUMMY_BOMB =
            ITEMS.register("dummy_bomb",
                    () -> new com.mimic.monstermod.item.PlacedBombItem(
                            com.mimic.monstermod.bomb.BombKind.DUMMY));


    // 登録処理
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
