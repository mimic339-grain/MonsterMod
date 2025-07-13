package net.mimic.monstermod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.item.ModItems;
import net.mimic.monstermod.networking.ModMessages;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.mimic.monstermod.event.ModEvents;
import org.slf4j.Logger;

// ★FMLClientSetupEvent, EntityRenderers, MimicRenderer のインポートを追加★
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.mimic.monstermod.client.renderer.MimicRenderer;


@Mod(MonsterMod.MOD_ID)
@EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = EventBusSubscriber.Bus.FORGE) // Forge Event Bus
public class MonsterMod {
    public static final String MOD_ID = "monstermod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MonsterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMessages.register();

        modEventBus.register(ModEvents.class);

        // ★★★ この行を追加してください ★★★
        // クライアントセットアップイベントをMODイベントバスに登録
        modEventBus.addListener(this::clientSetup);
    }

    // ★★★ このメソッドを追加してください ★★★
    private void clientSetup(final FMLClientSetupEvent event) {
        // Minecraftの描画スレッドで安全に実行されるようにenqueueWorkを使用
        event.enqueueWork(() -> {
            // ここでMimicエンティティのレンダラーを登録
            EntityRenderers.register(ModEntities.MIMIC.get(), MimicRenderer::new);
            LOGGER.info("MimicRenderer registered via clientSetup event."); // 登録確認用のログ
        });
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        net.mimic.monstermod.command.ModCommands.register(event.getDispatcher());
    }
}