package com.mimic.monstermod.Math;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class AttackPreview2DBlockMath {
    private AttackPreview2DBlockMath(){}

    /** 形状列挙 */
    public enum ShapeType { CIRCLEBlock2D, FANBlock2D, TRIANGLEBlock2D, RECTBlock2D, CROSSBlock2D, RANDOMBlock2D }

    /** 座標＋枠線を格納するデータ */
    public static class Area2DBlock {
        public final List<Vec3> blocks;    // 全ブロック
        public final Set<String> outline;  // 枠線ブロック key = "x:z"

        public Area2DBlock(List<Vec3> blocks, Set<String> outline){
            this.blocks = blocks;
            this.outline = outline;
        }
    }

    /**
     * ブロック単位範囲生成（枠線も自動算出、ワールドY補正対応）
     * @param level ワールド
     */
    public static Area2DBlock generateAreaWithOutline(Level level, ShapeType shape, Vec3 center,
                                                      double radius, double xRadius, double zRadius,
                                                      double base, Direction dir,
                                                      double minYDiff, double maxYDiff,
                                                      int randomCount) {
        List<Vec3> blocks = new ArrayList<>();
        switch(shape){
            case CIRCLEBlock2D -> blocks.addAll(generateCircle(level, center, radius, minYDiff, maxYDiff));
            case FANBlock2D -> blocks.addAll(generateFan(level, center, radius, 90, dir, minYDiff, maxYDiff));
            case TRIANGLEBlock2D -> blocks.addAll(generateTriangle(level, center, base, dir, minYDiff, maxYDiff));
            case RECTBlock2D -> blocks.addAll(generateRect(level, center, xRadius, zRadius, minYDiff, maxYDiff));
            case CROSSBlock2D -> blocks.addAll(generateCross(level, center, base, minYDiff, maxYDiff));
            case RANDOMBlock2D -> blocks.addAll(generateRandom(level, center, radius, minYDiff, maxYDiff, randomCount));
        }

        Set<String> outline = shape != ShapeType.RANDOMBlock2D ? computeOutline(blocks) : Collections.emptySet();
        return new Area2DBlock(blocks, outline);
    }

    // ==================== 形状生成 ====================
    private static List<Vec3> generateCircle(Level level, Vec3 c, double radius, double minYDiff, double maxYDiff){
        List<Vec3> pos = new ArrayList<>();
        int rInt = (int)Math.ceil(radius);
        for(int dx=-rInt; dx<=rInt; dx++){
            for(int dz=-rInt; dz<=rInt; dz++){
                if(dx*dx + dz*dz <= radius*radius){
                    Vec3 blockPos = getTopBlock(level, c.x+dx, c.z+dz, c.y + minYDiff, c.y + maxYDiff);
                    if(blockPos != null) pos.add(blockPos);
                }
            }
        }
        return pos;
    }

    private static List<Vec3> generateFan(Level level, Vec3 c, double radius, double angleDeg, Direction dir, double minYDiff, double maxYDiff){
        List<Vec3> pos = new ArrayList<>();
        int rInt = (int)Math.ceil(radius);
        double dirRad = switch(dir){
            case NORTH -> Math.toRadians(180);
            case SOUTH -> Math.toRadians(0);
            case WEST -> Math.toRadians(90);
            case EAST -> Math.toRadians(270);
            default -> 0;
        };
        double halfAngle = Math.toRadians(angleDeg/2);
        for(int dx=-rInt; dx<=rInt; dx++){
            for(int dz=-rInt; dz<=rInt; dz++){
                double dist = Math.sqrt(dx*dx + dz*dz);
                if(dist <= radius){
                    double angle = Math.atan2(dz, dx);
                    double relative = normalizeAngle(angle - dirRad);
                    if(relative >= -halfAngle && relative <= halfAngle){
                        Vec3 blockPos = getTopBlock(level, c.x+dx, c.z+dz, c.y + minYDiff, c.y + maxYDiff);
                        if(blockPos != null) pos.add(blockPos);
                    }
                }
            }
        }
        return pos;
    }

    private static List<Vec3> generateTriangle(Level level, Vec3 c, double base, Direction dir, double minYDiff, double maxYDiff){
        List<Vec3> pos = new ArrayList<>();
        int h = (int)Math.ceil(base);
        for(int dz=0; dz<h; dz++){
            int rowWidth = (int)Math.ceil((1.0 - dz/(double)h) * base);
            for(int dx=-rowWidth/2; dx<=rowWidth/2; dx++){
                double x = 0, z = 0;
                switch(dir){
                    case NORTH -> { x = c.x+dx; z = c.z-dz; }
                    case SOUTH -> { x = c.x+dx; z = c.z+dz; }
                    case WEST  -> { x = c.x-dz; z = c.z+dx; }
                    case EAST  -> { x = c.x+dz; z = c.z+dx; }
                }
                Vec3 blockPos = getTopBlock(level, x, z, c.y + minYDiff, c.y + maxYDiff);
                if(blockPos != null) pos.add(blockPos);
            }
        }
        return pos;
    }

    private static List<Vec3> generateRect(Level level, Vec3 c, double xRadius, double zRadius, double minYDiff, double maxYDiff){
        List<Vec3> pos = new ArrayList<>();
        int xr = (int)Math.ceil(xRadius);
        int zr = (int)Math.ceil(zRadius);
        for(int dx=-xr; dx<=xr; dx++){
            for(int dz=-zr; dz<=zr; dz++){
                Vec3 blockPos = getTopBlock(level, c.x+dx, c.z+dz, c.y + minYDiff, c.y + maxYDiff);
                if(blockPos != null) pos.add(blockPos);
            }
        }
        return pos;
    }

    private static List<Vec3> generateCross(Level level, Vec3 c, double size, double minYDiff, double maxYDiff){
        List<Vec3> pos = new ArrayList<>();
        int s = (int)Math.ceil(size);
        int w = Math.max(1, s/5);

        // 横棒
        for(int dx=-s/2; dx<=s/2; dx++){
            for(int dz=-w/2; dz<=w/2; dz++){
                Vec3 blockPos = getTopBlock(level, c.x+dx, c.z+dz, c.y + minYDiff, c.y + maxYDiff);
                if(blockPos != null) pos.add(blockPos);
            }
        }
        // 縦棒
        for(int dx=-w/2; dx<=w/2; dx++){
            for(int dz=-s/2; dz<=s/2; dz++){
                Vec3 blockPos = getTopBlock(level, c.x+dx, c.z+dz, c.y + minYDiff, c.y + maxYDiff);
                if(blockPos != null) pos.add(blockPos);
            }
        }
        return pos;
    }

    private static List<Vec3> generateRandom(Level level, Vec3 c, double radius, double minYDiff, double maxYDiff, int count){
        List<Vec3> pos = new ArrayList<>();
        Random rand = new Random();
        for(int i=0;i<count;i++){
            double dx = (rand.nextDouble()*2-1)*radius;
            double dz = (rand.nextDouble()*2-1)*radius;
            Vec3 blockPos = getTopBlock(level, c.x+dx, c.z+dz, c.y + minYDiff, c.y + maxYDiff);
            if(blockPos != null) pos.add(blockPos);
        }
        return pos;
    }

    // ==================== 共通処理 ====================
    private static double normalizeAngle(double angle){
        while(angle < -Math.PI) angle += 2*Math.PI;
        while(angle > Math.PI) angle -= 2*Math.PI;
        return angle;
    }

    /** 指定範囲の最上ブロックを取得、無ければnull返却 */
    private static Vec3 getTopBlock(Level level, double x, double z, double minY, double maxY){
        int minYi = (int)Math.floor(minY);
        int maxYi = (int)Math.ceil(maxY);
        for(int y=maxYi; y>=minYi; y--){
            BlockPos pos = new BlockPos((int)Math.floor(x), y, (int)Math.floor(z));
            if(!level.isEmptyBlock(pos)){
                return new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
            }
        }
        return null;
    }

    // ==================== 枠線抽出 ====================
    private static Set<String> computeOutline(List<Vec3> blocks){
        Set<String> blockSet = new HashSet<>();
        for(Vec3 p : blocks){
            blockSet.add(toKey(p));
        }

        Set<String> outline = new HashSet<>();
        int[] dx = {-1, 1, 0, 0};
        int[] dz = {0, 0, -1, 1};

        for(Vec3 p : blocks){
            boolean isEdge = false;
            for(int i=0; i<4; i++){
                String neighbor = ((int)(p.x+dx[i])) + ":" + ((int)(p.z+dz[i]));
                if(!blockSet.contains(neighbor)){
                    isEdge = true;
                    break;
                }
            }
            if(isEdge) outline.add(toKey(p));
        }
        return outline;
    }

    private static String toKey(Vec3 p){
        return (int)p.x + ":" + (int)p.z;
    }
}
