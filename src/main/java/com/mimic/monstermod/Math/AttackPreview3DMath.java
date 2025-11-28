package com.mimic.monstermod.Math;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * AttackPreview3DMath
 * 完全版: BOX / SPHERE / CYLINDER / CAPSULE / FAN3D / CROSS3D / TRIANGLE_PRISM に対応
 * Quad/Edge 生成を正確に行い、AoeRenderer3D で崩れず描画可能
 */
public final class AttackPreview3DMath {

    private AttackPreview3DMath() {}

    // ------------------------------------------
    // 共通データコンテナ
    // ------------------------------------------
    public static class ShapeData {
        public Shape shape;
        public Vec3 origin;
        public float yaw, pitch;
        public float xRadius, yRadius, zRadius; // BOX/CYL/FAN
        public float radius, height;            // SPHERE/CAPSULE/CYL
        public float angleDeg;                  // FAN
        public float size;                      // TRIANGLE_PRISM
        public final List<Quad> surfaces = new ArrayList<>();
        public final List<Edge> edges = new ArrayList<>();
    }

    public static class Quad {
        public Vec3[] pos = new Vec3[4];
        public Vec3 normal;
        public float[] uv = new float[8];   // 4頂点 × (u,v)
        public float[] rgba = new float[]{1f, 0f, 0f, 0.3f};//面と透明度
    }

    public static class Edge {
        public Vec3 a, b;
        public float[] rgba = new float[]{1f, 1f, 1f, 0.6f};
    }
    public enum Shape {
        BOX, SPHERE, CYLINDER, CAPSULE, FAN3D, CROSS3D, TRIANGLE_PRISM
    }

