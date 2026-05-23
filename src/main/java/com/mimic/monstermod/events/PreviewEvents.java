package com.mimic.monstermod.events;

import com.mimic.monstermod.Math.AoeMeshBuilder2D;
import com.mimic.monstermod.Math.AoeMeshBuilder3D;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.overlay.AoeRenderer2D;
import com.mimic.monstermod.overlay.AoeRenderer2DBlock;
import com.mimic.monstermod.overlay.AoeRenderer3D;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.skill.SkillLead;
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
public final class PreviewEvents {

    private static final List<Preview> PREVIEWS = new LinkedList<>();

    public static void spawnFromServer(Entity caster, SkillLead lead, MathMain math) {
        handlePreviewSpawning(caster, lead, math, "[Preview] S2C spawn: ");
    }

    private static void handlePreviewSpawning(Entity caster, SkillLead lead, MathMain math, String logPrefix) {
        // 【強化】EMERGENCY の場合、その術者(caster)に関連するプレビューを即座に全削除
        if (lead.category == SkillType.Category.CANCEL) {
            PREVIEWS.removeIf(p -> p.caster.getUUID().equals(caster.getUUID()));
            System.out.println(logPrefix + "EMERGENCYにより既存プレビューをクリア");
        }
        // 通常スキルの重複防止（クライアント側での表示重複を防ぐ）
        else if (lead.category == SkillType.Category.NORMAL) {
            if (PREVIEWS.stream().anyMatch(p -> p.caster.getUUID().equals(caster.getUUID()))) {
                return;
            }
        }

        PREVIEWS.add(new Preview(caster, lead, math));
    }

    /**
     * 【修正】プレビューの寿命管理のみを行う
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        Iterator<Preview> it = PREVIEWS.iterator();
        while (it.hasNext()) {
            Preview p = it.next();

            if (!p.caster.isAlive()) {
                it.remove();
                continue;
            }

            // 術者に追従する場合の座標更新
            if (p.lead.followCaster) {
                p.math.origin = p.caster.position();
            }

            p.life--;
            if (p.life <= 0) {
                it.remove();
            }
        }
    }

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

            if (lead.render2D) { AoeRenderer2D.render(e.getPoseStack(), buffers, new AoeMeshBuilder2D(math), tex); }
            if (lead.renderBlock2D) { AoeRenderer2DBlock.render(e.getPoseStack(), buffers, math, mc.level, tex, mc.player.position()); }
            if (lead.render3DPreview) { AoeRenderer3D.render(e.getPoseStack(), buffers, new AoeMeshBuilder3D(math), tex); }
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