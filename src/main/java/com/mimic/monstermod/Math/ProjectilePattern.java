package com.mimic.monstermod.Math;

import net.minecraft.world.phys.Vec3;

/**
 * 放射状/螺旋状に飛ぶ弾幕パターンの「唯一の真実」。
 *
 * 【目的】
 * 従来は「実際に飛ぶ弾(Entity)の速度・弾数・回転」と「プレビュー(予兆円)の見た目」を
 * それぞれ別の場所に手動でハードコーディングしており、数値が食い違う原因になっていた
 * (例: OnibiSkillの実射程と予兆円の長さが一致していなかった)。
 * このクラスを1箇所だけ定義し、実際の弾の発射ロジックとプレビュー描画の両方から
 * 参照することで、常に一致させる。
 *
 * rotationSpeedRad = 0 なら直進(Radial)、0以外なら螺旋(Spiral)として同じ式で表現できる。
 */
public record ProjectilePattern(int count, double speed, double rotationSpeedRad, int lifeTicks) {

    /** index番目の弾の発射角度(ラジアン、count方向に均等分割) */
    public double startAngle(int index) {
        return index * (Math.PI * 2 / count);
    }

    /** 直進弾(Radial)用の初速ベクトル */
    public Vec3 velocityAt(int index) {
        double a = startAngle(index);
        return new Vec3(Math.cos(a), 0, Math.sin(a)).scale(speed);
    }

    /**
     * 発射からtick後の、中心からの相対座標。
     * rotationSpeedRad=0なら直進、0以外なら螺旋になる共通の式。
     * (SpiralOnibiEntity.tick()の angle += rotationSpeed; radius += speed; という
     *  毎tick累積計算と数学的に同値の閉形式)
     */
    public Vec3 offsetAtTick(int index, int tick) {
        double angle = startAngle(index) + rotationSpeedRad * tick;
        double radius = speed * tick;
        return new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    /** 弾が寿命までに到達しうる最大射程。プレビュー円の半径の自動算出に使う */
    public double maxReach() {
        return speed * lifeTicks;
    }
}
