package com.mimic.monstermod.overlay;

import com.mimic.monstermod.Math.AoeMeshBuilder2D;
import com.mimic.monstermod.Math.AoeMeshBuilder3D;
import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.identity.impl.MimicIdentity;
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
        System.out.println("[Preview] Local spawn (Self Cast): " + lead.skillId());
        PREVIEWS.add(new Preview(caster, lead, math));
    }

    public static void spawnFromServer(Entity caster, SkillLead lead, MathMain math) {
        // 自分が撃ったパケットが返ってきた場合は無視（二重表示防止）
        if (caster == Minecraft.getInstance().player) {
            return;
        }
        System.out.println("[Preview] S2C spawn (Other Cast): " + lead.skillId());
        PREVIEWS.add(new Preview(caster, lead, math));
    }

    /**
     * クライアント側のTick処理。
     * ここで Identity のクールダウン進行と、プレビュー表示の寿命管理をまとめて行います。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        // ENDフェーズで1回だけ実行（STARTとENDで2回呼ばれるのを防ぐ）
        if (e.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // --- クールダウン進行の追加ロジック ---
        // 変身中の Identity を取得し、クライアント側で1Tickずつクールダウンを減らす
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

            // 術者が死んでいる場合は即削除
            if (!p.caster.isAlive()) {
                it.remove();
                continue;
            }

            // 追従設定がある場合、位置を更新
            if (p.lead.followCaster) {
                p.math.origin = p.caster.position();
            }

            // 寿命（totalPreviewTicks）を減らし、0になったら削除
            p.life--;
            if (p.life <= 0) {
                it.remove();
            }
        }
    }

    /**
     * ワールド描画イベント。
     * 登録されている全プレビューをレンダリングします。
     */
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        // 透明ブロック描画後に重ねて描画する（多くのエフェクトに適したタイミング）
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (PREVIEWS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        e.getPoseStack().pushPose();
        // カメラ座標に合わせて座標系を移動
        e.getPoseStack().translate(-cam.x, -cam.y, -cam.z);

        for (Preview p : PREVIEWS) {
            SkillLead lead = p.lead;
            MathMain math = p.math;
            ResourceLocation tex = lead.previewTexture;

            // 各レンダラーに処理を委託
            if (lead.render2D) {AoeRenderer2D.render(e.getPoseStack(), buffers, new AoeMeshBuilder2D(math), tex);}
            if (lead.renderBlock2D) {AoeRenderer2DBlock.render(e.getPoseStack(), buffers, math, mc.level, tex, mc.player.position());}
            if (lead.render3DPreview) {AoeRenderer3D.render(e.getPoseStack(), buffers, new AoeMeshBuilder3D(math), tex);}
        }

        e.getPoseStack().popPose();
        buffers.endBatch(); // 描画確定
    }

    /**
     * 表示中のプレビュー情報を保持する内部クラス
     */
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