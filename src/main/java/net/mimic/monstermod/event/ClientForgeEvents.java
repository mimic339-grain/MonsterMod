package net.mimic.monstermod.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;

/**
 * クライアントサイドでのForgeイベントを処理するクラス。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientForgeEvents {

    // クライアントのセットアップイベント。レンダラー登録など。
    @SubscribeEvent
    public static void onClientSetup(EntityRenderersEvent.RegisterRenderers event) {
        // PlayerIdentityRendererに、EntityRendererProvider.Contextを渡してレンダラーを登録
        // EntityRendererProvider.Contextは通常、EntityRenderersEvent.RegisterRenderersから取得します。
        // ここでは直接渡せないので、後でClientSetupイベントなどでPlayerIdentityRenderer.registerRenderersを呼び出す必要があります。
        // -> ModMainのコンストラクタ内でModEventBusにPlayerIdentityRendererを登録するように変更済み

        // ★修正: RendererRegisteryEventはRegisterRenderersイベントに置き換えられました。
        // そして、PlayerIdentityRenderer.registerRenderers()は、
        // MonsterModのコンストラクタでModEventBusにPlayerIdentityRendererクラス自体を登録し、
        // @SubscribeEventによって正しいタイミング（ClientSetup）で呼び出されるようにします。
    }

    // ClientSetupイベントはModEventBusで処理されるため、ModMainクラスのコンストラクタで
    // PlayerIdentityRendererをmodEventBus.register()することで、
    // PlayerIdentityRenderer内の@SubscribeEventが呼び出されます。
}