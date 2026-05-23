package com.mimic.monstermod.skill;

import com.mimic.monstermod.Math.MathMain;
import net.minecraft.world.phys.Vec3;

/**
 * SkillLeadUtil
 *
 * 【役割】
 * ・SkillLead を Math / Attack 実行へ変換する唯一のブリッジ
 *
 * 【重要原則】
 * ・MathMain が唯一の真理
 * ・Preview と Attack を絶対に混ぜない
 * ・Marker 概念は存在しない（Renderer 直 Math）
 */
public final class SkillLeadUtil {

    private SkillLeadUtil() {}

    /* =============================================================
     * MathMain 生成
     * ============================================================= */
    public static MathMain buildMath(SkillLead lead, Vec3 origin) {

        MathMain.Builder b = new MathMain.Builder()
                .origin(origin)
                .rotation(
                        lead.transform.yaw,
                        lead.transform.pitch,
                        lead.transform.roll
                )
                .shape(lead.shape);

        switch (lead.shape) {
            case SPHERE -> b.radius(lead.radius);
            case CYLINDER, RADIAL, SPIRAL -> b.radius(lead.radius).height(lead.height);
            case FAN -> b.fan(lead.radius, lead.angleDeg).height(lead.height);
            case RECT_PRISM, BOX -> b.rect(lead.xRadius, lead.zRadius).height(lead.height);
            case TRI_PRISM -> b.triangle(lead.baseHalf, lead.depth).height(lead.height);
            default -> {}
        }

        return b.build();
    }
}
