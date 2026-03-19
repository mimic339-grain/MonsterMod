package com.mimic.monstermod.Math;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * AoeMeshBuilder3D（完全版）
 * ✔ Sampler完全排除
 * ✔ MathMainのパラメータから直接メッシュ生成
 * ✔ containsは一切使わない
 * ✔ 面（ポリゴン）として3D形状を構築
 */
public final class AoeMeshBuilder3D {

    public record Quad(Vec3[] pos) {}

    private final MathMain math;

    public AoeMeshBuilder3D(MathMain math) {
        this.math = math;
    }

    public List<Quad> build() {

        return switch (math.shape) {
            case SPHERE -> buildSphere();
            case CYLINDER -> buildCylinder();
            case BOX, RECT_PRISM -> buildBox();
            default -> new ArrayList<>();
        };
    }

    /* =========================
     * 球
     * ========================= */
    private List<Quad> buildSphere() {

        List<Quad> list = new ArrayList<>();

        int lat = 12;
        int lon = 24;

        float r = math.radius;
        Vec3 c = math.origin;

        for (int i = 0; i < lat; i++) {

            double t0 = Math.PI * i / lat;
            double t1 = Math.PI * (i + 1) / lat;

            for (int j = 0; j < lon; j++) {

                double p0 = 2 * Math.PI * j / lon;
                double p1 = 2 * Math.PI * (j + 1) / lon;

                Vec3 v1 = sphere(r, t0, p0);
                Vec3 v2 = sphere(r, t0, p1);
                Vec3 v3 = sphere(r, t1, p1);
                Vec3 v4 = sphere(r, t1, p0);

                list.add(new Quad(new Vec3[]{
                        c.add(v1),
                        c.add(v2),
                        c.add(v3),
                        c.add(v4)
                }));
            }
        }

        return list;
    }

    private Vec3 sphere(float r, double t, double p) {
        double x = r * Math.sin(t) * Math.cos(p);
        double y = r * Math.cos(t);
        double z = r * Math.sin(t) * Math.sin(p);
        return rotateY(new Vec3(x, y, z));
    }

    /* =========================
     * 円柱
     * ========================= */
    private List<Quad> buildCylinder() {

        List<Quad> list = new ArrayList<>();

        int detail = 32;

        float r = math.radius;
        float h = math.height;

        Vec3 c = math.origin;

        double y0 = -h / 2;
        double y1 = h / 2;

        for (int i = 0; i < detail; i++) {

            double a0 = 2 * Math.PI * i / detail;
            double a1 = 2 * Math.PI * (i + 1) / detail;

            Vec3 p1 = rotateY(new Vec3(Math.cos(a0)*r, 0, Math.sin(a0)*r));
            Vec3 p2 = rotateY(new Vec3(Math.cos(a1)*r, 0, Math.sin(a1)*r));

            Vec3 v1 = c.add(p1.x, y0, p1.z);
            Vec3 v2 = c.add(p2.x, y0, p2.z);
            Vec3 v3 = c.add(p2.x, y1, p2.z);
            Vec3 v4 = c.add(p1.x, y1, p1.z);

            list.add(new Quad(new Vec3[]{v1, v2, v3, v4}));
        }

        return list;
    }

    /* =========================
     * Box
     * ========================= */
    private List<Quad> buildBox() {

        List<Quad> list = new ArrayList<>();

        float xr = math.xRadius;
        float zr = math.zRadius;
        float h = math.height;

        Vec3 c = math.origin;

        Vec3[] pts = {
                new Vec3(-xr, -h/2, -zr),
                new Vec3(xr, -h/2, -zr),
                new Vec3(xr, -h/2, zr),
                new Vec3(-xr, -h/2, zr),

                new Vec3(-xr, h/2, -zr),
                new Vec3(xr, h/2, -zr),
                new Vec3(xr, h/2, zr),
                new Vec3(-xr, h/2, zr),
        };

        for (int i = 0; i < pts.length; i++) {
            pts[i] = c.add(rotateY(pts[i]));
        }

        // bottom
        list.add(new Quad(new Vec3[]{pts[0], pts[1], pts[2], pts[3]}));
        // top
        list.add(new Quad(new Vec3[]{pts[4], pts[5], pts[6], pts[7]}));

        // sides
        list.add(new Quad(new Vec3[]{pts[0], pts[1], pts[5], pts[4]}));
        list.add(new Quad(new Vec3[]{pts[1], pts[2], pts[6], pts[5]}));
        list.add(new Quad(new Vec3[]{pts[2], pts[3], pts[7], pts[6]}));
        list.add(new Quad(new Vec3[]{pts[3], pts[0], pts[4], pts[7]}));

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