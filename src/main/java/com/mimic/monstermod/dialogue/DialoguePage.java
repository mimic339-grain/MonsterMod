package com.mimic.monstermod.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 会話1ページ分のデータ。クリック1回で1ページ進む。
 *
 * 表示は DialogueScreen が行い、
 * 保存は DialogueStore(ワールドデータ)、送信は S2C_StartDialoguePacket が担当する。
 */
public record DialoguePage(
        String speakerName,   // 話者名。自由入力("???" などもそのまま入れる)
        String text,          // 本文。改行(\n)可
        PortraitSpec portrait,
        String soundId,       // 効果音のID。空なら鳴らさない
        TextStyle style,
        int typewriterCps     // 1秒あたりの表示文字数。0なら最初から全文表示
) {
    /** 文体。表示時の演出を切り替える */
    public enum TextStyle {
        NORMAL,   // 通常
        SHAKE,    // 震える(脅し文句など)
        WAVE      // ゆらゆら揺れる
    }

    public static DialoguePage simple(String name, String text) {
        return new DialoguePage(name, text, PortraitSpec.NONE, "", TextStyle.NORMAL, 0);
    }

    /** タイプライター表示を使うか */
    public boolean isTypewriter() { return typewriterCps > 0; }

    // ---- NBT(ワールド保存用) ----
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", speakerName);
        tag.putString("text", text);
        tag.put("portrait", portrait.save());
        tag.putString("sound", soundId);
        tag.putString("style", style.name());
        tag.putInt("cps", typewriterCps);
        return tag;
    }

    public static DialoguePage load(CompoundTag tag) {
        TextStyle st;
        try {
            st = TextStyle.valueOf(tag.getString("style"));
        } catch (IllegalArgumentException e) {
            st = TextStyle.NORMAL;
        }
        return new DialoguePage(
                tag.getString("name"),
                tag.getString("text"),
                PortraitSpec.load(tag.getCompound("portrait")),
                tag.getString("sound"),
                st,
                tag.getInt("cps"));
    }

    // ---- ネットワーク ----
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(speakerName);
        buf.writeUtf(text);
        portrait.write(buf);
        buf.writeUtf(soundId);
        buf.writeEnum(style);
        buf.writeVarInt(typewriterCps);
    }

    public static DialoguePage read(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String text = buf.readUtf();
        PortraitSpec p = PortraitSpec.read(buf);
        String sound = buf.readUtf();
        TextStyle style = buf.readEnum(TextStyle.class);
        int cps = buf.readVarInt();
        return new DialoguePage(name, text, p, sound, style, cps);
    }

    /** 効果音のResourceLocation。未指定ならnull */
    public ResourceLocation soundLocation() {
        if (soundId == null || soundId.isEmpty()) return null;
        try {
            return new ResourceLocation(soundId);
        } catch (Exception e) {
            return null;
        }
    }
}
