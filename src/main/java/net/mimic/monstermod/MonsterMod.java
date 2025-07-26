package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext; // ModLoadingContext をインポート
import net.minecraftforge.fml.config.ModConfig; // ModConfig をインポート
import org.slf4j.Logger;

@Mod(MonsterMod.MOD_ID)
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Config の登録
        // `ModConfig` クラスと `ModConfig.SPEC` が存在し、正しく定義されていることを前提とします。
        // 例: `net.mimic.monstermod.config.ModConfig` というパスにコンフィグクラスがある場合
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, net.mimic.monstermod.config.ModConfig.SPEC);

        // 他の初期化処理
    }
}