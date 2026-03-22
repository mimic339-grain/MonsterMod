package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AoeMeshBuilder2D;
import com.mimic.monstermod.Math.AoeMeshBuilder3D;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.identity.impl.MimicIdentity;
import com.mimic.monstermod.skill.AttackType;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "monstermod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {

    private static final List<Preview> PREVIEWS = new LinkedList<>();

    public static void spawnLocal(Entity caster, SkillLead lead, MathMain math) {
        handlePreviewSpawning(caster, lead, math, "[Preview] Local spawn (Self Cast): ");
    }

    public static void spawnFromServer(Entity caster, SkillLead lead, MathMain math) {
        // 自分が撃ったパケットが返ってきた場合は無視（二重表示防止）
        if (caster == Minecraft.getInstance().player) {
            return;
        }
        handlePreviewSpawning(caster, lead, math, "[Preview] S2C spawn (Other Cast): ");
    }

    /**
     * プレビュー生成の共通ロジック
     * 緊急スキルのキャンセル処理や、通常スキルの重複チェックを行います。
     */
    private static void handlePreviewSpawning(Entity caster, SkillLead lead, MathMain math, String logPrefix) {
        System.out.println(logPrefix + lead.skillId());

        // 1. 緊急スキルの場合：この術者が出している既存のプレビューをすべて消去
        if (lead.category == AttackType.Category.EMERGENCY) {
            PREVIEWS.removeIf(p -> p.caster == caster);
        }
        // 2. 通常スキルの場合：既にプレビューが出ているなら、新しいプレビューを表示しない（上書き禁止の視覚化）
        else if (lead.category == AttackType.Category.NORMAL) {
            boolean alreadyExists = PREVIEWS.stream().anyMatch(p -> p.caster == caster);
            if (alreadyExists) return;
        }
        // ※ COMBO の場合は何もしない（そのまま追加して重ねる）

        PREVIEWS.add(new Preview(caster, lead, math));
    }

    /**
     * クライアント側のTick処理。
     * ここで Identity のクールダウン進行と、プレビュー表示の寿命管理をまとめて行います。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // --- クールダウン進行 ---
        mc.player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            var identity = trans.getIdentity();
            if (identity instanceof MimicIdentity mimic) {
                mimic.tickClient(mc.player);
            }
        });

        // --- プレビュー表示（PREVIEWS）の寿命管理 ---
        Iterator<Preview> it = PREVIEWS.iterator();
        while (it.hasNext()) {
            Preview p = it.next();

            if (!p.caster.isAlive()) {
                it.remove();
                continue;
            }

            if (p.lead.followCaster) {
                p.math.origin = p.caster.position();
            }

            p.life--;
            if (p.life <= 0) {
                it.remove();
            }
        }
    }

    /**
     * ワールド描画イベント。
     */
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (PREVIEWS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        e.getPoseStack().pushPose();
        e.getPoseStack().translate(-cam.x, -cam.y, -cam.z);

        for (Preview p : PREVIEWS) {
            SkillLead lead = p.lead;
            MathMain math = p.math;
            ResourceLocation tex = lead.previewTexture;

            if (lead.render2D) {AoeRenderer2D.render(e.getPoseStack(), buffers, new AoeMeshBuilder2D(math), tex);}
            if (lead.renderBlock2D) {AoeRenderer2DBlock.render(e.getPoseStack(), buffers, math, mc.level, tex, mc.player.position());}
            if (lead.render3DPreview) {AoeRenderer3D.render(e.getPoseStack(), buffers, new AoeMeshBuilder3D(math), tex);}
        }

        e.getPoseStack().popPose();
        buffers.endBatch();
    }

    private static final class Preview {
        final Entity caster;
        final SkillLead lead;
        final MathMain math;
        int life;

        Preview(Entity caster, SkillLead lead, MathMain math) {
            this.caster = caster;
            this.lead = lead;
            this.math = math;
            this.life = lead.totalPreviewTicks;
        }
    }
}