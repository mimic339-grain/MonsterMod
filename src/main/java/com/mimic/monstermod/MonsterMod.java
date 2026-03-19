package com.mimic.monstermod;

import com.mimic.monstermod.entity.ModEntitieType;
import com.mimic.monstermod.entity.ModEntityAttributes;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.identity.util.MimicSkillLeads;
import com.mimic.monstermod.item.ModItems;
import com.mimic.monstermod.network.ModMessages;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 追加: ネットワーク通信チャンネル
    public static SimpleChannel CHANNEL;

    public static Logger getLogger() {
        return LOGGER;
    }

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // エンティティ・アイテム・メッセージ登録
        ModEntitieType.register(modEventBus);
        ModItems.register(modEventBus);
        ModMessages.register(); // ここで CHANNEL を初期化

        // イベント登録
        modEventBus.register(ModEntityAttributes.class);
        modEventBus.register(BaseMonsterIdentityRegistry.class);

        /* =========================
           Skill 登録
        ========================= */

        MimicSkillLeads.registerAll();
    }
}
