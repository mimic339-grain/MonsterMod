package com.mimic.monstermod.identity;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.impl.MimicIdentity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * IdentityType
 * - Entityクラス・ID から Identity を一元生成・管理
 */
public class IdentityType {

    /** Entityクラス → IdentityType */
    private static final Map<Class<? extends BaseMonsterEntity>, IdentityType> ENTITY_MAP = new HashMap<>();
    /** ResourceLocation ID → IdentityType */
    static final Map<ResourceLocation, IdentityType> ID_MAP = new HashMap<>();

    private final ResourceLocation id;
    private final Class<? extends BaseMonsterEntity> entityClass;
    private final IdentityFactory factory;

    /** Identity生成用インターフェース */
    public interface IdentityFactory {
        BaseMonsterIdentity create(@Nullable BaseMonsterEntity entity);
    }

    private IdentityType(ResourceLocation id,
                         Class<? extends BaseMonsterEntity> entityClass,
                         IdentityFactory factory) {
        this.id = id;
        this.entityClass = entityClass;
        this.factory = factory;
    }

    public ResourceLocation getId() { return id; }
    public Class<? extends BaseMonsterEntity> getEntityClass() { return entityClass; }

    /** Identity を生成 */
    public BaseMonsterIdentity createIdentity(@Nullable BaseMonsterEntity entity) {
        return factory.create(entity);
    }

    // -----------------------------
    // 登録
    // -----------------------------
    public static void register(ResourceLocation id,
                                Class<? extends BaseMonsterEntity> entityClass,
                                IdentityFactory factory) {
        IdentityType type = new IdentityType(id, entityClass, factory);
        ENTITY_MAP.put(entityClass, type);
        ID_MAP.put(id, type);
    }

    // -----------------------------
    // Entity → IdentityType
    // -----------------------------
    @Nullable
    public static IdentityType fromEntity(BaseMonsterEntity entity) {
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
    // 初期登録（例：Mimic）
    // -----------------------------
    static {
        register(
                new ResourceLocation("monstermod", "mimic"),
                MimicEntity.class,
                MimicIdentity::new
        );
    }

    /** ID から Identity を生成（Entity が null でも OK） */
    @Nullable
    public static BaseMonsterIdentity createIdentity(ResourceLocation id, @Nullable BaseMonsterEntity entity) {
        IdentityType type = fromId(id);
        if (type == null) return null;
        return type.createIdentity(entity);
    }
}
