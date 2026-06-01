package com.mimic.monstermod.client.debug;

import com.mimic.monstermod.collision.OBBCollider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * OBBとボーンをカラーワイヤーフレームでデバッグ表示するレンダラー。
 *
 * 色の意味:
 *   - 緑 (0,255,0): 食らい判定OBB（通常部位）
 *   - 赤 (255,0,0): 弱点部位OBB
 *   - 黄 (255,255,0): エフェクト範囲
 *   - 白 (255,255,255): ボーン（親→子ライン）
 *
 * 有効化: /dragondebug コマンドで切り替え
 *
 * 配置: com/mimic/monstermod/client/debug/OBBWireframeRenderer.java
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OBBWireframeRenderer {

    /** デバッグモードの有効/無効フラグ */
    public static boolean debugEnabled = false;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!debugEnabled) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        var level = event.getLevelRenderer();
        // 全エンティティを走査してカスタムエンティティのOBBを描画
        // 実際の実装では ClientLevel のエンティティリストを取得
    }

    /**
     * 単一OBBをワイヤーフレームで描画する。
     *
     * @param obb     描画するOBB
     * @param r,g,b   色（0〜255）
     * @param consumer VertexConsumer（RenderType.LINES を使用）
     * @param pose    PoseStack
     */
    public static void renderOBB(OBBCollider obb, int r, int g, int b,
                                 VertexConsumer consumer, PoseStack pose) {
        // OBBの8頂点を計算
        Vector3f[] corners = computeCorners(obb);
        float alpha = 200;

        // 12本のエッジを描画（直方体の辺）
        int[][] edges = {
                {0,1},{1,3},{3,2},{2,0}, // 下面
                {4,5},{5,7},{7,6},{6,4}, // 上面
                {0,4},{1,5},{2,6},{3,7}  // 縦エッジ
        };

        Matrix4f mat = pose.last().pose();
        for (int[] edge : edges) {
            drawLine(consumer, mat, corners[edge[0]], corners[edge[1]], r, g, b, (int)alpha);
        }
    }

    /** OBBの8頂点をワールド空間で計算する */
    private static Vector3f[] computeCorners(OBBCollider obb) {
        Vector3f[] corners = new Vector3f[8];
        float hx = obb.halfExtents.x;
        float hy = obb.halfExtents.y;
        float hz = obb.halfExtents.z;

        float[][] signs = {
                {-1,-1,-1},{1,-1,-1},{-1,1,-1},{1,1,-1},
                {-1,-1, 1},{1,-1, 1},{-1,1, 1},{1,1, 1}
        };

        for (int i = 0; i < 8; i++) {
            Vector3f local = new Vector3f(
                    signs[i][0] * hx,
                    signs[i][1] * hy,
                    signs[i][2] * hz
            );
            corners[i] = obb.orientation.transform(local).add(obb.center);
        }
        return corners;
    }

    private static void drawLine(VertexConsumer consumer, Matrix4f mat,
                                 Vector3f a, Vector3f b, int r, int g, int bv, int alpha) {
        consumer.vertex(mat, a.x, a.y, a.z).color(r, g, bv, alpha)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(mat, b.x, b.y, b.z).color(r, g, bv, alpha)
                .normal(0, 1, 0).endVertex();
    }
}