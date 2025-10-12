package net.mimic.monstermod.identity;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BaseMonsterIdentityRegistry {

    private static final Map<ResourceLocation, BaseMonsterIdentity> IDENTITIES = new HashMap<>();

    public static final BaseMonsterIdentity MIMIC_IDENTITY = new MimicIdentity();

    @SubscribeEvent
    public static void registerIdentities(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerIdentity(MIMIC_IDENTITY);
            MonsterMod.getLogger().debug("BaseMonsterIdentityRegistry: {}個のIdentityを登録しました。", IDENTITIES.size());
        });
    }

    public static void registerIdentity(BaseMonsterIdentity identity) {
        ResourceLocation id = new ResourceLocation(identity.getId()); // String → ResourceLocation
        if (IDENTITIES.containsKey(id)) {
            MonsterMod.getLogger().warn("Identity IDが重複しています: {}", identity.getId());
            return;
        }
        IDENTITIES.put(id, identity);
    }
    @Nullable
    public static BaseMonsterIdentity getIdentity(ResourceLocation id) {
        return IDENTITIES.get(id);
    }

    public static boolean hasIdentity(ResourceLocation id) {
        return IDENTITIES.containsKey(id);
    }

    public static Set<ResourceLocation> getAllIdentityIds() {
        return new HashSet<>(IDENTITIES.keySet());
    }
}
