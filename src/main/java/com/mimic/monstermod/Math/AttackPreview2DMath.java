package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class AttackPreview2DMath {
    private AttackPreview2DMath(){}

    public enum ShapeType { CIRCLE2D, FAN2D, TRIANGLE2D, RECT2D, CROSS2D, RANDOM2D }

    /** 描画用パス＋Y補正取得 */
    public static List<Vec3[]> getShapePoints(Level level, ShapeType shape, Vec3 center,
                                              double radius, double base, double angleDeg, int segments){
        List<Vec3[]> points = new ArrayList<>();
        switch(shape){
            case CIRCLE2D -> points.addAll(getCircle(level, center, radius, segments));
            case FAN2D -> points.addAll(getFan(level, center, radius, angleDeg, segments));
            case TRIANGLE2D -> points.addAll(getTriangle(level, center, base, Direction.NORTH));
            case RECT2D -> points.addAll(getRect(level, center, base, base));
            case CROSS2D -> points.addAll(getCross(level, center, base));
            case RANDOM2D -> points.addAll(getRandom(level, center, radius, segments));
        }
        return points;
    }

    // ==================== 形状生成 ====================
    private static List<Vec3[]> getCircle(Level level, Vec3 c, double r, int seg){
        List<Vec3[]> q = new ArrayList<>();
        for(int i=0;i<seg;i++){
            double a0 = 2*Math.PI*i/seg, a1 = 2*Math.PI*(i+1)/seg;
            Vec3 p0 = getTopBlock(level, c.x + r*Math.cos(a0), c.z + r*Math.sin(a0), c.y);
            Vec3 p1 = getTopBlock(level, c.x + r*Math.cos(a1), c.z + r*Math.sin(a1), c.y);
            Vec3 pc = getTopBlock(level, c.x, c.z, c.y);
            if(p0!=null && p1!=null && pc!=null) q.add(new Vec3[]{p0,p1,pc});
        }
        return q;
    }

    private static List<Vec3[]> getFan(Level level, Vec3 c, double r, double angleDeg, int seg){
        List<Vec3[]> q = new ArrayList<>();
        double aStart = Math.toRadians(-angleDeg/2);
        double aStep = Math.toRadians(angleDeg/seg);
        for(int i=0;i<seg;i++){
            double a0 = aStart + i*aStep, a1 = aStart + (i+1)*aStep;
            Vec3 p0 = getTopBlock(level, c.x + r*Math.cos(a0), c.z + r*Math.sin(a0), c.y);
            Vec3 p1 = getTopBlock(level, c.x + r*Math.cos(a1), c.z + r*Math.sin(a1), c.y);
            Vec3 pc = getTopBlock(level, c.x, c.z, c.y);
            if(p0!=null && p1!=null && pc!=null) q.add(new Vec3[]{pc,p0,p1});
        }
        return q;
    }

    private static List<Vec3[]> getTriangle(Level level, Vec3 c, double base, Direction dir){
        List<Vec3[]> q = new ArrayList<>();
        Vec3 p0,p1,p2;
        switch(dir){
            case NORTH -> {
                p0 = getTopBlock(level,c.x-base/2,c.z-base/2,c.y);
                p1 = getTopBlock(level,c.x+base/2,c.z-base/2,c.y);
                p2 = getTopBlock(level,c.x,c.z+base/2,c.y);
            }
            case SOUTH -> {
                p0 = getTopBlock(level,c.x-base/2,c.z+base/2,c.y);
                p1 = getTopBlock(level,c.x+base/2,c.z+base/2,c.y);
                p2 = getTopBlock(level,c.x,c.z-base/2,c.y);
            }
            case WEST -> {
                p0 = getTopBlock(level,c.x-base/2,c.z+base/2,c.y);
                p1 = getTopBlock(level,c.x-base/2,c.z-base/2,c.y);
                p2 = getTopBlock(level,c.x+base/2,c.z,c.y);
            }
            case EAST -> {
                p0 = getTopBlock(level,c.x+base/2,c.z+base/2,c.y);
                p1 = getTopBlock(level,c.x+base/2,c.z-base/2,c.y);
                p2 = getTopBlock(level,c.x-base/2,c.z,c.y);
            }
            default -> {
                p0 = getTopBlock(level,c.x-base/2,c.z-base/2,c.y);
                p1 = getTopBlock(level,c.x+base/2,c.z-base/2,c.y);
                p2 = getTopBlock(level,c.x,c.z+base/2,c.y);
            }
        }
        if(p0!=null && p1!=null && p2!=null) q.add(new Vec3[]{p0,p1,p2});
        return q;
    }

    private static List<Vec3[]> getRect(Level level, Vec3 c, double w, double h){
        List<Vec3[]> q = new ArrayList<>();
        Vec3 p0 = getTopBlock(level,c.x-w/2,c.z-h/2,c.y);
        Vec3 p1 = getTopBlock(level,c.x+w/2,c.z-h/2,c.y);
        Vec3 p2 = getTopBlock(level,c.x+w/2,c.z+h/2,c.y);
        Vec3 p3 = getTopBlock(level,c.x-w/2,c.z+h/2,c.y);
        if(p0!=null && p1!=null && p2!=null && p3!=null) q.add(new Vec3[]{p0,p1,p2,p3});
        return q;
    }

    private static List<Vec3[]> getCross(Level level, Vec3 c, double size){
        List<Vec3[]> q = new ArrayList<>();
        double w = size/5;
        q.addAll(getRect(level,c,size,w));
        q.addAll(getRect(level,c,w,size));
        return q;
    }

    private static List<Vec3[]> getRandom(Level level, Vec3 c, double radius, int count){
        List<Vec3[]> q = new ArrayList<>();
        Random rand = new Random();
        for(int i=0;i<count;i++){
            double x = c.x + (rand.nextDouble()*2-1)*radius;
            double z = c.z + (rand.nextDouble()*2-1)*radius;
            Vec3 p = getTopBlock(level,x,z,c.y);
            if(p!=null) q.add(new Vec3[]{p});
        }
        return q;
    }

    // ==================== Y補正 ====================
    private static Vec3 getTopBlock(Level level, double x, double z, double centerY){
        int minY = (int)Math.floor(centerY-10);
        int maxY = (int)Math.ceil(centerY+10);
        for(int y=maxY;y>=minY;y--){
            BlockPos pos = new BlockPos((int)Math.floor(x),y,(int)Math.floor(z));
            if(!level.isEmptyBlock(pos)){
                return new Vec3(pos.getX()+0.5,pos.getY()+0.1,pos.getZ()+0.5);
            }
        }
        return null;
    }
}
