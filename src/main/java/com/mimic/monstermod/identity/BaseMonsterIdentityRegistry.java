package com.mimic.monstermod.identity;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Identity Registry
 * - IdentityType と統合
 * - Entity がまだ生成されていなくても Identity を生成可能
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BaseMonsterIdentityRegistry {

    /** ID → BaseMonsterIdentity のキャッシュ */
    private static final Map<ResourceLocation, BaseMonsterIdentity> IDENTITY_CACHE = new HashMap<>();

    /** 初期登録 */
    @SubscribeEvent
    public static void registerIdentities(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (ResourceLocation id : IdentityType.ID_MAP.keySet()) {
                // Entity がいない場合は null を渡す
                BaseMonsterIdentity identity = IdentityType.createIdentity(id, null);
                IDENTITY_CACHE.put(id, identity);
            }

            MonsterMod.getLogger().debug("BaseMonsterIdentityRegistry: {} 個の Identity を登録しました", IDENTITY_CACHE.size());
        });
    }

    /**
     * ID から Identity を取得
     * @param id ResourceLocation ID
     * @param entity 実際の Entity（まだ生成されていない場合は null）
     */
    public static BaseMonsterIdentity getIdentity(ResourceLocation id, BaseMonsterEntity entity) {
        IdentityType type = IdentityType.fromId(id);
        if (type == null) {
            MonsterMod.getLogger().warn("BaseMonsterIdentityRegistry: ID が存在しません {}", id);
            return null;
        }

        // Entity が null ならキャッシュ済みの Identity を返す
        if (entity == null) {
            return IDENTITY_CACHE.get(id);
        }

        return type.createIdentity(entity);
    }

    /** ID の存在確認 */
    public static boolean hasIdentity(ResourceLocation id) {
        return IDENTITY_CACHE.containsKey(id);
    }

    /** 全 Identity ID の取得 */
    public static Set<ResourceLocation> getAllIdentityIds() {
        return new HashSet<>(IDENTITY_CACHE.keySet());
    }
}