    // =====================================================
    //　共通API
    // =====================================================
    public static ShapeData makeBox(Vec3 origin, float yaw, float xr, float yr, float zr) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.BOX;
        d.xRadius = xr; d.yRadius = yr; d.zRadius = zr;
        computeBox(d);
        return d;
    }

    public static ShapeData makeSphere(Vec3 origin, float yaw, float radius) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.SPHERE;
        d.radius = radius;
        computeSphere(d);
        return d;
    }

    public static ShapeData makeCylinder(Vec3 origin, float yaw, float radius, float height) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.CYLINDER;
        d.radius = radius; d.height = height;
        computeCylinder(d);
        return d;
    }

    public static ShapeData makeCapsule(Vec3 origin, float yaw, float radius, float height) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.CAPSULE;
        d.radius = radius; d.height = height;
        computeCapsule(d);
        return d;
    }

    public static ShapeData makeFan3D(Vec3 origin, float yaw, float radius, float angleDeg) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.FAN3D;
        d.radius = radius; d.angleDeg = angleDeg;
        computeFan3D(d);
        return d;
    }

    public static ShapeData makeCross3D(Vec3 origin, float yaw, float radius) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.CROSS3D;
        d.radius = radius;
        computeCross3D(d);
        return d;
    }

    public static ShapeData makeTrianglePrism(Vec3 origin, float yaw, float size) {
        ShapeData d = base(origin, yaw);
        d.shape = Shape.TRIANGLE_PRISM;
        d.size = size;
        computeTrianglePrism(d);
        return d;
    }

    private static ShapeData base(Vec3 origin, float yaw) {
        ShapeData d = new ShapeData();
        d.origin = origin; d.yaw = yaw; d.pitch = 0;
        return d;
    }

    // =====================================================
    // 回転util
    // =====================================================
    private static Vec3 rotateY(Vec3 v, float yawDeg) {
        double yaw = Math.toRadians(-yawDeg);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = v.x * cos - v.z * sin;
        double z = v.x * sin + v.z * cos;
        return new Vec3(x, v.y, z);
    }

    private static Vec3 rotateX(Vec3 v, float pitchDeg) {
        double pitch = Math.toRadians(-pitchDeg);
        double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        double y = v.y * cos - v.z * sin;
        double z = v.y * sin + v.z * cos;
        return new Vec3(v.x, y, z);
    }

    // BOX
    private static void computeBox(ShapeData d) {
        float xr = d.xRadius, yr = d.yRadius, zr = d.zRadius;

        Vec3[] pts = {
                new Vec3(-xr,-yr,-zr), new Vec3(xr,-yr,-zr),
                new Vec3(xr,yr,-zr), new Vec3(-xr,yr,-zr),
                new Vec3(-xr,-yr,zr), new Vec3(xr,-yr,zr),
                new Vec3(xr,yr,zr), new Vec3(-xr,yr,zr)
        };
        int[][] faces = {
                {0,1,2,3}, {4,5,6,7}, {0,4,7,3},
                {1,5,6,2}, {3,2,6,7}, {0,1,5,4}
        };
        for(int[] f : faces){
            Quad q = new Quad();
            for(int i=0;i<4;i++){
                q.pos[i] = d.origin.add(rotateY(pts[f[i]], d.yaw));
                q.uv[i*2] = (i==0||i==3)?0f:1f;
                q.uv[i*2+1] = (i<2)?0f:1f;
            }
            Vec3 u = q.pos[1].subtract(q.pos[0]);
            Vec3 v = q.pos[3].subtract(q.pos[0]);
            q.normal = new Vec3(u.y*v.z - u.z*v.y, u.z*v.x - u.x*v.z, u.x*v.y - u.y*v.x);

            q.rgba = new float[]{1f, 0f, 0f, 0.1f};//不透明度を0.1fにする

            d.surfaces.add(q);

            for(int i=0;i<4;i++){
                Edge e = new Edge();
                e.a = q.pos[i];
                e.b = q.pos[(i+1)%4];
                d.edges.add(e);
            }
        }
    }

    //円柱　枠線コメントアウト
    private static void computeCylinder(ShapeData d) {
        int detail = 32;
        float r = d.radius, h = d.height;
        for(int i=0;i<detail;i++){
            double th0 = 2*Math.PI*i/detail;
            double th1 = 2*Math.PI*(i+1)/detail;
            Vec3 p0 = new Vec3(Math.cos(th0)*r, -h/2, Math.sin(th0)*r);
            Vec3 p1 = new Vec3(Math.cos(th1)*r, -h/2, Math.sin(th1)*r);
            Vec3 p2 = new Vec3(Math.cos(th1)*r, h/2, Math.sin(th1)*r);
            Vec3 p3 = new Vec3(Math.cos(th0)*r, h/2, Math.sin(th0)*r);

            Quad q = new Quad();
            q.pos[0]=d.origin.add(rotateY(p0,d.yaw));
            q.pos[1]=d.origin.add(rotateY(p1,d.yaw));
            q.pos[2]=d.origin.add(rotateY(p2,d.yaw));
            q.pos[3]=d.origin.add(rotateY(p3,d.yaw));

            Vec3 u = q.pos[1].subtract(q.pos[0]);
            Vec3 v = q.pos[3].subtract(q.pos[0]);
            q.normal = new Vec3(u.y*v.z - u.z*v.y, u.z*v.x - u.x*v.z, u.x*v.y - u.y*v.x);

            for(int j=0;j<4;j++){
                q.uv[j*2]= (j==0||j==3)?0f:1f;
                q.uv[j*2+1]= (j<2)?0f:1f;
            }

            d.surfaces.add(q);
            /*枠線なし
            for(int j=0;j<4;j++){
                Edge e = new Edge();
                e.a = q.pos[j];
                e.b = q.pos[(j+1)%4];
                d.edges.add(e);
            }*/
        }
    }

    // CAPSULE
    private static void computeCapsule(ShapeData d) {
        computeCylinder(d); // 中心シリンダー
        int detail = 16;
        float r = d.radius;
        float h = d.height/2f;
        for(int i=0;i<detail;i++){
            double theta0 = Math.PI*i/(2*detail);
            double theta1 = Math.PI*(i+1)/(2*detail);
            for(int sign : new int[]{1,-1}) { // 上下半球
                for(int j=0;j<32;j++){
                    double phi0 = 2*Math.PI*j/32;
                    double phi1 = 2*Math.PI*(j+1)/32;
                    Vec3 base = new Vec3(
                            r*Math.sin(theta0)*Math.cos(phi0),
                            r*Math.cos(theta0),
                            r*Math.sin(theta0)*Math.sin(phi0)
                    );
                    Vec3 next = new Vec3(
                            r*Math.sin(theta1)*Math.cos(phi0),
                            r*Math.cos(theta1),
                            r*Math.sin(theta1)*Math.sin(phi0)
                    );
                    Vec3 p0 = new Vec3(base.x, sign*(base.y+h), base.z);
                    Vec3 p1 = new Vec3(next.x, sign*(next.y+h), next.z);
                    Vec3 p2 = new Vec3(next.x, sign*(next.y+h), next.z);
                    Vec3 p3 = new Vec3(base.x, sign*(base.y+h), base.z);

                    Quad q = new Quad();
                    for(int k=0;k<4;k++){
                        q.pos[0] = d.origin.add(rotateY(p0,d.yaw));
                        q.pos[1] = d.origin.add(rotateY(p1,d.yaw));
                        q.pos[2] = d.origin.add(rotateY(p2,d.yaw));
                        q.pos[3] = d.origin.add(rotateY(p3,d.yaw));
                    }
                    Vec3 u = q.pos[1].subtract(q.pos[0]);
                    Vec3 v = q.pos[3].subtract(q.pos[0]);
                    q.normal = new Vec3(u.y*v.z - u.z*v.y, u.z*v.x - u.x*v.z, u.x*v.y - u.y*v.x);
                    for(int k=0;k<4;k++){
                        q.uv[k*2]=(k==0||k==3)?0f:1f;
                        q.uv[k*2+1]=(k<2)?0f:1f;
                    }
                    d.surfaces.add(q);

                    /*枠線なし
                    for(int k=0;k<4;k++){
                        Edge e = new Edge();
                        e.a=q.pos[k]; e.b=q.pos[(k+1)%4];
                        d.edges.add(e);
                    }*/
                }
            }
        }
    }

    //球体　枠線コメントアウト
    private static void computeSphere(ShapeData d){
        int detail = 16;
        float r = d.radius;
        for(int i=0;i<detail;i++){
            double theta0 = Math.PI*i/detail;
            double theta1 = Math.PI*(i+1)/detail;
            for(int j=0;j<detail;j++){
                double phi0 = 2*Math.PI*j/detail;
                double phi1 = 2*Math.PI*(j+1)/detail;

                Vec3 p0 = new Vec3(r*Math.sin(theta0)*Math.cos(phi0), r*Math.cos(theta0), r*Math.sin(theta0)*Math.sin(phi0));
                Vec3 p1 = new Vec3(r*Math.sin(theta0)*Math.cos(phi1), r*Math.cos(theta0), r*Math.sin(theta0)*Math.sin(phi1));
                Vec3 p2 = new Vec3(r*Math.sin(theta1)*Math.cos(phi1), r*Math.cos(theta1), r*Math.sin(theta1)*Math.sin(phi1));
                Vec3 p3 = new Vec3(r*Math.sin(theta1)*Math.cos(phi0), r*Math.cos(theta1), r*Math.sin(theta1)*Math.sin(phi0));

                Quad q = new Quad();
                q.pos[0]=d.origin.add(rotateY(p0,d.yaw));
                q.pos[1]=d.origin.add(rotateY(p1,d.yaw));
                q.pos[2]=d.origin.add(rotateY(p2,d.yaw));
                q.pos[3]=d.origin.add(rotateY(p3,d.yaw));

                Vec3 u = q.pos[1].subtract(q.pos[0]);
                Vec3 v = q.pos[3].subtract(q.pos[0]);
                q.normal = new Vec3(u.y*v.z - u.z*v.y, u.z*v.x - u.x*v.z, u.x*v.y - u.y*v.x);

                for(int k=0;k<4;k++){
                    q.uv[k*2]=(k==0||k==3)?0f:1f;
                    q.uv[k*2+1]=(k<2)?0f:1f;
                }

                d.surfaces.add(q);

                /*枠線なし
                for(int k=0;k<4;k++){
                    Edge e = new Edge();
                    e.a = q.pos[k];
                    e.b = q.pos[(k+1)%4];
                    d.edges.add(e);
                }*/
            }
        }
    }

    //扇形　枠線コメントアウト
    private static void computeFan3D(ShapeData d){
        int detail = 16;
        float r = d.radius;
        float halfAngle = d.angleDeg/2f;
        Vec3 center = d.origin;
        for(int i=0;i<detail;i++){
            double th0 = Math.toRadians(-halfAngle + d.angleDeg*i/detail);
            double th1 = Math.toRadians(-halfAngle + d.angleDeg*(i+1)/detail);
            Vec3 p0 = new Vec3(0,0,0);
            Vec3 p1 = new Vec3(Math.sin(th0)*r,0,Math.cos(th0)*r);
            Vec3 p2 = new Vec3(Math.sin(th1)*r,0,Math.cos(th1)*r);

            Quad q = new Quad();
            q.pos[0]=center.add(p0);
            q.pos[1]=center.add(p1);
            q.pos[2]=center.add(p2);
            q.pos[3]=center.add(p0); // 四角形化、degenerate なし
            q.normal = new Vec3(0,1,0);
            for(int k=0;k<4;k++){
                q.uv[k*2]=(k==0||k==3)?0f:1f;
                q.uv[k*2+1]=(k<2)?0f:1f;
            }
            d.surfaces.add(q);

            /*枠線なし
            d.edges.add(new Edge(){{
                a = q.pos[0]; b = q.pos[1];
            }});
            d.edges.add(new Edge(){{
                a = q.pos[1]; b = q.pos[2];
            }});
            d.edges.add(new Edge(){{
                a = q.pos[2]; b = q.pos[0];
            }});*/
        }
    }

    //十字形
    private static void computeCross3D(ShapeData d){
        float r = d.radius;
        Vec3[] pts = {new Vec3(-r,0,0), new Vec3(r,0,0), new Vec3(0,-r,0), new Vec3(0,r,0), new Vec3(0,0,-r), new Vec3(0,0,r)};
        for(int i=0;i<pts.length;i+=2){
            Edge e = new Edge();
            e.a=d.origin.add(pts[i]);
            e.b=d.origin.add(pts[i+1]);
            d.edges.add(e);
        }
    }

    //三角柱　
    private static void computeTrianglePrism(ShapeData d){
        float s=d.size;
        Vec3[] bottom = {
                new Vec3(0,0,s),
                new Vec3(s*0.866f,0,-s*0.5f),
                new Vec3(-s*0.866f,0,-s*0.5f)
        };
        Vec3 topOffset = new Vec3(0,s,0);
        for(int i=0;i<3;i++){
            Vec3 b0 = bottom[i];
            Vec3 b1 = bottom[(i+1)%3];
            Vec3 t0 = b0.add(topOffset);
            Vec3 t1 = b1.add(topOffset);

            Quad q = new Quad();
            q.pos[0]=d.origin.add(rotateY(b0,d.yaw));
            q.pos[1]=d.origin.add(rotateY(b1,d.yaw));
            q.pos[2]=d.origin.add(rotateY(t1,d.yaw));
            q.pos[3]=d.origin.add(rotateY(t0,d.yaw));

            Vec3 u = q.pos[1].subtract(q.pos[0]);
            Vec3 v = q.pos[3].subtract(q.pos[0]);
            q.normal = new Vec3(u.y*v.z - u.z*v.y, u.z*v.x - u.x*v.z, u.x*v.y - u.y*v.x);

            for(int k=0;k<4;k++){
                q.uv[k*2]=(k==0||k==3)?0f:1f;
                q.uv[k*2+1]=(k<2)?0f:1f;
            }
            d.surfaces.add(q);

            // エッジ生成
            for(int k=0;k<4;k++){
                Edge e = new Edge();
                e.a = q.pos[k];
                e.b = q.pos[(k+1)%4];
                d.edges.add(e);
            }
        }

        // 底面と上面の三角形もエッジとして追加
        for(int i=0;i<3;i++){
            Edge e1 = new Edge();
            e1.a = d.origin.add(bottom[i]);
            e1.b = d.origin.add(bottom[(i+1)%3]);
            d.edges.add(e1);

            Edge e2 = new Edge();
            e2.a = d.origin.add(bottom[i].add(topOffset));
            e2.b = d.origin.add(bottom[(i+1)%3].add(topOffset));
            d.edges.add(e2);
        }
    }
}
