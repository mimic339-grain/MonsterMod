package com.mimic.monstermod.Math;

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

    private final MathMain math;

    public AoeMeshBuilder2D(MathMain math) {
        this.math = math;
    }

    public List<Quad> build() {

        List<Quad> result = switch (math.shape) {
            case SPHERE, CYLINDER -> buildCircle();
            case FAN -> buildFan();
            case RECT_PRISM, BOX -> buildRect();
            case TRI_PRISM -> buildTriangle();
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