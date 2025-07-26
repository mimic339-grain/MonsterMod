package net.mimic.monstermod.config;

import net.minecraftforge.common.ForgeConfigSpec; // ForgeConfigSpec をインポート

public class ModConfig {
    public static final ForgeConfigSpec SPEC; // ForgeConfigSpec を定義

    // 設定値の例
    public static final ForgeConfigSpec.BooleanValue ENABLE_MIMIC_TRANSFORMATION;
    public static final ForgeConfigSpec.IntValue MIMIC_TRANSFORM_DURATION;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        // 設定項目の定義
        ENABLE_MIMIC_TRANSFORMATION = BUILDER
                .comment("Whether mimic transformation is enabled.")
                .define("enableMimicTransformation", true);

        MIMIC_TRANSFORM_DURATION = BUILDER
                .comment("Duration of mimic transformation in ticks.")
                .defineInRange("mimicTransformDuration", 600, 20, 12000); // 例: 600 ticks (30秒)

        SPEC = BUILDER.build();
    }
}