package net.mimic.monstermod.client;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
// import net.mimic.monstermod.client.render.PlayerMorphRenderer; // initメソッドがないならインポート不要かも

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // PlayerMorphRenderer.init() は Geckolib 4.x では使用しません。
        // この行は削除またはコメントアウトしてください。
        // レンダラーの登録は Forge のイベント (`EntityRenderersEvent.RegisterRenderers` など) を通じて行います。
        // ただし、Playerのレンダリングを乗っ取るのは少し特殊です。
    }
}