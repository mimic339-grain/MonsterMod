package com.mimic.monstermod.identity.util;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.skill.AttackType;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import net.minecraft.resources.ResourceLocation;

public final class MimicSkillLeads {

    public static final String MODID = "monstermod";

    /* ======================
     * SkillId
     * ====================== */

    public static final SkillId TEST_2D =
            new SkillId(new ResourceLocation(MODID, "test_2d"));

    public static final SkillId TEST_BLOCK =
            new SkillId(new ResourceLocation(MODID, "test_block"));

    public static final SkillId TEST_OVERLAY =
            new SkillId(new ResourceLocation(MODID, "test_overlay"));

    public static final SkillId TEST_3D =
            new SkillId(new ResourceLocation(MODID, "test_3d"));

    private MimicSkillLeads() {}

    /* ======================
     * Register
     * ====================== */

    public static void registerAll() {

        register2D();
        registerBlock();
        register3D();
    }

    /* ======================
     * 2D Preview
     * ====================== */

    private static void register2D() {
        SkillLead lead = new SkillLead.Builder(TEST_2D)
                .shape(MathMain.Shape.CYLINDER)
                .cylinder(8.0f, 2.0f)
                .followCaster(true)
                .attackType(AttackType.ENTITY_AOE)
                .render2D()
                .build();
        SkillLeadRegistry.register(lead);
    }

    /* ======================
     * Block Preview
     * ====================== */
    private static void registerBlock() {
        SkillLead lead = new SkillLead.Builder(TEST_BLOCK)
                .shape(MathMain.Shape.SPHERE)
                .sphere(8.0f)
                .followCaster(true)
                .attackType(AttackType.NONE)
                .renderBlock2D() // Mesh→Block描画
                .build();
        SkillLeadRegistry.register(lead);
    }

    /* ======================
     * 3D Preview
     * ====================== */

    private static void register3D() {

        SkillLead lead =
                new SkillLead.Builder(TEST_3D)
                        .shape(MathMain.Shape.SPHERE)
                        .sphere(10.0f)
                        .followCaster(true)
                        .attackType(AttackType.NONE)
                        .render3DPreview()
                        .build();

        SkillLeadRegistry.register(lead);
    }
}