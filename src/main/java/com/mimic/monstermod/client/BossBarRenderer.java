package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.boss.BossBarStyle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ボスバーをMOD独自のデザインで描き直す。
 *
 * 【Mixinを使っていない理由】
 * Forgeが用意している {@link CustomizeGuiOverlayEvent.BossEventProgress} は
 * 「バー1本ぶんの描画」の直前に呼ばれ、キャンセルするとバニラの描画だけを止められる。
 * 縦に積む位置の計算はバニラ側に残るので、他MODのボスバーとも共存できる。
 *
 * 【自分のバーだけを差し替える】
 * このイベントはエンダードラゴンなど他のボスでも呼ばれる。
 * {@link #STYLES} に載っているUUID(= MonsterModが出したバー)のときだけ差し替え、
 * それ以外は何もせずバニラに任せる。
 *
 * 枠デザインは {@link com.mimic.monstermod.network.server.S2C_BossBarStylePacket} で届く。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, value = Dist.CLIENT)
public final class BossBarRenderer {

    private BossBarRenderer() {}

    /** バーのUUID → 枠デザイン。ここに無いバーはバニラのまま描かれる */
    private static final Map<UUID, BossBarStyle> STYLES = new HashMap<>();

    /** 消え損ねた分が溜まり続けないための上限。普通は数本しか入らない */
    private static final int MAX_ENTRIES = 64;

    /** 名前とバーの間隔(バニラと同じ9px) */
    private static final int NAME_GAP = 9;

    /**
     * 枠デザインを受け取る。
     * 呼び出し元: {@link com.mimic.monstermod.network.server.S2C_BossBarStylePacket#handle}
     */
    public static void setStyle(UUID barId, BossBarStyle style) {
        if (STYLES.size() > MAX_ENTRIES) STYLES.clear();
        STYLES.put(barId, style);
    }

    /** ワールドを抜けたら持ち越さない。別のサーバーでUUIDが再利用されることはないが、念のため */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STYLES.clear();
    }

    /**
     * バー1本ぶんの描画。
     * バニラの描画をキャンセルして、枠テクスチャとゲージを自前で貼る。
     */
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bossEvent = event.getBossEvent();
        BossBarStyle style = STYLES.get(bossEvent.getId());
        if (style == null) return; // MonsterModのバーではないので触らない

        event.setCanceled(true);
        // 枠のぶんだけ縦を広く取る。これを忘れると2本目が重なる
        event.setIncrement(BossBarStyle.FRAME_H + NAME_GAP + 3);

        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int x = (screenWidth - BossBarStyle.FRAME_W) / 2;
        int y = event.getY();

        // 枠テクスチャには透明な部分(飾りの外側)があるので、必ず合成を有効にしてから貼る
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. 枠。ゲージが減ったときに見える濃い赤の溝もこの1枚に入っている
        graphics.blit(style.texture(), x, y,
                BossBarStyle.FRAME_W, BossBarStyle.FRAME_H,
                0.0F, 0.0F, BossBarStyle.FRAME_W, BossBarStyle.FRAME_H,
                BossBarStyle.TEX_W, BossBarStyle.TEX_H);

        // 2. ゲージの中身。残量ぶんだけ左から切り出して溝の中に貼る
        int filled = Mth.clamp(Math.round(BossBarStyle.BAR_W * bossEvent.getProgress()),
                0, BossBarStyle.BAR_W);
        if (filled > 0) {
            graphics.blit(style.texture(),
                    x + BossBarStyle.BAR_X, y + BossBarStyle.BAR_Y,
                    filled, BossBarStyle.BAR_H,
                    BossBarStyle.BAR_X, BossBarStyle.BAR_V,
                    filled, BossBarStyle.BAR_H,
                    BossBarStyle.TEX_W, BossBarStyle.TEX_H);

            // 減っている途中は先端に明るい線を入れて、どこまで残っているかを見やすくする
            if (filled < BossBarStyle.BAR_W) {
                int edgeX = x + BossBarStyle.BAR_X + filled - 1;
                graphics.fill(edgeX, y + BossBarStyle.BAR_Y,
                        edgeX + 1, y + BossBarStyle.BAR_Y + BossBarStyle.BAR_H, 0xFFE6F4FF);
            }
        }

        RenderSystem.disableBlend();

        // 3. 役職名。バニラと同じくバーの上に中央揃えで出す
        Component name = bossEvent.getName();
        int nameX = (screenWidth - font.width(name)) / 2;
        graphics.drawString(font, name, nameX, y - NAME_GAP, 0xFFFFFF, true);
    }
}
