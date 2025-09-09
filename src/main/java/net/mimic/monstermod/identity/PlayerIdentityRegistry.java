package net.mimic.monstermod.identity;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent; // ここを修正しました
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mod内の全てのIPlayerIdentityインスタンスを登録・管理するレジストリ。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerIdentityRegistry {
    private static final Map<ResourceLocation, IPlayerIdentity> IDENTITIES = new HashMap<>();

    // 定義済みIdentityインスタンス
    public static final IPlayerIdentity MIMIC_IDENTITY = new MimicIdentity();
    /**
     * FMLCommonSetupEventでIdentityを登録します。
     * このメソッドは、ForgeのModイベントバスによって自動的に呼び出されます。
     */
    @SubscribeEvent
    public static void registerIdentities(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerIdentity(MIMIC_IDENTITY);
            MonsterMod.getLogger().debug("PlayerIdentityRegistry: {}個のIdentityを登録しました。", IDENTITIES.size());
        });
    }

    private static void registerIdentity(IPlayerIdentity identity) {
        if (IDENTITIES.containsKey(identity.getId())) {
            MonsterMod.getLogger().warn("Identity IDが重複しています: {}", identity.getId());
            return;
        }
        IDENTITIES.put(identity.getId(), identity);
    }

    public static IPlayerIdentity getIdentity(ResourceLocation id) {
        return IDENTITIES.get(id);
    }

    public static boolean hasIdentity(ResourceLocation id) {
        return IDENTITIES.containsKey(id);
    }

    public static Set<ResourceLocation> getAllIdentityIds() {
        return new HashSet<>(IDENTITIES.keySet());
    }
}