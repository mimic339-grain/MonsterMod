package net.mimic.monstermod.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MonsterKeyBindings {
    public static final String KEY_CATEGORY_MONSTERMOD = "key.category." + MonsterMod.MOD_ID + ".monstermod";
    public static final String KEY_TRANSFORM = "key." + MonsterMod.MOD_ID + ".transform";

    public static KeyMapping TRANSFORM_KEY;

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        TRANSFORM_KEY = new KeyMapping(KEY_TRANSFORM, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_MONSTERMOD); // 例: 'G'キー
        event.register(TRANSFORM_KEY);
    }
}