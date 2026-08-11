package com.mimic.monstermod.Math;

import com.mimic.monstermod.skill.SkillLead;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * AoeMeshBuilder2D（完全版）
 * ✔ Sampler完全排除
 * ✔ MathMainのパラメータから直接ポリゴン生成
 * ✔ containsは一切使わない
 */
public final class AoeMeshBuilder2D {

    public record Quad(Vec3[] pos) {}

    private final SkillLead lead;
    private final MathMain math;

    public AoeMeshBuilder2D(SkillLead lead, MathMain math) {
        this.lead = lead;
        this.math = math;
    }

    public List<Quad> build() {

        List<Quad> result = switch (math.shape) {
            case SPHERE, CYLINDER -> buildCircle();
            case FAN -> buildFan();
            case RECT_PRISM, BOX -> buildRect();
            case TRI_PRISM -> buildTriangle();
            case RADIAL -> buildRadial();
            case SPIRAL -> buildSpiral();
            default -> new ArrayList<>();
        };

        return result;
    }

    /* =========================
     * 円
     * ========================= */
    private List<Quad> buildCircle() {

        List<Quad> list = new ArrayList<>();

        int detail = 32;
        float r = math.radius;

        Vec3 center = math.origin;

        double y = center.y + 0.1; // ★ 強制補正

        for (int i = 0; i < detail; i++) {

            double a0 = 2 * Math.PI * i / detail;
            double a1 = 2 * Math.PI * (i + 1) / detail;

            Vec3 p1 = rotateY(new Vec3(Math.cos(a0) * r, 0, Math.sin(a0) * r));
            Vec3 p2 = rotateY(new Vec3(Math.cos(a1) * r, 0, Math.sin(a1) * r));

            Vec3 v0 = new Vec3(center.x, y, center.z);
            Vec3 v1 = new Vec3(center.x + p1.x, y, center.z + p1.z);
            Vec3 v2 = new Vec3(center.x + p2.x, y, center.z + p2.z);

            list.add(new Quad(new Vec3[]{
                    v0, v1, v2, v0
            }));
        }

        return list;
    }

    /* =========================
     * 扇
     * ========================= */
    private List<Quad> buildFan() {

        List<Quad> list = new ArrayList<>();

        int detail = 32;
        float r = math.radius;
        float half = math.angleDeg / 2f;

        Vec3 c = math.origin;

        for (int i = 0; i < detail; i++) {

            double a0 = Math.toRadians(-half + math.angleDeg * i / detail);
            double a1 = Math.toRadians(-half + math.angleDeg * (i + 1) / detail);

            Vec3 p1 = rotateY(new Vec3(Math.sin(a0)*r,0,Math.cos(a0)*r));
            Vec3 p2 = rotateY(new Vec3(Math.sin(a1)*r,0,Math.cos(a1)*r));

            list.add(new Quad(new Vec3[]{
                    c,
                    c.add(p1),
                    c.add(p2),
                    c.add(p2)
            }));
        }

        return list;
    }

    /* =========================
     * 矩形
     * ========================= */
    private List<Quad> buildRect() {

        List<Quad> list = new ArrayList<>();

        float xr = math.xRadius;
        float zr = math.zRadius;

        Vec3[] pts = {
                new Vec3(-xr,0,-zr),
                new Vec3(xr,0,-zr),
                new Vec3(xr,0,zr),
                new Vec3(-xr,0,zr)
        };

        for (int i = 0; i < 4; i++) {
            pts[i] = math.origin.add(rotateY(pts[i]));
        }

        list.add(new Quad(pts));
        return list;
    }

    /* =========================
     * 三角
     * ========================= */
    private List<Quad> buildTriangle() {

        List<Quad> list = new ArrayList<>();

        float half = math.baseHalf;
        float depth = math.depth;

        Vec3 c = math.origin;

        Vec3 p1 = rotateY(new Vec3(-half,0,0));
        Vec3 p2 = rotateY(new Vec3(half,0,0));
        Vec3 p3 = rotateY(new Vec3(0,0,depth));

        list.add(new Quad(new Vec3[]{
                c.add(p1),
                c.add(p2),
                c.add(p3),
                c.add(p3)
        }));

        return list;
    }
    /* =========================
     * 放射状ライン (弾幕プレビュー用)
     * ========================= */
    private List<Quad> buildRadial() {
        List<Quad> list = new ArrayList<>();
        if (lead == null || lead.projectilePattern == null) return list;
        ProjectilePattern pattern = lead.projectilePattern;

        float lineWidth = 0.8f; // プレビューで見やすい太さ
        Vec3 center = math.origin;
        double y = center.y + 0.1; // 地面より少し上
        double r = pattern.maxReach(); // 実際の弾の射程と必ず一致する

        for (int i = 0; i < pattern.count(); i++) {
            // 実際の弾の初速方向と同じ式(ProjectilePattern)を使う
            double angle = pattern.startAngle(i);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // 線の幅（太さ）を持たせるためのオフセット
            double dx = -sin * (lineWidth / 2.0);
            double dz = cos * (lineWidth / 2.0);

            // 始点（中心側）
            Vec3 v0 = new Vec3(center.x + dx, y, center.z + dz);
            Vec3 v1 = new Vec3(center.x - dx, y, center.z - dz);
            // 終点（外側）
            Vec3 v2 = new Vec3(center.x + cos * r - dx, y, center.z + sin * r - dz);
            Vec3 v3 = new Vec3(center.x + cos * r + dx, y, center.z + sin * r + dz);

            list.add(new Quad(new Vec3[]{ v0, v1, v2, v3 }));
        }
        return list;
    }
    /* =========================
     * 螺旋状ライン (SpiralOnibi プレビュー用)
     * ========================= */
    private List<Quad> buildSpiral() {
        List<Quad> list = new ArrayList<>();
        if (lead == null || lead.projectilePattern == null) return list;
        ProjectilePattern pattern = lead.projectilePattern;

        float lineWidth = 0.3f; // 線の太さ

        Vec3 center = math.origin;
        double y = center.y + 0.1;

        for (int i = 0; i < pattern.count(); i++) {
            // 螺旋を線分（短い四角形の連続）で描画する。
            // 実際の弾(SpiralOnibiEntity)と同じProjectilePattern#offsetAtTickを使うため、
            // 見た目と実際の軌道・寿命が必ず一致する。
            for (int t = 0; t < pattern.lifeTicks(); t++) {
                Vec3 p0 = pattern.offsetAtTick(i, t);
                Vec3 p1 = pattern.offsetAtTick(i, t + 1);

                double dx = p1.x - p0.x;
                double dz = p1.z - p0.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len == 0) continue;

                double nx = -dz / len * (lineWidth / 2.0);
                double nz = dx / len * (lineWidth / 2.0);

                Vec3 v0 = new Vec3(center.x + p0.x + nx, y, center.z + p0.z + nz);
                Vec3 v1 = new Vec3(center.x + p0.x - nx, y, center.z + p0.z - nz);
                Vec3 v2 = new Vec3(center.x + p1.x - nx, y, center.z + p1.z - nz);
                Vec3 v3 = new Vec3(center.x + p1.x + nx, y, center.z + p1.z + nz);

                list.add(new Quad(new Vec3[]{ v0, v1, v2, v3 }));
            }
        }
        return list;
    }
    /* ========================= */
    private Vec3 rotateY(Vec3 v) {
        double r = Math.toRadians(-math.yaw);
        double c = Math.cos(r);
        double s = Math.sin(r);
        return new Vec3(
                v.x * c - v.z * s,
                v.y,
                v.x * s + v.z * c
        );
    }
}