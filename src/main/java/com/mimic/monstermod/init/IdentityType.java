package com.mimic.monstermod.init;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.entity.HunterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.entity.monster.YatagarasuEntity;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.identity.monster.MimicIdentity;
import com.mimic.monstermod.identity.monster.YatagarasuIdentity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * モンスター IdentityType を管理
 * - Entityクラス → IdentityType
 * - ID → IdentityType
 * - Identity生成を一元管理
 */
public class IdentityType {

    private static final Map<Class<? extends BaseEntity>, IdentityType> ENTITY_MAP = new HashMap<>();
    public static final Map<ResourceLocation, IdentityType> ID_MAP = new HashMap<>();

    private final ResourceLocation id;
    private final Class<? extends BaseEntity> entityClass;
    private final IdentityFactory factory;

    /** Identity生成用インターフェース */
    public interface IdentityFactory {
        BaseIdentity create(@Nullable BaseEntity entity);
    }

    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(ID_MAP.keySet());
    }

    public static boolean exists(ResourceLocation id) {
        return ID_MAP.containsKey(id);
    }

    private IdentityType(ResourceLocation id,
                         Class<? extends BaseEntity> entityClass,
                         IdentityFactory factory) {
        this.id = id;
        this.entityClass = entityClass;
        this.factory = factory;
    }

    public ResourceLocation getId() { return id; }
    public Class<? extends BaseEntity> getEntityClass() { return entityClass; }

    /** この IdentityType から Identity を生成 */
    public BaseIdentity createIdentity(@Nullable BaseEntity entity) {
        return factory.create(entity);
    }

    // -----------------------------
    // 登録
    // -----------------------------
    public static void register(ResourceLocation id,
                                Class<? extends BaseEntity> entityClass,
                                IdentityFactory factory) {
        IdentityType type = new IdentityType(id, entityClass, factory);
        ENTITY_MAP.put(entityClass, type);
        ID_MAP.put(id, type);
    }

    // -----------------------------
    // Entity → IdentityType
    // -----------------------------
    @Nullable
    public static IdentityType fromEntity(BaseEntity entity) {
        return ENTITY_MAP.get(entity.getClass());
    }

    // -----------------------------
    // ID → IdentityType
    // -----------------------------
    @Nullable
    public static IdentityType fromId(ResourceLocation id) {
        return ID_MAP.get(id);
    }

    // -----------------------------
    // 初期登録（Mimic）
    // -----------------------------
    static {
        register(
                new ResourceLocation("monstermod", "mimic"),
                MimicEntity.class,
                MimicIdentity::new // これで @Nullable BaseMonsterEntity entity に対応
        );
        register(
                new ResourceLocation("monstermod", "yatagarasu"),
                YatagarasuEntity.class,
                YatagarasuIdentity::new
        );
        register(
                new ResourceLocation("monstermod", "hunter"),
                HunterEntity.class,
                (entity) -> new com.mimic.monstermod.identity.HunterIdentity(entity, 3)
        );
    }

    /** ID から Identity を生成（Entity が null でも OK） */
    @Nullable
    public static BaseIdentity createIdentity(ResourceLocation id, @Nullable BaseEntity entity) {
        IdentityType type = fromId(id);
        if (type == null) return null;
        return type.createIdentity(entity);
    }
}
