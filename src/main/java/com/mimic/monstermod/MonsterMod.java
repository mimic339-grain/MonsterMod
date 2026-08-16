package com.mimic.monstermod;

import com.mimic.monstermod.effect.ModEffects;
import com.mimic.monstermod.identity.BaseMonsterIdentityRegistry;
import com.mimic.monstermod.init.ModEntitieType;
import com.mimic.monstermod.init.ModEntityAttributes;
import com.mimic.monstermod.init.ModItems;
import com.mimic.monstermod.init.ModParticles;
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


        // 1. 各種レジストリの登録（これはコンストラクタでOK）
        ModEntitieType.register(modEventBus);
        ModItems.register(modEventBus);
        com.mimic.monstermod.init.ModBlocks.register(modEventBus);
        com.mimic.monstermod.init.ModBlockEntities.register(modEventBus);
        ModMessages.register();
        ModEffects.MOB_EFFECTS.register(modEventBus);


        ModParticles.PARTICLE_TYPES.register(modEventBus);
        modEventBus.register(ModEntityAttributes.class);
        modEventBus.register(BaseMonsterIdentityRegistry.class);

        // 2. 「セットアップ完了後」にスキルを初期化するよう予約
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        // enqueueWork を使うことで、マルチスレッド環境でも安全に初期化できます
        event.enqueueWork(() -> {
            // すべての RegistryObject が get() 可能になった状態で初期化
            com.mimic.monstermod.identity.monster.YatagarasuIdentity.initSkillRegistry();
            com.mimic.monstermod.identity.bomber.BomberSkills.registerLeads();
            com.mimic.monstermod.identity.util.MimicSkillLeads.registerAll();
            LOGGER.info("MonsterMod: Skills and Leads have been initialized.");
        });
    }
}
