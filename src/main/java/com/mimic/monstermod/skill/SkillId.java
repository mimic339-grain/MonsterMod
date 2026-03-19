package com.mimic.monstermod.skill;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * SkillId
 *
 * 【役割】
 * ・スキルを一意に識別するための不変キー
 * ・Packet / Registry / Request の共通参照点
 */
public final class SkillId {

    private final ResourceLocation id;

    public SkillId(ResourceLocation id) {
        this.id = Objects.requireNonNull(id, "SkillId ResourceLocation cannot be null");
    }

    public static SkillId of(String namespace, String path) {
        return new SkillId(new ResourceLocation(namespace, path));
    }

    public static SkillId of(ResourceLocation id) {
        return new SkillId(id);
    }

    public ResourceLocation location() {
        return id;
    }

    /* =========================
     * Packet I/O
     * ========================= */

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
    }

    public static SkillId read(FriendlyByteBuf buf) {
        return new SkillId(buf.readResourceLocation());
    }

    /* ========================= */

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SkillId other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
