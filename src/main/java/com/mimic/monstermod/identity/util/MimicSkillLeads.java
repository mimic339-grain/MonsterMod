package com.mimic.monstermod.identity.util;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.skill.SkillType;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import net.minecraft.resources.ResourceLocation;

public final class MimicSkillLeads {

    public static final String MODID = "monstermod";
    public static final SkillId TEST_2D = new SkillId(new ResourceLocation(MODID, "test_2d"));
    public static final SkillId TEST_BLOCK = new SkillId(new ResourceLocation(MODID, "test_block"));
    public static final SkillId TEST_3D = new SkillId(new ResourceLocation(MODID, "test_3d"));
    public static final SkillId TEST_EMERGENCY = new SkillId(new ResourceLocation(MODID, "test_emergency"));

    private MimicSkillLeads() {}
    public static void registerAll() {
        register2D();
        registerBlock();
        register3D();
        registerEmergency();
    }

    private static void register2D() {
        SkillLead lead = new SkillLead.Builder(TEST_2D)
                .shape(MathMain.Shape.CYLINDER)
                .category(SkillType.Category.NORMAL)
                .cylinder(8.0f, 2.0f)
                .followCaster(true)
                .attackType(SkillType.STRIKE)
                .render2D()
                .build();
        SkillLeadRegistry.register(lead);
    }

    private static void registerBlock() {
        SkillLead lead = new SkillLead.Builder(TEST_BLOCK)
                .shape(MathMain.Shape.SPHERE)
                .category(SkillType.Category.NORMAL)
                .sphere(8.0f)
                .followCaster(true)
                .attackType(SkillType.STRIKE)
                .renderBlock2D()
                .build();
        SkillLeadRegistry.register(lead);
    }

    private static void register3D() {
        SkillLead lead = new SkillLead.Builder(TEST_3D)
                .shape(MathMain.Shape.SPHERE)
                .category(SkillType.Category.COMBO)
                .sphere(10.0f)
                .followCaster(true)
                .attackType(SkillType.STRIKE)
                .render3DPreview()
                .build();
        SkillLeadRegistry.register(lead);
    }
    private static void registerEmergency() {
        SkillLead lead = new SkillLead.Builder(TEST_EMERGENCY)
                .category(SkillType.Category.CANCEL)
                .canBeCanceled(true)
                .attackType(SkillType.MOVEMENT)
                .build();
        SkillLeadRegistry.register(lead);
    }
}