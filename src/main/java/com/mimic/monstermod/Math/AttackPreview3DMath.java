package com.mimic.monstermod.Math;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class AttackPreview3DMath {
    private AttackPreview3DMath(){}

    public enum ShapeType { BOX, SPHERE, CYLINDER, CAPSULE, FAN3D, CROSS3D, TRIANGLE_PRISM }

    public static class ShapeData {
        public final List<Vec3[]> surfaces;
        public final List<Vec3[]> edges;

        public ShapeData(List<Vec3[]> surfaces, List<Vec3[]> edges){
            this.surfaces = surfaces;
            this.edges = edges;
        }
    }

    // ==================== 統一呼び出し ====================
    public static ShapeData getShapeData(ShapeType shape, Vec3 center,
                                         double w, double h, double d,
                                         double r, double height,
                                         int segs, int heightSegs, int latDiv,
                                         double angle){
        List<Vec3[]> surfaces = new ArrayList<>();
        List<Vec3[]> edges = new ArrayList<>();
        switch(shape){
            case BOX:
                surfaces.addAll(getSurfaceQuadsBox(center,w,h,d));
                edges.addAll(getEdgeSegmentsBox(center,w,h,d));
                break;
            case SPHERE:
                surfaces.addAll(getSurfaceQuadsSphere(center,r,latDiv>0?latDiv:12,segs>0?segs:24));
                edges.addAll(getEdgeSegmentsSphereLight(center,r,segs>0?segs:8));
                break;
            case CYLINDER:
                surfaces.addAll(getSurfaceQuadsCylinder(center,r,height,segs>0?segs:24,heightSegs>0?heightSegs:1));
                edges.addAll(getEdgeSegmentsCylinderLight(center,r,height,segs>0?segs:8));
                break;
            case CAPSULE:
                surfaces.addAll(getSurfaceQuadsCapsule(center,r,height,segs>0?segs:24,latDiv>0?latDiv:8));
                edges.addAll(getEdgeSegmentsCapsuleLight(center,r,height,segs>0?segs:8));
                break;
            case FAN3D:
                surfaces.addAll(getSurfaceQuadsFan3D(center,r,angle>0?angle:Math.PI/2,height,segs>0?segs:12));
                edges.addAll(getEdgeSegmentsFan3D(center,r,angle>0?angle:Math.PI/2,height,segs>0?segs:12));
                break;
            case CROSS3D:
                surfaces.addAll(getSurfaceQuadsCross3D(center,w,h,d));
                edges.addAll(getEdgeSegmentsCross3D(center,w,h,d));
                break;
            case TRIANGLE_PRISM:
                surfaces.addAll(getSurfaceQuadsTrianglePrism(center,w,h,d));
                edges.addAll(getEdgeSegmentsTrianglePrism(center,w,h,d));
                break;
        }
        return new ShapeData(surfaces,edges);
    }

    // ==================== BOX ====================
    public static List<Vec3[]> getSurfaceQuadsBox(Vec3 c,double w,double h,double d){
        double x1=c.x-w/2,x2=c.x+w/2;
        double y1=c.y-h/2,y2=c.y+h/2;
        double z1=c.z-d/2,z2=c.z+d/2;
        List<Vec3[]> q=new ArrayList<>();
        q.add(new Vec3[]{new Vec3(x1,y2,z1),new Vec3(x2,y2,z1),new Vec3(x2,y2,z2),new Vec3(x1,y2,z2)});
        q.add(new Vec3[]{new Vec3(x1,y1,z2),new Vec3(x2,y1,z2),new Vec3(x2,y1,z1),new Vec3(x1,y1,z1)});
        q.add(new Vec3[]{new Vec3(x1,y1,z2),new Vec3(x2,y1,z2),new Vec3(x2,y2,z2),new Vec3(x1,y2,z2)});
        q.add(new Vec3[]{new Vec3(x2,y1,z1),new Vec3(x1,y1,z1),new Vec3(x1,y2,z1),new Vec3(x2,y2,z1)});
        q.add(new Vec3[]{new Vec3(x2,y1,z2),new Vec3(x2,y1,z1),new Vec3(x2,y2,z1),new Vec3(x2,y2,z2)});
        q.add(new Vec3[]{new Vec3(x1,y1,z1),new Vec3(x1,y1,z2),new Vec3(x1,y2,z2),new Vec3(x1,y2,z1)});
        return q;
    }

    public static List<Vec3[]> getEdgeSegmentsBox(Vec3 c,double w,double h,double d){
        double x1=c.x-w/2,x2=c.x+w/2;
        double y1=c.y-h/2,y2=c.y+h/2;
        double z1=c.z-d/2,z2=c.z+d/2;
        Vec3[] v={new Vec3(x1,y1,z1),new Vec3(x2,y1,z1),new Vec3(x2,y1,z2),new Vec3(x1,y1,z2),
                new Vec3(x1,y2,z1),new Vec3(x2,y2,z1),new Vec3(x2,y2,z2),new Vec3(x1,y2,z2)};
        int[][] p={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        List<Vec3[]> e=new ArrayList<>();
        for(int[] pp:p) e.add(new Vec3[]{v[pp[0]],v[pp[1]]});
        return e;
    }

    // ==================== SPHERE ====================
    public static List<Vec3[]> getSurfaceQuadsSphere(Vec3 c,double r,int lat,int lon){
        List<Vec3[]> q=new ArrayList<>();
        for(int i=0;i<lat;i++){
            double lat0=Math.PI*i/lat, lat1=Math.PI*(i+1)/lat;
            for(int j=0;j<lon;j++){
                double lon0=2*Math.PI*j/lon, lon1=2*Math.PI*(j+1)/lon;
                Vec3 p00=sph(c,r,lat0,lon0),p01=sph(c,r,lat0,lon1),p10=sph(c,r,lat1,lon0),p11=sph(c,r,lat1,lon1);
                q.add(new Vec3[]{p00,p01,p11,p10});
            }
        }
        return q;
    }

    public static List<Vec3[]> getEdgeSegmentsSphereLight(Vec3 c,double r,int lon){
        List<Vec3[]> e=new ArrayList<>();
        double y=c.y;
        for(int i=0;i<lon;i++){
            double a0=2*Math.PI*i/lon, a1=2*Math.PI*(i+1)/lon;
            e.add(new Vec3[]{new Vec3(c.x+r*Math.cos(a0),y,c.z+r*Math.sin(a0)),
                    new Vec3(c.x+r*Math.cos(a1),y,c.z+r*Math.sin(a1))});
        }
        for(int i=0;i<4;i++){
            double a=2*Math.PI*i/4;
            e.add(new Vec3[]{new Vec3(c.x+r*Math.cos(a),c.y-r,c.z+r*Math.sin(a)),
                    new Vec3(c.x+r*Math.cos(a),c.y+r,c.z+r*Math.sin(a))});
        }
        return e;
    }

    private static Vec3 sph(Vec3 c,double r,double lat,double lon){
        double x=c.x+r*Math.sin(lat)*Math.cos(lon);
        double y=c.y+r*Math.cos(lat);
        double z=c.z+r*Math.sin(lat)*Math.sin(lon);
        return new Vec3(x,y,z);
    }

    // ==================== CYLINDER ====================
    public static List<Vec3[]> getSurfaceQuadsCylinder(Vec3 c,double r,double h,int seg,int hSeg){
        List<Vec3[]> polys=new ArrayList<>();
        double y0=c.y-h/2,y1=c.y+h/2;
        for(int i=0;i<seg;i++){
            double a0=2*Math.PI*i/seg, a1=2*Math.PI*(i+1)/seg;
            for(int j=0;j<hSeg;j++){
                double t0=(double)j/hSeg,t1=(double)(j+1)/hSeg;
                double yb=y0+t0*h, yt=y0+t1*h;
                Vec3 v00=new Vec3(c.x+r*Math.cos(a0),yb,c.z+r*Math.sin(a0));
                Vec3 v10=new Vec3(c.x+r*Math.cos(a1),yb,c.z+r*Math.sin(a1));
                Vec3 v11=new Vec3(c.x+r*Math.cos(a1),yt,c.z+r*Math.sin(a1));
                Vec3 v01=new Vec3(c.x+r*Math.cos(a0),yt,c.z+r*Math.sin(a0));
                polys.add(new Vec3[]{v00,v10,v11,v01});
            }
        }
        return polys;
    }

    public static List<Vec3[]> getEdgeSegmentsCylinderLight(Vec3 c,double r,double h,int seg){
        List<Vec3[]> e=new ArrayList<>();
        double y0=c.y-h/2,y1=c.y+h/2;
        for(int i=0;i<seg;i++){
            double a0=2*Math.PI*i/seg, a1=2*Math.PI*(i+1)/seg;
            e.add(new Vec3[]{new Vec3(c.x+r*Math.cos(a0),y0,c.z+r*Math.sin(a0)),
                    new Vec3(c.x+r*Math.cos(a1),y0,c.z+r*Math.sin(a1))});
            e.add(new Vec3[]{new Vec3(c.x+r*Math.cos(a0),y1,c.z+r*Math.sin(a0)),
                    new Vec3(c.x+r*Math.cos(a1),y1,c.z+r*Math.sin(a1))});
        }
        for(int i=0;i<4;i++){
            double a=2*Math.PI*i/4;
            e.add(new Vec3[]{new Vec3(c.x+r*Math.cos(a),y0,c.z+r*Math.sin(a)),
                    new Vec3(c.x+r*Math.cos(a),y1,c.z+r*Math.sin(a))});
        }
        return e;
    }

    // ==================== CAPSULE ====================
    public static List<Vec3[]> getSurfaceQuadsCapsule(Vec3 c,double r,double h,int seg,int lat){
        List<Vec3[]> polys=new ArrayList<>();
        double cylH=h-2*r; if(cylH<0)cylH=0;
        if(cylH>0) polys.addAll(getSurfaceQuadsCylinder(new Vec3(c.x,c.y, c.z),r,cylH,seg,1));
        Vec3 bottom=new Vec3(c.x,c.y-cylH/2-r,c.z), top=new Vec3(c.x,c.y+cylH/2+r,c.z);
        for(int i=0;i<lat/2;i++){
            double lat0=Math.PI*i/lat, lat1=Math.PI*(i+1)/lat;
            for(int j=0;j<seg;j++){
                double lon0=2*Math.PI*j/seg, lon1=2*Math.PI*(j+1)/seg;
                polys.add(new Vec3[]{sph(bottom,r,lat0,lon0),sph(bottom,r,lat0,lon1),
                        sph(bottom,r,lat1,lon1),sph(bottom,r,lat1,lon0)});
                polys.add(new Vec3[]{sph(top,r,Math.PI-lat0,lon0),sph(top,r,Math.PI-lat0,lon1),
                        sph(top,r,Math.PI-lat1,lon1),sph(top,r,Math.PI-lat1,lon0)});
            }
        }
        return polys;
    }

    public static List<Vec3[]> getEdgeSegmentsCapsuleLight(Vec3 c,double r,double h,int seg){
        List<Vec3[]> e=new ArrayList<>();
        double cylH=h-2*r; if(cylH<0)cylH=0;
        e.addAll(getEdgeSegmentsCylinderLight(c,r,cylH,seg));
        double y0=c.y-cylH/2, y1=c.y+cylH/2;
        for(int i=0;i<4;i++){
            double a=2*Math.PI*i/4;
            e.add(new Vec3[]{new Vec3(c.x+r*Math.cos(a),y0-r,c.z+r*Math.sin(a)),
                    new Vec3(c.x+r*Math.cos(a),y1+r,c.z+r*Math.sin(a))});
        }
        return e;
    }

    // ==================== FAN3D ====================
    public static List<Vec3[]> getSurfaceQuadsFan3D(Vec3 c,double r,double angle,double h,int seg){
        List<Vec3[]> polys=new ArrayList<>();
        double y0=c.y, y1=c.y+h;
        double aStart=-angle/2, aStep=angle/seg;
        for(int i=0;i<seg;i++){
            double a0=aStart+i*aStep, a1=aStart+(i+1)*aStep;
            Vec3 p0=new Vec3(c.x+r*Math.cos(a0),y0,c.z+r*Math.sin(a0));
            Vec3 p1=new Vec3(c.x+r*Math.cos(a1),y0,c.z+r*Math.sin(a1));
            Vec3 p2=new Vec3(c.x+r*Math.cos(a1),y1,c.z+r*Math.sin(a1));
            Vec3 p3=new Vec3(c.x+r*Math.cos(a0),y1,c.z+r*Math.sin(a0));
            polys.add(new Vec3[]{p0,p1,p2,p3});
        }
        return polys;
    }

    public static List<Vec3[]> getEdgeSegmentsFan3D(Vec3 c,double r,double angle,double h,int seg){
        List<Vec3[]> e=new ArrayList<>();
        double y0=c.y,y1=c.y+h;
        double aStart=-angle/2,aStep=angle/seg;
        for(int i=0;i<=seg;i++){
            double a=aStart+i*aStep;
            Vec3 pb=new Vec3(c.x+r*Math.cos(a),y0,c.z+r*Math.sin(a));
            Vec3 pt=new Vec3(c.x+r*Math.cos(a),y1,c.z+r*Math.sin(a));
            e.add(new Vec3[]{pb,pt});
            if(i<seg){
                double a1=aStart+(i+1)*aStep;
                Vec3 pb1=new Vec3(c.x+r*Math.cos(a1),y0,c.z+r*Math.sin(a1));
                Vec3 pt1=new Vec3(c.x+r*Math.cos(a1),y1,c.z+r*Math.sin(a1));
                e.add(new Vec3[]{pb,pb1});
                e.add(new Vec3[]{pt,pt1});
            }
        }
        return e;
    }

    // ==================== CROSS3D ====================
    public static List<Vec3[]> getSurfaceQuadsCross3D(Vec3 c,double w,double h,double d){
        List<Vec3[]> polys=new ArrayList<>();
        polys.addAll(getSurfaceQuadsBox(c,w,h/5,d/5));
        polys.addAll(getSurfaceQuadsBox(c,w/5,h,d/5));
        polys.addAll(getSurfaceQuadsBox(c,w/5,h/5,d));
        return polys;
    }

    public static List<Vec3[]> getEdgeSegmentsCross3D(Vec3 c,double w,double h,double d){
        List<Vec3[]> edges=new ArrayList<>();
        edges.addAll(getEdgeSegmentsBox(c,w,h/5,d/5));
        edges.addAll(getEdgeSegmentsBox(c,w/5,h,d/5));
        edges.addAll(getEdgeSegmentsBox(c,w/5,h/5,d));
        return edges;
    }

    // ==================== TRIANGLE PRISM ====================
    public static List<Vec3[]> getSurfaceQuadsTrianglePrism(Vec3 c,double b,double h,double d){
        List<Vec3[]> polys=new ArrayList<>();
        double x=c.x, y=c.y, z=c.z;
        Vec3 p0=new Vec3(x-b/2,y,z-d/2),p1=new Vec3(x+b/2,y,z-d/2),p2=new Vec3(x,y+h,z-d/2),
                p3=new Vec3(x-b/2,y,z+d/2),p4=new Vec3(x+b/2,y,z+d/2),p5=new Vec3(x,y+h,z+d/2);
        polys.add(new Vec3[]{p0,p1,p4,p3});
        polys.add(new Vec3[]{p0,p2,p5,p3});
        polys.add(new Vec3[]{p1,p2,p5,p4});
        polys.add(new Vec3[]{p0,p1,p2});
        polys.add(new Vec3[]{p3,p4,p5});
        return polys;
    }

    public static List<Vec3[]> getEdgeSegmentsTrianglePrism(Vec3 c,double b,double h,double d){
        List<Vec3[]> edges=new ArrayList<>();
        double x=c.x, y=c.y, z=c.z;
        Vec3 p0=new Vec3(x-b/2,y,z-d/2),p1=new Vec3(x+b/2,y,z-d/2),p2=new Vec3(x,y+h,z-d/2),
                p3=new Vec3(x-b/2,y,z+d/2),p4=new Vec3(x+b/2,y,z+d/2),p5=new Vec3(x,y+h,z+d/2);
        edges.add(new Vec3[]{p0,p1});edges.add(new Vec3[]{p1,p2});edges.add(new Vec3[]{p2,p0});
        edges.add(new Vec3[]{p3,p4});edges.add(new Vec3[]{p4,p5});edges.add(new Vec3[]{p5,p3});
        edges.add(new Vec3[]{p0,p3});edges.add(new Vec3[]{p1,p4});edges.add(new Vec3[]{p2,p5});
        return edges;
    }
}
