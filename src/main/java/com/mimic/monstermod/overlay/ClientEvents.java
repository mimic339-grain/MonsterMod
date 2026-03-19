package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AoeMeshBuilder2D;
import com.mimic.monstermod.Math.AoeMeshBuilder3D;
import com.mimic.monstermod.Math.MathMain;
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

/**
 * ClientEvents
 * Client Preview の唯一の管理点
 * 設計思想
 * ・MathMain = 唯一の真理
 * ・Sampler はここで生成
 * ・Renderer は Sample のみ受け取る
 * ・Client は Attack に関与しない
 */

@Mod.EventBusSubscriber(
        modid = "monstermod",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientEvents {

    static {
        System.out.println("[ClientEvents] Loaded");
    }

    public static void spawnLocal(Entity caster, SkillLead lead, MathMain math) {

        System.out.println("[Preview] Local spawn " + lead.skillId());

        PREVIEWS.add(new Preview(caster, lead, math));
    }


    /* ===================== */
    /* Preview Entry         */
    /* ===================== */

    private static final class Preview {

        final Entity caster;
        final SkillLead lead;
        final MathMain math;

        int life;

        Preview(Entity caster, SkillLead lead, MathMain math) {
            this.caster = caster;
            this.lead = lead;
            this.math = math;
            this.life = lead.lifetimeTick;

            System.out.println("[Preview] Created " + lead.skillId());
        }
    }

    private static final List<Preview> PREVIEWS = new LinkedList<>();

    private ClientEvents() {}

    /* ===================== */
    /* Spawn (S2C Packet)    */
    /* ===================== */

    public static void spawnFromServer(Entity caster, SkillLead lead, MathMain math) {

        System.out.println("[Preview] S2C spawn " + lead.skillId());

        PREVIEWS.add(new Preview(caster, lead, math));
    }

    /* ===================== */
    /* Tick                  */
    /* ===================== */

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {

        if (e.phase != TickEvent.Phase.END) return;

        if (!PREVIEWS.isEmpty()) {
            System.out.println("[Preview] Tick previews=" + PREVIEWS.size());
        }

        Iterator<Preview> it = PREVIEWS.iterator();

        while (it.hasNext()) {

            Preview p = it.next();

            if (!p.caster.isAlive()) {
                System.out.println("[Preview] Removed (caster dead)");
                it.remove();
                continue;
            }

            p.life--;

            if (p.life <= 0) {
                System.out.println("[Preview] Removed (timeout)");
                it.remove();
            }
        }
    }

    /* ===================== */
    /* Render                */
    /* ===================== */

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {

        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;

        if (PREVIEWS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();

        MultiBufferSource.BufferSource buffers =
                mc.renderBuffers().bufferSource();

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        e.getPoseStack().pushPose();
        e.getPoseStack().translate(-cam.x, -cam.y, -cam.z);

        for (Preview p : PREVIEWS) {

            SkillLead lead = p.lead;
            MathMain math = p.math;

            float range = 32f;

            /* ---------- 2D ---------- */
            if (lead.render2D) {

                AoeMeshBuilder2D builder = new AoeMeshBuilder2D(math);

                AoeRenderer2D.render(
                        e.getPoseStack(),
                        buffers,
                        builder
                );
            }

            /* ---------- 2D Overlay ---------- */
            if (lead.render2DOverlay) {

                AoeMeshBuilder2D builder = new AoeMeshBuilder2D(math);

                AoeRenderer2DOverlay.render(
                        e.getPoseStack(),
                        buffers,
                        builder
                );
            }
            /* ---------- Block 2D ---------- */

            if (lead.renderBlock2D) {

                AoeRenderer2DBlock.render(
                        e.getPoseStack(),
                        buffers,
                        math,
                        (int) p.caster.getY(),
                        (int) range,
                        true,
                        new ResourceLocation(
                                "monstermod",
                                "textures/misc/attackpreview.png"
                        )
                );
            }
            /* ---------- 3D Preview ---------- */
            if (lead.render3DPreview) {

                AoeMeshBuilder3D builder =
                        new AoeMeshBuilder3D(math);

                AoeRenderer3D.render(
                        e.getPoseStack(),
                        buffers,
                        builder
                );
            }
        }

        e.getPoseStack().popPose();

        buffers.endBatch();
    }
}