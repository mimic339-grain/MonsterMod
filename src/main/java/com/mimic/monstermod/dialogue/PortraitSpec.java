package com.mimic.monstermod.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 会話ウィンドウ左側に出す「立ち絵」の指定。
 *
 * 3種類を扱える:
 *   NONE   立ち絵なし。テキストを左まで広げて詰める
 *   IMAGE  リソースパック/MOD内のテクスチャをそのまま描画(パス指定)
 *   ENTITY EntityTypeを指定して実際のモデルを描画。GeckoLib製のモデルも
 *          バニラのEntityRenderDispatcherを通るのでそのまま表示できる
 *
 * ゲーム内で任意の画像ファイルを読み込むことはできないため、
 * IMAGEの場合は事前にリソースパックへ置いたテクスチャのパスを指定する運用になる。
 */
public record PortraitSpec(Type type, ResourceLocation id) {

    public enum Type { NONE, IMAGE, ENTITY }

    public static final PortraitSpec NONE = new PortraitSpec(Type.NONE, null);

    public static PortraitSpec image(ResourceLocation texture) {
        return new PortraitSpec(Type.IMAGE, texture);
    }

    public static PortraitSpec entity(ResourceLocation entityTypeId) {
        return new PortraitSpec(Type.ENTITY, entityTypeId);
    }

    public boolean isNone() {
        return type == Type.NONE || id == null;
    }

    // ---- NBT(ワールド保存用) ----
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putString("id", id == null ? "" : id.toString());
        return tag;
    }

    public static PortraitSpec load(CompoundTag tag) {
        Type t;
        try {
            t = Type.valueOf(tag.getString("type"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
        String s = tag.getString("id");
        if (t == Type.NONE || s.isEmpty()) return NONE;
        return new PortraitSpec(t, new ResourceLocation(s));
    }

    // ---- ネットワーク ----
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeUtf(id == null ? "" : id.toString());
    }

    public static PortraitSpec read(FriendlyByteBuf buf) {
        Type t = buf.readEnum(Type.class);
        String s = buf.readUtf();
        if (t == Type.NONE || s.isEmpty()) return NONE;
        return new PortraitSpec(t, new ResourceLocation(s));
    }
}
