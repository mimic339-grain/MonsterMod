package com.mimic.monstermod.mixin.accessor;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface RenderStateShardAccessor {
    // 難読化を避けるため、MCP名や実際のフィールド名を確認してください。
    // 通常、これらの定数は RenderStateShard クラス内で以下のように定義されています。

    @Accessor("RENDERTYPE_ENTITY_TRANSLUCENT_SHADER")
    static RenderStateShard.ShaderStateShard getEntityTranslucentShader() { throw new AssertionError(); }

    @Accessor("TRANSLUCENT_TRANSPARENCY")
    static RenderStateShard.TransparencyStateShard getTranslucentTransparency() { throw new AssertionError(); }

    @Accessor("NO_CULL")
    static RenderStateShard.CullStateShard getNoCull() { throw new AssertionError(); }

    @Accessor("LIGHTMAP")
    static RenderStateShard.LightmapStateShard getLightmap() { throw new AssertionError(); }
}